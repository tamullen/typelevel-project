package com.tamullen.jobsboard.pages

import cats.effect.IO
import tyrian.*
import tyrian.Html.*
import com.tamullen.jobsboard.*

final case class JobPage(id: String) extends Page {
  override def initCmd: Cmd[IO, App.Msg] = Cmd.None // TODO

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = (this, Cmd.None) // TODO

  override def view(): Html[App.Msg] =
    div(s"Individual Job Page for $id - TODO")
}
