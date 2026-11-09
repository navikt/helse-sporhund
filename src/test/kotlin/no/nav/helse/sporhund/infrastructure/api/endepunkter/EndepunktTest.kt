package no.nav.helse.sporhund.infrastructure.api.endepunkter

import com.github.navikt.tbd_libs.access_token.AccessTokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.server.testing.*
import no.nav.helse.sporhund.application.*
import no.nav.helse.sporhund.application.kommando.DialogmeldingKommandoHåndterer
import no.nav.helse.sporhund.application.kommando.KommandoJobb
import no.nav.helse.sporhund.domain.Dialog
import no.nav.helse.sporhund.domain.Saksbehandler
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.domain.testhelpers.lagIdentitetsnummer
import no.nav.helse.sporhund.infrastructure.api.ApiBehandler
import no.nav.helse.sporhund.infrastructure.api.ApiBehandlerKategori
import no.nav.helse.sporhund.infrastructure.api.ApiBehandlerType
import no.nav.helse.sporhund.infrastructure.api.ApiFagomrade
import no.nav.helse.sporhund.infrastructure.api.ApiLegekontor
import no.nav.helse.sporhund.infrastructure.api.ApiNavn
import no.nav.helse.sporhund.infrastructure.api.ApiNyDialogmelding
import no.nav.helse.sporhund.infrastructure.api.ApiSoker
import no.nav.helse.sporhund.infrastructure.api.testhelpers.jsonClient
import no.nav.helse.sporhund.infrastructure.api.testhelpers.lagTestSaksbehandler
import no.nav.helse.sporhund.infrastructure.api.testhelpers.setupTestApp
import no.nav.helse.sporhund.infrastructure.api.testhelpers.utstedTokenMedLesTilgang
import no.nav.helse.sporhund.infrastructure.api.testhelpers.utstedTokenMedSkrivTilgang
import no.nav.helse.sporhund.infrastructure.clients.dokarkiv.DokarkivClient
import no.nav.helse.sporhund.infrastructure.clients.dokarkiv.DokarkivConfig
import no.nav.helse.sporhund.infrastructure.db.objectMapper
import no.nav.helse.sporhund.infrastructure.kafka.KafkaDialogmeldingSender
import no.nav.helse.sporhund.tilgangskontroll.tilgangsgrupperTilBrukerroller
import no.nav.helse.sporhund.tilgangskontroll.tilgangsgrupperTilTilganger
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.serialization.StringSerializer
import tools.jackson.databind.JsonNode
import java.time.LocalDate
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

abstract class EndepunktTest {
    protected val mockOAuth2Server = MockOAuth2Server()
    protected val saksbehandler: Saksbehandler = lagTestSaksbehandler()
    protected val personPseudoIdProvider = InMemoryPersonPseudoIdProvider()
    protected val transactionProvider = InMemoryTransactionProvider()
    protected val populasjonstilgangskontrollProvider = InMemoryPopulasjonstilgangskontrollProvider()
    protected val tilgangsgrupperTilTilganger = tilgangsgrupperTilTilganger()
    protected val tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukerroller()
    protected var vedleggProvider: VedleggProvider = VedleggProvider { emptyList() }
    protected val pdf = Random.nextBytes(256)
    protected val pdfInput = mutableListOf<MeldingTilBehandlerPdfInput>()
    protected val pdfProvider =
        object : PdfProvider {
            override fun genererPdf(meldingTilBehandlerPdfInput: MeldingTilBehandlerPdfInput) = pdf.also { pdfInput.add(meldingTilBehandlerPdfInput) }

            override fun genererPurringPdf(purringTilBehandlerPdfInput: PurringTilBehandlerPdfInput) = pdf
        }
    protected val producer = MockProducer(true, null, StringSerializer(), StringSerializer())
    protected val dokarkiv =
        MockEngine {
            respond(
                """{"dokumenter":[{"dokumentInfoId":"1"}],"journalpostId":"1","journalpostferdigstilt":true}""",
                HttpStatusCode.Created,
                headersOf(),
            )
        }
    protected val kommandoJobb =
        KommandoJobb(
            transactionProvider = transactionProvider,
            håndterer =
                DialogmeldingKommandoHåndterer(
                    pdfProvider = pdfProvider,
                    sender = KafkaDialogmeldingSender("topic", producer),
                    journalføringProvider =
                        DokarkivClient(
                            DokarkivConfig(baseUrl = "http://dokarkiv", scope = "scope"),
                            object : AccessTokenProvider {
                                override fun machineToken(scope: String) = "token"

                                override fun oboToken(
                                    accessToken: String,
                                    scope: String,
                                ) = "token"
                            },
                            HttpClient(dokarkiv),
                        ),
                ),
        )

    fun ApplicationTestBuilder.setupDefaultTestApp() {
        setupTestApp(
            personPseudoIdProvider,
            transactionProvider,
            mockOAuth2Server,
            populasjonstilgangskontrollProvider,
            tilgangsgrupperTilTilganger,
            tilgangsgrupperTilBrukerroller,
            { msgId -> vedleggProvider.hentVedlegg(msgId) },
        )
    }

    protected fun lesetoken(): String = mockOAuth2Server.utstedTokenMedLesTilgang(saksbehandler, tilgangsgrupperTilTilganger, tilgangsgrupperTilBrukerroller)

    protected fun skrivetoken(): String = mockOAuth2Server.utstedTokenMedSkrivTilgang(saksbehandler, tilgangsgrupperTilTilganger, tilgangsgrupperTilBrukerroller)

    protected fun lagreDialog(dialog: Dialog = lagDialog()): PersonPseudoId {
        transactionProvider.dialogRepository.lagre(dialog)
        return personPseudoIdProvider.nyPersonPseudoId(dialog.identitetsnummer)
    }

    protected fun kjørKommandoerTilKøenErTom() {
        while (kommandoJobb.utførNeste()) {
            // Fortsetter til det ikke er flere kommandoer i køen
        }
    }

    protected suspend fun sendtJournalpost(): JsonNode =
        objectMapper.readTree(
            dokarkiv.requestHistory
                .single()
                .body
                .toByteArray(),
        )

    protected fun JsonNode.fysiskDokument(): ByteArray = Base64.decode(this["dokumenter"][0]["dokumentvarianter"][0]["fysiskDokument"].asString())

    protected fun assertIngentingSendt() {
        kjørKommandoerTilKøenErTom()
        assertEquals(emptyList(), producer.history())
        assertEquals(emptyList(), dokarkiv.requestHistory)
    }

    protected suspend fun ApplicationTestBuilder.postNyForespørsel(
        melding: String,
        pseudoId: PersonPseudoId = personPseudoIdProvider.nyPersonPseudoId(lagIdentitetsnummer()),
    ): HttpResponse =
        jsonClient().post("/api/personer/${pseudoId.value}/dialogmelding") {
            bearerAuth(skrivetoken())
            contentType(ContentType.Application.Json)
            setBody(
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
                            hprNummer = Random.nextInt(1_000_000, 999_999_999),
                        ),
                    fagomrade = ApiFagomrade.TILBAKEDATERING,
                    melding = melding,
                ),
            )
        }

    protected fun testApp(block: suspend ApplicationTestBuilder.() -> Unit = { }) {
        testApplication {
            setupDefaultTestApp()
            block()
        }
    }

    @BeforeTest
    fun setUp() {
        mockOAuth2Server.start()
    }

    @AfterTest
    fun tearDown() {
        mockOAuth2Server.shutdown()
    }
}
