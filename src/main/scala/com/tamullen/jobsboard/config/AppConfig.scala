package com.tamullen.jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default._

final case class AppConfig(postgresConfig: PostgresConfig,
                           emberConfig: EmberConfig,
                           securityConfig: SecurityConfig
                          ) derives ConfigReader
