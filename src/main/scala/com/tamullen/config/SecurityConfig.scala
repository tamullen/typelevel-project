package com.tamullen.config

import pureconfig.ConfigReader
import com.comcast.ip4s.{Host, Port}
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert
import pureconfig.generic.derivation.default.*
import scala.concurrent.duration.FiniteDuration

case class SecurityConfig(secret: String, jwtExpiryDuration: FiniteDuration) derives ConfigReader
