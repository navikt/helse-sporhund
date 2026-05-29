package no.nav.helse.sporhund.application

import kotlin.reflect.KClass

class InMemoryOutbox : Outbox {
    private val meldinger = mutableListOf<OutboxMelding>()

    override fun nyMelding(melding: OutboxMelding) {
        meldinger.add(melding)
    }

    override fun <T : OutboxMelding> meldinger(type: KClass<T>): List<T> =
        meldinger.filterIsInstance(type.java)

    override fun meldingSendt(id: OutboxMeldingId) {
        meldinger.removeIf { it.id == id }
    }
}
