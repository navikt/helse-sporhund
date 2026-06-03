package no.nav.helse.sporhund.domain

import no.nav.helse.sporhund.domain.testhelpers.*
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SendPurringTest {
    @Test
    fun `sendPurring setter status til PurringSendt`() {
        val dialog = lagDialog(status = Dialogstatus.ForespørselSendt)

        dialog.sendPurring()

        assertEquals(Dialogstatus.PurringSendt, dialog.status)
    }

    @Test
    fun `sendPurring legger til ny FraNav-melding i dialogen`() {
        val dialog = lagDialog(status = Dialogstatus.ForespørselSendt)
        val antallFør = dialog.meldinger.size

        dialog.sendPurring()

        assertEquals(antallFør + 1, dialog.meldinger.size)
        assertIs<Dialogmelding.FraNav>(dialog.nyesteMelding())
    }

    @Test
    fun `sendPurring emitter event med erPurring = true og ny meldingId`() {
        val behandlerRef = lagBehandlerRef()
        val dialog =
            lagDialog(
                status = Dialogstatus.ForespørselSendt,
                melding = lagFraNavMelding(behandlerRef = behandlerRef),
            )

        dialog.sendPurring()

        val events = dialog.events()
        assertEquals(1, events.size)
        val event = events.single()
        assertTrue(event.erPurring)
        assertEquals(dialog.conversationRef, event.conversationRef)
        assertEquals(behandlerRef, event.behandlerRef)
        assertEquals(dialog.nyesteMelding().id, event.meldingId)
    }

    @Test
    fun `sendPurring oppdaterer frist til 21 dager etter purringen`() {
        val opprinnelig = Instant.parse("2026-05-01T10:00:00Z")
        val dialog =
            lagDialog(
                status = Dialogstatus.ForespørselSendt,
                melding = lagFraNavMelding(opprettet = opprinnelig),
            )

        dialog.sendPurring()

        val purringTidspunkt = (dialog.nyesteMelding() as Dialogmelding.FraNav).tidspunkt
        assertEquals(purringTidspunkt + Duration.ofDays(21), dialog.frist())
    }

    @Test
    fun `sendPurring bruker standard purring-tekst med dato for opprinnelig forespørsel`() {
        val opprinneligDato = Instant.parse("2026-05-01T10:00:00Z")
        val dialog =
            lagDialog(
                status = Dialogstatus.ForespørselSendt,
                melding = lagFraNavMelding(opprettet = opprinneligDato),
            )
        val forventetDatoStr =
            opprinneligDato
                .atZone(ZoneId.of("Europe/Oslo"))
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

        dialog.sendPurring()

        val purringMelding = dialog.nyesteMelding() as Dialogmelding.FraNav
        assertTrue(purringMelding.melding.contains(forventetDatoStr))
        assertTrue(purringMelding.melding.contains("Vi viser til tidligere forespørsel av"))
        assertTrue(purringMelding.melding.contains("Vi kan ikke se å ha mottatt svar"))
    }
}
