package com.tamullen.jobsboard.pages

import cats.effect.IO
import tyrian.*
import tyrian.Html.*
import com.tamullen.jobsboard.*
import com.tamullen.jobsboard.common.Constants

final case class NotFoundPage() extends Page {
  override def initCmd: Cmd[IO, App.Msg] = Cmd.None

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = (this, Cmd.None)

  override def view(): Html[App.Msg] =
    div(`class` := "row")(
      // title: Sign up
      div(`class` := "col-md-5 p-0")(
        div(`class` := "logo")(
          img(src   := Constants.logoImage)
        )
      ),
      div(`class` := "col-md-7")(
        div(`class` := "form-section")(
          div(`class` := "top-section")(
            h1("🤦 Ouch!"),
            div("This page doesn't exist.")
          )
        )
      )
    )
}
