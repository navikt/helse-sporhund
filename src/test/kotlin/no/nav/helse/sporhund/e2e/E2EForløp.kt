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
import no.nav.helse.sporhund.infrastructure.clients.sprinter.testhelpers.MockSprinterServer
import no.nav.helse.sporhund.infrastructure.db.objectMapper
import no.nav.helse.sporhund.infrastructure.kafka.testhelpers.lagDialogmelding
import no.nav.helse.sporhund.infrastructure.kafka.testhelpers.lagDialogmeldingFraBehandlerKafkaDto
import org.apache.kafka.clients.producer.ProducerRecord
import tools.jackson.module.kotlin.readValue
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.Base64
import java.util.UUID
import kotlin.random.Random
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Tilstanden og operasjonene i ett E2E-forløp: personen, dialogen og meldingene som er sendt.
 * DSL-fasene i [E2EKontekst] bruker disse, så testen trenger ikke sende tilstand mellom stegene.
 */
internal class E2EForløp(
    private val miljø: E2EMiljø,
) {
    private val identitetsnummer = lagIdentitetsnummer()
    private val saksbehandler = lagTestSaksbehandler()
    private val behandlerHprNummer = Random.nextInt(1_000_000, 9_999_999)

    val personPseudoId: PersonPseudoId = miljø.personPseudoIdProvider.nyPersonPseudoId(identitetsnummer)

    private var gjeldendeConversationRef: UUID? = null
    val conversationRef: UUID
        get() = checkNotNull(gjeldendeConversationRef) { "Dialogen finnes ikke ennå. Saksbehandler må sende en forespørsel først" }

    private val meldingerFraSaksbehandler = mutableListOf<String>()
    private val journalposterFraBehandler = mutableListOf<String>()

    private val dialogsti get() = "${miljø.baseUrl}/api/personer/${personPseudoId.value}/dialogmeldinger/$conversationRef"

    suspend fun sendForespørsel(tekst: String) {
        check(gjeldendeConversationRef == null) { "Saksbehandler har allerede sendt en forespørsel i dette forløpet" }
        val respons =
            miljø.httpClient.post("${miljø.baseUrl}/api/personer/${personPseudoId.value}/dialogmelding") {
                bearerAuth(token())
                contentType(ContentType.Application.Json)
                setBody(nyForespørsel(tekst))
            }
        val dialog = respons.forvent<ApiDialogDetails>(HttpStatusCode.Created)
        gjeldendeConversationRef = dialog.conversationRef
        meldingerFraSaksbehandler += tekst
    }

    suspend fun sendSvarFraSaksbehandler(tekst: String) {
        val respons =
            miljø.httpClient.post(dialogsti) {
                bearerAuth(token())
                contentType(ContentType.Application.Json)
                setBody(ApiSvarPaDialog(tekst))
            }
        val dialog = respons.forvent<ApiDialogDetails>(HttpStatusCode.Created)
        assertTrue(dialog.dialogmeldinger.filterIsInstance<ApiDialogmelding.FraNav>().any { it.melding == tekst })
        meldingerFraSaksbehandler += tekst
    }

    fun sendSvarFraBehandler(tekst: String) {
        val svar =
            lagDialogmeldingFraBehandlerKafkaDto(
                conversationRef = conversationRef.toString(),
                personIdentPasient = identitetsnummer.value,
                legehpr = behandlerHprNummer.toString(),
                dialogmelding = lagDialogmelding(tekstNotatInnhold = tekst),
                journalpostId = UUID.randomUUID().toString().also { journalposterFraBehandler += it },
                // Uten vedlegg, så appen ikke trenger padm2
                antallVedlegg = 0,
            )
        miljø.producer
            .send(ProducerRecord(DIALOGMELDING_FRA_BEHANDLER_TOPIC, conversationRef.toString(), objectMapper.writeValueAsString(svar)))
            .get()
    }

    suspend fun ferdigstill() {
        val respons =
            miljø.httpClient.patch(dialogsti) {
                bearerAuth(token())
                contentType(ContentType.Application.Json)
                setBody(ApiOppdaterDialogStatus(ferdigstilt = true))
            }
        assertEquals(ApiDialogmeldingStatus.FERDIGSTILT, respons.forvent<ApiDialogDetails>(HttpStatusCode.OK).status)
    }

    /**
     * Venter på at behandler har mottatt meldingen med [tekst] på Kafka,
     * og sjekker at hver melding fra saksbehandler er sendt nøyaktig én gang.
     */
    suspend fun sjekkMeldingTilBehandler(
        tekst: String,
        medVedlegg: Boolean,
    ) {
        val melding = meldingTilBehandler(tekst)
        val alleMeldinger = miljø.meldingerTilBehandler(conversationRef.toString()).filter { it.dialogmeldingKode == FORESPØRSEL }
        assertEquals(meldingerFraSaksbehandler, alleMeldinger.map { it.dialogmeldingTekst }, "Behandler skal ha fått nøyaktig én melding per melding fra saksbehandler")
        assertEquals(conversationRef.toString(), melding.dialogmeldingRefConversation)
        assertEquals(identitetsnummer.value, melding.personIdent)
        if (medVedlegg) {
            assertContentEquals(PDF_FRA_SPRINTER, melding.dialogmeldingVedlegg, "Vedlegget skal være PDF-en fra Sprinter")
        } else {
            assertNull(melding.dialogmeldingVedlegg, "Dialogmeldingen skal ikke ha vedlegg")
        }
    }

    /** Venter på at den siste meldingen fra saksbehandler er journalført i Dokarkiv med samme PDF som vedlegget til behandler. */
    suspend fun ventPåJournalføring() {
        val tekst = meldingerFraSaksbehandler.lastOrNull() ?: fail("Saksbehandler har ikke sendt noen melding ennå")
        val melding = meldingTilBehandler(tekst)
        val journalpost = journalpostFor(melding.dialogmeldingUuid)
        assertContentEquals(melding.dialogmeldingVedlegg, journalpost.pdf(), "Det journalførte dokumentet skal være det samme som vedlegget til behandler")
    }

    /** Kjører purringsjobben på et tidspunkt der fristen for svar på forespørselen er ute. */
    fun fristenGårUt() = miljø.kjørPurringsjobben(Instant.now().plus(Duration.ofDays(22)))

    /** Venter på at behandler har mottatt nøyaktig én purring, med PDF fra purring-malen i Sprinter. */
    suspend fun sjekkPurringTilBehandler() {
        val purring = purringTilBehandler()
        assertEquals(identitetsnummer.value, purring.personIdent)
        assertContentEquals(PDF_FRA_PURRINGSMALEN, purring.dialogmeldingVedlegg, "Vedlegget skal være PDF-en fra purring-malen")
        val pdfInput =
            miljø.sprinter
                .forespørslerTil(MockSprinterServer.PURRING_TIL_BEHANDLER)
                .map { objectMapper.readTree(it) }
                .single { it["conversationRef"].asString() == conversationRef.toString() }
        assertFalse(pdfInput.has("fra"), "Purring-malen skal ikke få «fra»")
        assertTrue(pdfInput["melding"].asString().contains("Vi kan ikke se å ha mottatt svar på vår forespørsel"), "PDF-en skal ha purringsteksten")
    }

    /** Venter på at purringen er journalført på samme sak, bruker og behandler som forespørselen, med samme PDF som vedlegget. */
    suspend fun ventPåJournalføringAvPurring() {
        val purring = purringTilBehandler()
        val forespørsel = journalpostFor(meldingTilBehandler(meldingerFraSaksbehandler.first()).dialogmeldingUuid)
        val journalpost = journalpostFor(purring.dialogmeldingUuid)
        assertEquals("Purring - tilbakedatering", journalpost.tittel)
        assertEquals("Purring - tilbakedatering", journalpost.dokumenter.single().tittel)
        assertEquals(conversationRef.toString(), journalpost.sak.fagsakId)
        assertEquals(forespørsel.bruker, journalpost.bruker)
        assertEquals(forespørsel.avsenderMottaker, journalpost.avsenderMottaker)
        assertContentEquals(purring.dialogmeldingVedlegg, journalpost.pdf(), "Det journalførte dokumentet skal være det samme som vedlegget til behandler")
    }

    private suspend fun purringTilBehandler() =
        ventPåVerdi("at behandler mottar purringen") {
            miljø
                .meldingerTilBehandler(conversationRef.toString())
                .filter { it.dialogmeldingKode == PURRING }
                .also { assertTrue(it.size <= 1, "Behandler har fått ${it.size} purringer") }
                .singleOrNull()
        }

    private suspend fun journalpostFor(dialogmeldingUuid: String) =
        ventPåVerdi("at dialogmeldingen $dialogmeldingUuid blir journalført") {
            miljø.dokarkiv
                .opprettedeJournalposter()
                .map { objectMapper.readValue<JournalpostForespørsel>(it) }
                .filter { it.eksternReferanseId == dialogmeldingUuid }
                .also { assertTrue(it.size <= 1, "Dialogmeldingen er journalført ${it.size} ganger") }
                .singleOrNull()
        }

    suspend fun ventPåKnytningAvSvar() {
        val journalpostId = journalposterFraBehandler.lastOrNull() ?: fail("Behandler har ikke svart ennå")
        val knytning =
            ventPåVerdi("at journalpost $journalpostId blir knyttet til saken") {
                miljø.dokarkiv
                    .knyttedeJournalposter()
                    .filter { it.journalpostId == journalpostId }
                    .also { assertTrue(it.size <= 1, "Journalposten er knyttet ${it.size} ganger") }
                    .singleOrNull()
            }
        assertTrue(journalpostId in miljø.dokarkiv.feilregistrerteJournalposter(), "Journalposten til svaret skal være feilregistrert")
        val forespørsel = objectMapper.readValue<KnyttTilAnnenSakForespørsel>(knytning.body)
        assertEquals(conversationRef.toString(), forespørsel.fagsakId)
        assertEquals(identitetsnummer.value, forespørsel.bruker["id"])
    }

    suspend fun ventPåSvarHosSaksbehandler(tekst: String) {
        ventPå("at saksbehandler ser svaret «$tekst» fra behandler") {
            val dialog = hentDialog()
            dialog.dialogmeldinger.filterIsInstance<ApiDialogmelding.FraBehandler>().any { it.melding == tekst } &&
                dialog.status == ApiDialogmeldingStatus.MOTTATT
        }
    }

    private suspend fun meldingTilBehandler(tekst: String) =
        ventPåVerdi("at behandler mottar dialogmeldingen «$tekst»") {
            miljø.meldingerTilBehandler(conversationRef.toString()).singleOrNull { it.dialogmeldingTekst == tekst }
        }

    suspend fun dialogstatus(): ApiDialogmeldingStatus = hentDialog().status

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

    private data class KnyttTilAnnenSakForespørsel(
        val fagsakId: String,
        val bruker: Map<String, Any?>,
    )

    private data class JournalpostForespørsel(
        val tittel: String,
        val eksternReferanseId: String,
        val sak: Sak,
        val bruker: Map<String, Any?>,
        val avsenderMottaker: Map<String, Any?>,
        val dokumenter: List<Dokument>,
    ) {
        fun pdf(): ByteArray =
            Base64.getDecoder().decode(
                dokumenter
                    .single()
                    .dokumentvarianter
                    .single()
                    .fysiskDokument,
            )

        data class Sak(
            val fagsakId: String,
        )

        data class Dokument(
            val tittel: String,
            val dokumentvarianter: List<Dokumentvariant>,
        )

        data class Dokumentvariant(
            val fysiskDokument: String,
        )
    }

    private companion object {
        const val FORESPØRSEL = 1
        const val PURRING = 2
        val PDF_FRA_SPRINTER = MockSprinterServer.PDF_PER_MAL.getValue(MockSprinterServer.MELDING_TIL_BEHANDLER)
        val PDF_FRA_PURRINGSMALEN = MockSprinterServer.PDF_PER_MAL.getValue(MockSprinterServer.PURRING_TIL_BEHANDLER)
    }
}
