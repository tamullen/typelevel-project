package com.tamullen.jobsboard.http.routes


import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import cats.effect.*
import cats.implicits.*
import org.typelevel.log4cats.Logger
import com.tamullen.jobsboard.http.validation.syntax.*
import com.tamullen.jobsboard.core.*
import com.tamullen.jobsboard.domain.auth.*
import com.tamullen.jobsboard.domain.user.*
import com.tamullen.jobsboard.domain.security.*
import com.tamullen.jobsboard.http.responses.FailureResponse
import tsec.authentication.{SecuredRequestHandler, asAuthed, TSecAuthService}
import org.http4s.{HttpRoutes, Response}
import org.http4s.server.Router
import org.http4s.Status

import scala.language.implicitConversions

class AuthRoutes[F[_]: Concurrent : Logger] private(auth: Auth[F]) extends HttpValidationDsl[F] {

  private val authenticator = auth.authenticator
  private val securedHandler: SecuredRequestHandler[F, String, User, JwtToken] = SecuredRequestHandler(authenticator)

  // POST /auth/login { LoginInfo } => 200 Ok with JWT as Autherization: Bearer
  private val loginRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "login" => {
      req.validate[LoginInfo] { loginInfo =>
        val maybeJwtToken = for {
//          loginInfo <- req.as[LoginInfo]
          maybeToken <- auth.login(loginInfo.email, loginInfo.password)
          _ <- Logger[F].info(s"User logging in: ${loginInfo.email}")
        } yield maybeToken

        maybeJwtToken.map {
          case Some(token) => authenticator.embed(Response(Status.Ok), token) // Authorization: Bearer
          case None => Response(Status.Unauthorized)
        }
      }
    }
  }

  // post /auth/users { NewUserInfo } => 201 Created or BadRequest if user already exists
  private val createUserRoute: HttpRoutes[F] =  HttpRoutes.of[F] {
    case req @ POST -> Root / "users" =>
      req.validate[NewUserInfo] { newUserInfo =>
        for {
          maybeNewUser <- auth.signUp(newUserInfo)
          response <- maybeNewUser match {
            case Some(user) => Created(user.email)
            case None => BadRequest(s"User with email ${newUserInfo.email} already exists.")
          }
        } yield response
      }
  }

  // PUT /auth/users/password { NewPasswordInfo } { Authorization: Bearer {JWT} } => 200 Ok
  private val changePasswordRoute: AuthRoute[F] = {
    case req @ PUT -> Root / "users" / "password" asAuthed user =>
      req.request.validate[NewPasswordInfo] { newPasswordInfo =>
        for {
          newPasswordInfo <- req.request.as[NewPasswordInfo]
          maybeUserOrError <- auth.changePassword(user.email, newPasswordInfo)
          resp <- maybeUserOrError match {
            case Right(Some(_)) => Ok()
            case Right(None) => NotFound(FailureResponse(s"User ${user.email} not found."))
            case Left(_) => Forbidden()
          }
        } yield resp
      }
  }

  // POST /auth/logout { Authorization: Bearer {JWT} } => 200 Ok
  private val logoutRoute: AuthRoute[F] = {
    case req @ POST -> Root / "logout" asAuthed _ =>
      val token = req.authenticator
      for {
        _ <- authenticator.discard(token)
        resp <- Ok()
      } yield resp
  }

  // DELETE /auth/users/email
  private val deleteUserRoute: AuthRoute[F] = {
    case req @ DELETE -> Root / "users" / email asAuthed user =>
      // auth - delete user
     auth.delete(email).flatMap {
       case true => Ok()
       case false => NotFound()
     }
  }

  val unauthedRoutes  = (loginRoute <+> createUserRoute)
  val authedRoutes = securedHandler.liftService(
    changePasswordRoute.restrictedTo(allRoles) |+|
      logoutRoute.restrictedTo(allRoles) |+|
      deleteUserRoute.restrictedTo(adminOnly)
  )

  val routes = Router (
    "/auth" -> (unauthedRoutes <+> authedRoutes)
  )
}

object AuthRoutes {
  def apply[F[_] : Concurrent : Logger] (auth: Auth[F]) =
    new AuthRoutes[F](auth)
}
