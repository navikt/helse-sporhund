package no.nav.helse.sporhund.domain

data class Behandler(
    val hprNummer: HprNummer,
    val navn: String,
    val kontor: String,
    val kontorOrganisasjonsnummer: Organisasjonsnummer,
)
