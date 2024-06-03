package com.tamullen.jobsboard.components

import tyrian.*
import tyrian.Html.*
import tyrian.http.*
import cats.effect.IO

trait Component[Msg, +Model] {
  def initCmd: Cmd[IO, Msg]

  // update
  def update(msg: Msg): (Model, Cmd[IO, Msg])

  def view(): Html[Msg]
}
