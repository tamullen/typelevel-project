package com.tamullen.jobsboard.modules

import cats.*
import cats.effect.*
import cats.data.*
import cats.implicits.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.server.Router
import org.typelevel.log4cats.Logger
import tsec.mac.jca.HMACSHA256
import tsec.authentication.{
  AugmentedJWT,
  BackingStore,
  IdentityStore,
  JWTAuthenticator,
  SecuredRequestHandler
}
import tsec.passwordhashers.jca.BCrypt
import tsec.passwordhashers.PasswordHash
import tsec.common.SecureRandomId
import com.tamullen.jobsboard.core.*
import com.tamullen.jobsboard.domain.security.*
import com.tamullen.jobsboard.http.routes.*
import com.tamullen.jobsboard.config.SecurityConfig
import com.tamullen.jobsboard.domain.user.User

class HttpApi[F[_]: Concurrent: Logger] private (core: Core[F], authenticator: Authenticator[F]) {
  given securedHandler: SecuredHandler[F] = SecuredRequestHandler(authenticator)
  private val healthRoutes                = HealthRoutes[F].routes
  private val jobRoutes                   = JobRoutes[F](core.jobs).routes
  private val authRoutes                  = AuthRoutes[F](core.auth, authenticator).routes

  val endpoints = Router(
    "/api" -> (healthRoutes <+> jobRoutes <+> authRoutes)
  )
}

object HttpApi {
  def createAuthenticator[F[_]: Sync](
      users: Users[F],
      securityConfig: SecurityConfig
  ): F[Authenticator[F]] = {

    // 1. identity store
    val idStore: IdentityStore[F, String, User] = (email: String) => OptionT(users.find(email))

    // 2. Backing store for JWT tokens: BackingStore[F, id, JwtToken
    val tokenStoreF = Ref.of[F, Map[SecureRandomId, JwtToken]](Map.empty).map { ref =>
      new BackingStore[F, SecureRandomId, JwtToken] {
        // could use a mutable map - race conditions (accessed concurrently)
        // use a ref to make operations atomic.
        override def get(id: SecureRandomId): OptionT[F, JwtToken] =
          OptionT( /*F[JwtToken]*/ ref.get.map(_.get(id)))

        override def put(elem: JwtToken): F[JwtToken] =
          ref.modify(store => (store + (elem.id -> elem), elem))

        override def update(v: JwtToken): F[JwtToken] =
          put(v)

        override def delete(id: SecureRandomId): F[Unit] =
          ref.modify(store => (store - id, ()))
      }
    }

    // 3. hashing key
    val keyF = HMACSHA256.buildKey[F](securityConfig.secret.getBytes("UTF-8"))
    // identity store to retrieve users

    // 4. authenticator
    for {
      key        <- keyF
      tokenStore <- tokenStoreF
    } yield JWTAuthenticator.backed.inBearerToken(
      expiryDuration = securityConfig.jwtExpiryDuration, // expiration of token
      maxIdle = None,                                    // max idle time (optional)
      tokenStore = tokenStore,                           // backing store
      identityStore = idStore,                           // identity store
      signingKey = key                                   // hash key
    )
  }

  def apply[F[_]: Async: Logger](
      core: Core[F],
      securityConfig: SecurityConfig
  ): Resource[F, HttpApi[F]] =
    Resource
      .eval(createAuthenticator(core.users, securityConfig))
      .map(authenticator => new HttpApi[F](core, authenticator))
}
