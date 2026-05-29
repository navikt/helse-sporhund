package no.nav.helse.sporhund.infrastructure.db

import javax.sql.DataSource
import kotliquery.sessionOf
import no.nav.helse.sporhund.application.SessionContext
import no.nav.helse.sporhund.application.TransactionProvider

class PgTransactionProvider(
    private val dataSource: DataSource
) : TransactionProvider {
    override fun <T> transaction(session: SessionContext.() -> T): T =
        sessionOf(dataSource, returnGeneratedKey = true, strict = true).use { session ->
            session.transaction { transactionalSession ->
                session(PgSessionContext(transactionalSession))
            }
        }
}
