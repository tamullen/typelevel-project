package com.tamullen.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default._

case class PaginationConfig(nPages: Int = 20) derives ConfigReader
