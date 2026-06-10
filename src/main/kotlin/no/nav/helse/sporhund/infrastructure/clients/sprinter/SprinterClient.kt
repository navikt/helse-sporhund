package no.nav.helse.sporhund.infrastructure.clients.sprinter

import com.github.navikt.tbd_libs.retry.retry
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.helse.sporhund.application.MeldingTilBehandlerPdfInput
import no.nav.helse.sporhund.application.PdfProvider
import no.nav.helse.sporhund.application.logg.teamLogs
import no.nav.helse.sporhund.infrastructure.db.objectMapper

class SprinterClient(
    val sprinterConfig: SprinterConfig,
    private val httpClient: HttpClient = HttpClient(CIO),
) : PdfProvider {
    override fun genererPdf(meldingTilBehandlerPdfInput: MeldingTilBehandlerPdfInput): ByteArray =
        runBlocking {
            produserPdfBytes(
                url = "${sprinterConfig.baseUrl}/api/v1/genpdf/sporhund/melding_til_behandler",
                input = meldingTilBehandlerPdfInput,
            )
        }

    private suspend fun produserPdfBytes(
        url: String,
        input: Any,
    ): ByteArray {
        val body = objectMapper.writeValueAsString(input)
        teamLogs.info("Payload til PDF-generering: $body")
        return retry {
            httpClient
                .preparePost(url) {
                    contentType(Json)
                    setBody(body)
                    expectSuccess = true
                }.execute { response ->
                    response.body<ByteArray?>()
                }
        }?.takeUnless { it.isEmpty() } ?: error("Fikk tom pdf")
    }
}
