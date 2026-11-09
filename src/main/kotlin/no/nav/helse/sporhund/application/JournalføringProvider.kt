package no.nav.helse.sporhund.application

import no.nav.helse.sporhund.domain.Behandler
import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.DialogmeldingId
import no.nav.helse.sporhund.domain.Fagområde
import no.nav.helse.sporhund.domain.Identitetsnummer
import java.util.UUID

interface JournalføringProvider {
    fun journalførUtgående(
        journalpost: UtgåendeDialogmeldingJournalpost,
        pdf: ByteArray,
    )

    /** Feilregistrerer journalposten til en innkommende dialogmelding og knytter den til saken for dialogen. */
    fun feilregistrerOgKnyttInnkommende(
        journalpostId: String,
        conversationRef: ConversationRef,
        gjelder: Identitetsnummer,
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
