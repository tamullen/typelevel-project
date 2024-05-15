package com.tamullen.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default._

final case class AppConfig(postgresConfig: PostgresConfig,
                           emberConfig: EmberConfig) derives ConfigReader
