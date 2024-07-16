package com.tamullen.jobsboard.pages
import cats.effect.IO
import cats.syntax.traverse.*
import com.tamullen.jobsboard.App
import com.tamullen.jobsboard.core.{Router, Session}
import com.tamullen.jobsboard.common.*
import com.tamullen.jobsboard.components.FilterPanel
import com.tamullen.jobsboard.domain.Job.JobInfo
import com.tamullen.jobsboard.pages.FormPage
import tyrian.*
import tyrian.Html.*
import tyrian.http.{Method, *}
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.*
import tyrian.cmds.Logger
import org.scalajs.dom.{
  CanvasRenderingContext2D,
  File,
  FileReader,
  HTMLCanvasElement,
  HTMLImageElement,
  HTMLInputElement,
  document
}

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
    tags: Option[String] = None,
    image: Option[String] = None,
    seniority: Option[String] = None,
    other: Option[String] = None,
    status: Option[Page.Status] = None
) extends FormPage("Post Job", status) {
  import PostJobPage.*

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
        Commands.postJob(promoted = true)(
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

  override protected def renderFormContent(): List[Html[App.Msg]] =
    if (!Session.isActive) renderInvalidContents()
    else
      List(
        renderInput("Company", "company", "text", true, UpdateCompany(_)),
        renderInput("Title", "title", "text", true, UpdateTitle(_)),
        renderTextArea("Description", "description", true, UpdateDescription(_)),
        renderInput("External Url", "externalUrl", "text", true, UpdateExternalUrl(_)),
        renderToggle("Remote", "remote", true, _ => ToggleRemote),
        renderInput("Location", "location", "text", true, UpdateLocation(_)),
        renderInput("Salary Low", "salaryLo", "number", false, s => UpdateSalaryLo(parseNumber(s))),
        renderInput(
          "Salary High",
          "salaryHi",
          "number",
          false,
          s => UpdateSalaryHi(parseNumber(s))
        ),
        renderInput("Currency", "currency", "text", false, UpdateCurrency(_)),
        renderInput("Country", "country", "text", false, UpdateCountry(_)),
        renderImageUploadInput("Logo", "logo", image, UpdateImageFile(_)),
        renderInput("Tags", "tags", "text", false, UpdateTags(_)),
        renderInput("Seniority", "seniority", "text", false, UpdateSeniority(_)),
        renderInput("Other", "Other", "text", false, UpdateOther(_)),
        button(`class` := "form-submit-btn", `type` := "button", onClick(AttemptPostJob))(
          "Post Job - $" + Constants.jobADvertPriceUSD
        )
      )

  // private API
  def renderInvalidContents() =
    List(
      p(`class` := "form-text")("You need to be logged in to post a job.")
    )

  // util
  def setErrorStatus(message: String): Page =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  def setSuccessStatus(message: String): Page =
    this.copy(status = Some(Page.Status("Success!", Page.StatusKind.SUCCESS)))

  private def parseNumber(s: String) =
    Try(s.toInt).getOrElse(0)

  private def renderRemoteCheckbox(remote: Boolean): Html[App.Msg] =
    div(`class` := "form-check")(
      label(`class` := "form-check-label", `for` := s"filter-checkbox")("Remote"),
      input(
        `class` := "form-check-input",
        `type`  := "checkbox",
        id      := "filter-checkbox",
        checked(remote),
        onEvent(
          "change",
          event => {
            // send message to insert value as a checked value inside the groupName's Set in the map
            val checkbox = event.target.asInstanceOf[HTMLInputElement]
            ToggleRemote
          }
        )
      )
    )
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
      override val onResponse: Response => Msg =
        Endpoint.onResponseText(PostJobSuccess(_), PostJobError(_))
    }

    val postJobPromoted = new Endpoint[App.Msg] {
      override val location: String              = Constants.endpoints.postJobPromoted
      override val method: Method                = Method.Post
      override val onError: HttpError => App.Msg = e => PostJobError(e.toString)
      override val onResponse: Response => App.Msg =
        Endpoint.onResponseText(Router.ExternalRedirect(_), PostJobError(_))
    }
  }

  object Commands {
    def postJob(promoted: Boolean = true)(
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
    ) = {
      val endpoint =
        if (promoted) Endpoints.postJobPromoted
        else Endpoints.postJob
      endpoint.callAuthorized(
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
    }

    def loadFileBasic(maybeFile: Option[File]) =
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
            reader.onload = _ => {
              // create new img tag
              val img = document.createElement("img").asInstanceOf[HTMLImageElement]
              img.addEventListener(
                "load",
                _ => {
                  // create a canvas on that image
                  val canvas  = document.createElement("canvas").asInstanceOf[HTMLCanvasElement]
                  val context = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
                  val (width, height) = computeDimensions(img.width, img.height)
                  canvas.width = width
                  canvas.height = height
                  // force the browser to "draw" the image on a fixed width/height
                  context.drawImage(img, 0, 0, canvas.width, canvas.height)
                  // call the cb(canvas.data)
                  cb(Right(canvas.toDataURL(file.`type`))) // png/base64....
                }
              )
              img.src = reader.result.toString // originial image.
            }
            // trigger the reader
            reader.readAsDataURL(file)
          }
        }
      )(UpdateImage(_))

    private def computeDimensions(w: Int, h: Int): (Int, Int) =
      if (w >= h) {
        val ratio = w * 1.0 / 256
        val w1    = w / ratio
        val h1    = h / ratio
        (w1.toInt, h1.toInt)
      } else {
        val (h1, w1) = computeDimensions(h, w)
        (w1, h1)
      }
  }
}
