package no.nav.helse.sporhund.api.testhelpers

import no.nav.helse.sporhund.application.DialogRepository
import no.nav.helse.sporhund.application.Outbox
import no.nav.helse.sporhund.application.OutboxMelding
import no.nav.helse.sporhund.application.OutboxMeldingId
import no.nav.helse.sporhund.application.SessionContext
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.Dialog
import no.nav.helse.sporhund.domain.Identitetsnummer

class FakeDialogRepository : DialogRepository {
    private val dialogs = mutableMapOf<ConversationRef, Dialog>()

    fun leggTil(dialog: Dialog) {
        dialogs[dialog.conversationRef] = dialog
    }

    override fun lagre(dialog: Dialog) {
        dialogs[dialog.conversationRef] = dialog
    }

    override fun finnDialog(conversationRef: ConversationRef): Dialog? = dialogs[conversationRef]

    override fun finnDialoger(identitetsnummer: Identitetsnummer): List<Dialog> = dialogs.values.filter { it.identitetsnummer == identitetsnummer }
}

class FakeOutbox : Outbox {
    private val sendte = mutableListOf<OutboxMelding>()

    override fun nyMelding(melding: OutboxMelding) {
        sendte.add(melding)
    }

    override fun meldinger(): List<OutboxMelding> = sendte.toList()

    override fun meldingSendt(id: OutboxMeldingId) {
        sendte.removeIf { it.id == id }
    }
}

class FakeSessionContext(
    override val dialogRepository: DialogRepository,
    override val outbox: Outbox,
) : SessionContext

class FakeTransactionProvider(
    val dialogRepository: FakeDialogRepository = FakeDialogRepository(),
    val outbox: FakeOutbox = FakeOutbox(),
) : TransactionProvider {
    override fun <T> transaction(session: SessionContext.() -> T): T = FakeSessionContext(dialogRepository, outbox).session()
}
