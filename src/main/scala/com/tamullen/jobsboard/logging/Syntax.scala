package com.tamullen.jobsboard.logging

import cats._
import org.typelevel.log4cats.Logger
import cats.implicits._

object Syntax {
  extension [F[_], E, A](fa: F[A])(using me: MonadError[F, E], logger: Logger[F])
    def log(success: A => String, error: E => String): F[A] = fa.attemptTap {
      case Left(e) => logger.error(error(e))
      case Right(a) => logger.info(success(a))
    } // F[Either[E, A]]

    def logError(error: E => String): F[A] = fa.attemptTap {
      case Left(e) => logger.error(error(e))
      case Right(_) => ().pure[F]
    }
}
