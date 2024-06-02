package com.tamullen.jobsboard.pages
import cats.effect.IO
import cats.syntax.traverse.*
import com.tamullen.jobsboard.App
import com.tamullen.jobsboard.core.Session
import com.tamullen.jobsboard.common.*
import com.tamullen.jobsboard.domain.Job.JobInfo
import tyrian.*
import tyrian.Html.*
import tyrian.http.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.*
import tyrian.cmds.Logger
import org.scalajs.dom.{File, FileReader}
import scala.util.Try

case class PostJobPage(
    company: String = "",
    title: String = "",
    description: String = "",
    externalUrl: String = "",
    remote: Boolean = false,
    location: String = "",
    salaryLo: Option[Int] = None,
    salaryHi: Option[Int] = None,
    currency: Option[String] = None,
    country: Option[String] = None,
    tags: Option[String] = None, // TODO Parse the tags before sending them to the server.
    image: Option[String] = None,
    seniority: Option[String] = None,
    other: Option[String] = None,
    status: Option[Page.Status] = None
) extends FormPage("Post Job", status) {
  import PostJobPage.*

  override def view(): Html[App.Msg] =
    if (Session.isActive) super.view()
    else renderInvalidPage

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
    case UpdateCompany(c) =>
      (this.copy(company = c), Cmd.None)
    case UpdateTitle(t) =>
      (this.copy(title = t), Cmd.None)
    case UpdateDescription(d) =>
      (this.copy(description = d), Cmd.None)
    case UpdateExternalUrl(u) =>
      (this.copy(externalUrl = u), Cmd.None)
    case ToggleRemote =>
      (this.copy(remote = !this.remote), Cmd.None)
    case UpdateLocation(l) =>
      (this.copy(location = l), Cmd.None)
    case UpdateSalaryLo(v) =>
      (this.copy(salaryLo = Some(v)), Cmd.None)
    case UpdateSalaryHi(v) =>
      (this.copy(salaryHi = Some(v)), Cmd.None)
    case UpdateCurrency(c) =>
      (this.copy(currency = Some(c)), Cmd.None)
    case UpdateCountry(c) =>
      (this.copy(country = Some(c)), Cmd.None)
    // text input => "scala, cats, cats effect, akka" - up to us to parse these when we do the http req
    case UpdateImageFile(maybeFile) =>
      (this, Commands.loadFile(maybeFile))
    case UpdateImage(maybeImage) =>
      (this.copy(image = maybeImage), Logger.consoleLog[IO](s"I HAZ IMAGE: " + maybeImage))
    case UpdateTags(t) =>
      (this.copy(tags = Some(t)), Cmd.None)
    case UpdateSeniority(s) =>
      (this.copy(seniority = Some(s)), Cmd.None)
    case UpdateOther(o) =>
      (this.copy(other = Some(o)), Cmd.None)
    case AttemptPostJob =>
      (
        this,
        Commands.postJob(
          company,
          title,
          description,
          externalUrl,
          remote,
          location,
          salaryLo,
          salaryHi,
          currency,
          country,
          tags,
          image,
          seniority,
          other
        )
      )
    case PostJobError(e) =>
      (setErrorStatus(e), Cmd.None)
    case PostJobSuccess(jobId) =>
      (setSuccessStatus(jobId), Logger.consoleLog[IO](s"Posted job with id $jobId"))
    case _ => (this, Cmd.None)
  }

  override protected def renderFormContent(): List[Html[App.Msg]] = List(
    renderInput("Company", "company", "text", true, UpdateCompany(_)),
    renderInput("Title", "title", "text", true, UpdateTitle(_)),
    renderTextArea("Description", "description", true, UpdateDescription(_)),
    renderInput("External Url", "externalUrl", "text", true, UpdateExternalUrl(_)),
    renderInput("Remote", "remote", "checkbox", true, _ => ToggleRemote),
    renderInput("Location", "location", "text", true, UpdateLocation(_)),
    renderInput("Salary Low", "salaryLo", "number", false, s => UpdateSalaryLo(parseNumber(s))),
    renderInput("Salary High", "salaryHi", "number", false, s => UpdateSalaryHi(parseNumber(s))),
    renderInput("Currency", "currency", "text", false, UpdateCurrency(_)),
    renderInput("Country", "country", "text", false, UpdateCountry(_)),
    renderImageUploadInput("Logo", "logo", image, UpdateImageFile(_)),
    renderInput("Tags", "tags", "text", false, UpdateTags(_)),
    renderInput("Seniority", "seniority", "text", false, UpdateSeniority(_)),
    renderInput("Other", "Other", "text", false, UpdateOther(_)),
    button(`type` := "button", onClick(AttemptPostJob))("Post Job")
  )

  // private API
  def renderInvalidPage =
    div(
      h1("Post Job"),
      div("You need to be logged in to post a job.")
    )

  // util
  def setErrorStatus(message: String): Page =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  def setSuccessStatus(message: String): Page =
    this.copy(status = Some(Page.Status("Success!", Page.StatusKind.SUCCESS)))

  private def parseNumber(s: String) =
    Try(s.toInt).getOrElse(0)
}

object PostJobPage {
  trait Msg                                           extends App.Msg
  case class UpdateCompany(company: String)           extends Msg
  case class UpdateTitle(title: String)               extends Msg
  case class UpdateDescription(description: String)   extends Msg
  case class UpdateExternalUrl(url: String)           extends Msg
  case class UpdateSalaryLo(salaryLo: Int)            extends Msg
  case class UpdateLocation(location: String)         extends Msg
  case class UpdateSalaryHi(salaryHi: Int)            extends Msg
  case class UpdateCurrency(currency: String)         extends Msg
  case class UpdateCountry(country: String)           extends Msg
  case class UpdateTags(tags: String)                 extends Msg
  case class UpdateSeniority(seniority: String)       extends Msg
  case class UpdateOther(other: String)               extends Msg
  case class PostJobError(error: String)              extends Msg
  case class PostJobSuccess(jobId: String)            extends Msg
  case class UpdateImageFile(maybeFile: Option[File]) extends Msg
  case class UpdateImage(maybeFile: Option[String])   extends Msg

  // actions
  case object ToggleRemote   extends Msg
  case object AttemptPostJob extends Msg

  object Endpoints {
    val postJob = new Endpoint[Msg] {
      override val location: String          = Constants.endpoints.postJob
      override val method: Method            = Method.Post
      override val onError: HttpError => Msg = e => PostJobError(e.toString)
      override val onResponse: Response => Msg = response =>
        response.status match {
          case Status(s, _) if s >= 200 && s < 300 =>
            val jobId = response.body
            PostJobSuccess(jobId)
          case Status(401, _) =>
            PostJobError("You are not authorized to post a job.")
          case Status(s, _) if s >= 400 && s < 500 =>
            val json   = response.body
            val parsed = parse(json).flatMap(_.hcursor.get[String]("error"))
            parsed match {
              case Left(e)      => PostJobError(s"Error $e")
              case Right(error) => PostJobError(error)
            }
          case _ =>
            PostJobError("Unknown reply from server.")
        }
    }
  }

  object Commands {
    def postJob(
        company: String,
        title: String,
        description: String,
        externalUrl: String,
        remote: Boolean,
        location: String,
        salaryLo: Option[Int],
        salaryHi: Option[Int],
        currency: Option[String],
        country: Option[String],
        tags: Option[String],
        image: Option[String],
        seniority: Option[String],
        other: Option[String]
    ) =
      Endpoints.postJob.callAuthorized(
        JobInfo(
          company,
          title,
          description,
          externalUrl,
          remote,
          location,
          salaryLo,
          salaryHi,
          currency,
          country,
          tags.map(text => text.split(",").map(_.trim).toList),
          image,
          seniority,
          other
        )
      )
    def loadFile(maybeFile: Option[File]) =
      Cmd.Run[IO, Option[String], Msg](
        // run the effect here that returns an Option[String]
        // Option[File] => Option[String]
        // Option[File].traverse(file => Io[String]) => IO[Option[String]]
        // Option[String] => Msg
        maybeFile.traverse { file =>
          IO.async_ { cb =>
            // create a reader
            val reader = new FileReader
            // set the onload
            reader.onload = _ => cb(Right(reader.result.toString))
            // trigger the reader
            reader.readAsDataURL(file)
          }
        }
      )(UpdateImage(_))

  }
}
