package no.nav.helse.sporhund

import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.sporhund.auth.AzureAdConfig
import no.nav.helse.sporhund.db.testhelpers.TestcontainersDatabase
import no.nav.helse.sporhund.domain.testhelpers.lagSaksbehandler
import no.nav.helse.sporhund.kafka.KafkaConfig
import no.nav.helse.sporhund.kafka.ReadTopics
import no.nav.helse.sporhund.kafka.testhelpers.TestcontainersKafka
import no.nav.security.mock.oauth2.MockOAuth2Server

fun main() {
    val clientId = "en-client-id"
    val issuerId = "LocalTestIssuer"
    val mockOAuth2Server = MockOAuth2Server().also { it.start() }
    val kafka = TestcontainersKafka("local-app")
    val postgres = TestcontainersDatabase("local-app")

    val saksbehandler = lagSaksbehandler()

    fun localToken(): String =
        mockOAuth2Server
            .issueToken(
                issuerId = issuerId,
                audience = clientId,
                subject = saksbehandler.id.value.toString(),
                claims =
                    mapOf(
                        "NAVident" to saksbehandler.ident.value,
                        "preferred_username" to saksbehandler.epost,
                        "oid" to saksbehandler.id.value.toString(),
                        "name" to saksbehandler.navn,
                    ),
            ).serialize()

    val azureAdConfig =
        AzureAdConfig(
            clientId = clientId,
            issuerUrl = mockOAuth2Server.issuerUrl(issuerId).toString(),
            jwkProviderUri = mockOAuth2Server.jwksUrl(issuerId).toString(),
        )

    fun addAdditionalRoutings(routing: Routing) {
        with(routing) {
            get("/local-token") {
                return@get call.respond<String>(message = localToken())
            }
        }
    }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            kafka.stop()
            postgres.stop()
        },
    )

    val topics =
        ReadTopics(
            dialogmeldingFraBehandlerTopic = DIALOGMELDING_FRA_BEHANDLER_TOPIC,
            dialogmeldingStatusTopic = DIALOGMELDING_STATUS_TOPIC,
            legeerklæringTopic = LEGEERKLÆRING_TOPIC,
        )

    app(
        kafkaConfig =
            KafkaConfig(
                aivenConfig = kafka.config,
                readTopics = topics,
                writeTopic = DIALOGMELDING_FRA_NAY_TOPIC,
            ),
        dbConfig = postgres.dbConfig,
        azureAdConfig = azureAdConfig,
        additionalRoutes = { addAdditionalRoutings(this) },
        port = 8282,
    )
}
