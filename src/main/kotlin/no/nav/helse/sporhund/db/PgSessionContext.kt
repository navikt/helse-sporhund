package no.nav.helse.sporhund.db

import kotliquery.Session
import no.nav.helse.sporhund.application.Outbox
import no.nav.helse.sporhund.application.SessionContext

class PgSessionContext(
    session: Session,
) : SessionContext {
    override val outbox: Outbox = PgOutbox(session)
    override val dialogRepository = PgDialogRepository(session)
}
