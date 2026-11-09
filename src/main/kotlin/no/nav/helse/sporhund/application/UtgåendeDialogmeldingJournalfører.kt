package no.nav.helse.sporhund.application

import no.nav.helse.sporhund.domain.Behandler
import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.DialogmeldingId
import no.nav.helse.sporhund.domain.Fagområde
import no.nav.helse.sporhund.domain.Identitetsnummer
import java.util.UUID

fun interface UtgåendeDialogmeldingJournalfører {
    fun journalfør(
        journalpost: UtgåendeDialogmeldingJournalpost,
        pdf: ByteArray,
    )
}

data class UtgåendeDialogmeldingJournalpost(
    val conversationRef: ConversationRef,
    val meldingId: DialogmeldingId<UUID>,
    val mottaker: Behandler,
    val gjelder: Identitetsnummer,
    val fagområde: Fagområde,
    val erPurring: Boolean = false,
)
