package db.testhelpers

import db.PgSessionContext
import kotliquery.sessionOf
import org.junit.jupiter.api.AfterEach

abstract class DbTest {
    private val db = TestDb

    protected val session = sessionOf(dataSource = db.dataSource, returnGeneratedKey = true)
    protected val sessionContext = PgSessionContext(session)

    @AfterEach
    fun tearDown() = session.close()
}
