package com.tamullen.jobsboard.components

import tyrian.*
import tyrian.Html.*
import com.tamullen.jobsboard.App

import scala.scalajs.js.Date

object Footer {
  def view(): Html[App.Msg] =
    div(`class` := "footer")(
      p(
        text("Written in "),
        a(href := "https://scala-lang.org", target := "blank")("Scala"),
        text(" with ♥ at "),
        a(href := "https://rockthejvm.com", target := "blank")("Rock the JVM")
      ),
      p(s"© tamullen ${new Date().getFullYear()}, don't copy me!")
    )
}
