package com.tamullen.jobsboard.pages

import cats.effect.IO
import io.circe.syntax.*
import io.circe.generic.auto._
import io.circe.parser.*
import com.tamullen.jobsboard.common.*
import com.tamullen.jobsboard.domain.auth.*
import tyrian.*
import tyrian.Html.*
import tyrian.cmds.Logger
import tyrian.http.*
import com.tamullen.jobsboard.*

// form
/*
    inputs
      - email
      - password
      - confirm password
      - first name
      - last name
      - company
    button - trigger a sign up
 */
final case class SignUpPage(
    email: String = "",
    password: String = "",
    confirmPassword: String = "",
    firstName: String = "",
    lastName: String = "",
    company: String = "",
    status: Option[Page.Status] = None
) extends FormPage("Sign Up", status) {
  import SignUpPage._

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
    case UpdateEmail(email)              => (this.copy(email = email), Cmd.None)
    case UpdatePassword(password)        => (this.copy(password = password), Cmd.None)
    case UpdateConfirmPassword(password) => (this.copy(confirmPassword = password), Cmd.None)
    case UpdateFirstName(name)           => (this.copy(firstName = name), Cmd.None)
    case UpdateLastName(name)            => (this.copy(lastName = name), Cmd.None)
    case UpdateCompany(company)          => (this.copy(company = company), Cmd.None)
    case AttemptSignUp =>
      if (!email.matches(Constants.emailRegex))
        (setErrorStatus("Email is invalid"), Cmd.None)
      else if (password.isEmpty)
        (setErrorStatus("Please enter a password"), Cmd.None)
      else if (password != confirmPassword)
        (setErrorStatus("Password fields do not match"), Cmd.None)
      else
        (
          this,
          Commands.signup(
            NewUserInfo(
              email,
              password,
              Option(firstName).filter(_.nonEmpty),
              Option(lastName).filter(_.nonEmpty),
              Option(company).filter(_.nonEmpty)
            )
          )
        )
    case SignUpError(message) =>
      (setErrorStatus(message), Cmd.None)
    case SignUpSuccess(message) =>
      (setSuccessStatus(message), Cmd.None)
    case _ => (this, Cmd.None)
  }

  override protected def renderFormContent(): List[Html[App.Msg]] = List(
    // 6 inputs
    renderInput("Email", "email", "text", true, UpdateEmail(_)),
    renderInput("Password", "password", "password", true, UpdatePassword(_)),
    renderInput(
      "Confirm Password",
      "cPassword",
      "password",
      true,
      UpdateConfirmPassword(
        _
      )
    ),
    renderInput("First Name", "firstName", "text", false, UpdateFirstName(_)),
    renderInput("Last Name", "lastName", "text", false, UpdateLastName(_)),
    renderInput("Company", "company", "text", false, UpdateCompany(_)),
    // button
    button(`type` := "button", onClick(AttemptSignUp))("Sign Up")
  )

  /////////////////////////////////////////////////////////////////////////////////////
  // Private
  /////////////////////////////////////////////////////////////////////////////////////

  // util
  def setErrorStatus(message: String): Page =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  def setSuccessStatus(message: String): Page =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))
}

object SignUpPage {
  trait Msg                                             extends App.Msg
  case class UpdateEmail(email: String)                 extends Msg
  case class UpdatePassword(pass: String)               extends Msg
  case class UpdateConfirmPassword(confirmPass: String) extends Msg
  case class UpdateFirstName(name: String)              extends Msg
  case class UpdateLastName(lName: String)              extends Msg
  case class UpdateCompany(company: String)             extends Msg
  // actions
  case object AttemptSignUp extends Msg
  case object NoOp          extends Msg
  // statuses
  case class SignUpError(message: String)   extends Msg
  case class SignUpSuccess(message: String) extends Msg

  object Endpoints {
    val signUp = new Endpoint[Msg] {
      val location = Constants.endpoints.signup
      val method   = Method.Post
      val onResponse: Response => Msg = response =>
        response.status match {
          case Status(201, _) =>
            SignUpSuccess("Success! Log in now.")
          case Status(s, _) if s >= 400 && s < 500 =>
            val json   = response.body
            val parsed = parse(json).flatMap(json => json.hcursor.get[String]("error"))
            parsed match {
              case Left(e)  => SignUpError(s"Error: ${e.getMessage}")
              case Right(e) => SignUpError(e)
            }
        }

      val onError: HttpError => Msg =
        e => SignUpError(e.toString)
    }
  }
  object Commands {
    def signup(newUserInfo: NewUserInfo): Cmd[IO, Msg] = {
      Endpoints.signUp.call(newUserInfo)
    }
  }
}
