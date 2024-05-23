package com.tamullen.jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default._

case class PostgresConfig(nThreads: Int, url: String, user: String, pass: String) derives ConfigReader
