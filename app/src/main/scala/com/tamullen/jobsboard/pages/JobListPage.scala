package com.tamullen.jobsboard.pages
import tyrian._
import tyrian.Html._
import cats.effect.IO

case class JobListPage() extends Page {
  override def initCmd: Cmd[IO, Page.Msg] = Cmd.None // TODO

  override def update(msg: Page.Msg): (Page, Cmd[IO, Page.Msg]) = (this, Cmd.None) // TODO

  override def view(): Html[Page.Msg] =
    div("Job List Page - TODO")
}
