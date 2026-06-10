package no.nav.helse.sporhund.infrastructure.db.testhelpers

import kotliquery.sessionOf
import no.nav.helse.sporhund.application.SessionContext
import no.nav.helse.sporhund.infrastructure.db.PgSessionContext

abstract class DbTest {
    private val db = TestDb

    protected fun test(test: SessionContext.() -> Unit) {
        sessionOf(dataSource = db.dataSource, returnGeneratedKey = true).use { session ->
            session.connection.begin()
            try {
                PgSessionContext(session).test()
            } finally {
                session.connection.rollback()
            }
        }
    }
}
