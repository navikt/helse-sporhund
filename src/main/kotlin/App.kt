import api.*
import com.github.navikt.tbd_libs.kafka.AivenConfig
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.naisful.naisApp
import db.DataSourceBuilder
import db.DbConfig
import db.PgTransactionProvider
import db.objectMapper
import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kafka.KafkaConfig
import kafka.KafkaConsumer
import kafka.KafkaProducer
import kafka.ReadTopics
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

const val DIALOGMELDING_FRA_NAY_TOPIC = "teamsykefravr.isdialogmelding-behandler-dialogmelding-bestilling"
const val DIALOGMELDING_STATUS_TOPIC = "teamsykefravr.behandler-dialogmelding-status"
const val DIALOGMELDING_FRA_BEHANDLER_TOPIC = "teamsykefravr.dialogmelding"
const val LEGEERKLÆRING_TOPIC = "teamsykmelding.legeerklaering"

fun main() {
    val env = System.getenv()
    val kafkaConfig =
        KafkaConfig(
            aivenConfig = AivenConfig.default,
            readTopics =
                ReadTopics(
                    dialogmeldingFraBehandlerTopic = DIALOGMELDING_FRA_BEHANDLER_TOPIC,
                    dialogmeldingStatusTopic = DIALOGMELDING_STATUS_TOPIC,
                    legeerklæringTopic = DIALOGMELDING_STATUS_TOPIC, // TODO: Endre tilbake til legeerklæring-topicet når vi har fått tilgang
                ),
            writeTopic = DIALOGMELDING_FRA_NAY_TOPIC,
        )
    val dbConfig =
        DbConfig(
            jdbcUrl = env.getValue("DATABASE_JDBC_URL"),
            username = env.getValue("DATABASE_USERNAME"),
            password = env.getValue("DATABASE_PASSWORD"),
        )
    app(
        kafkaConfig = kafkaConfig,
        dbConfig = dbConfig,
    )
}

fun app(
    kafkaConfig: KafkaConfig,
    dbConfig: DbConfig,
    port: Int = 8080,
) {
    val factory = ConsumerProducerFactory(kafkaConfig.aivenConfig)
    val running = AtomicBoolean(false)

    val dataSourceBuilder = DataSourceBuilder(dbConfig)

    val transactionProvider = PgTransactionProvider(dataSourceBuilder.build())
    val kafkaConsumer =
        KafkaConsumer(
            topics = kafkaConfig.readTopics,
            consumerGroupId = System.getenv("KAFKA_CONSUMER_GROUP_ID") ?: "local-group",
            readyToConsume = running,
            consumerProducerFactory = factory,
            transactionProvider = transactionProvider,
        )
    val kafkaProducer = KafkaProducer(kafkaConfig.writeTopic, factory, transactionProvider)

    naisApp(
        meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        objectMapper = objectMapper,
        applicationLogger = LoggerFactory.getLogger("tjenestekall"),
        callLogger = LoggerFactory.getLogger("tjenestekall"),
        port = port,
        developmentMode = false,
        gracefulShutdownDelay = 10.seconds,
        applicationModule = {
            this.monitor.subscribe(ApplicationStarting) {
                dataSourceBuilder.migrate()
            }
            this.monitor.subscribe(ApplicationStarted) {
                running.set(true)
                val exceptionHandler =
                    CoroutineExceptionHandler { _, throwable ->
                        it.log.error("Feil i coroutine, terminerer appen", throwable)
                        it.engine.stop()
                    }
                launch(exceptionHandler) { kafkaConsumer.start() }
                launch(exceptionHandler) { kafkaProducer.start() }
            }
            this.monitor.subscribe(ApplicationStopping) {
                running.set(false)
            }

            install(OpenApi) { configureOpenApiPlugin() }
            // TODO auth???
            routing {
                route("/api") {
                    route("/openapi.json") {
                        openApi()
                    }
                    route("swagger") {
                        swaggerUI("../openapi.json")
                    }

                    get("/personer/{pseudoId}/dialogmeldinger", {
                        operationId = "getDialogmeldinger"
                        description = "Hent oversikt over alle dialoger gruppert per behandler"
                        request {
                            pathParameter<String>("pseudoId") {
                                description = "Pseudonymisert person-ID"
                                required = true
                            }
                        }
                        response {
                            HttpStatusCode.OK to {
                                description = "Dialogoversikt gruppert per behandler"
                                body<List<ApiBehandlerMedDialoger>>()
                            }
                        }
                    }) {
//                        val pseudoId = call.parameters["pseudoId"]
//                        veksle pseudoId med fødselsnummer her
                        call.respond(MockStore.hentOversikt())
                    }

                    get("/personer/{pseudoId}/dialogmeldinger/{dialogId}", {
                        operationId = "getDialogmelding"
                        description = "Hent en enkelt dialog med alle meldinger"
                        request {
                            pathParameter<String>("pseudoId") {
                                description = "Pseudonymisert person-ID"
                                required = true
                            }
                            pathParameter<String>("dialogId") {
                                description = "ID til dialogen"
                                required = true
                            }
                        }
                        response {
                            HttpStatusCode.OK to {
                                description = "Full dialog med alle meldinger"
                                body<ApiDialogDetails>()
                            }
                            HttpStatusCode.NotFound to {
                                description = "Dialog ikke funnet"
                            }
                        }
                    }) {
//                        val pseudoId = call.parameters["pseudoId"]
//                        veksle pseudoId med fødselsnummer her
                        val dialogId = call.parameters["dialogId"]!!
                        val dialog = MockStore.hentDialog(dialogId)
                        if (dialog != null) {
                            call.respond(dialog)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }

                    post("/personer/{pseudoId}/dialogmelding", {
                        operationId = "postDialogmelding"
                        description = "Send ny dialogmelding"
                        request {
                            pathParameter<String>("pseudoId") {
                                description = "Pseudonymisert person-ID"
                                required = true
                            }
                            body<ApiNyDialogmelding>()
                        }
                        response {
                            HttpStatusCode.Created to {
                                description = "Dialogmelding opprettet"
                                body<ApiDialogDetails>()
                            }
                        }
                    }) {
//                        val pseudoId = call.parameters["pseudoId"]
//                        veksle pseudoId med fødselsnummer her
                        val ny = call.receive<ApiNyDialogmelding>()
                        val opprettet = MockStore.leggTilMelding(ny)
                        call.respond(HttpStatusCode.Created, opprettet)
                    }

                    post("/personer/{pseudoId}/dialogmeldinger/{dialogId}/svar", {
                        operationId = "postSvarPaDialog"
                        description = "Svar på en eksisterende dialog"
                        request {
                            pathParameter<String>("pseudoId") {
                                description = "Pseudonymisert person-ID"
                                required = true
                            }
                            pathParameter<String>("dialogId") {
                                description = "ID til dialogen"
                                required = true
                            }
                            body<ApiSvarPaDialog>()
                        }
                        response {
                            HttpStatusCode.Created to {
                                description = "Svar lagt til i dialogen"
                                body<ApiDialogDetails>()
                            }
                            HttpStatusCode.NotFound to {
                                description = "Dialog ikke funnet"
                            }
                        }
                    }) {
//                        val pseudoId = call.parameters["pseudoId"]
//                        veksle pseudoId med fødselsnummer her
                        val dialogId = call.parameters["dialogId"]!!
                        val svar = call.receive<ApiSvarPaDialog>()
                        val oppdatert = MockStore.svarPåDialog(dialogId, svar)
                        if (oppdatert != null) {
                            call.respond(HttpStatusCode.Created, oppdatert)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                }
            }
        },
    ).start(wait = true)
}
