package com.tamullen.jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default._

final case class TokenConfig(tokenDuration: Long) derives ConfigReader {

}
