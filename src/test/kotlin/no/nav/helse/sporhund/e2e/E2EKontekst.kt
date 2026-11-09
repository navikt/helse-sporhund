package no.nav.helse.sporhund.e2e

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.helse.sporhund.DIALOGMELDING_FRA_BEHANDLER_TOPIC
import no.nav.helse.sporhund.application.PersonPseudoId
import no.nav.helse.sporhund.domain.testhelpers.lagIdentitetsnummer
import no.nav.helse.sporhund.infrastructure.api.ApiBehandler
import no.nav.helse.sporhund.infrastructure.api.ApiBehandlerKategori
import no.nav.helse.sporhund.infrastructure.api.ApiBehandlerType
import no.nav.helse.sporhund.infrastructure.api.ApiDialogDetails
import no.nav.helse.sporhund.infrastructure.api.ApiDialogmelding
import no.nav.helse.sporhund.infrastructure.api.ApiDialogmeldingStatus
import no.nav.helse.sporhund.infrastructure.api.ApiFagomrade
import no.nav.helse.sporhund.infrastructure.api.ApiLegekontor
import no.nav.helse.sporhund.infrastructure.api.ApiNavn
import no.nav.helse.sporhund.infrastructure.api.ApiNyDialogmelding
import no.nav.helse.sporhund.infrastructure.api.ApiOppdaterDialogStatus
import no.nav.helse.sporhund.infrastructure.api.ApiSoker
import no.nav.helse.sporhund.infrastructure.api.ApiSvarPaDialog
import no.nav.helse.sporhund.infrastructure.api.testhelpers.lagTestSaksbehandler
import no.nav.helse.sporhund.infrastructure.api.testhelpers.utstedTokenMedSkrivTilgang
import no.nav.helse.sporhund.infrastructure.db.objectMapper
import no.nav.helse.sporhund.infrastructure.kafka.DialogmeldingTilBehandlerKafkaDto
import no.nav.helse.sporhund.infrastructure.kafka.testhelpers.lagDialogmelding
import no.nav.helse.sporhund.infrastructure.kafka.testhelpers.lagDialogmeldingFraBehandlerKafkaDto
import org.apache.kafka.clients.producer.ProducerRecord
import tools.jackson.module.kotlin.readValue
import java.time.LocalDate
import java.util.Base64
import java.util.UUID
import kotlin.random.Random
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Kjører [block] mot den delte E2E-appen med en ny person. Se [E2EKontekst].
 */
fun e2e(block: suspend E2EKontekst.() -> Unit) =
    runBlocking {
        E2EKontekst(E2EMiljø.instans).block()
    }

/**
 * Tilstanden til ett E2E-forløp: personen, dialogen og meldingene som er sendt.
 * Stegene leser og oppdaterer tilstanden selv, så testen trenger ikke sende noe mellom dem.
 */
class E2EKontekst internal constructor(
    private val miljø: E2EMiljø,
) {
    private val identitetsnummer = lagIdentitetsnummer()
    private val saksbehandler = lagTestSaksbehandler()
    private val behandlerHprNummer = Random.nextInt(1_000_000, 9_999_999)

    val personPseudoId: PersonPseudoId = miljø.personPseudoIdProvider.nyPersonPseudoId(identitetsnummer)

    private var gjeldendeConversationRef: UUID? = null
    val conversationRef: UUID
        get() = checkNotNull(gjeldendeConversationRef) { "Dialogen finnes ikke ennå. Start med saksbehandlerSenderForespørsel(...)" }

    private val meldingerFraSaksbehandler = mutableListOf<String>()
    private var sisteMeldingTilBehandler: DialogmeldingTilBehandlerKafkaDto? = null

    private val dialogsti get() = "${miljø.baseUrl}/api/personer/${personPseudoId.value}/dialogmeldinger/$conversationRef"

    suspend fun saksbehandlerSenderForespørsel(melding: String) {
        check(gjeldendeConversationRef == null) { "Saksbehandler har allerede sendt en forespørsel i dette forløpet" }
        val respons =
            miljø.httpClient.post("${miljø.baseUrl}/api/personer/${personPseudoId.value}/dialogmelding") {
                bearerAuth(token())
                contentType(ContentType.Application.Json)
                setBody(nyForespørsel(melding))
            }
        val dialog = respons.forvent<ApiDialogDetails>(HttpStatusCode.Created)
        gjeldendeConversationRef = dialog.conversationRef
        meldingerFraSaksbehandler += melding
    }

    /**
     * Venter på at den siste meldingen fra saksbehandler er sendt til behandler på Kafka,
     * og sjekker at hver melding fra saksbehandler er sendt nøyaktig én gang.
     */
    suspend fun behandlerHarMottattDialogmelding(medVedlegg: Boolean) {
        val forventetTekst = meldingerFraSaksbehandler.lastOrNull() ?: fail("Saksbehandler har ikke sendt noen melding ennå")
        val melding =
            ventPåVerdi("at behandler mottar dialogmeldingen «$forventetTekst»") {
                miljø.meldingerTilBehandler(conversationRef.toString()).singleOrNull { it.dialogmeldingTekst == forventetTekst }
            }
        val alleMeldinger = miljø.meldingerTilBehandler(conversationRef.toString())
        assertEquals(meldingerFraSaksbehandler, alleMeldinger.map { it.dialogmeldingTekst }, "Behandler skal ha fått nøyaktig én melding per melding fra saksbehandler")
        assertEquals(conversationRef.toString(), melding.dialogmeldingRefConversation)
        assertEquals(identitetsnummer.value, melding.personIdent)
        if (medVedlegg) {
            assertContentEquals(PDF_FRA_SPRINTER, melding.dialogmeldingVedlegg, "Vedlegget skal være PDF-en fra Sprinter")
        } else {
            assertNull(melding.dialogmeldingVedlegg, "Dialogmeldingen skal ikke ha vedlegg")
        }
        sisteMeldingTilBehandler = melding
    }

    /** Venter på at den siste dialogmeldingen til behandler er journalført i Dokarkiv med samme PDF som vedlegget. */
    suspend fun dialogmeldingenErJournalført() {
        val melding = sisteMeldingTilBehandler ?: fail("Behandler har ikke mottatt noen dialogmelding ennå")
        val journalpost =
            ventPåVerdi("at dialogmeldingen ${melding.dialogmeldingUuid} blir journalført") {
                miljø.dokarkiv
                    .opprettedeJournalposter()
                    .map { objectMapper.readValue<JournalpostForespørsel>(it) }
                    .filter { it.eksternReferanseId == melding.dialogmeldingUuid }
                    .also { assertTrue(it.size <= 1, "Dialogmeldingen er journalført ${it.size} ganger") }
                    .singleOrNull()
            }
        val dokument =
            Base64.getDecoder().decode(
                journalpost.dokumenter
                    .single()
                    .dokumentvarianter
                    .single()
                    .fysiskDokument,
            )
        assertContentEquals(melding.dialogmeldingVedlegg, dokument, "Det journalførte dokumentet skal være det samme som vedlegget til behandler")
    }

    fun behandlerSvarer(melding: String) {
        val svar =
            lagDialogmeldingFraBehandlerKafkaDto(
                conversationRef = conversationRef.toString(),
                personIdentPasient = identitetsnummer.value,
                legehpr = behandlerHprNummer.toString(),
                dialogmelding = lagDialogmelding(tekstNotatInnhold = melding),
                // Uten vedlegg, så appen ikke trenger padm2
                antallVedlegg = 0,
            )
        miljø.producer
            .send(ProducerRecord(DIALOGMELDING_FRA_BEHANDLER_TOPIC, conversationRef.toString(), objectMapper.writeValueAsString(svar)))
            .get()
    }

    suspend fun saksbehandlerSerSvaret(melding: String) {
        ventPå("at saksbehandler ser svaret «$melding» fra behandler") {
            val dialog = hentDialog()
            dialog.dialogmeldinger.filterIsInstance<ApiDialogmelding.FraBehandler>().any { it.melding == melding } &&
                dialog.status == ApiDialogmeldingStatus.MOTTATT
        }
    }

    suspend fun saksbehandlerSvarer(melding: String) {
        val respons =
            miljø.httpClient.post(dialogsti) {
                bearerAuth(token())
                contentType(ContentType.Application.Json)
                setBody(ApiSvarPaDialog(melding))
            }
        val dialog = respons.forvent<ApiDialogDetails>(HttpStatusCode.Created)
        assertTrue(dialog.dialogmeldinger.filterIsInstance<ApiDialogmelding.FraNav>().any { it.melding == melding })
        meldingerFraSaksbehandler += melding
    }

    suspend fun saksbehandlerFerdigstillerDialogen() {
        val respons =
            miljø.httpClient.patch(dialogsti) {
                bearerAuth(token())
                contentType(ContentType.Application.Json)
                setBody(ApiOppdaterDialogStatus(ferdigstilt = true))
            }
        assertEquals(ApiDialogmeldingStatus.FERDIGSTILT, respons.forvent<ApiDialogDetails>(HttpStatusCode.OK).status)
        assertEquals(ApiDialogmeldingStatus.FERDIGSTILT, hentDialog().status, "Dialogen skal være lukket etter ferdigstilling")
    }

    private suspend fun hentDialog(): ApiDialogDetails =
        miljø.httpClient
            .get(dialogsti) { bearerAuth(token()) }
            .forvent(HttpStatusCode.OK)

    private suspend inline fun <reified T> HttpResponse.forvent(status: HttpStatusCode): T {
        assertEquals(status, this.status, "Uventet svar fra ${call.request.method.value} ${call.request.url.encodedPath}: ${bodyAsText()}")
        return assertNotNull(body<T>())
    }

    private fun token() =
        miljø.oauth2Server.utstedTokenMedSkrivTilgang(
            saksbehandler = saksbehandler,
            tilgangsgrupperTilTilganger = miljø.tilgangsgrupperTilTilganger,
            tilgangsgrupperTilBrukerroller = miljø.tilgangsgrupperTilBrukerroller,
        )

    private fun nyForespørsel(melding: String) =
        ApiNyDialogmelding(
            soker =
                ApiSoker(
                    navn = ApiNavn(fornavn = "Slapp", mellomnavn = null, etternavn = "Appelsin"),
                    fodselsdato = LocalDate.of(1990, 6, 15),
                ),
            behandler =
                ApiBehandler(
                    id = UUID.randomUUID().toString(),
                    navn = ApiNavn(fornavn = "Ola", mellomnavn = null, etternavn = "Lege"),
                    type = ApiBehandlerType.FASTLEGE,
                    kategori = ApiBehandlerKategori.LEGE,
                    legekontor =
                        ApiLegekontor(
                            kontor = "Legesenteret",
                            orgnummer = "912345678",
                            adresse = "Storgata 1",
                            postnummer = "0181",
                            poststed = "Oslo",
                        ),
                    telefonnummer = "22334455",
                    hprNummer = behandlerHprNummer,
                ),
            fagomrade = ApiFagomrade.TILBAKEDATERING,
            melding = melding,
        )

    private data class JournalpostForespørsel(
        val eksternReferanseId: String,
        val dokumenter: List<Dokument>,
    ) {
        data class Dokument(
            val dokumentvarianter: List<Dokumentvariant>,
        )

        data class Dokumentvariant(
            val fysiskDokument: String,
        )
    }

    private companion object {
        // Det MockSprinterServer svarer med
        val PDF_FRA_SPRINTER = "%PDF-1.4 mock".toByteArray()
    }
}
