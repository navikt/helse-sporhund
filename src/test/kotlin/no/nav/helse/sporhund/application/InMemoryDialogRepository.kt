package no.nav.helse.sporhund.application

import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.Dialog

class InMemoryDialogRepository : DialogRepository {
    private val dialoger = mutableMapOf<ConversationRef, Dialog>()

    override fun lagre(dialog: Dialog) {
        dialoger[dialog.conversationRef] = dialog
    }

    override fun finnDialog(conversationRef: ConversationRef): Dialog? = dialoger[conversationRef]
}
