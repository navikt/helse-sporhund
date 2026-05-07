package db

import application.Outbox
import application.SessionContext
import kotliquery.TransactionalSession

class PgSessionContext(
    transactionalSession: TransactionalSession,
) : SessionContext {
    override val outbox: Outbox = PgOutbox(transactionalSession)
}
