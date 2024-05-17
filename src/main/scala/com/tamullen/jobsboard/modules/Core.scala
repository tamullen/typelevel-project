package com.tamullen.jobsboard.modules

import cats.effect._
import cats.implicits._

import doobie._
import doobie.hikari.HikariTransactor
import doobie.util._

import com.tamullen.jobsboard.core._
import com.tamullen.jobsboard.core.Jobs

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

final class Core[F[_]] private (val jobs: Jobs[F]) {

}

// postgres -> jobs -> core -> httpApi -> app
given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

object Core {
  def apply[F[_]: Async: Logger](xa: Transactor[F]): Resource[F, Core[F]] = {
    Resource.Eval(LiveJobs[F](xa))
      .map(jobs => new Core(jobs))
  }
}
