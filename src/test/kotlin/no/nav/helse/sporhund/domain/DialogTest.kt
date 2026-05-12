package no.nav.helse.sporhund.domain

import no.nav.helse.sporhund.domain.testhelpers.lagBehandler
import no.nav.helse.sporhund.domain.testhelpers.lagBehandlerRef
import no.nav.helse.sporhund.domain.testhelpers.lagIdentitetsnummer
import no.nav.helse.sporhund.domain.testhelpers.lagNavIdent
import kotlin.test.Test
import kotlin.test.assertEquals

class DialogTest {
    @Test
    fun `nytt event når det sendes en melding fra Nav`() {
        val behandlerRef = lagBehandlerRef()
        val dialog =
            Dialog.ny(
                identitetsnummer = lagIdentitetsnummer(),
                melding =
                    Dialogmelding.FraNav.ny(
                        saksbehandler = lagNavIdent(),
                        behandler = lagBehandler(),
                        behandlerRef = behandlerRef,
                        melding = "En melding",
                    ),
            )
        val events = dialog.events()
        assertEquals(1, events.size)
        assertEquals(dialog.conversationRef, events.single().conversationRef)
        assertEquals(dialog.identitetsnummer, events.single().identitetsnummer)
        assertEquals(behandlerRef, events.single().behandlerRef)
    }
}
