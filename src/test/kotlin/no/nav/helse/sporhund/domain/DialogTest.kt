package no.nav.helse.sporhund.domain

import no.nav.helse.sporhund.domain.testhelpers.*
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DialogTest {
    @Test
    fun `nytt event når det sendes en melding fra Nav`() {
        val behandlerRef = lagBehandlerRef()
        val dialog =
            Dialog.ny(
                identitetsnummer = lagIdentitetsnummer(),
                søkernavn = lagNavn(),
                melding =
                    lagFraNavMelding(
                        ident = lagNavIdent(),
                        behandler = lagBehandler(),
                        behandlerRef = behandlerRef,
                        melding = "En melding",
                    ),
                fagområde = Fagområde.EnkeltståendeBehandlingsdager,
                dialogtype = Dialogtype.MedisinskeOpplysninger,
            )
        val events = dialog.events()
        assertEquals(1, events.size)
        assertEquals(dialog.conversationRef, events.single().conversationRef)
        assertEquals(dialog.identitetsnummer, events.single().identitetsnummer)
        assertEquals(behandlerRef, events.single().behandlerRef)
    }

    @Test
    fun `nyesteMelding returnerer den eneste meldingen i en ny dialog`() {
        val fraNavMelding = lagFraNavMelding()
        val dialog = lagDialog(melding = fraNavMelding)

        val nyest = dialog.nyesteMelding()

        assertIs<Dialogmelding.FraNav>(nyest)
        assertEquals(fraNavMelding.id, nyest.id)
    }

    @Test
    fun `nyesteMelding returnerer FraBehandler etter at behandler har svart`() {
        val dialog = lagDialog()
        val fraBehandlerMelding = lagFraBehandlerMelding()
        dialog.nyMelding(fraBehandlerMelding)

        val nyest = dialog.nyesteMelding()

        assertIs<Dialogmelding.FraBehandler>(nyest)
        assertEquals(fraBehandlerMelding.id, nyest.id)
    }

    @Test
    fun `nyesteMelding returnerer siste FraNav etter purring`() {
        val dialog = lagDialog()
        dialog.nyMelding(lagFraBehandlerMelding())
        val purring = lagFraNavMelding()
        dialog.nyMelding(purring)

        val nyest = dialog.nyesteMelding()

        assertIs<Dialogmelding.FraNav>(nyest)
        assertEquals(purring.id, nyest.id)
    }

    @Test
    fun `frist er 21 dager etter den opprinnelige FraNav-meldingen`() {
        val tidspunkt = Instant.parse("2026-05-01T10:00:00Z")
        val fraNavMelding = lagFraNavMelding(opprettet = tidspunkt)
        val dialog = lagDialog(melding = fraNavMelding)

        assertEquals(tidspunkt + Duration.ofDays(21), dialog.frist())
    }

    @Test
    fun `frist endres ikke når behandler svarer`() {
        val tidspunkt = Instant.parse("2026-05-01T10:00:00Z")
        val dialog = lagDialog(melding = lagFraNavMelding(opprettet = tidspunkt))
        dialog.nyMelding(lagFraBehandlerMelding())

        assertEquals(tidspunkt + Duration.ofDays(21), dialog.frist())
    }

    @Test
    fun `frist oppdateres til 21 dager etter purring`() {
        val opprinnelig = Instant.parse("2026-05-01T10:00:00Z")
        val purringTidspunkt = Instant.parse("2026-05-15T10:00:00Z")
        val dialog = lagDialog(melding = lagFraNavMelding(opprettet = opprinnelig))
        dialog.nyMelding(lagFraBehandlerMelding())
        dialog.nyMelding(lagFraNavMelding(opprettet = purringTidspunkt))

        assertEquals(purringTidspunkt + Duration.ofDays(21), dialog.frist())
    }
}
