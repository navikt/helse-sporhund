package no.nav.helse.sporhund.application.kommando

interface KommandoKø {
    fun leggTil(kommando: Kommando)

    /** Henter og låser den eldste kommandoen som ikke er låst av en annen transaksjon. */
    fun nesteKommando(): Kommando?

    fun utført(id: KommandoId)
}
