package no.nav.helse.sporhund.application

class InMemoryTransactionProvider : TransactionProvider {
    val outbox: Outbox = InMemoryOutbox()
    val dialogRepository = InMemoryDialogRepository()

    private val sessionContext =
        object : SessionContext {
            override val outbox: Outbox = this@InMemoryTransactionProvider.outbox
            override val dialogRepository: DialogRepository = this@InMemoryTransactionProvider.dialogRepository
        }

    override fun <T> transaction(session: SessionContext.() -> T): T = session(sessionContext)
}
