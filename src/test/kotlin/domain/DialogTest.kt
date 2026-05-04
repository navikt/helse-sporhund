package domain

import domain.testhelpers.lagBehandlerRef
import domain.testhelpers.lagIdentitetsnummer
import domain.testhelpers.lagNavIdent
import kotlin.test.Test
import kotlin.test.assertEquals

class DialogTest {
    @Test
    fun `nytt event når det sendes en melding fra Nav`() {
        val mottaker = lagBehandlerRef()
        val dialog = Dialog.ny(lagIdentitetsnummer(), Dialogmelding.FraNav.ny(lagNavIdent(), mottaker, "En melding"))
        val events = dialog.events()
        assertEquals(1, events.size)
        assertEquals(dialog.conversationRef, events.single().conversationRef)
        assertEquals(dialog.identitetsnummer, events.single().identitetsnummer)
        assertEquals(mottaker, events.single().behandlerRef)
        // TODO: assertEquals(type, events.single().type)
    }
}
