package com.tamullen.jobsboard

import cats.effect.*

import scala.scalajs.js.annotation.*
import org.scalajs.dom.*
import tyrian.*
import tyrian.Html.*

import scala.concurrent.duration.*
import tyrian.cmds.Logger
import core._
import com.tamullen.jobsboard.components._
import com.tamullen.jobsboard.pages._

object App {
//  type Msg = Router.Msg | Session.Msg | App.Msg
  trait Msg

  case class Model(router: Router, session: Session, page: Page)
  case object NoOp extends Msg
}

@JSExportTopLevel("RockTheJvmApp")
class App extends TyrianApp[App.Msg, App.Model] {
  import App._
  // ^^ message     ^^ model = "state"
  /*
    we can send messages by
      - trigger a command
      - create a subscription (listen for things)
      - listening for an event
   */
  override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = {
    val location                    = window.location.pathname
    val page                        = Page.get(location)
    val pageCmd                     = page.initCmd
    val (router: Router, routerCmd) = Router.startAt(location)
    val session                     = Session()
    val sessionCmd                  = session.initCmd
    (Model(router, session, page), routerCmd |+| sessionCmd |+| pageCmd)
  }

  // potentially endless stream of messages..
  override def subscriptions(model: Model): Sub[IO, Msg] =
    Sub
      .make(
        "urlChange",
        model.router.history.state.discrete
      ) // listener for browser history changes
      .map(_.get)
      .map(newLocation => Router.ChangeLocation(newLocation, true))

  // model can change by recieving messages.
  // model => message => (new model, ______)
  // update triggered whenever we get a new message
  override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    case msg: Router.Msg =>
      val (newRouter, routerCmd) = model.router.update(msg)
      if (model.router == newRouter) // not change is necessary
        (model, Cmd.None)
      else {
        // location changed, need to re-render the appropriate page.
        val newPage    = Page.get(newRouter.location)
        val newPageCmd = newPage.initCmd
        (model.copy(router = newRouter, page = newPage), routerCmd |+| newPageCmd)
      }
    case msg: Session.Msg =>
      val (newSession, cmd) = model.session.update(msg)
      (model.copy(session = newSession), cmd)
    case msg: App.Msg =>
      // update the page
      val (newPage, cmd) = model.page.update(msg)
      (model.copy(page = newPage), cmd)

  // triggered whenever the model changes.
  override def view(model: Model): Html[Msg] =
    div(
      Header.view(model),
      model.page.view(),
      div(model.session.email.getOrElse("Unauthenticated"))
//      div(s"You are now at: ${model.router.location}")
    )

}
