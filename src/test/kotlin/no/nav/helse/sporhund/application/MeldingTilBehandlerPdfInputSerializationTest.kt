package no.nav.helse.sporhund.application

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.sporhund.infrastructure.db.objectMapper
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MeldingTilBehandlerPdfInputSerializationTest {
    private val input =
        MeldingTilBehandlerPdfInput(
            conversationRef = "conv-123",
            fra =
                MeldingTilBehandlerPdfInput.Fra(
                    NAVIdent = "A123456",
                    navn = "Saksbehandler Navn",
                ),
            til =
                MeldingTilBehandlerPdfInput.Til(
                    navn = "Lege Legesen",
                    kontor =
                        MeldingTilBehandlerPdfInput.Til.Kontor(
                            navn = "Legekontor AS",
                            organisasjonsnummer = "999999999",
                            adresse =
                                MeldingTilBehandlerPdfInput.Til.Kontor.Adresse(
                                    gate = "Storgata 1",
                                    postnummer = "0001",
                                    poststed = "Oslo",
                                ),
                        ),
                ),
            tidspunkt = LocalDateTime.of(2026, 6, 10, 12, 0),
            gjelder =
                MeldingTilBehandlerPdfInput.Gjelder(
                    fødselsnummer = "12345678901",
                    navn = "Pasient Pasientsen",
                ),
            meldingstype = "DIALOG_NOTAT",
            fagområde = "Sykepenger",
            melding = "Hei, dette er en test.",
        )

    @Test
    fun `NAVIdent serialiseres med korrekt store bokstaver`() {
        val json = objectMapper.writeValueAsString(input)

        assertTrue(json.contains("\"NAVIdent\""), "Forventet nøkkelen \"NAVIdent\" i JSON, men fant: $json")
    }

    @Test
    fun `NAVIdent skal ikke bli lowercase i JSON`() {
        val json = objectMapper.writeValueAsString(input)

        assertFalse(json.contains("\"navident\""), "NAVIdent ble feilaktig lowercaset til \"navident\" i JSON: $json")
        assertFalse(json.contains("\"nAVIdent\""), "NAVIdent ble feilaktig endret til \"nAVIdent\" i JSON: $json")
    }

    @Test
    fun `NAVIdent-verdien bevares korrekt ved serialisering`() {
        val json = objectMapper.writeValueAsString(input)
        val node = objectMapper.readTree(json)

        assertEquals("A123456", node["fra"]["NAVIdent"].asText())
    }

    @Test
    fun `round-trip serialisering og deserialisering bevarer NAVIdent`() {
        val json = objectMapper.writeValueAsString(input)
        val deserialisert = objectMapper.readValue<MeldingTilBehandlerPdfInput>(json)

        assertEquals(input.fra.NAVIdent, deserialisert.fra.NAVIdent)
    }
}
