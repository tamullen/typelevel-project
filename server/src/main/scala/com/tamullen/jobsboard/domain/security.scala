package com.tamullen.jobsboard.domain

import cats.*
import cats.implicits.*
import cats.effect.*
import tsec.authentication.*
import tsec.mac.jca.HMACSHA256
import com.tamullen.jobsboard.domain.user.*
import com.tamullen.jobsboard.domain.security.*
import com.tamullen.jobsboard.domain.user.Role.{ADMIN, RECRUITER}
import org.http4s.{Response, Status}
import tsec.authorization.BasicRBAC
import tsec.authorization.AuthorizationInfo

object security {
  type Crypto               = HMACSHA256
  type JwtToken             = AugmentedJWT[Crypto, String]
  type Authenticator[F[_]]  = JWTAuthenticator[F, String, User, Crypto]
  type AuthRoute[F[_]]      = PartialFunction[SecuredRequest[F, User, JwtToken], F[Response[F]]]
  type SecuredHandler[F[_]] = SecuredRequestHandler[F, String, User, JwtToken]
  type AuthRBAC[F[_]]       = BasicRBAC[F, Role, User, JwtToken]

  object SecuredHandler {
    def apply[F[_]](using handler: SecuredHandler[F]): SecuredHandler[F] = handler
  }

  // Role based access control
  // BasicRBAC[F, Role, User, JwtToken]
  given authRole[F[_]: Applicative]: AuthorizationInfo[F, Role, User] with {
    override def fetchInfo(u: User): F[Role] = u.role.pure[F]
  }

  def recruiterOnly[F[_]: MonadThrow]: AuthRBAC[F] =
    BasicRBAC(RECRUITER)

  def allRoles[F[_]: MonadThrow]: AuthRBAC[F] =
    BasicRBAC.all[F, Role, User, JwtToken]

  def adminOnly[F[_]: MonadThrow]: AuthRBAC[F] =
    BasicRBAC(ADMIN)

  // authorization
  case class Authorizations[F[_]](rbacRoutes: Map[AuthRBAC[F], List[AuthRoute[F]]])
  object Authorizations {
    // 3. Semigroup for Authorization
    given combiner[F[_]]: Semigroup[Authorizations[F]] = Semigroup.instance { (authA, authB) =>
      Authorizations(authA.rbacRoutes |+| authB.rbacRoutes)
    }
  }

  // AuthRoute -> Authorizations -> TSecAuthService -> HttpRoute
  // 1. AuthRoute -> Authorization = .restrictedTo extension method
  extension [F[_]](authRoute: AuthRoute[F])
    def restrictedTo(rbac: AuthRBAC[F]): Authorizations[F] =
      Authorizations(Map(rbac -> List(authRoute)))

  // 2. Authorizations -> TSecAuthService = implicit conversion
  given auth2tsec[F[_]: Monad]
      : Conversion[Authorizations[F], TSecAuthService[User, JwtToken, F]] = { authz =>
    {
      // this responds with 401 always
      val unauthorizedService: TSecAuthService[User, JwtToken, F] =
        TSecAuthService[User, JwtToken, F] { _ =>
          Response[F](Status.Unauthorized).pure[F]
        }

//      val rbac: AuthRBAC[F] = ???
//      val authRoute: AuthRoute[F] = ???
//      val tsec = TSecAuthService.withAuthorizationHandler(rbac)(authRoute, unauthorizedService)
      authz.rbacRoutes // Map[RBAC, List[Routes]]
        .toSeq
        .foldLeft(unauthorizedService) { case (acc, (rbac, routes)) =>
          // merge routes into one.
          val bigRoute = routes.reduce(_.orElse(_))
          // build a new service, fall back the acc if rbac/route fails
          TSecAuthService.withAuthorizationHandler(rbac)(bigRoute, acc.run)
        }
    }
  }

}
