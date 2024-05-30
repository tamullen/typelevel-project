package com.tamullen.jobsboard.domain

object pagination {
  final case class Pagination(limit: Int, offset: Int)

  object Pagination {

    def apply(maybeLimit: Option[Int], maybeOffset: Option[Int]) = {
      new Pagination(maybeLimit.getOrElse(20), maybeOffset.getOrElse(0))
    }

    def default =
      new Pagination(limit = 20, 0)
  }
}
