package com.tamullen.jobsboard.components
import tyrian._
import tyrian.Html._
import com.tamullen.jobsboard.core._
import com.tamullen.jobsboard.App._
import scala.scalajs.js
import scala.scalajs.js.annotation._
import com.tamullen.jobsboard.pages._
import com.tamullen.jobsboard.*

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
      renderSimpleNavLink("Jobs", Page.Urls.JOBS),
      renderSimpleNavLink("Post Job", Page.Urls.POST_JOB)
    )

    val unauthedLinks = List(
      renderSimpleNavLink("Login", Page.Urls.LOGIN),
      renderSimpleNavLink("Sign Up", Page.Urls.SIGNUP)
    )

    val authedLinks = List(
      renderSimpleNavLink("Profile", Page.Urls.PROFILE),
      renderNavLink("Log out", Page.Urls.HASH)(_ => Session.Logout)
    )

    constantLinks ++ (
      if (Session.isActive) authedLinks
      else unauthedLinks
    )
  }

  private def renderSimpleNavLink(text: String, location: String) = {
    renderNavLink(text, location)(Router.ChangeLocation(_))
  }

  private def renderNavLink(text: String, location: String)(location2msg: String => App.Msg) = {
    li(`class` := "nav-item")(
      a(
        href    := location,
        `class` := "nav-link",
        onEvent(
          "click",
          e => {
            e.preventDefault() // native JS - prevent reloading the page.
            location2msg(location)
          }
        )
      )(text)
    )
  }
}
