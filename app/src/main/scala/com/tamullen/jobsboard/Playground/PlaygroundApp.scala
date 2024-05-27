package com.tamullen.jobsboard.Playground

import cats.effect.*
import org.scalajs.dom.*
import tyrian.*
import tyrian.Html.*
import tyrian.cmds.Logger

import scala.concurrent.duration.*
import scala.scalajs.js.annotation.*

object PlaygroundApp {
  sealed trait Msg
  case class Increment(amount: Int) extends Msg

  case class Model(count: Int)
}

@JSExportTopLevel("RockTheJvmApp")
class PlaygroundApp extends TyrianApp[PlaygroundApp.Msg, PlaygroundApp.Model] {
  import PlaygroundApp.*
  // ^^ message     ^^ model = "state"
  /*
    we can send messages by
      - trigger a command
      - create a subscription (listen for things)
      - listening for an event
   */
  override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) =
    (Model(0), Cmd.None)

  // potentially endless stream of messages..
  override def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.every[IO](1.second).map(_ => Increment(1))

  // model can change by recieving messages.
  // model => message => (new model, ______)
  // update triggered whenever we get a new message
  override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) =
    case Increment(amount) =>
      (
        model.copy(count = model.count + amount),
        Logger.consoleLog[IO](s"Changing count by $amount")
      )

  // triggered whenever the model changes.
  override def view(model: Model): Html[Msg] =
    div(
      button(onClick(Increment(1)))("increase"),
      button(onClick(Increment(-1)))("decrease"),
      div(s"Tyrian running: ${model.count}")
    )

}
