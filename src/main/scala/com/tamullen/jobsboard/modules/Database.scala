package com.tamullen.jobsboard.modules

import cats.effect.*
import com.tamullen.jobsboard.config.PostgresConfig
import doobie.hikari.HikariTransactor
import doobie.*
import doobie.util.*

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
