package db

import application.SessionContext
import application.TransactionProvider
import kotliquery.sessionOf
import javax.sql.DataSource

class PgTransactionProvider(
    private val dataSource: DataSource,
) : TransactionProvider {
    override fun <T> transaction(session: SessionContext.() -> T): T =
        sessionOf(dataSource, returnGeneratedKey = true, strict = true).use { session ->
            session.transaction { transactionalSession ->
                session(PgSessionContext(transactionalSession))
            }
        }
}
