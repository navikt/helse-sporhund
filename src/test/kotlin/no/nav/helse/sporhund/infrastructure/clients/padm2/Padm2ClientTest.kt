package no.nav.helse.sporhund.infrastructure.clients.padm2

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.access_token.AccessTokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.util.Base64
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Padm2ClientTest {
    private val objectMapper = jacksonObjectMapper()
    private val msgId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val config = Padm2Config(baseUrl = "http://padm2", scope = "padm2-scope")
    private val fakeTokenProvider =
        object : AccessTokenProvider {
            override fun machineToken(scope: String) = "fake-token"

            override fun oboToken(
                accessToken: String,
                scope: String,
            ) = error("ikke i bruk i disse testene")
        }

    private fun lagPdf(tekst: String) = "PDF:$tekst".toByteArray()

    private fun lagMockEngine(
        forventetPath: String,
        responseBody: String,
        statusCode: HttpStatusCode = HttpStatusCode.OK,
    ): HttpClient =
        HttpClient(
            MockEngine { request ->
                assertEquals(forventetPath, request.url.encodedPath)
                assertEquals("Bearer fake-token", request.headers[HttpHeaders.Authorization])
                respond(
                    content = responseBody,
                    status = statusCode,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            },
        )

    @Test
    fun `hentVedlegg returnerer liste med PDFer`() {
        val pdf1 = lagPdf("vedlegg1")
        val pdf2 = lagPdf("vedlegg2")
        val responseBody =
            objectMapper.writeValueAsString(
                listOf(
                    mapOf("content" to Base64.getEncoder().encodeToString(pdf1)),
                    mapOf("content" to Base64.getEncoder().encodeToString(pdf2)),
                ),
            )
        val client =
            Padm2Client(
                config = config,
                accessTokenProvider = fakeTokenProvider,
                httpClient = lagMockEngine("/api/system/v1/vedlegg/$msgId", responseBody),
            )

        val resultat = client.hentVedlegg(msgId)

        assertEquals(2, resultat.size)
        assertContentEquals(pdf1, resultat[0])
        assertContentEquals(pdf2, resultat[1])
    }

    @Test
    fun `hentVedlegg returnerer tom liste når det ikke finnes vedlegg`() {
        val responseBody = objectMapper.writeValueAsString(emptyList<Any>())
        val client =
            Padm2Client(
                config = config,
                accessTokenProvider = fakeTokenProvider,
                httpClient = lagMockEngine("/api/system/v1/vedlegg/$msgId", responseBody),
            )

        val resultat = client.hentVedlegg(msgId)

        assertEquals(0, resultat.size)
    }

    @Test
    fun `hentVedlegg kaster exception ved ikke-OK statuskode`() {
        val client =
            Padm2Client(
                config = config,
                accessTokenProvider = fakeTokenProvider,
                httpClient =
                    lagMockEngine(
                        "/api/system/v1/vedlegg/$msgId",
                        "",
                        HttpStatusCode.NotFound,
                    ),
            )

        assertFailsWith<IllegalStateException> {
            client.hentVedlegg(msgId)
        }
    }

    @Test
    fun `hentVedlegg sender Bearer token i Authorization-header`() {
        var capturedToken: String? = null
        val mockHttpClient =
            HttpClient(
                MockEngine { request ->
                    capturedToken = request.headers[HttpHeaders.Authorization]
                    respond(
                        content = objectMapper.writeValueAsString(emptyList<Any>()),
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                },
            )
        val client =
            Padm2Client(
                config = config,
                accessTokenProvider = fakeTokenProvider,
                httpClient = mockHttpClient,
            )

        client.hentVedlegg(msgId)

        assertEquals("Bearer fake-token", capturedToken)
    }
}
