package db

import application.SessionContext
import application.TransactionProvider

class PgTransactionProvider : TransactionProvider {
    override fun transaction(session: SessionContext.() -> Unit) {
        TODO("Not yet implemented")
    }
}
