package com.tamullen.jobsboard.components
import cats.effect.IO
import com.tamullen.jobsboard.*
import com.tamullen.jobsboard.common.*
import com.tamullen.jobsboard.domain.Job.*
import tyrian.*
import tyrian.Html.*
import tyrian.http.*
import tyrian.http.Method.Get
import io.circe.generic.auto.*
import org.scalajs.dom.HTMLInputElement
import tyrian.cmds.Logger

case class FilterPanel(
    possibleFilters: JobFilter = JobFilter(),
    selectedFilters: Map[String, Set[String]] = Map(),
    maybeError: Option[String] = None,
    maxSalary: Int = 0,
    remote: Boolean = false,
    dirty: Boolean = false,
    filterAction: Map[String, Set[String]] => App.Msg = _ => App.NoOp
) extends Component[App.Msg, FilterPanel] {
  import FilterPanel.*
  override def initCmd: Cmd[IO, App.Msg] =
    Commands.getFilters

  override def update(msg: App.Msg): (FilterPanel, Cmd[IO, App.Msg]) = msg match {
    case TriggerFilter =>
      (this.copy(dirty = false), Cmd.Emit(filterAction(selectedFilters)))
    case SetPossibleFilters(possibleFilters) =>
      (this.copy(possibleFilters = possibleFilters), Cmd.None)
    case FilterPanelError(e) =>
      (this.copy(maybeError = Some(e)), Cmd.None)
    case UpdateSalaryInput(salary) =>
      (this.copy(maxSalary = salary, dirty = true), Cmd.None)
    case UpdateValueChecked(groupName, value, checked) =>
      val oldGroup  = selectedFilters.get(groupName).getOrElse(Set())
      val newGroup  = if (checked) oldGroup + value else oldGroup - value
      val newGroups = selectedFilters + (groupName -> newGroup)
      (
        this.copy(selectedFilters = newGroups, dirty = true),
        Logger.consoleLog[IO](s"Filters: $newGroups. Dirty: ${!dirty}")
      )
    case UpdateRemote(remote) =>
      (this.copy(remote = remote, dirty = true), Cmd.None)
    case _ => (this, Cmd.None)
  }

  /*
    Salary
      above _______ (local currency)
    Locations
      _ Bucharest
      _ NYC
      _ ...
    Countries
      _ ...
    ...
    Remote __
   */
  override def view(): Html[App.Msg] =
    div(`class` := "accordion accordion-flush", id := "accordionFlushExample")(
      div(`class` := "accordion-item")(
        h2(`class` := "accordion-header", id := "flush-headingOne")(
          button(
            `class` := "accordion-button",
            id      := "accordion-search-filter",
            `type`  := "button",
            attribute("data-bs-toggle", "collapse"),
            attribute("data-bs-target", "#flush-collapseOne"),
            attribute("aria-expanded", "true"),
            attribute("aria-controls", "flush-collapseOne")
          )(
            div(`class` := "jvm-recent-jobs-accordion-body-heading")(
              h3(span("Search"), text(" Filters"))
            )
          )
        ),
        div(
          `class` := "accordion-collapse collapse show",
          id      := "flush-collapseOne",
          attribute("aria-labelledby", "flush-headingOne"),
          attribute("data-bs-parent", "#accordionFlushExample")
        )(
          div(`class` := "accordion-body p-0")(
            div(`class` := "filter-panel-container")(
              maybeRenderError(),
              renderSalaryFilter(),
              renderRemoteCheckbox(),
              renderCheckBoxGroup("Companies", possibleFilters.companies),
              renderCheckBoxGroup("Locations", possibleFilters.locations),
              renderCheckBoxGroup("Countries", possibleFilters.countries),
              renderCheckBoxGroup("Tags", possibleFilters.tags),
              renderCheckBoxGroup("Seniorities", possibleFilters.seniorities),
              renderApplyFiltersButton()
            )
          )
        )
      )
    )

  // private
  private def maybeRenderError() =
    maybeError
      .map { e =>
        div(`class` := "page-status-errors")(e)
      }
      .getOrElse(div())

  private def renderFilterGroup(groupName: String, contents: Html[App.Msg]) =
    div(`class` := "accordion-item")(
      h2(`class` := "accordion-header", id := s"heading$groupName")(
        button(
          `class` := "accordion-button collapsed",
          `type`  := "button",
          attribute("data-bs-toggle", "collapse"),
          attribute("data-bs-target", s"#collapse$groupName"),
          attribute("aria-expanded", "false"),
          attribute("aria-controls", s"collapse$groupName")
        )(
          groupName
        )
      ),
      div(
        `class` := "accordion-collapse collapse",
        id      := s"collapse$groupName",
        attribute("aria-labelledby", "headingOne"),
        attribute("data-bs-parent", "#accordionExample")
      )(
        div(`class` := "accordion-body")(
          contents // <--- inject things here
        )
      )
    )

  private def renderSalaryFilter() =
    renderFilterGroup(
      "Salary",
      div(`class` := "mb-3")(
        label(`for` := "filter-salary")("Min (in local currency)"),
        input(
          `type` := "number",
          id     := "filter-salary",
          onInput(s => UpdateSalaryInput(if (s.isEmpty) 0 else s.toInt))
        )
      )
    )

  private def renderRemoteCheckbox() =
    renderFilterGroup(
      "Remote",
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
              UpdateRemote(checkbox.checked)
            }
          )
        )
      )
    )

  private def renderCheckBoxGroup(groupName: String, possibleValues: List[String]) =
    val selectedValues = selectedFilters.get(groupName).getOrElse(Set())
    renderFilterGroup(
      groupName,
      div(`class` := "mb-3")(
        possibleValues.map(value => renderCheckbox(groupName, value, selectedValues))
      )
    )

  private def renderCheckbox(groupName: String, value: String, selectedValues: Set[String]) =
    div(`class` := "form-check")(
      label(`class` := "form-check-label", `for` := s"filter-$groupName-$value")(value),
      input(
        `class` := "form-check-input",
        `type`  := "checkbox",
        id      := s"filter-$groupName-$value",
        checked(selectedValues.contains(value)),
        onEvent(
          "change",
          event => {
            // send message to insert value as a checked value inside the groupName's Set in the map
            val checkbox = event.target.asInstanceOf[HTMLInputElement]
            UpdateValueChecked(groupName, value, checkbox.checked)
          }
        )
      )
    )

  private def renderApplyFiltersButton() =
    div(`class` := "jvm-accordion-search-btn")(
      button(
        `class` := "btn btn-primary",
        `type`  := "button",
        disabled(!dirty),
        onClick(TriggerFilter)
      )("Apply Filters")
    )
}

object FilterPanel {
  trait Msg                                                 extends App.Msg
  case class FilterPanelError(error: String)                extends Msg
  case class SetPossibleFilters(possibleFilters: JobFilter) extends Msg
  // content
  case class UpdateSalaryInput(salary: Int)                                         extends Msg
  case class UpdateValueChecked(groupName: String, value: String, checked: Boolean) extends Msg
  case class UpdateRemote(remote: Boolean)                                          extends Msg
  // actions
  case object TriggerFilter extends Msg

  object Endpoints {
    val getFilters = new Endpoint[Msg] {
      override val location: String          = Constants.endpoints.filters
      override val method: Method            = Get
      override val onError: HttpError => Msg = e => FilterPanelError(e.toString)
      override val onResponse: Response => Msg =
        Endpoint.onResponse[JobFilter, Msg](
          SetPossibleFilters(_),
          FilterPanelError(_)
        )
    }
  }

  object Commands {
    def getFilters = Endpoints.getFilters.call()
  }
}
