package no.nav.helse.sporhund.application

import no.nav.helse.sporhund.domain.*
import java.util.*

class InMemoryDialogRepository : DialogRepository {
    private val dialoger = mutableMapOf<ConversationRef, Dialog>()

    override fun lagre(dialog: Dialog) {
        dialoger[dialog.conversationRef] = dialog
    }

    override fun finnDialog(conversationRef: ConversationRef): Dialog? = dialoger[conversationRef]

    override fun finnDialoger(identitetsnummer: Identitetsnummer): List<Dialog> = dialoger.values.filter { it.identitetsnummer == identitetsnummer }

    override fun finnÅpneDialoger(): List<Dialog> = dialoger.values.filter { it.status != Dialogstatus.DialogLukket }

    override fun finnDialogVedMeldingId(meldingId: UUID): Dialog? =
        dialoger.values.firstOrNull { dialog ->
            dialog.meldinger.any { melding ->
                melding.id == DialogmeldingId(meldingId) &&
                    when (melding) {
                        is Dialogmelding.FraNav -> !melding.kvitteringMottatt
                        is Dialogmelding.FraSystem -> !melding.kvitteringMottatt
                        else -> false
                    }
            }
        }
}
