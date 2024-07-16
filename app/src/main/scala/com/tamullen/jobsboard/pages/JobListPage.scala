package com.tamullen.jobsboard.pages
import tyrian.*
import tyrian.Html.*
import cats.effect.IO
import com.tamullen.jobsboard.*
import com.tamullen.jobsboard.components.*
import com.tamullen.jobsboard.common.*
import com.tamullen.jobsboard.domain.Job.*
import tyrian.http.*
import io.circe.parser.*
import io.circe.generic.auto.*

final case class JobListPage(
    filterPanel: FilterPanel = FilterPanel(
      filterAction = FilterJobs(_)
    ),
    jobFilter: JobFilter = JobFilter(),
    jobs: List[Job] = List(),
    canLoadMore: Boolean = true,
    status: Option[Page.Status] = Some(Page.Status.LOADING)
) extends Page {
  import JobListPage.*

  override def initCmd: Cmd[IO, App.Msg] =
    filterPanel.initCmd |+| Commands.getJobs()

  override def update(msg: App.Msg): (Page, Cmd[IO, App.Msg]) = msg match {
    case AddJobs(list, clm) =>
      (setSuccessStatus("Loaded").copy(jobs = this.jobs ++ list, canLoadMore = clm), Cmd.None)
    case SetErrorStatus(e) =>
      (setErrorStatus(e), Cmd.None)
    case LoadMoreJobs =>
      (this, Commands.getJobs(filter = jobFilter, offset = jobs.length))
    case FilterJobs(selectedFilters) =>
      val newJobFilter = createJobFilter(selectedFilters)
      (this.copy(jobs = List(), jobFilter = newJobFilter), Commands.getJobs(filter = newJobFilter))
    case msg: FilterPanel.Msg =>
      val (newFilterPanel, cmd) = filterPanel.update(msg)
      (this.copy(filterPanel = newFilterPanel), cmd)
    case _ => (this, Cmd.None)
  }

  def view(): Html[App.Msg] =
    section(`class` := "section-1")(
      div(`class` := "container job-list-hero")(
        h1(`class` := "job-list-title")("Rock the JVM Jobsboard")
      ),
      div(`class` := "container")(
        div(`class` := "row jvm-recent-jobs-body")(
          div(`class` := "col-lg-4")(
            filterPanel.view()
          ),
          div(`class` := "col-lg-8")(
            jobs.map(JobComponents.card) ++ maybeRenderLoadMore
          )
        )
      )
    )

//  override def view(): Html[App.Msg] =
//    div(`class` := "job-list-page")(
//      filterPanel.view(),
//      div(`class` := "jobs-container")(
//        jobs.map(job => renderJob(job)) ++ maybeRenderLoadMore
//      )
//    )

  def setErrorStatus(message: String) =
    this.copy(status = Some(Page.Status(message, Page.StatusKind.ERROR)))

  def setSuccessStatus(message: String) =
    this.copy(status = Some(Page.Status("Success!", Page.StatusKind.SUCCESS)))

  /////////////////////////////////////////////////////////////////////////////
  //  Private
  /////////////////////////////////////////////////////////////////////////////

  // UI

  private def maybeRenderLoadMore: Option[Html[App.Msg]] = {
    status.map { s =>
      div(`class` := "load-more-action")(
        s match {
          case Page.Status(_, Page.StatusKind.LOADING) =>
            div(`class` := "page-status-loading")("Loading...")
          case Page.Status(e, Page.StatusKind.ERROR) => div(`class` := "page-status-errors")(e)
          case Page.Status(_, Page.StatusKind.SUCCESS) =>
            if (canLoadMore)
              button(`class` := "load-more-btn", `type` := "button", onClick(LoadMoreJobs))(
                "Load More"
              )
            else
              div("All Jobs Loaded")
        }
      )
    }
  }

  // util
  private def createJobFilter(selectedFilters: Map[String, Set[String]]) =
    JobFilter(
      companies = selectedFilters.get("Companies").getOrElse(Set()).toList,
      locations = selectedFilters.get("Locations").getOrElse(Set()).toList,
      countries = selectedFilters.get("Countries").getOrElse(Set()).toList,
      seniorities = selectedFilters.get("Seniorities").getOrElse(Set()).toList,
      tags = selectedFilters.get("Tags").getOrElse(Set()).toList,
      maxSalary = Some(filterPanel.maxSalary).filter(_ > 0), // this is always a value (0 default)
      filterPanel.remote
    )
}

object JobListPage {

  trait Msg extends App.Msg
  // state
  case class SetErrorStatus(error: String)                  extends Msg
  case class AddJobs(list: List[Job], canLoadMore: Boolean) extends Msg
  // actions
  case object LoadMoreJobs                                         extends Msg
  case class FilterJobs(selectedFilters: Map[String, Set[String]]) extends Msg

  object Endpoints {
    def getJobs(limit: Int = Constants.defaultPageSize, offset: Int = 0) = new Endpoint[Msg] {
      override val location: String = Constants.endpoints.jobs + s"?limit=$limit&offset=$offset"
      override val method: Method   = Method.Post
      override val onError: HttpError => Msg = e => SetErrorStatus(e.toString)
      override val onResponse: Response => Msg =
        Endpoint.onResponse[List[Job], Msg](
          list => AddJobs(list, canLoadMore = offset == 0 || !list.isEmpty),
          SetErrorStatus(_)
        )
    }
  }

  object Commands {
    def getJobs(
        filter: JobFilter = JobFilter(),
        limit: Int = Constants.defaultPageSize,
        offset: Int = 0
    ): Cmd[IO, Msg] = {
      Endpoints.getJobs(limit, offset).call(filter)
    }
  }
}
