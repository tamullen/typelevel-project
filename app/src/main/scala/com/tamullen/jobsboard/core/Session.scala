package com.tamullen.jobsboard.core

import tyrian.*
import tyrian.http.*
import cats.effect.IO
import tyrian.cmds.Logger
import com.tamullen.jobsboard.*
import com.tamullen.jobsboard.common.{Constants, Endpoint}
import com.tamullen.jobsboard.pages.Page
import org.scalajs.dom.document
import tyrian.http.Method.Post

import io.circe.Encoder
import io.circe.generic.auto.*
import io.circe.syntax.*

import scala.scalajs.js.Date

final case class Session(email: Option[String] = None, token: Option[String] = None) {
  import Session.*

  def update(msg: Msg): (Session, Cmd[IO, App.Msg]) = msg match {
    case SetToken(e, t, isNewUser) =>
      val cookieCmd = Commands.setAllSessionCookies(e, t, true)
      val routingCmd =
        if (isNewUser) Cmd.emit(Router.ChangeLocation(Page.Urls.HOME)) // new user
        else Commands.checkToken // check whether token is still valid on the server

      (
        this.copy(email = Some(e), token = Some(t)),
        cookieCmd |+| routingCmd
      )
    // check token
    case CheckToken =>
      (this, Commands.checkToken)
    case KeepToken =>
      (this, Cmd.None)
    case InvalidateToken =>
      (this.copy(email = None, token = None), Commands.clearAllSessionCookies())
    // logout action
    case Logout =>
      val cmd = token.map(_ => Commands.logout).getOrElse(Cmd.None)
      (this, cmd)
    case LogoutSuccess =>
      (
        this.copy(email = None, token = None),
        // trigger an AUTHORIZED http request
        Commands.clearAllSessionCookies() |+| Cmd.Emit(Router.ChangeLocation(Page.Urls.HOME))
      )

  }

  def initCmd: Cmd[IO, Msg] = {
    val maybeCommand = for {
      email <- getCookie(Constants.cookies.email)
      token <- getCookie(Constants.cookies.token)
    } yield Cmd.emit(SetToken(email, token, isNewUser = false))

    maybeCommand.getOrElse(Cmd.None)
  }
}

object Session {
  trait Msg                                                                     extends App.Msg
  case class SetToken(email: String, token: String, isNewUser: Boolean = false) extends Msg
  // check the token
  case object CheckToken      extends Msg
  case object KeepToken       extends Msg
  case object InvalidateToken extends Msg
  // logout action
  case object Logout        extends Msg
  case object LogoutSuccess extends Msg
  case object LogoutFailure extends Msg

  def isActive =
    getUserToken().nonEmpty

  def getUserToken() = getCookie(Constants.cookies.token)

  object Endpoints {
    val logout = new Endpoint[Msg] {
      val location                   = Constants.endpoints.logout
      val method                     = Post
      val onSuccess: Response => Msg = _ => LogoutSuccess
      val onError: HttpError => Msg  = _ => LogoutFailure
    }
    val checkToken = new Endpoint[Msg] {
      override val location: String = Constants.endpoints.checkToken
      override val method: Method   = Method.Get
      override val onSuccess: Response => Msg = response =>
        response.status match {
          case Status(200, _) => KeepToken
          case _              => InvalidateToken
        }
      override val onError: HttpError => Msg = _ => InvalidateToken
    }
  }

  object Commands {
    def logout: Cmd[IO, Msg] =
      Endpoints.logout.callAuthorized()
    def checkToken: Cmd[IO, Msg] =
      Logger.consoleLog[IO]("Checking Token")
      Endpoints.checkToken.callAuthorized()

    def setSessionCookie(name: String, value: String, isFresh: Boolean): Cmd[IO, Msg] =
      Cmd.SideEffect[IO] {
        if (getCookie(name).isEmpty || isFresh)
          document.cookie =
            s"$name=$value;expires=${new Date(Date.now() + Constants.cookies.duration)};path=/"
      }

    def setAllSessionCookies(
        email: String,
        token: String,
        isFresh: Boolean = false
    ): Cmd[IO, Msg] =
      setSessionCookie(Constants.cookies.email, email, isFresh) |+|
        setSessionCookie(Constants.cookies.token, token, isFresh)

    def clearSessionCookie(name: String): Cmd[IO, Msg] =
      Cmd.SideEffect[IO] {
        document.cookie = s"$name=;expires=${new Date(0)};path=/"
      }

    def clearAllSessionCookies(): Cmd[IO, Msg] =
      clearSessionCookie(Constants.cookies.email) |+|
        clearSessionCookie(Constants.cookies.token)

  }

  private def getCookie(name: String): Option[String] =
    document.cookie
      .split(";")
      .map(_.trim)
      .find(_.startsWith(s"$name=")) // option of "name=value"
      .map(_.split("="))
      .map(_(1))
}
