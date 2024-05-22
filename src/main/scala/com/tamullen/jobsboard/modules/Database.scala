package com.tamullen.jobsboard.modules

import cats.effect._

import doobie.hikari.HikariTransactor
import doobie._
import doobie.util._

import com.tamullen.config.PostgresConfig

object Database {
  def makePostgresResource[F[_] : Async](config: PostgresConfig): Resource[F, HikariTransactor[F]] = for {
    ec <- ExecutionContexts.fixedThreadPool(config.nThreads)
    xa <- HikariTransactor.newHikariTransactor[F](
      "org.postgresql.Driver",
      config.url,
      config.user,
      config.pass,
      ec
    )
  } yield xa
}
