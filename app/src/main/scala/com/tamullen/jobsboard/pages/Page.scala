package com.tamullen.jobsboard.pages

import tyrian._
import cats.effect.IO
import com.tamullen.jobsboard.*

object Page {
  trait Msg

  enum StatusKind {
    case SUCCESS, ERROR, LOADING
  }
  final case class Status(message: String, kind: StatusKind)

  object Urls {
    val LOGIN            = "/login"
    val SIGNUP           = "/signup"
    val FORGOT_PASSWORD  = "/forgotpassword"
    val RECOVER_PASSWORD = "/recoverpassword"
    val JOBS             = "/jobs"
    val EMPTY            = ""
    val HOME             = "/"
  }

  import Urls._
  def get(location: String) = location match {
    case `LOGIN`                   => LoginPage() // ``must match exactly
    case `SIGNUP`                  => SignUpPage()
    case `FORGOT_PASSWORD`         => ForgotPasswordPage()
    case `RECOVER_PASSWORD`        => RecoverPasswordPage()
    case `EMPTY` | `HOME` | `JOBS` => JobListPage()
    case s"/jobs/$id"              => JobPage(id)
    case _                         => NotFoundPage()
  }
}

abstract class Page {
  // API
  // Send a command upon instantiating.
  def initCmd: Cmd[IO, App.Msg]
  // update
  def update(msg: App.Msg): (Page, Cmd[IO, App.Msg])

  def view(): Html[App.Msg]
}

// login page
// signup page
// recover password page
// forgot password page
// job list page == home page
// individual job page
//
// not found page
