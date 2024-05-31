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

class AuthRoutes[F[_]: Concurrent: Logger: SecuredHandler] private (
    auth: Auth[F],
    authenticator: Authenticator[F]
) extends HttpValidationDsl[F] {

  // POST /auth/login { LoginInfo } => 200 Ok with JWT as Autherization: Bearer
  private val loginRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "login" => {
      req.validate[LoginInfo] { loginInfo =>
        val maybeJwtToken = for {
//          loginInfo <- req.as[LoginInfo]
          maybeUser  <- auth.login(loginInfo.email, loginInfo.password)
          _          <- Logger[F].info(s"User logging in: ${loginInfo.email}")
          maybeToken <- maybeUser.traverse(user => authenticator.create(user.email))
        } yield maybeToken

        maybeJwtToken.map {
          case Some(token) =>
            authenticator.embed(Response(Status.Ok), token) // Authorization: Bearer
          case None => Response(Status.Unauthorized)
        }
      }
    }
  }

  // POST /auth/reset { ForgotPasswordInfo }
  private val forgotPasswordRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "reset" =>
      for {
        forgotPasswordInfo <- req.as[ForgotPasswordInfo]
        _                  <- auth.sendPasswordRecoveryToken(forgotPasswordInfo.email)
        resp               <- Ok()
      } yield resp
  }

  // POST /auth/recover { RecoverPasswordInfo }
  private val recoverPasswordRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "recover" =>
      for {
        rpInfo <- req.as[RecoverPasswordInfo]
        recoverySuccessful <- auth.recoverPasswordFromToken(
          rpInfo.email,
          rpInfo.token,
          rpInfo.newPassword
        )
        resp <-
          if (recoverySuccessful) Ok()
          else Forbidden(FailureResponse("Email/token combination is incorrect."))
      } yield resp
  }

  // post /auth/users { NewUserInfo } => 201 Created or BadRequest if user already exists
  private val createUserRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "users" =>
      req.validate[NewUserInfo] { newUserInfo =>
        for {
          maybeNewUser <- auth.signUp(newUserInfo)
          response <- maybeNewUser match {
            case Some(user) => Created(user.email)
            case None =>
              BadRequest(FailureResponse(s"User with email ${newUserInfo.email} already exists."))
          }
        } yield response
      }
  }

  // PUT /auth/users/password { NewPasswordInfo } { Authorization: Bearer {JWT} } => 200 Ok
  private val changePasswordRoute: AuthRoute[F] = {
    case req @ PUT -> Root / "users" / "password" asAuthed user =>
      req.request.validate[NewPasswordInfo] { newPasswordInfo =>
        for {
          _                <- Logger[F].info(s"Trying to change password for ${req.authenticator}")
          newPasswordInfo  <- req.request.as[NewPasswordInfo]
          maybeUserOrError <- auth.changePassword(user.email, newPasswordInfo)
          resp <- maybeUserOrError match {
            case Right(Some(_)) => Ok()
            case Right(None)    => NotFound(FailureResponse(s"User ${user.email} not found."))
            case Left(_)        => Forbidden()
          }
        } yield resp
      }
  }

  // POST /auth/logout { Authorization: Bearer {JWT} } => 200 Ok
  private val logoutRoute: AuthRoute[F] = { case req @ POST -> Root / "logout" asAuthed _ =>
    val token = req.authenticator
    for {
      _    <- Logger[F].info(s"Logging out ${req.identity.email}")
      _    <- authenticator.discard(token)
      resp <- Ok()
    } yield resp
  }

  // DELETE /auth/users/email
  private val deleteUserRoute: AuthRoute[F] = {
    case req @ DELETE -> Root / "users" / email asAuthed user =>
      // auth - delete user
      auth.delete(email).flatMap {
        case true  => Ok()
        case false => NotFound()
      }
  }

  val unauthedRoutes =
    (loginRoute <+> createUserRoute <+> forgotPasswordRoute <+> recoverPasswordRoute)
  val authedRoutes = SecuredHandler[F].liftService(
    changePasswordRoute.restrictedTo(allRoles) |+|
      logoutRoute.restrictedTo(allRoles) |+|
      deleteUserRoute.restrictedTo(adminOnly)
  )

  val routes = Router(
    "/auth" -> (unauthedRoutes <+> authedRoutes)
  )
}
/*
  - need a CAPABILITY, instead of intermediate values (use Dependency Injection in that case) [F[_] : Concurrent : Logger : SecuredHandler]
  - instantiated ONC in the entire app.
 */
object AuthRoutes {
  def apply[F[_]: Concurrent: Logger: SecuredHandler](
      auth: Auth[F],
      authenticator: Authenticator[F]
  ) =
    new AuthRoutes[F](auth, authenticator)
}
