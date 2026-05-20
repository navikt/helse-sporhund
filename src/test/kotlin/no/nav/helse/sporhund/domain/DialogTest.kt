package no.nav.helse.sporhund.domain

import no.nav.helse.sporhund.domain.testhelpers.lagBehandler
import no.nav.helse.sporhund.domain.testhelpers.lagBehandlerRef
import no.nav.helse.sporhund.domain.testhelpers.lagIdentitetsnummer
import no.nav.helse.sporhund.domain.testhelpers.lagNavIdent
import java.time.Duration
import java.time.Instant
import java.util.UUID
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

    @Test
    fun `nyesteMelding returnerer den eneste meldingen i en ny dialog`() {
        val fraNavMelding = nyFraNavMelding()
        val dialog = Dialog.ny(identitetsnummer = lagIdentitetsnummer(), melding = fraNavMelding)

        val nyest = dialog.nyesteMelding()

        assertIs<Dialogmelding.FraNav>(nyest)
        assertEquals(fraNavMelding.id, nyest.id)
    }

    @Test
    fun `nyesteMelding returnerer FraBehandler etter at behandler har svart`() {
        val dialog = Dialog.ny(identitetsnummer = lagIdentitetsnummer(), melding = nyFraNavMelding())
        val fraBehandlerMelding = nyFraBehandlerMelding()
        dialog.nyMelding(fraBehandlerMelding)

        val nyest = dialog.nyesteMelding()

        assertIs<Dialogmelding.FraBehandler>(nyest)
        assertEquals(fraBehandlerMelding.id, nyest.id)
    }

    @Test
    fun `nyesteMelding returnerer siste FraNav etter purring`() {
        val dialog = Dialog.ny(identitetsnummer = lagIdentitetsnummer(), melding = nyFraNavMelding())
        dialog.nyMelding(nyFraBehandlerMelding())
        val purring = nyFraNavMelding()
        dialog.nyMelding(purring)

        val nyest = dialog.nyesteMelding()

        assertIs<Dialogmelding.FraNav>(nyest)
        assertEquals(purring.id, nyest.id)
    }

    @Test
    fun `frist er 21 dager etter den opprinnelige FraNav-meldingen`() {
        val tidspunkt = Instant.parse("2026-05-01T10:00:00Z")
        val fraNavMelding = fraNavMeldingMedTidspunkt(tidspunkt)
        val dialog = Dialog.ny(identitetsnummer = lagIdentitetsnummer(), melding = fraNavMelding)

        assertEquals(tidspunkt + Duration.ofDays(21), dialog.frist())
    }

    @Test
    fun `frist endres ikke når behandler svarer`() {
        val tidspunkt = Instant.parse("2026-05-01T10:00:00Z")
        val dialog = Dialog.ny(identitetsnummer = lagIdentitetsnummer(), melding = fraNavMeldingMedTidspunkt(tidspunkt))
        dialog.nyMelding(nyFraBehandlerMelding())

        assertEquals(tidspunkt + Duration.ofDays(21), dialog.frist())
    }

    @Test
    fun `frist oppdateres til 21 dager etter purring`() {
        val opprinnelig = Instant.parse("2026-05-01T10:00:00Z")
        val purringTidspunkt = Instant.parse("2026-05-15T10:00:00Z")
        val dialog = Dialog.ny(identitetsnummer = lagIdentitetsnummer(), melding = fraNavMeldingMedTidspunkt(opprinnelig))
        dialog.nyMelding(nyFraBehandlerMelding())
        dialog.nyMelding(fraNavMeldingMedTidspunkt(purringTidspunkt))

        assertEquals(purringTidspunkt + Duration.ofDays(21), dialog.frist())
    }

    private fun nyFraNavMelding(): Dialogmelding.FraNav =
        Dialogmelding.FraNav.ny(
            saksbehandler = lagNavIdent(),
            behandler = lagBehandler(),
            behandlerRef = lagBehandlerRef(),
            melding = "En melding",
        )

    private fun fraNavMeldingMedTidspunkt(tidspunkt: Instant): Dialogmelding.FraNav =
        Dialogmelding.FraNav.fraLagring(
            id = DialogmeldingId(UUID.randomUUID()),
            tidspunkt = tidspunkt,
            melding = "En melding",
            saksbehandler = lagNavIdent(),
            behandler = lagBehandler(),
            behandlerRef = lagBehandlerRef(),
        )

    private fun nyFraBehandlerMelding(): Dialogmelding.FraBehandler =
        Dialogmelding.FraBehandler(
            id = DialogmeldingId(UUID.randomUUID()),
            tidspunkt = Instant.now(),
            melding = "Svar fra behandler",
            behandler = lagBehandler(),
            antallVedlegg = 0,
        )
}
