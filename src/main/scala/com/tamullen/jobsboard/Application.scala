package com.tamullen.jobsboard

import cats.Monad
import cats.effect.*
import cats.effect.IO
import cats.implicits.*
import pureconfig.ConfigSource
import org.http4s.ember.server.EmberServerBuilder
import pureconfig.error.ConfigReaderException
import com.tamullen.config.*
import com.tamullen.config.syntax.*
import com.tamullen.jobsboard.http.HttpApi
// import com.tamullen.jobsboard.http.routes.JobRoutes


object Application extends IOApp.Simple {
  /* Objectives
    1 - add a plain health endpoint to our app
    2 - add minimal configuration
    3 - establish basic http server layout
   */
  val configSource = ConfigSource.default.load[EmberConfig]
  override def run: IO[Unit] = ConfigSource.default.loadF[IO, EmberConfig].flatMap { config =>
    EmberServerBuilder
      .default[IO]
      .withHost(config.host)
      .withPort(config.port)
      .withHttpApp(HttpApi[IO].endpoints.orNotFound)
      .build
      .use(_ => IO.println("Testing Routes!") *> IO.never)
  }
}
