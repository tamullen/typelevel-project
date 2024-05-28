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

object App {
  type Msg = Router.Msg

  case class Model(router: Router)
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
    val (router: Router, cmd: Cmd[IO, Msg]) = Router.startAt(window.location.pathname)
    (Model(router), cmd)
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
      val (newRouter, cmd) = model.router.update(msg)
      (model.copy(router = newRouter), cmd)

  // triggered whenever the model changes.
  override def view(model: Model): Html[Msg] =
    div(
      Header.view(model),
      div(s"You are now at: ${model.router.location}")
    )

}
