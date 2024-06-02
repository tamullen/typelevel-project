package com.tamullen.jobsboard.pages
import cats.effect.IO
import com.tamullen.jobsboard.App
import tyrian.Html.*
import tyrian.*
import com.tamullen.jobsboard.common.*
import com.tamullen.jobsboard.domain.auth.{NewPasswordInfo, RecoverPasswordInfo}
import com.tamullen.jobsboard
import com.tamullen.jobsboard.core.Session
import io.circe.*
import io.circe.generic.auto.*
import laika.bundle.BundleOrigin.User
import tyrian.http.*
import io.circe.parser.*
import io.circe.generic.auto.*

final case class ProfilePage(
    oldPassword: String = "",
    newPassword: String = "",
    status: Option[Page.Status] = None
) extends FormPage("Profile", status) {
  import ProfilePage.*

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
    case UpdateOldPassword(p) =>
      (this.copy(oldPassword = p), Cmd.None)
    case UpdateNewPassword(p) =>
      (this.copy(newPassword = p), Cmd.None)
    case AttemptChangePassword =>
      (this, Commands.changePassword(oldPassword, newPassword))
    case ChangePasswordError(e) => (setErrorStatus(e), Cmd.None)
    case ChangePasswordSuccess =>
      (setSuccessStatus("Success!"), Cmd.None)
    case _ =>
      (this, Cmd.None)
  }

  override def view(): Html[App.Msg] =
    if (Session.isActive) super.view()
    else renderInvalidPage

  override protected def renderFormContent(): List[Html[App.Msg]] = List(
    renderInput("Old Password", "oldPassword", "password", true, UpdateOldPassword(_)),
    renderInput("New Password", "newPassword", "password", true, UpdateNewPassword(_)),
    button(`type` := "button", onClick(AttemptChangePassword))("Change Password")
  )

  ////////////////////////////////////////////////////////////////////////////
  // Private
  ////////////////////////////////////////////////////////////////////////////

  // UI
  private def renderInvalidPage =
    div(
      h1("Profile"),
      div("Ouch! It seems you're not logged in yet.")
    )
  // util
  private def setErrorStatus(message: String): Page =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  private def setSuccessStatus(message: String): Page =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.SUCCESS)))
}

object ProfilePage {
  trait Msg                                      extends App.Msg
  case class UpdateOldPassword(password: String) extends Msg
  case class UpdateNewPassword(password: String) extends Msg
  case class ChangePasswordError(error: String)  extends Msg
  case object ChangePasswordSuccess              extends Msg
  case object AttemptChangePassword              extends Msg

  object Endpoints {
    val changePassword = new Endpoint[Msg] {
      override val location: String          = Constants.endpoints.changePassword
      override val method: Method            = Method.Put
      override val onError: HttpError => Msg = e => ChangePasswordError(e.toString)
      override val onResponse: Response => Msg = response =>
        response.status match {
          case Status(200, _) => ChangePasswordSuccess
          case Status(404, _) => ChangePasswordError("Funny. Server says this user doesn't exist.")
          case Status(s, _) if s >= 400 && s < 500 => ChangePasswordError("Invalid credentials")
          case _ => ChangePasswordError("Unknown reply from server.")
        }
    }
  }

  object Commands {
    def changePassword(oldPassword: String, newPassword: String): Cmd[IO, Msg] = {
      Endpoints.changePassword.callAuthorized(NewPasswordInfo(oldPassword, newPassword))
    }
  }

}
