package no.nav.helse.sporhund.domain

data class Saksbehandler(
    val navn: String,
    val epost: String,
    val ident: NavIdent,
)
