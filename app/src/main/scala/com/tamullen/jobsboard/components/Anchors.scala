package com.tamullen.jobsboard.components

import tyrian.http.*
import tyrian.*
import tyrian.Html.*

import com.tamullen.jobsboard.core.*
import com.tamullen.jobsboard.*

object Anchors {
  def renderSimpleNavLink(text: String, location: String, cssClass: String = "") = {
    renderNavLink(text, location, cssClass)(Router.ChangeLocation(_))
  }

  def renderNavLink(text: String, location: String, cssClass: String = "")(
      location2msg: String => App.Msg
  ) = {
    a(
      href    := location,
      `class` := cssClass,
      onEvent(
        "click",
        e => {
          e.preventDefault() // native JS - prevent reloading the page.
          location2msg(location)
        }
      )
    )(text)
  }
}
