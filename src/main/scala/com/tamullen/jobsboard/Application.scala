package com.tamullen.jobsboard

import cats.Monad
import cats.effect.*
import cats.effect.IO
import cats.implicits.*
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
import pureconfig.error.ConfigReaderException
import org.http4s.ember.server.EmberServerBuilder
import com.tamullen.jobsboard.config._
import com.tamullen.jobsboard.config.syntax._
import com.tamullen.jobsboard.modules.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
// import com.tamullen.jobsboard.http.routes.JobRoutes


object Application extends IOApp.Simple {
//  val configSource = ConfigSource.default.load[EmberConfig]

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]


  override def run: IO[Unit] = ConfigSource.default.loadF[IO, AppConfig].flatMap {
    case AppConfig(postgresConfig, emberConfig, securityConfig) =>
      val appResource = for {
        xa <- Database.makePostgresResource[IO](postgresConfig)
        core <- Core[IO](xa)
        httpApi <- HttpApi[IO](core, securityConfig)
        server <- EmberServerBuilder
          .default[IO]
          .withHost(emberConfig.host)
          .withPort(emberConfig.port)
          .withHttpApp(httpApi.endpoints.orNotFound)
          .build
      } yield server
      appResource.use(_ => IO.println("Testing Routes!") *> IO.never)
  }
}
