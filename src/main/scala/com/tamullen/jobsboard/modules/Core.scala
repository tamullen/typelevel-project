package com.tamullen.jobsboard.modules

import cats.effect.*
import cats.implicits.*
import com.tamullen.jobsboard.config.SecurityConfig
import doobie.*
import doobie.hikari.HikariTransactor
import doobie.util.*
import com.tamullen.jobsboard.core.*
import com.tamullen.jobsboard.core.Jobs
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

final class Core[F[_]] private (val jobs: Jobs[F], val users: Users[F], val auth: Auth[F]) {

}

// postgres -> jobs -> core -> httpApi -> app
given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

object Core {
  def apply[F[_]: Async: Logger](xa: Transactor[F]): Resource[F, Core[F]] = {
    val coreF = for {
      jobs <- LiveJobs[F](xa)
      users <- LiveUsers[F](xa)
      auth <- LiveAuth[F](users)
    } yield new Core(jobs, users, auth)

    Resource.eval(coreF)
  }
}
