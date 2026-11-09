package no.nav.helse.sporhund.application

import no.nav.helse.sporhund.domain.NyDialogmeldingFraNavEvent

fun interface DialogmeldingTilBehandlerSender {
    fun send(
        melding: NyDialogmeldingFraNavEvent,
        vedlegg: ByteArray,
    )
}
