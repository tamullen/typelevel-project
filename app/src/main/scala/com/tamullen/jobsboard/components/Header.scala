package com.tamullen.jobsboard.components
import tyrian._
import tyrian.Html._
import com.tamullen.jobsboard.core._
import com.tamullen.jobsboard.App._
import scala.scalajs.js
import scala.scalajs.js.annotation._

object Header {
  // Public API
  def view(model: Model): Html[Msg] =
    div(`class` := "header-container")(
      renderLogo(),
      div(`class` := "header-nav")(
        ul(`class` := "header-links")(
          renderNavLink("Jobs", "/jobs"),
          renderNavLink("Login", "/login"),
          renderNavLink("Sign Up", "/signup")
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

  private def renderNavLink(text: String, location: String) = {
    li(`class` := "nav-item")(
      a(
        href    := location,
        `class` := "nav-link",
        onEvent(
          "click",
          e => {
            e.preventDefault() // native JS - prevent reloading the page.
            Router.ChangeLocation(location)
          }
        )
      )(text)
    )
  }

}
