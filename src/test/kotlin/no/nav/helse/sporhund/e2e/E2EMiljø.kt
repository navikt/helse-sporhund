package no.nav.helse.sporhund.e2e

import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson3.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.routing.application
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.helse.sporhund.DIALOGMELDING_FRA_BEHANDLER_TOPIC
import no.nav.helse.sporhund.DIALOGMELDING_FRA_NAY_TOPIC
import no.nav.helse.sporhund.DIALOGMELDING_STATUS_TOPIC
import no.nav.helse.sporhund.app
import no.nav.helse.sporhund.infrastructure.api.auth.AzureAdConfig
import no.nav.helse.sporhund.infrastructure.api.testhelpers.TEST_CLIENT_ID
import no.nav.helse.sporhund.infrastructure.api.testhelpers.TEST_ISSUER_ID
import no.nav.helse.sporhund.infrastructure.clients.accesstokenprovider.AccessTokenProviderConfig
import no.nav.helse.sporhund.infrastructure.clients.accesstokenprovider.testhelpers.MockTexasServer
import no.nav.helse.sporhund.infrastructure.clients.dokarkiv.DokarkivConfig
import no.nav.helse.sporhund.infrastructure.clients.dokarkiv.testhelpers.MockDokarkivServer
import no.nav.helse.sporhund.infrastructure.clients.padm2.Padm2Config
import no.nav.helse.sporhund.infrastructure.clients.personpseudoid.ValkeyPersonPseudoIdProvider
import no.nav.helse.sporhund.infrastructure.clients.personpseudoid.testhelpers.TestcontainersValkey
import no.nav.helse.sporhund.infrastructure.clients.populasjonstilgangskontroll.PopulasjonstilgangskontrollConfig
import no.nav.helse.sporhund.infrastructure.clients.populasjonstilgangskontroll.testhelpers.MockTilgangsmaskinenServer
import no.nav.helse.sporhund.infrastructure.clients.sprinter.SprinterConfig
import no.nav.helse.sporhund.infrastructure.clients.sprinter.testhelpers.MockSprinterServer
import no.nav.helse.sporhund.infrastructure.db.objectMapper
import no.nav.helse.sporhund.infrastructure.db.testhelpers.TestcontainersDatabase
import no.nav.helse.sporhund.infrastructure.kafka.DialogmeldingTilBehandlerKafkaDto
import no.nav.helse.sporhund.infrastructure.kafka.KafkaConfig
import no.nav.helse.sporhund.infrastructure.kafka.ReadTopics
import no.nav.helse.sporhund.infrastructure.kafka.testhelpers.TestcontainersKafka
import no.nav.helse.sporhund.infrastructure.kafka.testhelpers.lagDialogmeldingFraBehandlerKafkaDto
import no.nav.helse.sporhund.infrastructure.kafka.testhelpers.lagDialogmeldingUtenForesporselssvar
import no.nav.helse.sporhund.tilgangskontroll.tilgangsgrupperTilBrukerroller
import no.nav.helse.sporhund.tilgangskontroll.tilgangsgrupperTilTilganger
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.errors.WakeupException
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.module.kotlin.readValue
import java.net.ServerSocket
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Hele sporhund kjørt via [app] mot ekte containere (Postgres, Kafka, Valkey) og mock-servere.
 * Startes én gang per test-JVM og deles av alle E2E-tester. Hver test bruker sin egen person, se [e2e].
 * Alt stoppes i en shutdown hook når JVM-en avslutter.
 */
internal class E2EMiljø private constructor() {
    private val mockOAuth2Server = MockOAuth2Server().also { it.start() }.stoppesVedAvslutning("MockOAuth2Server") { it.shutdown() }
    private val kafka = TestcontainersKafka(MODULE_LABEL).stoppesVedAvslutning("Kafka") { it.stop() }
    private val postgres = TestcontainersDatabase(MODULE_LABEL).stoppesVedAvslutning("Postgres") { it.stop() }
    private val valkey = TestcontainersValkey(MODULE_LABEL).stoppesVedAvslutning("Valkey") { it.stop() }
    private val mockTexasServer = MockTexasServer().stoppesVedAvslutning("MockTexasServer") { it.stop() }
    private val mockTilgangsmaskinenServer = MockTilgangsmaskinenServer().stoppesVedAvslutning("MockTilgangsmaskinenServer") { it.stop() }
    private val mockSprinterServer = MockSprinterServer().stoppesVedAvslutning("MockSprinterServer") { it.stop() }
    val dokarkiv = MockDokarkivServer().stoppesVedAvslutning("MockDokarkivServer") { it.stop() }

    val oauth2Server: MockOAuth2Server get() = mockOAuth2Server
    val tilgangsgrupperTilTilganger = tilgangsgrupperTilTilganger()
    val tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukerroller()
    val personPseudoIdProvider = ValkeyPersonPseudoIdProvider(valkey.personPseudoIdConfig)

    private val consumerProducerFactory = ConsumerProducerFactory(kafka.config)
    val producer = consumerProducerFactory.createProducer().stoppesVedAvslutning("Kafka-producer") { it.close() }

    private val port = ServerSocket(0).use { it.localPort }
    val baseUrl = "http://localhost:$port"

    val httpClient =
        HttpClient(CIO) {
            install(ContentNegotiation) {
                jackson {
                    disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                }
            }
        }.stoppesVedAvslutning("HTTP-klient") { it.close() }

    private val meldingerTilBehandler = ConcurrentHashMap<String, CopyOnWriteArrayList<DialogmeldingTilBehandlerKafkaDto>>()

    @Volatile
    private var kjørendeApp: Application? = null

    @Volatile
    private var appFeil: Throwable? = null

    /** Meldingene sporhund har sendt til behandler på [DIALOGMELDING_FRA_NAY_TOPIC] med nøkkelen [conversationRef]. */
    fun meldingerTilBehandler(conversationRef: String): List<DialogmeldingTilBehandlerKafkaDto> = meldingerTilBehandler[conversationRef]?.toList().orEmpty()

    private fun start() {
        opprettTopics()
        startLytterPåMeldingerTilBehandler()
        startApp()
        runBlocking {
            ventTilAppenErKlar()
            ventTilAppenLeserFraBehandlerTopic()
        }
    }

    private fun opprettTopics() {
        consumerProducerFactory.adminClient().use { admin ->
            val topics = listOf(DIALOGMELDING_FRA_BEHANDLER_TOPIC, DIALOGMELDING_STATUS_TOPIC, DIALOGMELDING_FRA_NAY_TOPIC)
            admin.createTopics(topics.map { NewTopic(it, 1, 1.toShort()) }).all().get()
        }
    }

    private fun startLytterPåMeldingerTilBehandler() {
        val kjører = AtomicBoolean(true)
        val consumer =
            consumerProducerFactory.createConsumer(
                "e2e-lytter-til-behandler",
                Properties().apply { this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest" },
            )
        val tråd =
            Thread({
                consumer.use {
                    it.subscribe(listOf(DIALOGMELDING_FRA_NAY_TOPIC))
                    try {
                        while (kjører.get()) {
                            it.poll(100.milliseconds.toJavaDuration()).forEach { record ->
                                val melding = objectMapper.readValue<DialogmeldingTilBehandlerKafkaDto>(record.value())
                                meldingerTilBehandler.computeIfAbsent(record.key()) { CopyOnWriteArrayList() }.add(melding)
                            }
                        }
                    } catch (_: WakeupException) {
                        // Kastes når lytteren stoppes ved avslutning
                    }
                }
            }, "e2e-lytter-til-behandler").apply {
                isDaemon = true
                start()
            }
        registrerStopp("Lytter på meldinger til behandler") {
            kjører.set(false)
            consumer.wakeup()
            tråd.join(5_000)
        }
    }

    private fun startApp() {
        Thread({
            runCatching {
                app(
                    kafkaConfig =
                        KafkaConfig(
                            aivenConfig = kafka.config,
                            readTopics =
                                ReadTopics(
                                    dialogmeldingFraBehandlerTopic = DIALOGMELDING_FRA_BEHANDLER_TOPIC,
                                    dialogmeldingStatusTopic = DIALOGMELDING_STATUS_TOPIC,
                                ),
                            writeTopic = DIALOGMELDING_FRA_NAY_TOPIC,
                        ),
                    dbConfig = postgres.dbConfig,
                    azureAdConfig =
                        AzureAdConfig(
                            clientId = TEST_CLIENT_ID,
                            issuerUrl = mockOAuth2Server.issuerUrl(TEST_ISSUER_ID).toString(),
                            jwkProviderUri = mockOAuth2Server.jwksUrl(TEST_ISSUER_ID).toString(),
                        ),
                    personPseudoIdConfig = valkey.personPseudoIdConfig,
                    populasjonstilgangskontrollConfig =
                        PopulasjonstilgangskontrollConfig(
                            scope = "e2e-tilgangsmaskinen-scope",
                            baseUrl = mockTilgangsmaskinenServer.baseUrl,
                        ),
                    tilgangsgrupperTilTilganger = tilgangsgrupperTilTilganger,
                    tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukerroller,
                    accessTokenProviderConfig =
                        AccessTokenProviderConfig(
                            tokenEndpoint = mockTexasServer.tokenEndpoint,
                            exchangeEndpoint = mockTexasServer.exchangeEndpoint,
                        ),
                    // E2E-testene henter ikke vedlegg fra behandler, så padm2 blir aldri kalt
                    padm2Config = Padm2Config(baseUrl = "http://localhost:1", scope = "e2e-padm2-scope"),
                    sprinterConfig = SprinterConfig(baseUrl = mockSprinterServer.baseUrl),
                    dokarkivConfig = DokarkivConfig(baseUrl = dokarkiv.baseUrl, scope = "e2e-dokarkiv-scope"),
                    port = port,
                    kommandoJobbVentetid = 100.milliseconds,
                ) { kjørendeApp = application }
            }.onFailure { appFeil = it }
        }, "e2e-sporhund").apply {
            isDaemon = true
            start()
        }
        registrerStopp("sporhund") {
            // Stopper bakgrunnsjobbene før containerne stoppes, så de ikke logger feil mens JVM-en avslutter
            kjørendeApp?.let { it.monitor.raise(ApplicationStopping, it) }
        }
    }

    private suspend fun ventTilAppenErKlar() {
        ventPå("at sporhund svarer på /isalive og /isready", timeout = 60.seconds, intervall = 200.milliseconds) {
            appFeil?.let { throw IllegalStateException("sporhund krasjet under oppstart", it) }
            listOf("/isalive", "/isready").all { sti ->
                runCatching { httpClient.get("$baseUrl$sti").status == HttpStatusCode.OK }.getOrDefault(false)
            }
        }
    }

    /**
     * Consumeren i appen begynner å lese fra «latest». Meldinger som produseres før den har fått tildelt partisjoner, blir derfor aldri lest.
     * Vi sender irrelevante meldinger til consumer-gruppen har lest og committet en av dem.
     */
    private suspend fun ventTilAppenLeserFraBehandlerTopic() {
        kafka.waitForConsumerGroupAssignment(APPENS_CONSUMER_GROUP, timeoutMs = 30_000)
        ventPå("at sporhund leser meldinger fra $DIALOGMELDING_FRA_BEHANDLER_TOPIC", timeout = 30.seconds) {
            val irrelevantMelding = lagDialogmeldingFraBehandlerKafkaDto(dialogmelding = lagDialogmeldingUtenForesporselssvar())
            producer.send(ProducerRecord(DIALOGMELDING_FRA_BEHANDLER_TOPIC, objectMapper.writeValueAsString(irrelevantMelding))).get()
            repeat(10) {
                if (kafka.isOnLatestOffset(APPENS_CONSUMER_GROUP, DIALOGMELDING_FRA_BEHANDLER_TOPIC)) return@ventPå true
                delay(100.milliseconds)
            }
            false
        }
    }

    companion object {
        private const val MODULE_LABEL = "e2e"

        // Samme default som i app(...) når KAFKA_CONSUMER_GROUP_ID ikke er satt
        private const val APPENS_CONSUMER_GROUP = "local-group"

        private val stoppeliste = mutableListOf<Pair<String, () -> Unit>>()

        val instans: E2EMiljø by lazy {
            Runtime.getRuntime().addShutdownHook(Thread(::stoppAlt, "e2e-avslutning"))
            E2EMiljø().also { it.start() }
        }

        private fun registrerStopp(
            navn: String,
            stopp: () -> Unit,
        ) {
            synchronized(stoppeliste) { stoppeliste.add(navn to stopp) }
        }

        private fun <T> T.stoppesVedAvslutning(
            navn: String,
            stopp: (T) -> Unit,
        ): T = also { registrerStopp(navn) { stopp(it) } }

        // Stopper i motsatt rekkefølge av oppstarten
        private fun stoppAlt() {
            val alt = synchronized(stoppeliste) { stoppeliste.toList().asReversed() }
            alt.forEach { (navn, stopp) ->
                runCatching(stopp).onFailure { System.err.println("E2E: klarte ikke stoppe $navn: $it") }
            }
        }
    }
}
