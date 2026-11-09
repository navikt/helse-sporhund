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
import io.ktor.serialization.jackson3.jackson
import no.nav.helse.sporhund.application.UtgåendeDialogmeldingJournalpost
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertTrue

class DokarkivClientTest {
    private val baseUrl = "http://dokarkiv"
    private val config = DokarkivConfig(baseUrl = baseUrl, scope = "test-scope")
    private val accessTokenProvider =
        object : AccessTokenProvider {
            override fun machineToken(scope: String): String = "test-token"

            override fun oboToken(
                accessToken: String,
                scope: String,
            ): String = "test-token"
        }

    @Test
    fun `journalførUtgående sender POST til riktig URL`() {
        val engine =
            lagMockEngine {
                respond(
                    """
                    {
                      "dokumenter": [
                        {
                          "dokumentInfoId": "123"
                        }
                      ],
                      "journalpostId": "en-id",
                      "journalpostferdigstilt": true
                    }
                    """.trimIndent(),
                    HttpStatusCode.Created,
                    headersOf(),
                )
            }
        lagDokarkivClient(engine).journalførUtgående(lagUtgåendeJournalpost(), ByteArray(16))

        assertEquals(1, engine.requestHistory.size)
        assertEquals(HttpMethod.Post, engine.requestHistory[0].method)
        assertEquals("/rest/journalpostapi/v1/journalpost", engine.requestHistory[0].url.encodedPath)
    }

    @Test
    fun `journalførUtgående ignorerer 409 Conflict`() {
        val engine = lagMockEngine { respond("", HttpStatusCode.Conflict, headersOf()) }
        assertDoesNotThrow {
            lagDokarkivClient(engine).journalførUtgående(lagUtgåendeJournalpost(), ByteArray(16))
        }
    }

    @Test
    fun `journalførUtgående kaster exception ved feil fra dokarkiv`() {
        val engine = lagMockEngine { respond("feil", HttpStatusCode.InternalServerError, headersOf()) }
        assertThrows<Exception> {
            lagDokarkivClient(engine).journalførUtgående(lagUtgåendeJournalpost(), ByteArray(16))
        }
    }

    @Test
    fun `feilregistrerOgKnyttInnkommende sender PATCH, så PUT`() {
        val engine = lagMockEngine { respond("""{ "nyJournalpostId": 12344512 }""", HttpStatusCode.OK, headersOf()) }
        lagDokarkivClient(engine).feilregistrerOgKnyttInnkommende("JP-123")

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
    fun `feilregistrerOgKnyttInnkommende kaller ikke knyttTilAnnenSak dersom feilregistrer feiler`() {
        val engine = lagMockEngine { respond("feil", HttpStatusCode.InternalServerError, headersOf()) }
        assertThrows<Exception> {
            lagDokarkivClient(engine).feilregistrerOgKnyttInnkommende("JP-456")
        }

        assertEquals(4, engine.requestHistory.size)
        assertTrue { engine.requestHistory.all { it.method == HttpMethod.Patch } }
    }

    private fun lagMockEngine(handler: suspend MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData) = MockEngine(handler)

    private fun lagDokarkivClient(engine: MockEngine) =
        DokarkivClient(
            dokarkivConfig = config,
            accessTokenProvider = accessTokenProvider,
            httpClient =
                HttpClient(engine) {
                    install(ContentNegotiation) { jackson() }
                },
        )

    private fun lagUtgåendeJournalpost(): UtgåendeDialogmeldingJournalpost {
        val dialog = lagDialog()
        val melding = dialog.nyesteMeldingFraNav()
        return UtgåendeDialogmeldingJournalpost(
            conversationRef = dialog.conversationRef,
            meldingId = melding.id,
            mottaker = melding.behandler,
            gjelder = dialog.identitetsnummer,
            fagområde = dialog.fagområde,
        )
    }

    private fun DokarkivClient.feilregistrerOgKnyttInnkommende(journalpostId: String) {
        val dialog = lagDialog()
        feilregistrerOgKnyttInnkommende(journalpostId, dialog.conversationRef, dialog.identitetsnummer)
    }
}
