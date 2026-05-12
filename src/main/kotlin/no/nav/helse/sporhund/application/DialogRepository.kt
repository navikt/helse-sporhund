package no.nav.helse.sporhund.application

import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.Dialog

interface DialogRepository {
    fun lagre(dialog: Dialog)

    fun finnDialog(conversationRef: ConversationRef): Dialog?
}
