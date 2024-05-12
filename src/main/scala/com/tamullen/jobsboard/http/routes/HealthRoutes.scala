package com.tamullen.jobsboard.http.routes

import cats.Monad
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.*
import org.http4s.server.Router


class HealthRoutes[F[_] : Monad] extends Http4sDsl[F] {
  val healthRoute: HttpRoutes[F] = {
    HttpRoutes.of[F] {
      case GET -> Root / "health" => Ok("All going great!")
    }
  }

  val routes = Router(
    "/health" -> healthRoute
  )
}

object HealthRoutes {
  def apply[F[_]: Monad] = new HealthRoutes[F]
}
