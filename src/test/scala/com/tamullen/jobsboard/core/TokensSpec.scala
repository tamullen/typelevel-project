package com.tamullen.jobsboard.core

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.*
import cats.implicits.*
import com.tamullen.jobsboard.config.PaginationConfig
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import com.tamullen.jobsboard.fixtures._
import com.tamullen.jobsboard.domain.user._
import com.tamullen.jobsboard.config._

import scala.concurrent.duration._

class TokensSpec extends AsyncFreeSpec
  with AsyncIOSpec
  with Matchers
  with DoobieSpec
  with UsersFixture {

  val initScript: String = "sql/recoverytokens.sql"

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  "Tokens 'algebra/module" - {
    "should not create a token for a user that doesn't exist" in {
      transactor.use { xa =>
        val program = for {
          tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(100000000L))
          token <- tokens.getToken("somebody@someemail.com")
        } yield token

        program.asserting(_ shouldBe None)
      }
    }

    "should create a token for an existing user" in {
      transactor.use { xa =>
        val program = for {
          tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(100000000L))
          token <- tokens.getToken(travisEmail)
        } yield token

        program.asserting(_ shouldBe defined)
      }
    }

    "should not validate expired tokens" in {
      transactor.use { xa =>
        val program = for {
          tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(100L))
          maybeToken <- tokens.getToken(travisEmail)
          _ <- IO.sleep(500.millis)
          isTokenValid <- maybeToken match {
            case Some(token) => tokens.checkToken(travisEmail, token)
            case None => IO.pure(false)
          }
        } yield isTokenValid

        program.asserting(_ shouldBe false)
      }
    }

    "should validate unexpired token" in {
      transactor.use { xa =>
        val program = for {
          tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(100000000L))
          maybeToken <- tokens.getToken(travisEmail)
          isTokenValid <- maybeToken match {
            case Some(token) => tokens.checkToken(travisEmail, token)
            case None => IO.pure(false)
          }
        } yield isTokenValid

        program.asserting(_ shouldBe true)
      }
    }

    "should only validate tokens for the user that generated them" in {
      transactor.use { xa =>
        val program = for {
          tokens <- LiveTokens[IO](mockedUsers)(xa, TokenConfig(100000000L))
          maybeToken <- tokens.getToken(travisEmail)
          _ <- IO.sleep(500.millis)
          isTravisTokenValid <- maybeToken match {
            case Some(token) => tokens.checkToken(travisEmail, token)
            case None => IO.pure(false)
          }
          isOtherTokenValid <- maybeToken match {
            case Some(token) => tokens.checkToken("test@test.com", token)
            case None => IO.pure(false)
          }
        } yield (isTravisTokenValid, isOtherTokenValid)

        program.asserting {
          case (isTravisTokenValid, isOtherTokenValid) =>
            isTravisTokenValid shouldBe true
            isOtherTokenValid shouldBe false
        }
      }
    }
  }
}
