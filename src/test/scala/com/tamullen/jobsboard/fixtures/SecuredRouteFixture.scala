package com.tamullen.jobsboard.fixtures

import cats.data.OptionT
import cats.effect.IO
import com.tamullen.jobsboard.domain.security.Authenticator
import tsec.authentication.*
import tsec.mac.jca.HMACSHA256
import tsec.jws.mac.JWTMac
import com.tamullen.jobsboard.fixtures.UsersFixture
import com.tamullen.jobsboard.domain.user.*
import org.http4s.{AuthScheme, Credentials, Request}
import org.http4s.headers.*
import com.tamullen.jobsboard.domain.security.*

import scala.concurrent.duration.*
import tsec.passwordhashers.PasswordHash
import tsec.passwordhashers.jca.BCrypt

trait SecuredRouteFixture extends UsersFixture {

  val mockedAuthenticator: Authenticator[IO] = {
    // key for hashing
    val key = HMACSHA256.unsafeGenerateKey
    // identity store to retrieve users
    val idStore: IdentityStore[IO, String, User] = (email: String) =>
      if (email == travisEmail) OptionT.pure(Travis)
      else if (email == amberEmail) OptionT.pure(Amber)
      else OptionT.none[IO, User]

    JWTAuthenticator.unbacked.inBearerToken(
      1.day, // expiration of tokesn
      None, // max idle time (optional)
      idStore, // identity store
      key // hash key
    )
  }

  extension (r: Request[IO])
    def withBearerToken(a: JwtToken): Request[IO] =
      r.putHeaders {
        val jwtString = JWTMac.toEncodedString[IO, HMACSHA256](a.jwt)
        // Authroization: Bearer {JWT}
        Authorization(Credentials.Token(AuthScheme.Bearer, jwtString))
      }
  given securedHandler: SecuredHandler[IO] = SecuredRequestHandler(mockedAuthenticator)
}
