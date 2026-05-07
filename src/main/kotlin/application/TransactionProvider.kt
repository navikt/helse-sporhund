package application

interface TransactionProvider {
    fun <T> transaction(session: SessionContext.() -> T): T
}
