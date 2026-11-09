package no.nav.helse.sporhund.infrastructure.db

import kotliquery.Session
import no.nav.helse.sporhund.application.SessionContext

class PgSessionContext(
    session: Session,
) : SessionContext {
    override val dialogRepository = PgDialogRepository(session)
    override val kommandoKø = PgKommandoKø(session)
    override val dialogmeldingPdfRepository = PgDialogmeldingPdfRepository(session)
}
