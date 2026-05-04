package application

fun interface TransactionProvider {
    fun transaction(session: SessionContext.() -> Unit)
}
