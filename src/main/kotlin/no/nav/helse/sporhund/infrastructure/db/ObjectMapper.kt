package no.nav.helse.sporhund.infrastructure.db

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder

val objectMapper: JsonMapper =
    jacksonMapperBuilder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build()
