package no.nav.helse.sporhund.application

import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.Dialog
import no.nav.helse.sporhund.domain.Identitetsnummer
import java.util.UUID

interface DialogRepository {
    fun lagre(dialog: Dialog)

    fun finnDialog(conversationRef: ConversationRef): Dialog?

    fun finnDialoger(identitetsnummer: Identitetsnummer): List<Dialog>

    fun finnÅpneDialoger(): List<Dialog>

    fun finnDialogVedMeldingId(meldingId: UUID): Dialog?
}
