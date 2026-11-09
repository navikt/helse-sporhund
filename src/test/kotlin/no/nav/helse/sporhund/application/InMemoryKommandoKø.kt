package no.nav.helse.sporhund.application

import no.nav.helse.sporhund.application.kommando.Kommando
import no.nav.helse.sporhund.application.kommando.KommandoId
import no.nav.helse.sporhund.application.kommando.KommandoKø
import no.nav.helse.sporhund.application.kommando.KøetKommando
import java.time.Instant

class InMemoryKommandoKø : KommandoKø {
    private val kommandoer = mutableListOf<KøetKommando>()

    fun kommandoer(): List<Kommando> = kommandoer.map { it.kommando }

    override fun leggTil(kommando: Kommando) {
        kommandoer.add(KøetKommando(kommando, antallForsøk = 0))
    }

    override fun nesteKommando(nå: Instant): KøetKommando? = kommandoer.firstOrNull()

    override fun utført(id: KommandoId) {
        kommandoer.removeIf { it.kommando.id == id }
    }

    override fun feilet(
        id: KommandoId,
        antallForsøk: Int,
        feilklasse: String,
        nesteForsøk: Instant,
    ) {
        kommandoer.replaceAll { if (it.kommando.id == id) it.copy(antallForsøk = antallForsøk) else it }
    }
}
