package com.tamullen.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.Monad
import cats.*
import cats.effect.*
import cats.implicits.*

import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.Router
import org.typelevel.log4cats.Logger

import scala.collection.mutable
import scala.collection.mutable.Map
import java.util.UUID

import com.tamullen.jobsboard.domain.Job.*
import com.tamullen.jobsboard.core._
import com.tamullen.jobsboard.http.responses.*
import com.tamullen.jobsboard.core.Jobs
import com.tamullen.jobsboard.logging.Syntax._


class JobRoutes[F[_] : Concurrent: Logger] private (jobs: Jobs[F]) extends Http4sDsl[F] {

  // "database"
  //private val database = mutable.Map[UUID, Job]()


  // POST -> /jobs?offset=x&limit=y { filters } // TODO add query params and filters
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root =>
      for {
        jobslist <- jobs.all()
        resp <- Ok(jobslist)
      } yield resp
  }

  // Get /jobs/uuid
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / UUIDVar(id) =>
     jobs.find(id).flatMap {
       case Some(job) => Ok(job)
       case None => NotFound(FailureResponse(s"Job $id not found."))
     }
  }

  // POST /jobs { jobInfo }

  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "create" =>
      for {
        _ <- Logger[F].info("Trying to add job")
        jobInfo <- req.as[JobInfo].logError(e => s"Parsing payload failed: $e")
        _ <- Logger[F].info(s"Parsed job info: $jobInfo")
        jobId <- jobs.create("TODO@tamullen.com", jobInfo)
        _ <- Logger[F].info(s"Created Job: $jobId")
        resp <- Created(jobId)
      } yield resp
  }

  // PUT /jobs/uuid { jobInfo }
  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ PUT -> Root / UUIDVar(id) =>
        for {
          jobInfo <- req.as[JobInfo]
          maybeNewJob <- jobs.update(id, jobInfo)
          resp <- maybeNewJob match {
            case Some(job) => Ok()
            case None => NotFound(FailureResponse(s"Cannot update job $id: Not found"))
          }
        } yield resp
  }

  // DELETE /jobs/uuid
  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ DELETE -> Root / UUIDVar(id) =>
      jobs.find(id).flatMap {
        case Some(job) =>
          for {
            _ <- Logger[F].info(s"Deleting: ${id}")
            _ <- jobs.delete(job.id)
            resp <- Ok()
          } yield resp
        case None => NotFound(FailureResponse(s"Cannot delete job $id: Not found"))
      }
  }

  val routes = Router(
    "/jobs" -> (allJobsRoute <+> findJobRoute <+> createJobRoute <+> updateJobRoute <+> deleteJobRoute)
  )
}

object JobRoutes {
  def apply[F[_]: Concurrent: Logger] (jobs: Jobs[F]) = new JobRoutes[F](jobs)
}
