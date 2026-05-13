package no.nav.helse.sporhund.domain

import java.util.UUID

data class Saksbehandler(
    val id: SaksbehandlerOid,
    val navn: String,
    val epost: String,
    val ident: NavIdent,
)

@JvmInline
value class SaksbehandlerOid(
    val value: UUID,
)
