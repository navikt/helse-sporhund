package no.nav.helse.sporhund.infrastructure.clients.padm2

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.access_token.AccessTokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import no.nav.helse.sporhund.application.VedleggProvider
import org.slf4j.LoggerFactory
import java.util.Base64
import java.util.UUID

internal data class VedleggDto(
    val content: String,
)

class Padm2Client(
    private val config: Padm2Config,
    private val accessTokenProvider: AccessTokenProvider,
    private val httpClient: HttpClient = HttpClient(CIO),
) : VedleggProvider {
    private val log = LoggerFactory.getLogger(Padm2Client::class.java)
    private val objectMapper = jacksonObjectMapper()

    override fun hentVedlegg(msgId: UUID): List<ByteArray> {
        val token = accessTokenProvider.machineToken(config.scope)
        return runCatching {
            kotlinx.coroutines.runBlocking {
                val response =
                    httpClient.get("${config.baseUrl}/api/system/v1/vedlegg/$msgId") {
                        bearerAuth(token)
                    }
                check(response.status == HttpStatusCode.OK) {
                    "Uventet statuskode fra padm2: ${response.status} for msgId=$msgId"
                }
                val vedleggListe: List<VedleggDto> = objectMapper.readValue(response.bodyAsText())
                vedleggListe.map { Base64.getDecoder().decode(it.content) }
            }
        }.onFailure {
            log.error("Feil ved henting av vedlegg fra padm2 for msgId=$msgId", it)
        }.getOrThrow()
    }
}
