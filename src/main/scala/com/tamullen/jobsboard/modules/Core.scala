package com.tamullen.jobsboard.modules

import cats.effect._
import cats.implicits._
import doobie._
import doobie.hikari.HikariTransactor
import doobie.util._

import com.tamullen.jobsboard.core._
import com.tamullen.jobsboard.core.Jobs

final class Core[F[_]] private (val jobs: Jobs[F]) {

}

// postgres -> jobs -> core -> httpApi -> app
object Core {
  def apply[F[_]: Async](xa: Transactor[F]): Resource[F, Core[F]] = {
    Resource.Eval(LiveJobs[F](xa))
      .map(jobs => new Core(jobs))
  }
}
