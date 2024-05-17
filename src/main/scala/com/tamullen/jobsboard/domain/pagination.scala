package com.tamullen.jobsboard.domain

import pureconfig.ConfigSource
import cats._
import cats.effect._
import com.tamullen.config.PaginationConfig


import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException
import pureconfig.error.ConfigReaderException

import com.tamullen.config.*
import com.tamullen.config.syntax.*
import com.tamullen.jobsboard.modules._

object pagination {
  final case class Pagination(limit: Int, offset: Int)

  object Pagination {
    def apply(maybeLimit: Option[Int], maybeOffset: Option[Int])(pages: PaginationConfig) = {
        new Pagination(maybeLimit.getOrElse(pages.nPages), maybeOffset.getOrElse(0))
    }
  }
}


