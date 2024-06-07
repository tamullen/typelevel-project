package com.tamullen.jobsboard.http.routes

import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.Monad
import cats.*
import cats.effect.*
import cats.implicits.*
import com.tamullen.jobsboard.config.PaginationConfig
import tsec.authentication.{SecuredRequestHandler, asAuthed}
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.Router
import org.typelevel.log4cats.Logger

import scala.collection.mutable
import scala.collection.mutable.Map

import java.util.UUID
import com.tamullen.jobsboard.domain.Job.*
import com.tamullen.jobsboard.domain.security.*
import com.tamullen.jobsboard.domain.security.AuthRoute
import com.tamullen.jobsboard.core.*
import com.tamullen.jobsboard.http.responses.*
import com.tamullen.jobsboard.core.Jobs
import com.tamullen.jobsboard.logging.Syntax.*
import com.tamullen.jobsboard.http.validation.syntax.*
import com.tamullen.jobsboard.http.validation.*
import com.tamullen.jobsboard.domain.pagination.*
import com.tamullen.jobsboard.domain.user.User

//import scala.language.implicitConversions
//import com.tamullen.jobsboard.domain.pagination.Pagination.Pages

class JobRoutes[F[_]: Concurrent: Logger: SecuredHandler] private (jobs: Jobs[F], stripe: Stripe[F])
    extends HttpValidationDsl[F] {
//  given Pages: PaginationConfig =
//    new PaginationConfig

  given pages: PaginationConfig = new PaginationConfig()
//  private val securedHandler: SecuredHandler[F] = SecuredRequestHandler(authenticator)

  object OffsetQueryParam extends OptionalQueryParamDecoderMatcher[Int]("offset")
  object LimitQueryParam  extends OptionalQueryParamDecoderMatcher[Int]("limit")

  // GET /jobs/filters => { filters }
  private val allFiltersRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "filters" =>
    jobs.possibleFilters().flatMap(jobFilters => Ok(jobFilters))
  }

  // POST -> /jobs?limit=x&offset=y { filters }
  private val allJobsRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root :? LimitQueryParam(limit) +& OffsetQueryParam(offset) =>
      for {
        filter   <- req.as[JobFilter]
        jobslist <- jobs.all(filter, Pagination(limit, offset)) // (new PaginationConfig))
        resp     <- Ok(jobslist)
      } yield resp
  }

  // Get /jobs/uuid
  private val findJobRoute: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / UUIDVar(id) =>
    jobs.find(id).flatMap {
      case Some(job) => Ok(job)
      case None      => NotFound(FailureResponse(s"Job $id not found."))
    }
  }

  // refined library
  // checked at compile time - increase compile time
  // lowers Developer Experience

  // POST /jobs/create { jobInfo }
  private val createJobRoute: AuthRoute[F] = { case req @ POST -> Root / "create" asAuthed user =>
    req.request.validate[JobInfo] { jobInfo =>
      for {
        _     <- Logger[F].info("Trying to add job")
        _     <- Logger[F].info(s"Parsed job info: $jobInfo")
        jobId <- jobs.create(user.email, jobInfo)
        _     <- Logger[F].info(s"Created Job: $jobId")
        resp  <- Created(jobId)
      } yield resp
    }
  }

  // Stripe Endpoints.
  // POST /jobs/promoted
  private val promotedJobRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "promoted" =>
      req.validate[JobInfo] { jobInfo =>
        for {
          jobId   <- jobs.create("TODO@rockthejvm.com", jobInfo)
          _       <- Logger[F].info(s"Created Job: $jobId")
          session <- stripe.createCheckoutSession(jobId.toString, "TODO@rockthejvm.com")
          resp    <- session.map(sesh => Ok(sesh.getUrl())).getOrElse(NotFound())
        } yield resp
      }
  }

  // PUT /jobs/uuid { jobInfo }
  private val updateJobRoute: AuthRoute[F] = { case req @ PUT -> Root / UUIDVar(id) asAuthed user =>
    req.request.validate[JobInfo] { jobInfo =>
      jobs.find(id).flatMap {
        case None =>
          NotFound(FailureResponse(s"Cannot update job $id: Not Found"))
        case Some(job) if user.owns(job) || user.isAdmin =>
          jobs.update(id, jobInfo) *> Ok()
        case _ =>
          Forbidden(FailureResponse("You can only delete your own jobs."))
      }
    //        for {
    ////          jobInfo <- req.as[JobInfo] don't need because validate parses the JobInfo already.
    //          maybeNewJob <- jobs.update(id, jobInfo)
    //          resp <- maybeNewJob match {
    //            case Some(job) => Ok()
    //            case None => NotFound(FailureResponse(s"Cannot update job $id: Not found"))
    //          }
    //        } yield resp
    }
  }

  // DELETE /jobs/uuid
  private val deleteJobRoute: AuthRoute[F] = {
    case req @ DELETE -> Root / UUIDVar(id) asAuthed user =>
      jobs.find(id).flatMap {
        case None => NotFound(FailureResponse(s"Cannot delete job $id: Not found"))
        case Some(job) if user.owns(job) || user.isAdmin =>
          Logger[F].info(s"Deleting: ${id}") *>
            jobs.delete(id) *>
            Ok()
        case _ => Forbidden(FailureResponse("You can only delete your own jobs."))
      }
  }

  val authedRoutes = SecuredHandler[F].liftService(
    createJobRoute.restrictedTo(allRoles) |+|
      updateJobRoute.restrictedTo(allRoles) |+|
      deleteJobRoute.restrictedTo(allRoles)
  )

  val unauthedRoutes = allJobsRoute <+> allFiltersRoute <+> findJobRoute <+> promotedJobRoute
  val routes = Router(
    "/jobs" -> (unauthedRoutes <+> authedRoutes)
  )
}

object JobRoutes {
  def apply[F[_]: Concurrent: Logger: SecuredHandler](jobs: Jobs[F], stripe: Stripe[F]) =
    new JobRoutes[F](jobs, stripe)
}
