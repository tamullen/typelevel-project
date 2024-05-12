package com.tamullen.jobsboard

import cats.Monad
import cats.effect.*
import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.*
import cats.*
import com.tamullen.jobsboard.http.routes.HealthRoutes
import org.http4s.server.Router


object Application extends IOApp.Simple {
  /* Objectives
    1 - add a plain health endpoint to our app
    2 - add minimal configuration
    3 - establish basic http server layout
   */

  override def run: IO[Unit] = EmberServerBuilder
    .default[IO]
    .withHttpApp(HealthRoutes[IO].routes.orNotFound)
    .build
    .use(_ => IO.println("Server Ready!") *> IO.never)
}
