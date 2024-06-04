package com.tamullen.jobsboard.pages

import cats.effect.IO
import tyrian.*
import tyrian.Html.*
import tyrian.http.*
import com.tamullen.jobsboard.*
import com.tamullen.jobsboard.common.*
import com.tamullen.jobsboard.components.*
import com.tamullen.jobsboard.domain.auth.ForgotPasswordInfo
import io.circe.generic.auto.*

final case class ForgotPasswordPage(email: String = "", status: Option[Page.Status] = None)
    extends FormPage("Reset Password", status) {
  import ForgotPasswordPage.*

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
    case UpdateEmail(e) =>
      (this.copy(email = e), Cmd.None)
    case AttemptResetPassword =>
      if (!email.matches(Constants.emailRegex))
        (setErrorStatus("Please insert a valid email."), Cmd.None)
      else
        (this, Commands.resetPassword(email))
    case ResetSuccess =>
      (setSuccessStatus("Check your email!"), Cmd.None)
    case ResetFailure(e) =>
      (setErrorStatus(e), Cmd.None)
    case _ => (this, Cmd.None)
  }

  override protected def renderFormContent(): List[Html[App.Msg]] = List(
    renderInput("Email:", "email", "text", true, UpdateEmail(_)),
    button(`type` := "button", onClick(AttemptResetPassword))("Send Email"),
    Anchors.renderSimpleNavLink("Have a token?", Page.Urls.RESET_PASSWORD)
  )

  def setErrorStatus(message: String): Page =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  def setSuccessStatus(message: String): Page =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))
}

object ForgotPasswordPage {
  trait Msg                              extends App.Msg
  case class UpdateEmail(email: String)  extends Msg
  case object AttemptResetPassword       extends Msg
  case class ResetFailure(error: String) extends Msg
  case object ResetSuccess               extends Msg

  object Endpoints {
    val resetPassword = new Endpoint[Msg] {
      override val location: String            = Constants.endpoints.forgotPassword
      override val method: Method              = Method.Post
      override val onError: HttpError => Msg   = e => ResetFailure(e.toString)
      override val onResponse: Response => Msg = _ => ResetSuccess
    }
  }

  object Commands {
    def resetPassword(email: String) =
      Endpoints.resetPassword.call(ForgotPasswordInfo(email))
  }
}
