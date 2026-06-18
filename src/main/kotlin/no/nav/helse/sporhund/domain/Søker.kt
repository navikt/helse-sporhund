package no.nav.helse.sporhund.domain

import java.time.LocalDate

data class Søker(
    val navn: Navn,
    val fødselsdato: LocalDate,
)
