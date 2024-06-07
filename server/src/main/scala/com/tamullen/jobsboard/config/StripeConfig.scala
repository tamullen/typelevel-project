package com.tamullen.jobsboard.config

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

final case class StripeConfig(
    val key: String,
    val price: String,
    val successUrl: String,
    val cancelUrl: String
) derives ConfigReader {}
