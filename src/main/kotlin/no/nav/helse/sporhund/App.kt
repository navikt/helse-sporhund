package no.nav.helse.sporhund

import com.github.navikt.tbd_libs.access_token.TexasClient
import com.github.navikt.tbd_libs.kafka.AivenConfig
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.naisful.naisApp
import com.github.navikt.tbd_libs.populasjonstilgang.client.TilgangsmaskinenClient
import io.github.smiley4.ktoropenapi.OpenApi
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.helse.sporhund.application.logg.loggError
import no.nav.helse.sporhund.application.tilgangskontroll.TilgangsgrupperTilBrukerroller
import no.nav.helse.sporhund.application.tilgangskontroll.TilgangsgrupperTilTilganger
import no.nav.helse.sporhund.infrastructure.api.appRoutes
import no.nav.helse.sporhund.infrastructure.api.auth.AzureAdConfig
import no.nav.helse.sporhund.infrastructure.api.auth.configureJwtAuthentication
import no.nav.helse.sporhund.infrastructure.api.configureOpenApiPlugin
import no.nav.helse.sporhund.infrastructure.clients.accesstokenprovider.AccessTokenProviderConfig
import no.nav.helse.sporhund.infrastructure.clients.dokarkiv.DokarkivClient
import no.nav.helse.sporhund.infrastructure.clients.dokarkiv.DokarkivConfig
import no.nav.helse.sporhund.infrastructure.clients.dokarkiv.JournalførerJobb
import no.nav.helse.sporhund.infrastructure.clients.padm2.Padm2Client
import no.nav.helse.sporhund.infrastructure.clients.padm2.Padm2Config
import no.nav.helse.sporhund.infrastructure.clients.personpseudoid.PersonPseudoIdConfig
import no.nav.helse.sporhund.infrastructure.clients.personpseudoid.ValkeyPersonPseudoIdProvider
import no.nav.helse.sporhund.infrastructure.clients.populasjonstilgangskontroll.PopulasjonstilgangskontrollConfig
import no.nav.helse.sporhund.infrastructure.clients.sprinter.SprinterClient
import no.nav.helse.sporhund.infrastructure.clients.sprinter.SprinterConfig
import no.nav.helse.sporhund.infrastructure.db.DataSourceBuilder
import no.nav.helse.sporhund.infrastructure.db.DbConfig
import no.nav.helse.sporhund.infrastructure.db.PgTransactionProvider
import no.nav.helse.sporhund.infrastructure.db.objectMapper
import no.nav.helse.sporhund.infrastructure.kafka.KafkaConfig
import no.nav.helse.sporhund.infrastructure.kafka.KafkaConsumerJobb
import no.nav.helse.sporhund.infrastructure.kafka.KafkaProducerJobb
import no.nav.helse.sporhund.infrastructure.kafka.ReadTopics
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

const val DIALOGMELDING_FRA_NAY_TOPIC = "teamsykefravr.isdialogmelding-behandler-dialogmelding-bestilling"
const val DIALOGMELDING_FRA_BEHANDLER_TOPIC = "teamsykefravr.dialogmelding"

fun main() {
    val env = System.getenv()
    val kafkaConfig =
        KafkaConfig(
            aivenConfig = AivenConfig.default,
            readTopics =
                ReadTopics(
                    dialogmeldingFraBehandlerTopic = DIALOGMELDING_FRA_BEHANDLER_TOPIC,
                ),
            writeTopic = DIALOGMELDING_FRA_NAY_TOPIC,
        )
    val dbConfig =
        DbConfig(
            jdbcUrl = env.getValue("DATABASE_JDBC_URL"),
            username = env.getValue("DATABASE_USERNAME"),
            password = env.getValue("DATABASE_PASSWORD"),
        )
    val azureAdConfig =
        AzureAdConfig(
            clientId = env.getValue("AZURE_APP_CLIENT_ID"),
            issuerUrl = env.getValue("AZURE_OPENID_CONFIG_ISSUER"),
            jwkProviderUri = env.getValue("AZURE_OPENID_CONFIG_JWKS_URI"),
        )
    val personPseudoIdConfig =
        PersonPseudoIdConfig(
            valkeyBrukernavn = env.getValue("VALKEY_USERNAME_PERSONPSEUDOID"),
            valkeyPassord = env.getValue("VALKEY_PASSWORD_PERSONPSEUDOID"),
            valkeyConnectionString = env.getValue("VALKEY_URI_PERSONPSEUDOID"),
        )
    val populasjonstilgangskontrollConfig =
        PopulasjonstilgangskontrollConfig(
            scope = env.getValue("TILGANGSMASKINEN_SCOPE"),
            baseUrl = env.getValue("TILGANGSMASKINEN_BASE_URL"),
        )
    val tilgangsgrupperTilTilganger =
        TilgangsgrupperTilTilganger(
            skrivetilgang = env.getUUIDList("TILGANG_SKRIV"),
            lesetilgang = env.getUUIDList("TILGANG_LES"),
        )
    val tilgangsgrupperTilBrukeroller =
        TilgangsgrupperTilBrukerroller(
            dialogmelding = env.getUUIDList("ROLLE_DIALOGMELDING"),
        )
    val accessTokenProviderConfig =
        AccessTokenProviderConfig(
            tokenEndpoint = env.getValue("NAIS_TOKEN_ENDPOINT"),
            exchangeEndpoint = env.getValue("NAIS_TOKEN_EXCHANGE_ENDPOINT"),
        )
    val padm2Config =
        Padm2Config(
            baseUrl = env.getValue("PADM2_BASE_URL"),
            scope = env.getValue("PADM2_SCOPE"),
        )

    val sprinterConfig =
        SprinterConfig(
            baseUrl = env.getValue("SPRINTER_BASE_URL"),
        )

    val dokarkivConfig =
        DokarkivConfig(
            baseUrl = env.getValue("DOKARKIV_BASE_URL"),
            scope = env.getValue("DOKARKIV_SCOPE"),
        )

    app(
        kafkaConfig = kafkaConfig,
        dbConfig = dbConfig,
        azureAdConfig = azureAdConfig,
        personPseudoIdConfig = personPseudoIdConfig,
        populasjonstilgangskontrollConfig = populasjonstilgangskontrollConfig,
        tilgangsgrupperTilTilganger = tilgangsgrupperTilTilganger,
        tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukeroller,
        accessTokenProviderConfig = accessTokenProviderConfig,
        padm2Config = padm2Config,
        sprinterConfig = sprinterConfig,
        dokarkivConfig = dokarkivConfig,
    )
}

fun app(
    kafkaConfig: KafkaConfig,
    dbConfig: DbConfig,
    azureAdConfig: AzureAdConfig,
    personPseudoIdConfig: PersonPseudoIdConfig,
    populasjonstilgangskontrollConfig: PopulasjonstilgangskontrollConfig,
    tilgangsgrupperTilTilganger: TilgangsgrupperTilTilganger,
    tilgangsgrupperTilBrukerroller: TilgangsgrupperTilBrukerroller,
    accessTokenProviderConfig: AccessTokenProviderConfig,
    padm2Config: Padm2Config,
    sprinterConfig: SprinterConfig,
    dokarkivConfig: DokarkivConfig,
    port: Int = 8080,
    additionalRoutes: Routing.() -> Unit = { },
) {
    val factory = ConsumerProducerFactory(kafkaConfig.aivenConfig)
    val running = AtomicBoolean(false)

    val dataSourceBuilder = DataSourceBuilder(dbConfig)

    val transactionProvider = PgTransactionProvider(dataSourceBuilder.build())
    val personPseudoIdProvider = ValkeyPersonPseudoIdProvider(personPseudoIdConfig)
    val kafkaConsumerJobb =
        KafkaConsumerJobb(
            topics = kafkaConfig.readTopics,
            consumerGroupId = System.getenv("KAFKA_CONSUMER_GROUP_ID") ?: "local-group",
            readyToConsume = running,
            consumerProducerFactory = factory,
            transactionProvider = transactionProvider,
        )
    val kafkaProducerJobb =
        KafkaProducerJobb(
            dialogmeldingFraNayTopic = kafkaConfig.writeTopic,
            readyToProduce = running,
            consumerProducerFactory = factory,
            transactionProvider = transactionProvider,
        )

    val accessTokenProvider =
        TexasClient(
            tokenEndpoint = URI(accessTokenProviderConfig.tokenEndpoint),
            tokenExchangeEndpoint = URI(accessTokenProviderConfig.exchangeEndpoint),
        )

    val padm2Client = Padm2Client(padm2Config, accessTokenProvider)

    val pdfProvider = SprinterClient(sprinterConfig)

    val dokarkivClient = DokarkivClient(dokarkivConfig, pdfProvider, accessTokenProvider)

    val journalførerJobb = JournalførerJobb(running, transactionProvider, dokarkivClient)

    val tilgangsmaskinenClient =
        TilgangsmaskinenClient(
            scope = populasjonstilgangskontrollConfig.scope,
            baseUrl = populasjonstilgangskontrollConfig.baseUrl,
            tokenProvider = accessTokenProvider,
        )

    var producerJob: Job? = null
    var consumerJob: Job? = null
    var journalførerJob: Job? = null
    naisApp(
        meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        objectMapper = objectMapper,
        applicationLogger = LoggerFactory.getLogger("sporhund"),
        callLogger = LoggerFactory.getLogger("sporhund"),
        port = port,
        developmentMode = false,
        gracefulShutdownDelay = 10.seconds,
        applicationModule = {
            this.monitor.subscribe(ApplicationStarted) {
                dataSourceBuilder.migrate()
                running.set(true)
                val exceptionHandler =
                    CoroutineExceptionHandler { _, throwable ->
                        loggError("Feil i coroutine, terminerer appen", throwable)
                        it.engine.stop()
                    }
                journalførerJob = launch(exceptionHandler) { journalførerJobb.start() }
                consumerJob = launch(exceptionHandler) { kafkaConsumerJobb.start() }
                producerJob = launch(exceptionHandler) { kafkaProducerJobb.start() }
            }
            this.monitor.subscribe(ApplicationStopping) {
                running.set(false)
                runBlocking {
                    consumerJob?.join()
                    producerJob?.join()
                    journalførerJob?.join()
                }
            }

            install(OpenApi) { configureOpenApiPlugin() }

            authentication {
                jwt("oidc") {
                    configureJwtAuthentication(
                        azureAdConfig = azureAdConfig,
                        tilgangsgrupperTilTilganger = tilgangsgrupperTilTilganger,
                        tilgangsgrupperTilBrukerroller = tilgangsgrupperTilBrukerroller,
                    )
                }
            }

            routing {
                additionalRoutes()
                appRoutes(personPseudoIdProvider, transactionProvider, tilgangsmaskinenClient, padm2Client)
            }
        },
    ).start(wait = true)
}

private fun Map<String, String>.getUUIDList(key: String): List<UUID> = this[key]?.split(",")?.map { UUID.fromString(it.trim()) } ?: emptyList()
