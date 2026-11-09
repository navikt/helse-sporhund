package no.nav.helse.sporhund.application

import no.nav.helse.sporhund.application.kommando.KommandoKø

class InMemoryTransactionProvider : TransactionProvider {
    val outbox: Outbox = InMemoryOutbox()
    val dialogRepository = InMemoryDialogRepository()
    val kommandoKø = InMemoryKommandoKø()
    val dialogmeldingPdfRepository = InMemoryDialogmeldingPdfRepository()

    private val sessionContext =
        object : SessionContext {
            override val outbox: Outbox = this@InMemoryTransactionProvider.outbox
            override val dialogRepository: DialogRepository = this@InMemoryTransactionProvider.dialogRepository
            override val kommandoKø: KommandoKø = this@InMemoryTransactionProvider.kommandoKø
            override val dialogmeldingPdfRepository: DialogmeldingPdfRepository = this@InMemoryTransactionProvider.dialogmeldingPdfRepository
        }

    override fun <T> transaction(session: SessionContext.() -> T): T = session(sessionContext)
}
