package db

import application.Outbox
import application.OutboxMelding
import application.OutboxMeldingId
import kotliquery.TransactionalSession

class PgOutbox(
    private val session: TransactionalSession,
) : Outbox {
    override fun nyMelding(melding: OutboxMelding) {
        TODO("Not yet implemented")
    }

    override fun meldinger(): List<OutboxMelding> {
        TODO("Not yet implemented")
    }

    override fun meldingSendt(id: OutboxMeldingId) {
        TODO("Not yet implemented")
    }
}
