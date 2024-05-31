package com.tamullen.jobsboard.pages

import cats.effect.IO
import io.circe.syntax.*
import io.circe.generic.auto.*
import io.circe.parser.*
import com.tamullen.jobsboard.*
import com.tamullen.jobsboard.common.*
import com.tamullen.jobsboard.core.Session
import com.tamullen.jobsboard.domain.auth.*
import tyrian.*
import tyrian.Html.*
import tyrian.cmds.Logger
import tyrian.http.*
import tyrian.http.Method.Post
/*
  form
    -email
    -password
    -button
  status (success or failure)
 */
case class LoginPage(email: String = "", password: String = "", status: Option[Page.Status] = None)
    extends Page {
  import LoginPage.*
  override def initCmd: Cmd[IO, App.Msg] = Cmd.None // TODO

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
    case UpdateEmail(email)       => (this.copy(email = email), Cmd.None)
    case UpdatePassword(password) => (this.copy(password = password), Cmd.None)
    case AttemptLogin =>
      if (!email.matches(Constants.emailRegex))
        (setErrorStatus("Invlaid Email"), Cmd.None)
      else if (password.isEmpty)
        (setErrorStatus("Please enter a password"), Cmd.None)
      else (this, Commands.login(LoginInfo(email, password)))
    case LoginError(error) =>
      (setErrorStatus(error), Cmd.None)
    case LoginSuccess(token) =>
      (setSuccessStatus("Success!"), Cmd.emit(Session.SetToken(email, token, isNewUser = true)))
    //                                      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ should be App.Msg,
    //                                      so had each Msg replaced with App.Msg and extended
    //                                      App.Msg in each Msg trait.
    case _ => (this, Cmd.None)
  }

  override def view(): Html[App.Msg] =
    div(`class` := "form-section")(
      // title: Sign up
      div(`class` := "top-section")(
        h1("Log In")
      ),
      // form
      form(
        name    := "signin",
        `class` := "form",
        onEvent(
          "submit",
          e => {
            e.preventDefault()
            NoOp
          }
        )
      )(
        renderInput("Email", "email", "text", true, UpdateEmail(_)),
        renderInput("Password", "password", "password", true, UpdatePassword(_)),
        button(`type` := "button", onClick(AttemptLogin))("Log In")
      ),
      status.map(s => div(s.message)).getOrElse(div())
    )

  /////////////////////////////////////////////////////////////////////////////////////
  // Private
  /////////////////////////////////////////////////////////////////////////////////////

  // UI
  private def renderInput(
      name: String,
      uid: String,
      kind: String,
      isRequired: Boolean,
      onChange: String => Msg
  ) =
    div(`class` := "form-input")(
      label(`for` := name, `class` := "form-label")(
        if (isRequired) span("*") else span(),
        text(name)
      ),
      input(`type` := kind, `class` := "form-control", id := uid, onInput(onChange))
    )

  // util
  def setErrorStatus(message: String): Page =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  def setSuccessStatus(message: String): Page =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))
}

object LoginPage {
  trait Msg                               extends App.Msg
  case class UpdateEmail(email: String)   extends Msg
  case class UpdatePassword(pass: String) extends Msg
  // actions
  case object AttemptLogin extends Msg
  case object NoOp         extends Msg
  // status/results
  case class LoginError(error: String)   extends Msg
  case class LoginSuccess(token: String) extends Msg

  object Endpoints {
    val login = new Endpoint[Msg] {
      override val location: String = Constants.endpoints.login
      override val method: Method   = Post
      override val onError: HttpError => Msg =
        e => LoginError(e.toString)
      override val onSuccess: Response => Msg = response => {
        val maybeToken = response.headers.get("authorization")
        maybeToken match {
          case Some(token) => LoginSuccess(token)
          case None        => LoginError("Invalid username or password")
        }
      }
    }
  }

  object Commands {
    def login(loginInfo: LoginInfo) =
      Endpoints.login.call(loginInfo)
  }
}
