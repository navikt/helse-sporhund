package no.nav.helse.sporhund.infrastructure.db.testhelpers

import kotliquery.sessionOf
import no.nav.helse.sporhund.infrastructure.db.PgSessionContext
import org.junit.jupiter.api.AfterEach

abstract class DbTest {
    private val db = TestDb

    protected val session = sessionOf(dataSource = db.dataSource, returnGeneratedKey = true)
    protected val sessionContext = PgSessionContext(session)

    fun tømDatabase() = db.truncate()

    @AfterEach
    fun tearDown() = session.close()
}
