package no.nav.helse.sporhund.infrastructure.clients.dokarkiv

import com.github.navikt.tbd_libs.access_token.AccessTokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import no.nav.helse.sporhund.application.KnyttInnkommendeJournalpost
import no.nav.helse.sporhund.application.MeldingTilBehandlerPdfInput
import no.nav.helse.sporhund.application.OpprettUtgåendeJournalpost
import no.nav.helse.sporhund.application.OutboxMelding
import no.nav.helse.sporhund.application.PdfProvider
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.domain.testhelpers.lagFraNavMelding
import no.nav.helse.sporhund.domain.testhelpers.lagSaksbehandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertTrue

class DokarkivClientTest {
    private val baseUrl = "http://dokarkiv"
    private val config = DokarkivConfig(baseUrl = baseUrl, scope = "test-scope")
    private val pdfProvider =
        object : PdfProvider {
            override fun genererPdf(meldingTilBehandlerPdfInput: MeldingTilBehandlerPdfInput) = ByteArray(16)
        }
    private val accessTokenProvider =
        object : AccessTokenProvider {
            override fun machineToken(scope: String): String = "test-token"

            override fun oboToken(
                accessToken: String,
                scope: String,
            ): String = "test-token"
        }

    @Test
    fun `journalførUtgåendeDialogmelding sender POST til riktig URL`() {
        val engine = lagMockEngine { respond("", HttpStatusCode.Created, headersOf()) }
        lagDokarkivClient(engine).journalførUtgåendeDialogmelding(lagOpprettUtgåendeJournalpost())

        assertEquals(1, engine.requestHistory.size)
        assertEquals(HttpMethod.Post, engine.requestHistory[0].method)
        assertEquals("/rest/journalpostapi/v1/journalpost", engine.requestHistory[0].url.encodedPath)
    }

    @Test
    fun `journalførUtgåendeDialogmelding ignorerer 409 Conflict`() {
        val engine = lagMockEngine { respond("", HttpStatusCode.Conflict, headersOf()) }
        assertDoesNotThrow {
            lagDokarkivClient(engine).journalførUtgåendeDialogmelding(lagOpprettUtgåendeJournalpost())
        }
    }

    @Test
    fun `journalførUtgåendeDialogmelding kaster exception ved feil fra dokarkiv`() {
        val engine = lagMockEngine { respond("feil", HttpStatusCode.InternalServerError, headersOf()) }
        assertThrows<Exception> {
            lagDokarkivClient(engine).journalførUtgåendeDialogmelding(lagOpprettUtgåendeJournalpost())
        }
    }

    @Test
    fun `feilregistrerOgKnyttJournalpost sender PATCH og deretter PUT`() {
        val engine = lagMockEngine { respond("", HttpStatusCode.OK, headersOf()) }
        lagDokarkivClient(engine).feilregistrerOgKnyttJournalpost(lagKnyttInnkommendeJournalpost("JP-123"))

        assertEquals(2, engine.requestHistory.size)
        assertEquals(HttpMethod.Patch, engine.requestHistory[0].method)
        assertEquals(
            "/rest/journalpostapi/v1/journalpost/JP-123/feilregistrer/feilregistrerSakstilknytning",
            engine.requestHistory[0].url.encodedPath,
        )
        assertEquals(HttpMethod.Put, engine.requestHistory[1].method)
        assertEquals(
            "/rest/journalpostapi/v1/journalpost/JP-123/knyttTilAnnenSak",
            engine.requestHistory[1].url.encodedPath,
        )
    }

    @Test
    fun `feilregistrerOgKnyttJournalpost kaller ikke knyttTilAnnenSak dersom feilregistrer feiler`() {
        val engine = lagMockEngine { respond("feil", HttpStatusCode.InternalServerError, headersOf()) }
        assertThrows<Exception> {
            lagDokarkivClient(engine).feilregistrerOgKnyttJournalpost(lagKnyttInnkommendeJournalpost())
        }

        assertEquals(4, engine.requestHistory.size)
        assertTrue { engine.requestHistory.all { it.method == HttpMethod.Patch } }
    }

    private fun lagMockEngine(handler: suspend MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData) = MockEngine(handler)

    private fun lagDokarkivClient(engine: MockEngine) =
        DokarkivClient(
            dokarkivConfig = config,
            pdfProvider = pdfProvider,
            accessTokenProvider = accessTokenProvider,
            httpClient =
                HttpClient(engine) {
                    install(ContentNegotiation) { jackson() }
                },
        )

    private fun lagOpprettUtgåendeJournalpost(): OpprettUtgåendeJournalpost =
        OutboxMelding.opprettUtgåendeJournalpost(
            melding = lagFraNavMelding(),
            dialog = lagDialog(),
            avsender = lagSaksbehandler(),
        )

    private fun lagKnyttInnkommendeJournalpost(journalpostId: String = "JP-456"): KnyttInnkommendeJournalpost = OutboxMelding.knyttInnkommendeJournalpost(journalpostId, lagDialog())
}
