package no.nav.helse.sporhund.application.kommando

import java.time.Instant

data class KøetKommando(
    val kommando: Kommando,
    val antallForsøk: Int,
)

interface KommandoKø {
    fun leggTil(kommando: Kommando)

    /**
     * Henter og låser den eldste kommandoen som er klar til å utføres ved [nå] og ikke er låst av en annen transaksjon.
     * En kommando venter til alle eldre kommandoer av samme type i samme dialog er utført.
     */
    fun nesteKommando(nå: Instant): KøetKommando?

    fun utført(id: KommandoId)

    fun feilet(
        id: KommandoId,
        antallForsøk: Int,
        feilklasse: String,
        nesteForsøk: Instant,
    )
}
