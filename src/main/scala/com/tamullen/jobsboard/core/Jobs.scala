package com.tamullen.jobsboard.core

import cats.*
import cats.effect._
import cats.implicits.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.*

import java.util.UUID
import com.tamullen.jobsboard.domain.Job.*

trait Jobs[F[_]] {
  // "Algebra"
  // CRUD
  def create(ownerEmail: String, jobInfo: JobInfo): F[UUID]
  def all():  F[List[Job]]
  def find(id: UUID): F[Option[Job]]
  def update(id: UUID, jobInfo: JobInfo): F[Option[Job]]
  def delete(id: UUID): F[Int]
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
class LiveJobs[F[_]: MonadCancelThrow] private(xa: Transactor[F]) extends Jobs[F] {
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
       """
      .update
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
       """
      .update
      .run
      .transact(xa)
      .flatMap(_ => find(id)) // return updated job

  override def delete(id: UUID): F[Int] =
    sql"""
         DELETE from jobs
         WHERE id = $id
       """
      .update
      .run
      .transact(xa)
}

object LiveJobs {
  def apply[F[_]: MonadCancelThrow](xa: Transactor[F]): F[LiveJobs[F]] = new LiveJobs[F](xa).pure[F]
}
