package no.nav.helse.sporhund.infrastructure.clients.dokarkiv

import com.github.navikt.tbd_libs.access_token.AccessTokenProvider
import com.github.navikt.tbd_libs.retry.retry
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.preparePatch
import io.ktor.client.request.preparePost
import io.ktor.client.request.preparePut
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.sporhund.application.KnyttInnkommendeJournalpost
import no.nav.helse.sporhund.application.MeldingTilBehandlerPdfInput
import no.nav.helse.sporhund.application.OpprettUtgåendeJournalpost
import no.nav.helse.sporhund.application.PdfProvider
import no.nav.helse.sporhund.application.logg.logg
import no.nav.helse.sporhund.application.logg.teamLogs
import no.nav.helse.sporhund.domain.Fagområde
import no.nav.helse.sporhund.domain.Identitetsnummer
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.net.ssl.SSLHandshakeException
import kotlin.io.encoding.Base64

class DokarkivClient(
    private val dokarkivConfig: DokarkivConfig,
    private val pdfProvider: PdfProvider,
    private val accessTokenProvider: AccessTokenProvider,
    private val httpClient: HttpClient = HttpClient(CIO),
) {
    fun journalførUtgåendeDialogmelding(melding: OpprettUtgåendeJournalpost) {
        val pdf = pdfProvider.genererPdf(melding.tilPdfInput())
        val fagområde =
            when (melding.fagområde) {
                Fagområde.EnkeltståendeBehandlingsdager -> "enkeltstående behandlingsdager"
                Fagområde.Tilbakedatering -> "tilbakedatering"
                Fagområde.Yrkesskade -> "yrkesskade"
                Fagområde.Bestridelse -> "bestridelse"
            }
        val journalpostPayload =
            JournalpostPayload(
                tittel = "Dialogmelding - $fagområde",
                journalpostType = "SYK",
                bruker =
                    Bruker(
                        id = melding.gjelder.value,
                    ),
                sak =
                    JournalpostPayload.Sak(
                        fagsakId = melding.conversationRef.value.toString(),
                    ),
                dokumenter =
                    listOf(
                        JournalpostPayload.Dokument(
                            "Dialogmelding - $fagområde",
                            dokumentvarianter =
                                listOf(
                                    JournalpostPayload.Dokument.DokumentVariant(
                                        fysiskDokument = Base64.encode(pdf),
                                    ),
                                ),
                        ),
                    ),
                eksternReferanseId = melding.meldingId.value.toString(),
            )
        runBlocking {
            httpClient
                .preparePost("${dokarkivConfig.baseUrl}/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=true") {
                    bearerAuth(accessTokenProvider.machineToken(dokarkivConfig.scope))
                    contentType(ContentType.Application.Json)
                    setBody(journalpostPayload)
                }.executeRetry(avbryt = { it::class !in forsøkPåNy }) {
                    håndterPostJournalpostResponse(it, journalpostPayload)
                }
        }
    }

    fun feilregistrerOgKnyttJournalpost(melding: KnyttInnkommendeJournalpost) {
        feilregistrer(melding.journalpostId)
        knyttTilAnnenSak(melding.journalpostId, melding.conversationRef.value.toString(), melding.identitetsnummer)
    }

    private fun feilregistrer(journalpostId: String) {
        runBlocking {
            httpClient
                .preparePatch("${dokarkivConfig.baseUrl}/rest/journalpostapi/v1/journalpost/$journalpostId/feilregistrer/feilregistrerSakstilknytning") {
                    bearerAuth(accessTokenProvider.machineToken(dokarkivConfig.scope))
                }.executeRetry(avbryt = { it::class !in forsøkPåNy }) {
                    when (it.status.value) {
                        in (200 until 300) -> logg.info("Journalpost $journalpostId feilregistrert")

                        else -> {
                            val error = it.body<String>()
                            logg.error("Feil fra Dokarkiv ved feilregistrering av journalpost: {}", keyValue("response", error))
                            teamLogs.error(
                                "Feil fra Dokarkiv ved feilregistrering av journalpost: {}, {}",
                                keyValue("journalpostId", journalpostId),
                                keyValue("response", error),
                            )
                            throw DokarkivClientException("Feil fra Dokarkiv ved feilregistrering av journalpost: $error")
                        }
                    }
                }
        }
    }

    private fun knyttTilAnnenSak(
        journalpostId: String,
        fagsakId: String,
        identitetsnummer: Identitetsnummer,
    ) {
        val payload = KnyttTilAnnenSakPayload(fagsakId = fagsakId, bruker = Bruker(id = identitetsnummer.value))
        runBlocking {
            httpClient
                .preparePut("${dokarkivConfig.baseUrl}/rest/journalpostapi/v1/journalpost/$journalpostId/knyttTilAnnenSak") {
                    bearerAuth(accessTokenProvider.machineToken(dokarkivConfig.scope))
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }.executeRetry(avbryt = { it::class !in forsøkPåNy }) {
                    when (it.status.value) {
                        in (200 until 300) -> logg.info("Journalpost $journalpostId knyttet til sak $fagsakId")

                        else -> {
                            val error = it.body<String>()
                            logg.error("Feil fra Dokarkiv ved knytting av journalpost til annen sak: {}", keyValue("response", error))
                            teamLogs.error(
                                "Feil fra Dokarkiv ved knytting av journalpost til annen sak: {}, {}",
                                keyValue("journalpostId", journalpostId),
                                keyValue("response", error),
                            )
                            throw DokarkivClientException("Feil fra Dokarkiv ved knytting av journalpost til annen sak: $error")
                        }
                    }
                }
        }
    }

    private suspend fun håndterPostJournalpostResponse(
        response: HttpResponse,
        journalpostPayload: JournalpostPayload,
    ): Boolean =
        when (response.status.value) {
            in (200 until 300) -> true

            409 -> {
                logg.info(
                    "Fikk HTTP 409 (Conflict) fra Dokarkiv ved journalføring," +
                        " går videre ettersom dette skal bety at noe allerede er journalført knyttet til denne dialogmeldingen" +
                        " (eksternReferanseId ${journalpostPayload.eksternReferanseId})",
                )
                true
            }

            else -> {
                val error = response.body<String>()
                logg.error("Feil fra Dokarkiv: {}", keyValue("response", error))
                teamLogs.error(
                    "Feil fra Dokarkiv: {}, {}",
                    keyValue(journalpostPayload.bruker.idType, journalpostPayload.bruker.id),
                    keyValue("response", error),
                )
                throw DokarkivClientException("Feil fra Dokarkiv: $error")
            }
        }

    internal companion object {
        internal class DokarkivClientException(
            feil: String,
        ) : RuntimeException(feil)

        private val forsøkPåNy = setOf(ClosedReceiveChannelException::class, SSLHandshakeException::class, HttpRequestTimeoutException::class, DokarkivClientException::class)

        private suspend fun <T> HttpStatement.executeRetry(
            avbryt: (throwable: Throwable) -> Boolean = { false },
            block: suspend (response: HttpResponse) -> T,
        ) = retry(avbryt = avbryt) { execute { block(it) } }
    }
}

private data class KnyttTilAnnenSakPayload(
    val sakstype: String = "FAGSAK",
    val fagsakId: String,
    val fagsaksystem: String = "SPEIL",
    val tema: String = "SYK",
    val journalfoerendeEnhet: String = "9999",
    val bruker: Bruker,
)

private data class Bruker(
    val id: String,
    val idType: String = "FNR",
)

private data class JournalpostPayload(
    val tittel: String,
    val journalpostType: String = "UTGAAENDE",
    val tema: String = "SYK",
    val behandlingstema: String = "ab0061",
    val journalfoerendeEnhet: String = "9999",
    val bruker: Bruker,
    val sak: Sak,
    val dokumenter: List<Dokument>,
    val eksternReferanseId: String,
) {
    data class Sak(
        val sakstype: String = "FAGSAK",
        val fagsakId: String,
        val fagsaksystem: String = "SPEIL",
    )

    data class Dokument(
        val tittel: String,
        val dokumentvarianter: List<DokumentVariant>,
    ) {
        data class DokumentVariant(
            val filtype: String = "PDFA",
            val fysiskDokument: String,
            val variantformat: String = "ARKIV",
        )
    }
}

private val tidspunktFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Oslo"))

private fun OpprettUtgåendeJournalpost.tilPdfInput() =
    MeldingTilBehandlerPdfInput(
        conversationRef = conversationRef.value.toString(),
        fra =
            MeldingTilBehandlerPdfInput.Fra(
                NAVIdent = avsender.ident.value,
                navn = avsender.navn,
            ),
        til =
            MeldingTilBehandlerPdfInput.Til(
                navn = mottaker.navn.let { "${it.fornavn}${it.mellomnavn?.let { m -> " $m" } ?: ""} ${it.etternavn}" },
                kontor =
                    MeldingTilBehandlerPdfInput.Til.Kontor(
                        navn = mottaker.kontor.navn ?: "",
                        organisasjonsnummer = mottaker.kontor.organisasjonsnummer?.value ?: "",
                        adresse =
                            MeldingTilBehandlerPdfInput.Til.Kontor.Adresse(
                                gate = mottaker.kontor.adresse?.veiadresse ?: "",
                                postnummer = mottaker.kontor.adresse?.postnummer ?: "",
                                poststed = mottaker.kontor.adresse?.poststed ?: "",
                            ),
                    ),
            ),
        tidspunkt = tidspunktFormatter.format(tidspunkt),
        gjelder =
            MeldingTilBehandlerPdfInput.Gjelder(
                fødselsnummer = gjelder.value,
                navn = søkernavn.let { "${it.fornavn}${it.mellomnavn?.let { m -> " $m" } ?: ""} ${it.etternavn}" },
            ),
        meldingstype = dialogtype.name,
        fagområde = fagområde.name,
        melding = tekst,
    )
