package com.tamullen.jobsboard.core

import cats.data.*
import cats.*
import cats.effect.*
import cats.effect.testing.scalatest.AsyncIOSpec
import com.tamullen.jobsboard.config.SecurityConfig
import com.tamullen.jobsboard.domain.auth.NewPasswordInfo
import com.tamullen.jobsboard.domain.user.*
import com.tamullen.jobsboard.domain.security.*
import com.tamullen.jobsboard.fixtures.*
import com.tamullen.jobsboard.http.validation.*
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import tsec.authentication.{IdentityStore, JWTAuthenticator}
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

import scala.concurrent.duration.*

class AuthSpec
  extends AsyncFreeSpec
  with AsyncIOSpec
  with Matchers
  with UsersFixture{

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val mockedConfig = SecurityConfig("secret", 1.day)
  val mockedTokens: Tokens[IO] = new Tokens[IO] {
    override def getToken(email: String): IO[Option[String]] =
      if (email == travisEmail) IO.pure(Some("ABC123"))
      else IO.pure(None)

    override def checkToken(email: String, token: String): IO[Boolean] =
      IO.pure(token == "ABC123")
  }

  val mockedEmails: Emails[IO] = new Emails[IO] {
    override def sendEmail(to: String, subject: String, content: String): IO[Unit] = IO.unit

    override def sendPasswordRecoveryEmail(to: String, token: String): IO[Unit] = IO.unit
  }

  def probedEmails(users: Ref[IO, Set[String]]): Emails[IO] = new Emails[IO] {
    override def sendEmail(to: String, subject: String, content: String): IO[Unit] =
      users.modify(set => (set + to, ()))

    override def sendPasswordRecoveryEmail(to: String, token: String): IO[Unit] =
      sendEmail(to, "your token", "token")
  }



  //  val mockedAuthenticator: Authenticator[IO] = {
//    // key for hashing
//    val key = HMACSHA256.unsafeGenerateKey
//    // identity store to retrieve users
//    val idStore: IdentityStore[IO, String, User] = (email: String) =>
//      if (email == travisEmail) OptionT.pure(Travis)
//      else if (email == amberEmail) OptionT.pure(Amber)
//      else OptionT.none[IO, User]
//
//    JWTAuthenticator.unbacked.inBearerToken(
//      1.day, // expiration of tokesn
//      None, // max idle time (optional)
//      idStore, // identity store
//      key // hash key
//    )
//  }


  "Auth 'algebra'" - {
    "Login should return None if the user doesn't exist" in {
        val program = for {
          auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
          maybeToken <- auth.login("none@test.com", "password")
        } yield maybeToken
        program.asserting(_ shouldBe None)
    }

    "Login should return None if the user exists but the password is wrong" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        maybeToken <- auth.login(travisEmail, "password")
      } yield maybeToken
      program.asserting(_ shouldBe None)
    }

    "Login should return a token if the user exists and the password is correct" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        maybeToken <- auth.login(travisEmail, "tamullen")
      } yield maybeToken
      program.asserting(_ shouldBe defined)
    }

    "signing up should not create a user with an existing email" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        maybeUser <- auth.signUp(NewUserInfo(
          travisEmail,
          "somePassword",
          Some("travis"),
          Some("whatever"),
          Some("other company")
        ))
      } yield maybeUser

      program.asserting(_ shouldBe None)
    }

    "signing up should create a completely new user" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        maybeUser <- auth.signUp(NewUserInfo(
          "bob@test.com",
          "somePassword",
          Some("Bob"),
          Some("Jones"),
          Some ("other company")
        ))
      } yield maybeUser

      program.asserting {
        case Some(user) =>
          user.email shouldBe "bob@test.com"
          user.firstName shouldBe Some("Bob")
          user.lastName shouldBe Some("Jones")
          user.company shouldBe Some("other company")
          user.role shouldBe Role.RECRUITER
        case _ =>
          fail()
      }
    }

    "changePassword should return None if the user doesn't exist" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.changePassword("alice@test.com", NewPasswordInfo("oldpw", "newpw"))
      } yield result

      program.asserting(_ shouldBe Right(None))
    }

    "changePassword should return a Left with an error if the user exists and the password is incorrect." in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.changePassword(travisEmail, NewPasswordInfo("oldpw", "newpw"))
      } yield result

      program.asserting(_ shouldBe Left("Invalid Password"))
    }

    "changePassword should correctly change the password if all the details are correct" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.changePassword(travisEmail, NewPasswordInfo("tamullen", "scalarocks"))
        isNicePassword <- result match {
          case Right(Some(user)) =>
            BCrypt
              .checkpwBool[IO](
                "scalarocks",
                PasswordHash[BCrypt](user.hashedPassword)
              )
          case _ =>
            IO.pure(false)
        }
      } yield isNicePassword

      program.asserting(_ shouldBe true)
    }

    "recoverPassword should fail for a user that does not exist, even if the token is correct" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result1 <- auth.recoverPasswordFromToken("someone@gmail.com", "ABC123", "igotya")
        result2 <- auth.recoverPasswordFromToken("someone@gmail.com", "wrongToken", "igotya")
      } yield (result1, result2)

      program.asserting(_ shouldBe (false, false))
    }

    "recoverPassword should fail for a user that does exist, but the token is wrong" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.recoverPasswordFromToken(travisEmail, "wrongToken", "igotya")
      } yield result

      program.asserting(_ shouldBe false)
    }

    "recoverPassword should succeed for the correct user and token" in {
      val program = for {
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, mockedEmails)
        result <- auth.recoverPasswordFromToken(travisEmail, "ABC123", "test")
      } yield result

      program.asserting(_ shouldBe true)
    }

    "sending recovery passwords should fail for a user that doesn't exist" in {
      val program = for {
        set <- Ref.of[IO, Set[String]](Set())
        emails <- IO(probedEmails(set))
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, emails)
        result <- auth.sendPasswordRecoveryToken("someone@whatever.com")
        usersBeingSentEmails <- set.get
      } yield usersBeingSentEmails

      program.asserting(_ shouldBe empty)
    }

    "sending recovery passwords should succeed for a user that exists" in {
      val program = for {
        set <- Ref.of[IO, Set[String]](Set())
        emails <- IO(probedEmails(set))
        auth <- LiveAuth[IO](mockedUsers, mockedTokens, emails)
        result <- auth.sendPasswordRecoveryToken(travisEmail)
        usersBeingSentEmails <- set.get
      } yield usersBeingSentEmails

      program.asserting(_ should contain(travisEmail))
    }
  }

}
