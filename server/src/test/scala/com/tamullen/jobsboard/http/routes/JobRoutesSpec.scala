package com.tamullen.jobsboard.http.routes

import cats.effect.*
import cats.implicits.*
import cats.effect.testing.scalatest.AsyncIOSpec

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*

import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import java.util.UUID

import org.http4s.HttpRoutes
import org.http4s.dsl.*
import org.http4s.*
import org.http4s.implicits.*

import com.tamullen.jobsboard.fixtures.*
import com.tamullen.jobsboard.core.*
import com.tamullen.jobsboard.domain.Job.*
import com.tamullen.jobsboard.domain.pagination._
import com.tamullen.jobsboard.http.validation._
import com.tamullen.jobsboard.http.routes.AuthRoutes

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class JobRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with JobFixture
    with SecuredRouteFixture {

  ///////////////////////////////////////////////////////////////////////
  // Prep
  //////////////////////////////////////////////////////////////////////

  val jobs: Jobs[IO] = new Jobs[IO] {
    override def create(ownerEmail: String, jobInfo: JobInfo): IO[UUID] =
      IO.pure(NewJobUuid)

    override def all(): IO[List[Job]] =
      IO.pure(List(AwesomeJob))

    override def all(filter: JobFilter, pagination: Pagination): IO[List[Job]] =
      if (filter.remote) IO.pure(List())
      else IO.pure(List(AwesomeJob))

    override def find(id: UUID): IO[Option[Job]] =
      if (id == AwesomeJobUuid)
        IO.pure(Some(AwesomeJob))
      else
        IO.pure(None)

    override def update(id: UUID, jobInfo: JobInfo): IO[Option[Job]] =
      if (id == AwesomeJobUuid)
        IO.pure(Some(UpdatedAwesomeJob))
      else
        IO.pure(None)

    override def delete(id: UUID): IO[Int] =
      if (id == AwesomeJobUuid) IO.pure(1)
      else IO.pure(0)

    override def possibleFilters(): IO[JobFilter] = IO(
      defaultFilter
    )
  }

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  // this is what is being tested.
  val jobRoutes: HttpRoutes[IO] = JobRoutes[IO](jobs).routes
  val defaultFilter: JobFilter = JobFilter(
    companies = List("Awesome Company")
  )

  ///////////////////////////////////////////////////////////////////////
  // Tests
  //////////////////////////////////////////////////////////////////////
  "JobRoutes" - {
    "should return a job with a given id" in {
      // code under test
      for {
        // simulate an HTTP request
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.GET, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
        )
        // get the HTTP response
        retrieved <- response.as[Job]
        // make some assertions
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe AwesomeJob
      }
    }

    "should return a all jobs" in {
      // code under test
      for {
        // simulate an HTTP request
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs/")
            .withEntity(JobFilter()) // empty filter
        )
        // get the HTTP response
        retrieved <- response.as[List[Job]]
        // make some assertions
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe List(AwesomeJob)
      }
    }

    "should return a all jobs that satisfy a filter" in {
      // code under test
      for {
        // simulate an HTTP request
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/jobs/")
            .withEntity(JobFilter(remote = true)) // remote filter
        )
        // get the HTTP response
        retrieved <- response.as[List[Job]]
        // make some assertions
      } yield {
        response.status shouldBe Status.Ok
        retrieved shouldBe List()
      }
    }

    "should create a new job" in {
      // code under test
      println(Logger[IO].info(s"Info: ${AwesomeJob.jobInfo}").value)
      for {
        // simulate an HTTP request
        jwtToken <- mockedAuthenticator.create(travisEmail)
        response <- jobRoutes.orNotFound.run(
          Request[IO](method = Method.POST, uri = uri"/jobs/create")
            .withEntity(AwesomeJob.jobInfo)
            .withBearerToken(jwtToken)
        )
        // get the HTTP response
        retrieved <- response.as[UUID]
        // make some assertions
      } yield {
        response.status shouldBe Status.Created
        retrieved shouldBe NewJobUuid
      }
    }

    "should only update a job that exists" in {
      // code under test
      for {
        // simulate an HTTP request
        jwtToken <- mockedAuthenticator.create(travisEmail)
        responseOk <- jobRoutes.orNotFound.run(
          Request[IO](method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
            .withEntity(UpdatedAwesomeJob.jobInfo)
            .withBearerToken(jwtToken)
        )
        // get the HTTP response
        responseInvalid <- jobRoutes.orNotFound.run(
          Request[IO](method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-000000000000")
            .withEntity(UpdatedAwesomeJob.jobInfo)
            .withBearerToken(jwtToken)
        )
        // make some assertions
      } yield {
        responseOk.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound
      }
    }

    "should only delete a job that exists" in {
      // code under test
      for {
        // simulate an HTTP request
        jwtToken <- mockedAuthenticator.create(travisEmail)
        responseOk <- jobRoutes.orNotFound.run(
          Request[IO](method = Method.DELETE, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
            .withBearerToken(jwtToken)
        )
        // get the HTTP response
        responseInvalid <- jobRoutes.orNotFound.run(
          Request[IO](method = Method.DELETE, uri = uri"/jobs/843df718-ec6e-4d49-9289-000000000000")
            .withBearerToken(jwtToken)
        )
        // make some assertions
      } yield {
        responseOk.status shouldBe Status.Ok
        responseInvalid.status shouldBe Status.NotFound
      }
    }

    "should forbid the update of a job that the user doesn't own." in {
      // code under test
      for {
        // simulate an HTTP request
        jwtToken <- mockedAuthenticator.create("someone@gmail.com")
        response <- jobRoutes.orNotFound.run(
          Request[IO](method = Method.PUT, uri = uri"/jobs/843df718-ec6e-4d49-9289-f799c0f40064")
            .withEntity(UpdatedAwesomeJob.jobInfo)
            .withBearerToken(jwtToken)
        )
        // make some assertions
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should surface all possible filters" in {
      for {
        response <- jobRoutes.orNotFound.run(
          Request(method = Method.GET, uri = uri"/jobs/filters")
        )
        filter <- response.as[JobFilter]
      } yield {
        filter shouldBe defaultFilter
      }
    }
  }
}
