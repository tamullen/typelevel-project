package com.tamullen.jobsboard.pages
import tyrian._
import tyrian.Html._
import cats.effect.IO
import com.tamullen.jobsboard.*

case class JobListPage() extends Page {
  override def initCmd: Cmd[IO, App.Msg] = Cmd.None // TODO

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = (this, Cmd.None) // TODO

  override def view(): Html[App.Msg] =
    div("Job List Page - TODO")
}
