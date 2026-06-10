package no.nav.helse.sporhund.infrastructure.clients.sprinter

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import no.nav.helse.sporhund.application.MeldingTilBehandlerPdfInput
import no.nav.helse.sporhund.infrastructure.db.objectMapper
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SprinterClientTest {
    private val fakePdf = "%PDF-1.4 mock".toByteArray()

    private val input =
        MeldingTilBehandlerPdfInput(
            conversationRef = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
            fra =
                MeldingTilBehandlerPdfInput.Fra(
                    NAVIdent = "Z123456",
                    navn = "Ola Nordmann",
                ),
            til =
                MeldingTilBehandlerPdfInput.Til(
                    navn = "Kari Lise Nordmann",
                    kontor =
                        MeldingTilBehandlerPdfInput.Til.Kontor(
                            navn = "Oslo Legekontor",
                            organisasjonsnummer = "123456789",
                            adresse =
                                MeldingTilBehandlerPdfInput.Til.Kontor.Adresse(
                                    gate = "Storgata 1",
                                    postnummer = "0123",
                                    poststed = "Oslo",
                                ),
                        ),
                ),
            tidspunkt = LocalDateTime.of(2024, 6, 1, 12, 0),
            gjelder =
                MeldingTilBehandlerPdfInput.Gjelder(
                    fødselsnummer = "01010112345",
                    navn = "Mia Cathrine Svendsen",
                ),
            meldingstype = "Ekstra uttalelser fra lege",
            fagområde = "Bestridelse",
            melding = "Hei, vedlagt finner dere den forespurte dokumentasjonen. Jeg har lagt ved relevant journaldokumentasjon og vurdering av pasientens tilstand. Ta gjerne kontakt dersom dere trenger ytterligere opplysninger.",
        )

    @Test
    fun `genererPdf sender JSON til riktig endepunkt`() {
        val (engine, _) = lagClientOgEngine()
        lagSprinterClient(engine).genererPdf(input)

        assertEquals(1, engine.requestHistory.size)
        assertEquals(
            "/api/v1/genpdf/sporhund/melding_til_behandler",
            engine.requestHistory[0].url.encodedPath,
        )
    }

    @Test
    fun `genererPdf sender JSON med korrekte feltnavn på toppnivå`() {
        val (engine, capturedBodies) = lagClientOgEngine()
        lagSprinterClient(engine).genererPdf(input)

        val node = objectMapper.readTree(capturedBodies.single())
        assertNotNull(node["conversationRef"], "Mangler felt: conversationRef")
        assertNotNull(node["fra"], "Mangler felt: fra")
        assertNotNull(node["til"], "Mangler felt: til")
        assertNotNull(node["tidspunkt"], "Mangler felt: tidspunkt")
        assertNotNull(node["gjelder"], "Mangler felt: gjelder")
        assertNotNull(node["meldingstype"], "Mangler felt: meldingstype")
        assertNotNull(node["fagområde"], "Mangler felt: fagområde")
        assertNotNull(node["melding"], "Mangler felt: melding")
    }

    @Test
    fun `genererPdf serialiserer NAVIdent med korrekt store bokstaver`() {
        val (engine, capturedBodies) = lagClientOgEngine()
        lagSprinterClient(engine).genererPdf(input)

        val fra = objectMapper.readTree(capturedBodies.single())["fra"]
        assertNotNull(fra["NAVIdent"], "Mangler felt: fra.NAVIdent — ble det kanskje lowercaset til 'navident'?")
        assertEquals("Z123456", fra["NAVIdent"].asText())
    }

    @Test
    fun `genererPdf serialiserer fødselsnummer med korrekt norsk tegn`() {
        val (engine, capturedBodies) = lagClientOgEngine()
        lagSprinterClient(engine).genererPdf(input)

        val gjelder = objectMapper.readTree(capturedBodies.single())["gjelder"]
        assertNotNull(gjelder["fødselsnummer"], "Mangler felt: gjelder.fødselsnummer")
        assertEquals("01010112345", gjelder["fødselsnummer"].asText())
    }

    @Test
    fun `genererPdf serialiserer fagområde med korrekt norsk tegn`() {
        val (engine, capturedBodies) = lagClientOgEngine()
        lagSprinterClient(engine).genererPdf(input)

        val node = objectMapper.readTree(capturedBodies.single())
        assertNotNull(node["fagområde"], "Mangler felt: fagområde")
        assertEquals("Bestridelse", node["fagområde"].asText())
    }

    @Test
    fun `genererPdf serialiserer tidspunkt i ISO-format`() {
        val (engine, capturedBodies) = lagClientOgEngine()
        lagSprinterClient(engine).genererPdf(input)

        val tidspunkt = objectMapper.readTree(capturedBodies.single())["tidspunkt"].asText()
        assertEquals("2024-06-01T12:00:00", tidspunkt)
    }

    private fun lagClientOgEngine(): Pair<MockEngine, MutableList<String>> {
        val capturedBodies = mutableListOf<String>()
        val engine =
            MockEngine { request ->
                capturedBodies += (request.body as TextContent).text
                respond(
                    content = fakePdf,
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "application/pdf"),
                )
            }
        return engine to capturedBodies
    }

    private fun lagSprinterClient(engine: MockEngine) =
        SprinterClient(
            sprinterConfig = SprinterConfig(baseUrl = "http://sprinter"),
            httpClient = HttpClient(engine),
        )
}
