package com.tamullen.jobsboard.core

import cats.*
import cats.effect.*
import cats.implicits.*

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.*

import org.typelevel.log4cats.Logger

import java.util.UUID

import com.tamullen.jobsboard.domain.Job.*
import com.tamullen.jobsboard.domain.pagination.*
import com.tamullen.jobsboard.logging.Syntax._

trait Jobs[F[_]] {
  // "Algebra"
  // CRUD
  def create(ownerEmail: String, jobInfo: JobInfo): F[UUID]
  def all(): F[List[Job]] // TODO: Fix thoughts on the all() method
  def all(filter: JobFilter, pagination: Pagination): F[List[Job]]
  def find(id: UUID): F[Option[Job]]
  def update(id: UUID, jobInfo: JobInfo): F[Option[Job]]
  def delete(id: UUID): F[Int]
  def possibleFilters(): F[JobFilter]
}
// Might need to come back to this video, added code for Read, but said it wasn't necessary
/*
      id: UUID,
      date: Long,
      ownerEmail: String,
      jobInfo: JobInfo,
      active: Boolean = false

      company: String,
      title: String,
      description: String,
      externalUrl: String,
      remote: Boolean = false,
      location: String,
      salaryLo: Option[Int],
      salaryHi: Option[Int],
      currency: Option[String],
      country: Option[String],
      tags: Option[List[String]],
      image: Option[String],
      seniority: Option[String],
      other: Option[String]
 */
class LiveJobs[F[_]: MonadCancelThrow: Logger] private (xa: Transactor[F]) extends Jobs[F] {
  override def create(ownerEmail: String, jobInfo: JobInfo): F[UUID] =
    sql"""
         INSERT INTO jobs(
            date,
            ownerEmail,
            company,
            title,
            description,
            externalUrl,
            remote,
            location,
            salaryLo,
            salaryHi,
            currency,
            country,
            tags,
            image,
            seniority,
            other,
            active
          ) VALUES (
            ${System.currentTimeMillis()},
            ${ownerEmail},
            ${jobInfo.company},
            ${jobInfo.title},
            ${jobInfo.description},
            ${jobInfo.externalUrl},
            ${jobInfo.remote},
            ${jobInfo.location},
            ${jobInfo.salaryLo},
            ${jobInfo.salaryHi},
            ${jobInfo.currency},
            ${jobInfo.country},
            ${jobInfo.tags},
            ${jobInfo.image},
            ${jobInfo.seniority},
            ${jobInfo.other},
            false
          )
       """.update
      .withUniqueGeneratedKeys[UUID]("id")
      .transact(xa)

  override def all(): F[List[Job]] =
    sql"""
         SELECT
            id,
            date,
            ownerEmail,
            company,
            title,
            description,
            externalUrl,
            remote,
            location,
            salaryLo,
            salaryHi,
            currency,
            country,
            tags,
            image,
            seniority,
            other,
            active
        FROM jobs
       """
      .query[Job]
      .to[List]
      .transact(xa)

  override def all(filter: JobFilter, pagination: Pagination): F[List[Job]] = {
    val selectFragment: Fragment =
      fr"""
          SELECT
              id,
              date,
              ownerEmail,
              company,
              title,
              description,
              externalUrl,
              remote,
              location,
              salaryLo,
              salaryHi,
              currency,
              country,
              tags,
              image,
              seniority,
              other,
              active
        """
    val fromFragment: Fragment =
      fr"""
          FROM jobs
        """
    val whereFragment: Fragment = Fragments.whereAndOpt(
      filter.companies.toNel.map(companies =>
        Fragments.in(fr"company", companies)
      ), // Option[NonEmptyList] => Option[Fragment] => Option["Where company in companies"]
      filter.locations.toNel.map(locations => Fragments.in(fr"location", locations)),
      filter.countries.toNel.map(countries => Fragments.in(fr"country", countries)),
      filter.seniorities.toNel.map(seniorities => Fragments.in(fr"seniority", seniorities)),
      filter.tags.toNel.map(tags => // intersection between filter.tags and row's tags.
        Fragments.or(tags.toList.map(tag => fr"$tag=any(tags)"): _*)
      ),
      filter.maxSalary.map(salary => fr"salaryHi > $salary"),
      filter.remote.some.filter(identity).map(remote => fr"remote = $remote")
    )

    val paginationFragment: Fragment =
      fr"ORDER BY id LIMIT ${pagination.limit} OFFSET ${pagination.offset}"

    val statement = selectFragment |+| fromFragment |+| whereFragment |+| paginationFragment

    Logger[F].info(statement.toString)
    statement
      .query[Job]
      .to[List]
      .transact(xa)
      .logError(e => s"Failed query: ${e.getMessage}")

    /*
        WHERE company in [filter.companies]
        AND location in [filter.locations]
        AND country in [filter.countires]
        AND seniority in [filter.seniority]
        AND tag1 = ( any(tags)
          OR tag2 = any(tags)
          OR ... (for every tag in filter.tags)
        )
        AND salaryHi > [filter.salary]
        AND remote = [filter.remote]
     */
    // List().pure[F]
  }

  override def find(id: UUID): F[Option[Job]] =
    sql"""
         SELECT
            id,
            date,
            ownerEmail,
            company,
            title,
            description,
            externalUrl,
            remote,
            location,
            salaryLo,
            salaryHi,
            currency,
            country,
            tags,
            image,
            seniority,
            other,
            active
         FROM jobs
         WHERE id = $id
       """
      .query[Job]
      .option
      .transact(xa)

  override def update(id: UUID, jobInfo: JobInfo): F[Option[Job]] =
    sql"""
         UPDATE jobs
         SET
            company = ${jobInfo.company},
            title = ${jobInfo.title},
            description = ${jobInfo.description},
            externalUrl = ${jobInfo.externalUrl},
            remote = ${jobInfo.remote},
            location = ${jobInfo.location},
            salaryLo = ${jobInfo.salaryLo},
            salaryHi = ${jobInfo.salaryHi},
            currency = ${jobInfo.currency},
            country = ${jobInfo.country},
            tags = ${jobInfo.tags},
            image = ${jobInfo.image},
            seniority = ${jobInfo.seniority},
            other = ${jobInfo.other}
         WHERE id = $id
       """.update.run
      .transact(xa)
      .flatMap(_ => find(id)) // return updated job

  override def delete(id: UUID): F[Int] =
    sql"""
         DELETE from jobs
         WHERE id = $id
       """.update.run
      .transact(xa)

  // select all unique values for companies, locations, countries, seniorities, tags
  override def possibleFilters(): F[JobFilter] =
    sql"""
    SELECT
      ARRAY (SELECT DISTINCT(company) FROM jobs) AS companies,
      ARRAY (SELECT DISTINCT(location) FROM jobs) AS locations,
      ARRAY (SELECT DISTINCT(country) FROM jobs WHERE country IS NOT NULL) AS countries,
      ARRAY (SELECT DISTINCT(seniority) FROM jobs WHERE seniority IS NOT NULL) AS seniorities,
      ARRAY (SELECT DISTINCT(UNNEST(tags)) FROM jobs) AS tags,
      MAX(salaryHi),
      false FROM jobs
      """
      .query[JobFilter]
      .option
      .transact(xa)
      .map(_.getOrElse(JobFilter()))
}

object LiveJobs {

  /*
      id,
      date,
      ownerEmail,
      company,
      title,
      description,
      externalUrl,
      remote,
      location,
      salaryLo,
      salaryHi,
      currency,
      country,
      tags,
      image,
      seniority,
      other,
      active
   */
  given jobFilterRead: Read[JobFilter] = Read[
    (
        List[String],
        List[String],
        List[String],
        List[String],
        List[String],
        Option[Int],
        Boolean
    )
  ].map { case (companies, locations, countries, seniorites, tags, maxSalary, remote) =>
    JobFilter(companies, locations, countries, seniorites, tags, maxSalary, remote)
  }

  given jobRead: Read[Job] = Read[
    (
        UUID,                 // id
        Long,                 // date,
        String,               // ownerEmail
        String,               // company
        String,               // title
        String,               // description
        String,               // externalUrl
        Boolean,              // remote
        String,               // location
        Option[Int],          // salarryHi
        Option[Int],          // salaryLo,
        Option[String],       // currency
        Option[String],       // country
        Option[List[String]], // tags
        Option[String],       // image
        Option[String],       // seniority
        Option[String],       // other
        Boolean               // active
    )
  ].map {
    case (
          id,
          date,
          ownerEmail,
          company,
          title,
          description,
          externalUrl,
          remote,
          location,
          salaryLo,
          salaryHi,
          currency,
          country,
          tags,
          image,
          seniority,
          other,
          active
        ) =>
      Job(
        id,
        date,
        ownerEmail,
        JobInfo(
          company,
          title,
          description,
          externalUrl,
          remote,
          location,
          salaryLo,
          salaryHi,
          currency,
          country,
          tags,
          image,
          seniority,
          other
        ),
        active
      )
  }
  def apply[F[_]: MonadCancelThrow: Logger](xa: Transactor[F]): F[LiveJobs[F]] =
    new LiveJobs[F](xa).pure[F]
}
