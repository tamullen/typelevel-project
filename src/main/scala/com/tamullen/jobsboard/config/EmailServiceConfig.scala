package com.tamullen.jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class EmailServiceConfig(
  val host: String,
  val port: Int,
  val user: String,
  val pass: String,
  val frontendUrl: String) derives ConfigReader {

}
