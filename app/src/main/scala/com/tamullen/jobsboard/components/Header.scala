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
    // <!--  ================ START NAVBAR =================  -->
    div(`class` := "container-fluid p-0")(
      div(`class` := "jvm-nav")(
        div(`class` := "container")(
          nav(`class` := "navbar navbar-expand-lg navbar-light JVM-nav")(
            div(`class` := "container")(
              renderLogo(),
              button(
                `class` := "navbar-toggler",
                `type`  := "button",
                attribute("data-bs-toggle", "collapse"),
                attribute("data-bs-target", "#navbarNav"),
                attribute("aria-controls", "navbarNav"),
                attribute("aria-expanded", "false"),
                attribute("aria-label", "Toggle navigation")
              )(
                span(`class` := "navbar-toggler-icon")()
              ),
              div(`class` := "collapse navbar-collapse", id := "navbarNav")(
                ul(
                  `class` := "navbar-nav ms-auto menu align-center expanded text-center SMN_effect-3"
                )(
                  renderNavLinks()
                )
              )
            )
          )
        )
      )
    )
//                <a class="navbar-brand" href="index.html">
//                  <img src="img/nav.png" alt="">
//                  </a>
//                  <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav" aria-controls="navbarNav" aria-expanded="false" aria-label="Toggle navigation">
//                    <span class="navbar-toggler-icon"></span>
//                  </button>
//                  <div class="collapse navbar-collapse" id="navbarNav">
//                    <ul class="navbar-nav ms-auto menu align-center expanded text-center SMN_effect-3 ">
//                      <li class="nav-item">
//                        <a class="nav-link jvm-item Home active-item" href="index.html">Home</a>
//
//                      </li>
//                      <li class="nav-item">
//                        <a class="nav-link jvm-item job" href="jobs.html">Jobs</a>
//                      </li>
//                      <li class="nav-item">
//                        <a class="nav-link jvm-item contect-us" href="Contact-us.html">Contact Us</a>
//                      </li>
//                      <li class="nav-item">
//                        <a class="nav-link jvm-iteme" href="#">|</a>
//                      </li>
//                      <li class="nav-item">
//                        <a class="nav-link jvm-item post-a-job" href="Post-a-Job.html">Post A Job</a>
//                      </li>
//                      <!-- start-button   -->
//
//                      <li class="nav-item">
//                        <a class="nav-link " href="signin.html"><button class="btn-register" type="text">Register</button></a>
//                      </li>
//                      <li class="nav-item">
//                        <a class="nav-link " href="login.html"><button class="btn-logout" type="text">Login</button></a>
//                      </li>
//
//                      <!-- End-button   -->
//
//                    </ul>
//                  </div>
//                </div>
//              </nav>
  //  <!--  ================ END NAVBAR =================  -->

  // Private API
  @js.native
  @JSImport("/static/img/fiery-lava 128x128.png", JSImport.Default)
  private val logoImage: String = js.native

  private def renderLogo() =
    a(
      href    := "/",
      `class` := "navbar-brand",
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

  private def renderSimpleNavLink(text: String, location: String) =
    renderNavLink(text, location)(Router.ChangeLocation(_))

  private def renderNavLink(text: String, location: String)(
      location2msg: String => App.Msg
  ) = {
    li(`class` := "nav-item")(
      Anchors.renderNavLink(text, location, "nav-link jvm-item")(
        location2msg
      )
    )
  }

}
