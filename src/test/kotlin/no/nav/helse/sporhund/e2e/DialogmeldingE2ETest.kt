package no.nav.helse.sporhund.e2e

import kotlin.test.Test

class DialogmeldingE2ETest {
    @Test
    fun `forespørsel, svar fra behandler, svar fra saksbehandler og ferdigstilling`() =
        e2e {
            saksbehandlerSenderForespørsel("Kan du sende journalnotat?")
            behandlerHarMottattDialogmelding(medVedlegg = true)
            dialogmeldingenErJournalført()
            behandlerSvarer("Her er notatet")
            saksbehandlerSerSvaret("Her er notatet")
            saksbehandlerSvarer("Takk")
            behandlerHarMottattDialogmelding(medVedlegg = true)
            dialogmeldingenErJournalført()
            saksbehandlerFerdigstillerDialogen()
        }
}
