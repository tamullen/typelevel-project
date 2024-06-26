package com.tamullen.jobsboard.http.routes

import cats.data.OptionT
import cats.effect.*
import cats.implicits.*
import cats.effect.testing.scalatest.AsyncIOSpec
import io.circe.generic.auto.*
import org.http4s.circe.CirceEntityCodec.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.http4s.HttpRoutes
import org.http4s.dsl.*
import org.http4s.*
import org.http4s.implicits.*
import com.tamullen.jobsboard.http.validation._
import com.tamullen.jobsboard.fixtures.*
import com.tamullen.jobsboard.core.*
import com.tamullen.jobsboard.domain.Job.*
import com.tamullen.jobsboard.domain.security.*
import com.tamullen.jobsboard.domain.user.*
import com.tamullen.jobsboard.domain.auth.*
import org.http4s.headers.Authorization
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.ci.CIStringSyntax
import tsec.authentication.{IdentityStore, JWTAuthenticator}
import tsec.jws.mac.JWTMac
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

import scala.concurrent.duration.*

class AuthRoutesSpec
    extends AsyncFreeSpec
    with AsyncIOSpec
    with Matchers
    with Http4sDsl[IO]
    with UsersFixture
    with SecuredRouteFixture {

  ///////////////////////////////////////////////////////////////////////
  // Prep
  //////////////////////////////////////////////////////////////////////

  val mockedAuth: Auth[IO] = probedAuth(None)
  def probedAuth(userMap: Option[Ref[IO, Map[String, String]]]) = new Auth[IO] {
    override def login(email: String, password: String): IO[Option[User]] =
      if (email == travisEmail && password == travisPassword)
        IO(Some(Travis))
      else IO.pure(None)

    override def signUp(newUserInfo: NewUserInfo): IO[Option[User]] =
      if (newUserInfo.email == amberEmail)
        IO.pure(Some(Amber))
      else
        IO.pure(None)

    override def changePassword(
        email: String,
        newPasswordInfo: NewPasswordInfo
    ): IO[Either[String, Option[User]]] =
      if (email == travisEmail)
        if (newPasswordInfo.oldPassword == travisPassword)
          IO.pure(Right(Some(Travis)))
        else
          IO.pure(Left("Invalid password"))
      else
        IO.pure(Right(None))

//    override def authenticator: Authenticator[IO] = mockedAuthenticator

    override def delete(email: String): IO[Boolean] = IO.pure(true)

    // allow password recovery
    override def sendPasswordRecoveryToken(email: String): IO[Unit] =
      userMap
        .traverse { userMapRef =>
          userMapRef.modify { userMap =>
            (userMap + (email -> "ABC123"), ())
          }
        }
        .map(_ => ())

    override def recoverPasswordFromToken(
        email: String,
        token: String,
        newPassword: String
    ): IO[Boolean] =
      userMap
        .traverse { userMapRef =>
          userMapRef.get
            .map { userMap =>
              userMap.get(email).filter(_ == token) // Option[String]
            }                                       // IO[Option[String]]
            .map(_.nonEmpty)
        }
        .map(_.getOrElse(false))
  }

  given logger: Logger[IO]       = Slf4jLogger.getLogger[IO]
  val authRoutes: HttpRoutes[IO] = AuthRoutes[IO](mockedAuth, mockedAuthenticator).routes

  ///////////////////////////////////////////////////////////////////////
  // Tests
  //////////////////////////////////////////////////////////////////////
  "AuthRoutes" - {
    "should return a 401 - unauthorized if login fails" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/login")
            .withEntity(LoginInfo(travisEmail, "wrongpassword"))
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return a 200 - Ok + JWT if login is successful if login fails" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/login")
            .withEntity(LoginInfo(travisEmail, travisPassword))
        )
      } yield {
        response.status shouldBe Status.Ok
        response.headers.get(ci"Authorization") shouldBe defined
      }
    }

    "should return a 400 - BadRequest the user is already in the database" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/users")
            .withEntity(NewUserTravis)
        )
      } yield {
        response.status shouldBe Status.BadRequest
      }
    }

    "should return a 201 - Created  if the user is created" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/users")
            .withEntity(NewUserAmber)
        )
      } yield {
        response.status shouldBe Status.Created
      }
    }

    "should return a 200 - OK if logging out with a valid JWT token" in {
      for {
        jwtToken <- mockedAuthenticator.create(travisEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/logout")
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Ok
      }
    }

    "should return a 401 - Unauthrozied if logging out without a valid JWT token" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/logout")
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    // change password - happy path => 200 - Ok
    // change password - user doesn't exist => 404 - Not found
    "should return a 404 Not found if changing password for a user that doesn't exist" in {
      for {
        jwtToken <- mockedAuthenticator.create(amberEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(amberPassword, "newpassword"))
        )
      } yield {
        response.status shouldBe Status.NotFound
      }
    }

    // change password - invalid old password => 403 - Forbidden
    "should return a 403 - Forbidden if old password is incorrect" in {
      for {
        jwtToken <- mockedAuthenticator.create(travisEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo("wrongpassword", "newpassword"))
        )
      } yield {
        response.status shouldBe Status.Forbidden
      }
    }

    // if JWT is invalid => 401 - Unauthorized if
    "should return a 401 - Unauthorized if changing password without authorized JWT" in {
      for {
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withEntity(NewPasswordInfo(amberPassword, "newpassword"))
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return a 200 - Ok if changing password for a user with valid JWT and password" in {
      for {
        jwtToken <- mockedAuthenticator.create(travisEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.PUT, uri = uri"/auth/users/password")
            .withBearerToken(jwtToken)
            .withEntity(NewPasswordInfo(travisPassword, "newpassword"))
        )
      } yield {
        response.status shouldBe Status.Ok
      }
    }

    "should return a 401 - Unauthorized if a non-admin tries to delete a user" in {
      for {
        jwtToken <- mockedAuthenticator.create(amberEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/auth/users/travis@test.com")
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Unauthorized
      }
    }

    "should return a 200 - Ok if an admin tries to delete a user" in {
      for {
        jwtToken <- mockedAuthenticator.create(travisEmail)
        response <- authRoutes.orNotFound.run(
          Request(method = Method.DELETE, uri = uri"/auth/users/travis@test.com")
            .withBearerToken(jwtToken)
        )
      } yield {
        response.status shouldBe Status.Ok
      }
    }

    "should return a 200 - Ok when resetting a password, and an email should be triggered" in {
      for {
        userMapRef <- Ref.of[IO, Map[String, String]](Map())
        auth       <- IO(probedAuth(Some(userMapRef)))
        routes     <- IO(AuthRoutes(auth, mockedAuthenticator).routes)
        response <- routes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/reset")
            .withEntity(ForgotPasswordInfo(travisEmail))
        )
        userMap <- userMapRef.get
      } yield {
        response.status shouldBe Status.Ok
        userMap should contain key (travisEmail)
      }
    }

    "should return a 200 - Ok when recovering a password, for a correct user/token combination" in {
      for {
        userMapRef <- Ref.of[IO, Map[String, String]](Map(travisEmail -> "ABC123"))
        auth       <- IO(probedAuth(Some(userMapRef)))
        routes     <- IO(AuthRoutes(auth, mockedAuthenticator).routes)
        response <- routes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/recover")
            .withEntity(RecoverPasswordInfo(travisEmail, "ABC123", "newPass"))
        )
        userMap <- userMapRef.get
      } yield {
        response.status shouldBe Status.Ok
        userMap should contain key (travisEmail)
      }
    }

    "should return a 403 - Forbidden when recovering a password, for an incorrect user/token combination" in {
      for {
        userMapRef <- Ref.of[IO, Map[String, String]](Map(travisEmail -> "ABC123"))
        auth       <- IO(probedAuth(Some(userMapRef)))
        routes     <- IO(AuthRoutes(auth, mockedAuthenticator).routes)
        response <- routes.orNotFound.run(
          Request(method = Method.POST, uri = uri"/auth/recover")
            .withEntity(RecoverPasswordInfo(travisEmail, "wrongToken", "newPass"))
        )
        userMap <- userMapRef.get
      } yield {
        response.status shouldBe Status.Forbidden

      }
    }

  }

}
