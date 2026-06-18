package no.nav.helse.sporhund.infrastructure.clients.dokarkiv

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.access_token.AccessTokenProvider
import com.github.navikt.tbd_libs.retry.retry
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.runBlocking
import no.nav.helse.sporhund.application.KnyttInnkommendeJournalpost
import no.nav.helse.sporhund.application.MeldingTilBehandlerPdfInput
import no.nav.helse.sporhund.application.OpprettUtgåendeJournalpost
import no.nav.helse.sporhund.application.PdfProvider
import no.nav.helse.sporhund.application.logg.loggError
import no.nav.helse.sporhund.application.logg.loggInfo
import no.nav.helse.sporhund.domain.Fagområde
import no.nav.helse.sporhund.domain.Identitetsnummer
import no.nav.helse.sporhund.infrastructure.db.objectMapper
import org.intellij.lang.annotations.Language
import java.time.ZoneId
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
                bruker =
                    Bruker(
                        id = melding.gjelder.value,
                    ),
                avsenderMottaker =
                    AvsenderMottaker(
                        melding.mottaker.navn.fulltNavn(),
                        id =
                            melding.mottaker.hprNummer.value
                                .toString(),
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
                    setBody(objectMapper.writeValueAsString(journalpostPayload))
                }.executeRetry(avbryt = { it::class !in forsøkPåNy }) {
                    håndterPostJournalpostResponse(it, journalpostPayload)
                }
        }
    }

    fun feilregistrerOgKnyttJournalpost(melding: KnyttInnkommendeJournalpost) {
        feilregistrer(melding.journalpostId)
        val response =
            knyttTilAnnenSak(melding.journalpostId, melding.conversationRef.value.toString(), melding.identitetsnummer)
        ferdigstill(response.nyJournalpostId)
    }

    private fun feilregistrer(journalpostId: String) {
        runBlocking {
            httpClient
                .preparePatch("${dokarkivConfig.baseUrl}/rest/journalpostapi/v1/journalpost/$journalpostId/feilregistrer/feilregistrerSakstilknytning") {
                    bearerAuth(accessTokenProvider.machineToken(dokarkivConfig.scope))
                }.executeRetry(avbryt = { it::class !in forsøkPåNy }) {
                    when (it.status.value) {
                        in (200 until 300) -> loggInfo("Journalpost med journalpostId=$journalpostId feilregistrert")

                        else -> {
                            val error = it.body<String>()
                            loggError("Feil fra Dokarkiv ved feilregistrering av journalpost med journalpostId=$journalpostId: response=$error")
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
    ): DokarkivPutKnyttTilAnnenSakResponse =
        runBlocking {
            httpClient
                .preparePut("${dokarkivConfig.baseUrl}/rest/journalpostapi/v1/journalpost/$journalpostId/knyttTilAnnenSak") {
                    bearerAuth(accessTokenProvider.machineToken(dokarkivConfig.scope))
                    contentType(ContentType.Application.Json)
                    setBody(
                        objectMapper.writeValueAsString(
                            KnyttTilAnnenSakPayload(
                                fagsakId = fagsakId,
                                bruker = Bruker(id = identitetsnummer.value),
                            ),
                        ),
                    )
                }.executeRetry(avbryt = { it::class !in forsøkPåNy }) {
                    when (it.status.value) {
                        in (200 until 300) -> {
                            val response = objectMapper.readValue<DokarkivPutKnyttTilAnnenSakResponse>(it.bodyAsText())
                            loggInfo("Journalpost med opprinnelig journalpostId=$journalpostId har fått ny journalpostId=${response.nyJournalpostId} og blitt knyttet til sak med fagsakId=$fagsakId")
                            response
                        }

                        else -> {
                            val error = it.body<String>()
                            loggError("Feil fra Dokarkiv ved knytting av journalpost med journalpostId=$journalpostId til annen sak: response=$error")
                            throw DokarkivClientException("Feil fra Dokarkiv ved knytting av journalpost til annen sak: $error")
                        }
                    }
                }
        }

    private fun ferdigstill(journalpostId: String) {
        runBlocking {
            httpClient
                .preparePatch("${dokarkivConfig.baseUrl}/rest/journalpostapi/v1/journalpost/$journalpostId/ferdigstill") {
                    bearerAuth(accessTokenProvider.machineToken(dokarkivConfig.scope))
                    contentType(ContentType.Application.Json)
                    @Language("JSON")
                    val body = """ { "journalfoerendeEnhet": "9999" } """
                    setBody(body)
                }.executeRetry(avbryt = { it::class !in forsøkPåNy }) {
                    when (it.status.value) {
                        in (200 until 300) -> loggInfo("Journalpost med journalpostId=$journalpostId ferdigstilt")

                        else -> {
                            val error = it.body<String>()
                            loggError("Feil fra Dokarkiv ved ferdigstilling av journalpost med journalpostId=$journalpostId: response=$error")
                            throw DokarkivClientException("Feil fra Dokarkiv ved ferdigstilling av journalpost: $error")
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
            in (200 until 300) -> {
                val response = objectMapper.readValue<DokarkivPostJournalpostResponse>(response.bodyAsText())
                loggInfo("Journalpost opprettet. Respons fra dokarkiv: journalpostId=${response.journalpostId}, ferdigstilt=${response.journalpostferdigstilt}")
                true
            }

            409 -> {
                loggInfo(
                    "Fikk HTTP 409 (Conflict) fra Dokarkiv ved journalføring," +
                        " går videre ettersom dette skal bety at noe allerede er journalført knyttet til denne dialogmeldingen" +
                        " (eksternReferanseId ${journalpostPayload.eksternReferanseId})",
                )
                true
            }

            else -> {
                val error = response.body<String>()
                loggError("Feil fra Dokarkiv: response=$error", "identitetsnummer" to journalpostPayload.bruker.id)
                throw DokarkivClientException("Feil fra Dokarkiv: $error")
            }
        }

    internal companion object {
        internal class DokarkivClientException(
            feil: String,
        ) : RuntimeException(feil)

        private val forsøkPåNy =
            setOf(
                ClosedReceiveChannelException::class,
                SSLHandshakeException::class,
                HttpRequestTimeoutException::class,
                DokarkivClientException::class,
            )

        private suspend fun <T> HttpStatement.executeRetry(
            avbryt: (throwable: Throwable) -> Boolean = { false },
            block: suspend (response: HttpResponse) -> T,
        ) = retry(avbryt = avbryt) { execute { block(it) } }
    }
}

private data class DokarkivPutKnyttTilAnnenSakResponse(
    val nyJournalpostId: String,
)

private data class DokarkivPostJournalpostResponse(
    val dokumenter: List<DokumentInfo>,
    val journalpostId: String,
    val journalpostferdigstilt: Boolean,
) {
    data class DokumentInfo(
        val dokumentInfoId: String,
    )
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

private data class AvsenderMottaker(
    val navn: String,
    val idType: String = "HPRNR",
    val id: String,
)

private data class JournalpostPayload(
    val tittel: String,
    val journalpostType: String = "UTGAAENDE",
    val avsenderMottaker: AvsenderMottaker,
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
        tidspunkt = tidspunkt.atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime(),
        gjelder =
            MeldingTilBehandlerPdfInput.Gjelder(
                fødselsnummer = gjelder.value,
                navn = søkernavn.let { "${it.fornavn}${it.mellomnavn?.let { m -> " $m" } ?: ""} ${it.etternavn}" },
            ),
        fagområde =
            when (fagområde) {
                Fagområde.EnkeltståendeBehandlingsdager -> "Enkeltstående behandlingsdager"
                Fagområde.Tilbakedatering -> "Tilbakedatering"
                Fagområde.Yrkesskade -> "Yrkesskade"
                Fagområde.Bestridelse -> "Bestridelse"
            },
        melding = tekst,
    )
