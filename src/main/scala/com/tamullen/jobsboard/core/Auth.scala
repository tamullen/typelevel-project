package com.tamullen.jobsboard.core

import cats.effect.kernel.MonadCancelThrow
import cats.effect.*
import cats.implicits.*
import org.typelevel.log4cats.Logger
import tsec.authentication.{AugmentedJWT, JWTAuthenticator}
import tsec.passwordhashers.jca.BCrypt
import tsec.passwordhashers.PasswordHash
import tsec.mac.jca.HMACSHA256
import com.tamullen.jobsboard.domain.security.*
import com.tamullen.jobsboard.domain.auth.*
import com.tamullen.jobsboard.domain.user.*
import com.tamullen.jobsboard.domain.user.Role.RECRUITER
import com.tamullen.jobsboard.http.validation._


trait Auth[F[_]] {
  def login(email: String, password: String): F[Option[JwtToken]]
  def signUp(newUserInfo: NewUserInfo): F[Option[User]]
  def changePassword(email: String, newPasswordInfo: NewPasswordInfo): F[Either[String, Option[User]]]
}

class LiveAuth[F[_] : Async : Logger] private (users: Users[F], authenticator: Authenticator[F]) extends Auth[F] {
  override def login(
                      email: String,
                      password: String
                    ): F[Option[JwtToken]] = {
    // find the user in the DB => return None if no user
    // check password
    // return a new token if password matches
    for {
      maybeUser <- users.find(email)
      maybeValidatedUser <- maybeUser.filterA(user =>
        BCrypt.checkpwBool[F](
          password,
          PasswordHash[BCrypt](user.hashedPassword)
        )
      )
      maybeJWTToken <- maybeValidatedUser.traverse(user => authenticator.create(user.email))
      // Option[User].map(User => F[JWTToken] => Option[F[JWTToken]]
    } yield maybeJWTToken
  }

  override def signUp(newUserInfo: NewUserInfo): F[Option[User]] =
    //find the user in the db. If we did, return None
    users.find(newUserInfo.email).flatMap {
      case Some(user) => None.pure[F]
      case None => for { // hash the new password
        hashedPassword <- BCrypt.hashpw[F](newUserInfo.password)
        user <- User(
          newUserInfo.email,
          hashedPassword,
          newUserInfo.firstName,
          newUserInfo.lastName,
          newUserInfo.company,
          RECRUITER
        ).pure[F]
        // create a new user in the db
        _ <- users.create(user)
      } yield Some(user)
    }

  override def changePassword(
                               email: String,
                               newPasswordInfo: NewPasswordInfo
                             ): F[Either[String, Option[User]]] = {
    // find user
    def updateUser(user: User, newPassword: String): F[Option[User]] =
      for {
        hashedPassword <- BCrypt.hashpw[F](newPassword)
        updatedUser <- users.update(user.copy(hashedPassword = hashedPassword))
      } yield updatedUser

    def checkAndUpdate(user: User, oldPassword: String, newPassword: String): F[Either[String, Option[User]]] = {
      for {
        // check password
        passCheck <- BCrypt
          .checkpwBool[F](
            newPasswordInfo.oldPassword,
            PasswordHash[BCrypt](user.hashedPassword)
          )
        updateResult <-
          if (passCheck) {
            updateUser(user, newPassword).map(Right(_))
          } else {
            Left("Invalid Password").pure[F]
          }
      } yield updateResult
    }

    users.find(email).flatMap {
      case None => Right(None).pure[F]
      case Some(user) =>
        val NewPasswordInfo(oldPassword, newPassword) = newPasswordInfo
        checkAndUpdate(user, oldPassword, newPassword)
    }
  }
}

object LiveAuth {
  def apply[F[_] : Async : Logger](
                                               users: Users[F],
                                               authenticator: Authenticator[F]
                                             ): F[LiveAuth[F]] = new LiveAuth[F](users, authenticator).pure[F]
}
