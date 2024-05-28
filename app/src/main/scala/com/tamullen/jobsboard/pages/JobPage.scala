package com.tamullen.jobsboard.pages

import cats.effect.IO
import tyrian.*
import tyrian.Html.*

case class JobPage(id: String) extends Page {
  override def initCmd: Cmd[IO, Page.Msg] = Cmd.None // TODO

  override def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) = (this, Cmd.None) // TODO

  override def view(): Html[Page.Msg] =
    div(s"Individual Job Page for $id - TODO")
}
