package com.tamullen.jobsboard

import cats.Monad
import cats.effect.*
import cats.effect.IO
import cats.implicits.*
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
import org.http4s.ember.server.EmberServerBuilder
import pureconfig.error.ConfigReaderException
import com.tamullen.config.*
import com.tamullen.config.syntax.*
import com.tamullen.jobsboard.http.HttpApi
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
// import com.tamullen.jobsboard.http.routes.JobRoutes


object Application extends IOApp.Simple {
//  val configSource = ConfigSource.default.load[EmberConfig]

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]


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
