package com.tamullen.jobsboard.pages

import cats.effect.IO
import tyrian.*
import tyrian.Html.*

case class LoginPage() extends Page {
  override def initCmd: Cmd[IO, Page.Msg] = Cmd.None // TODO

  override def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) = (this, Cmd.None) // TODO

  override def view(): Html[Page.Msg] =
    div("Login Page - TODO")
}
