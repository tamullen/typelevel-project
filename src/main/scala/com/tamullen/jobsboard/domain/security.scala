package com.tamullen.jobsboard.domain

import tsec.authentication._
import tsec.mac.jca.HMACSHA256

import com.tamullen.jobsboard.domain.user._

object security {
  type Crypto = HMACSHA256
  type JwtToken = AugmentedJWT[Crypto, String]
  type Authenticator[F[_]] = JWTAuthenticator[F, String, User, Crypto]

}
