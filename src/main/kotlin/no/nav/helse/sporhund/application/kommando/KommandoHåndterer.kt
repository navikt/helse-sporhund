package no.nav.helse.sporhund.application.kommando

import no.nav.helse.sporhund.application.SessionContext

fun interface KommandoHåndterer {
    /** Utfører kommandoen og returnerer kommandoene som skal utføres etterpå. */
    fun håndter(
        kommando: Kommando,
        session: SessionContext,
    ): List<Kommando>
}
