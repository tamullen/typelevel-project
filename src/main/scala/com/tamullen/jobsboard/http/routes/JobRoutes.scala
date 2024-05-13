package com.tamullen.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.Monad
import cats.*
import cats.effect._
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
import com.tamullen.jobsboard.http.responses.*



class JobRoutes[F[_] : Concurrent: Logger] extends Http4sDsl[F] {

  // "database"
  private val database = mutable.Map[UUID, Job]()


  // POST -> /jobs?offset=x&limit=y { filters } // TODO add query params and filters
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case POST -> Root =>
      Ok(database.values)
  }

  // Get /jobs/uuid
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / UUIDVar(id) =>
     database.get(id) match {
       case Some(job) => Ok(job)
       case None => NotFound(FailureResponse(s"Job $id not found."))
     }
  }

  // POST /jobs { jobInfo }
  private def createJob(jobInfo: JobInfo): F[Job] = {
    Job(
      id = UUID.randomUUID(),
      date = System.currentTimeMillis(),
      ownerEmail = "TODO@rockthejvm.com",
      jobInfo = jobInfo,
      active = true
    ).pure[F]
  }

  import com.tamullen.jobsboard.logging.Syntax._
  private val createJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "create" =>
      for {
        _ <- Logger[F].info("Trying to add job")
        jobInfo <- req.as[JobInfo].logError(e => s"Parsing payload failed: $e")
        _ <- Logger[F].info(s"Parsed job info: $jobInfo")
        job <- createJob(jobInfo)
        _ <- Logger[F].info(s"Created Job: $job")
        _ <- database.put(job.id, job).pure[F]
        resp <- Created(job.id)
      } yield resp
  }

  // PUT /jobs/uuid { jobInfo }
  private val updateJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ PUT -> Root / UUIDVar(id) =>
      database.get(id) match {
        case Some(job) =>
          for {
            jobInfo <- req.as[JobInfo]
            _ <- database.put(id, job.copy(jobInfo = jobInfo)).pure[F]
            resp <- Ok()
          } yield resp
        case None => NotFound(FailureResponse(s"Cannot update job $id: Not found"))
      }
  }

  // DELETE /jobs/uuid
  private val deleteJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ DELETE -> Root / UUIDVar(id) =>
      database.get(id) match {
        case Some(job) =>
          for {
            _ <- Logger[F].info(s"Deleting: ${database.get(id)}")
            _ <- database.remove(job.id).pure[F]
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
  def apply[F[_]: Concurrent: Logger] = new JobRoutes[F]
}
