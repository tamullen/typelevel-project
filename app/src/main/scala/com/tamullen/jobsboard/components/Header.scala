package com.tamullen.jobsboard.components
import tyrian._
import tyrian.Html._

import scala.scalajs.js
import scala.scalajs.js.annotation._

import com.tamullen.jobsboard.pages._
import com.tamullen.jobsboard.core._
import com.tamullen.jobsboard.App._
import com.tamullen.jobsboard.*
import com.tamullen.jobsboard.components.*

object Header {
  // Public API
  def view(model: Model): Html[Msg] =
    div(`class` := "header-container")(
      renderLogo(),
      div(`class` := "header-nav")(
        ul(`class` := "header-links")(
          renderNavLinks()
        )
      )
    )

  // Private API
  @js.native
  @JSImport("/static/img/fiery-lava 128x128.png", JSImport.Default)
  private val logoImage: String = js.native

  private def renderLogo() =
    a(
      href := "/",
//      `class` := "nav-link",
      onEvent(
        "click",
        e => {
          e.preventDefault() // native JS - prevent reloading the page.
          Router.ChangeLocation("/")
        }
      )
    )(
      img(`class` := "home-logo", src := logoImage, alt := "RockTheJVM")
    )

  private def renderNavLinks(): List[Html[App.Msg]] = {
    val constantLinks = List(
      Anchors.renderSimpleNavLink("Jobs", Page.Urls.JOBS),
      Anchors.renderSimpleNavLink("Post Job", Page.Urls.POST_JOB)
    )

    val unauthedLinks = List(
      Anchors.renderSimpleNavLink("Login", Page.Urls.LOGIN),
      Anchors.renderSimpleNavLink("Sign Up", Page.Urls.SIGNUP)
    )

    val authedLinks = List(
      Anchors.renderSimpleNavLink("Profile", Page.Urls.PROFILE),
      Anchors.renderNavLink("Log out", Page.Urls.HASH)(_ => Session.Logout)
    )

    constantLinks ++ (
      if (Session.isActive) authedLinks
      else unauthedLinks
    )
  }

}
