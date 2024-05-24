package com.tamullen.jobsboard.core

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.*
import cats.implicits.*
import doobie.util.*
import doobie.postgres.implicits.*
import doobie.implicits.*
import doobie.hikari.HikariTransactor
import doobie.util.*
import org.testcontainers.containers.PostgreSQLContainer
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import com.tamullen.jobsboard.fixtures.*
import com.tamullen.jobsboard.domain.user.*
import org.postgresql.util.PSQLException
import org.scalatest.Inside

class UsersSpec
  extends AsyncFreeSpec
  with AsyncIOSpec
  with Matchers
  with Inside
  with DoobieSpec
  with UsersFixture {

  override val initScript: String ="sql/users.sql"
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  "Users 'algebra'" - {
    "should retrieve a user by email" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          retrieved <- users.find("travis@test.com")
        } yield retrieved

        program.asserting(_ shouldBe Some(Travis))
      }
    }

    "should None if email doesn't exist" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          retrieved <- users.find("NotFound@test.com")
        } yield retrieved

        program.asserting(_ shouldBe None)
      }
    }

    "should create a new user" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          userId <- users.create(NewUser)
          maybeUser <-
              sql"SELECT * FROM users WHERE email = ${NewUser.email}"
                .query[User]
                .option
                .transact(xa)
        } yield (maybeUser, userId)

        program.asserting {
          case (maybeUser, userId) =>
            userId shouldBe NewUser.email
            maybeUser shouldBe Some(NewUser)
        }
      }
    }

    "Should fail creating a new user if the email already exists" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          userId <- users.create(Travis).attempt // IO[Either[Throwable, String]]
        } yield userId

        program.asserting { outcome =>
          inside(outcome) {
            case Left(e) => e shouldBe a[PSQLException]
            case _ => fail()
          }
        }
      }
    }

    "Should return None when updating a user that does not exist" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          maybeUser <- users.update(NewUser) // IO[Either[Throwable, String]]
        } yield maybeUser

        program.asserting(_ shouldBe None)
      }
    }

    "Should update an existing user" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          maybeUser <- users.update(UpdatedTravis) // IO[Either[Throwable, String]]
        } yield maybeUser

        program.asserting(_ shouldBe Some(UpdatedTravis))
      }
    }

    "Should delete a user" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          result <- users.delete("travis@test.com") // IO[Either[Throwable, String]]
          maybeUser <-
            sql"SELECT * FROM users WHERE email = 'travis@test.com'"
            .query[User]
            .option
            .transact(xa)
        } yield (result, maybeUser)

        program.asserting {
          case (result, maybeUser) =>
            result shouldBe true
            maybeUser shouldBe None
        }
      }
    }

    "Should not delete a user that does not exist" in {
      transactor.use { xa =>
        val program = for {
          users <- LiveUsers[IO](xa)
          result <- users.delete("nobody@test.com") // IO[Either[Throwable, String]]
        } yield result

        program.asserting(_ shouldBe false)
      }
    }



  }
}
