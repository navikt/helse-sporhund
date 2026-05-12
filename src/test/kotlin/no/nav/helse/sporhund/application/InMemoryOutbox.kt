package no.nav.helse.sporhund.application

class InMemoryOutbox : Outbox {
    private val meldinger = mutableListOf<OutboxMelding>()

    override fun nyMelding(melding: OutboxMelding) {
        meldinger.add(melding)
    }

    override fun meldinger(): List<OutboxMelding> = meldinger.toList()

    override fun meldingSendt(id: OutboxMeldingId) {
        meldinger.removeIf { it.id == id }
    }
}
