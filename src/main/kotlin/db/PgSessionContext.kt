package db

import application.Outbox
import application.SessionContext
import kotliquery.Session

class PgSessionContext(
    session: Session,
) : SessionContext {
    override val outbox: Outbox = PgOutbox(session)
}
