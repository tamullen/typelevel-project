package com.tamullen.jobsboard

import scala.scalajs.js.annotation._
import org.scalajs.dom._

@JSExportTopLevel("RockTheJvmApp")
class App {
  @JSExport
  def doSomething(containerId: String) =
    document.getElementById(containerId).innerHTML = "Rock the JVM!"

  // in JS: document.getElementByID(...) .innerHTML = "This is my html"
}
