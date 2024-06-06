package com.tamullen.jobsboard.components

import tyrian.Html
import tyrian.Html.*
import com.tamullen.jobsboard.*
import com.tamullen.jobsboard.domain.Job
import com.tamullen.jobsboard.domain.Job.*
import com.tamullen.jobsboard.pages.Page
import laika.api.Transformer
import laika.api.*
import laika.format.*

object JobComponents {

  def card(job: Job): Html[App.Msg] =
    div(`class` := "jvm-recent-jobs-cards")(
      div(`class` := "jvm-recent-jobs-card-img")(
        img(
          `class` := "img-fluid",
          src     := job.jobInfo.image.getOrElse(""),
          alt     := job.jobInfo.title
        )
      ),
      div(`class` := "jvm-recent-jobs-card-contents")(
        h5(
          Anchors
            .renderSimpleNavLink(
              s"${job.jobInfo.company} - ${job.jobInfo.title}",
              Page.Urls.JOB(job.id.toString()),
              "job-title-link"
            )
        ),
        renderJobSummary(job)
      ),
      div(`class` := "jvm-recent-jobs-card-btn-apply")(
        a(href := job.jobInfo.externalUrl, target := "blank")(
          button(`class` := "btn btn-danger", `type` := "button")("Apply")
        )
      )
    )

  def renderDetail(icon: String, value: String): Html[App.Msg] =
    div(`class` := "job-detail")(
      i(`class` := s"fa fa-$icon job-detail-icon")(),
      p(`class` := "job-detail-values")(value)
    )

  def renderJobSummary(job: Job): Html[App.Msg] =
    div(`class` := "job-summary")(
      renderDetail("dollar", fullSalaryString(job)),
      renderDetail("location-dot", fullLocationString(job)),
      maybeRenderDetail("ranking-star", job.jobInfo.seniority),
      maybeRenderDetail("tags", job.jobInfo.tags.map(_.mkString(",")))
    )

  val markDownTransformer = Transformer
    .from(Markdown)
    .to(HTML)
    .build

  def renderJobDescription(job: Job) = {
    val descriptionHtml = markDownTransformer.transform(job.jobInfo.description) match {
      case Left(e) =>
        """Dammit. 
          |Had an error showing Markdown for this job.
          |Just hit the apply button (that should still work) - also let them know about the problem.
          |"""
      case Right(html) => html
    }

    div(`class` := "job-description")().innerHtml(descriptionHtml)
  }

  def maybeRenderDetail(icon: String, maybeValue: Option[String]): Html[App.Msg] =
    maybeValue.map(value => renderDetail(icon, value)).getOrElse(div())

  // private
  private def fullSalaryString(job: Job) =
    val currency = job.jobInfo.currency.getOrElse("")
    (job.jobInfo.salaryLo, job.jobInfo.salaryHi) match {
      case (Some(lo), Some(hi)) =>
        s"$currency $lo-$hi"
      case (Some(lo), None) =>
        s"$currency $lo"
      case (None, Some(hi)) =>
        s"up to $currency $hi"
      case _ => "unspecified = ∞"
    }

  private def fullLocationString(job: Job) = job.jobInfo.country match {
    case Some(country) => s"${job.jobInfo.location}, $country"
    case None          => job.jobInfo.location
  }

}
