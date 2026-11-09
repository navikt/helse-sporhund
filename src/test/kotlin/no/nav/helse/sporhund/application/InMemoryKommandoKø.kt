package no.nav.helse.sporhund.application

import no.nav.helse.sporhund.application.kommando.Kommando
import no.nav.helse.sporhund.application.kommando.KommandoId
import no.nav.helse.sporhund.application.kommando.KommandoKø

class InMemoryKommandoKø : KommandoKø {
    private val kommandoer = mutableListOf<Kommando>()

    override fun leggTil(kommando: Kommando) {
        kommandoer.add(kommando)
    }

    override fun nesteKommando(): Kommando? = kommandoer.firstOrNull()

    override fun utført(id: KommandoId) {
        kommandoer.removeIf { it.id == id }
    }
}
