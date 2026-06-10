package no.nav.helse.sporhund

import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import no.nav.helse.sporhund.domain.testhelpers.lagSaksbehandler
import no.nav.helse.sporhund.infrastructure.api.auth.AzureAdConfig
import no.nav.helse.sporhund.infrastructure.clients.accesstokenprovider.AccessTokenProviderConfig
import no.nav.helse.sporhund.infrastructure.clients.accesstokenprovider.testhelpers.MockTexasServer
import no.nav.helse.sporhund.infrastructure.clients.dokarkiv.DokarkivConfig
import no.nav.helse.sporhund.infrastructure.clients.dokarkiv.testhelpers.MockDokarkivServer
import no.nav.helse.sporhund.infrastructure.clients.padm2.Padm2Config
import no.nav.helse.sporhund.infrastructure.clients.personpseudoid.testhelpers.TestcontainersValkey
import no.nav.helse.sporhund.infrastructure.clients.populasjonstilgangskontroll.PopulasjonstilgangskontrollConfig
import no.nav.helse.sporhund.infrastructure.clients.populasjonstilgangskontroll.testhelpers.MockTilgangsmaskinenServer
import no.nav.helse.sporhund.infrastructure.clients.sprinter.SprinterConfig
import no.nav.helse.sporhund.infrastructure.clients.sprinter.testhelpers.MockSprinterServer
import no.nav.helse.sporhund.infrastructure.db.testhelpers.TestcontainersDatabase
import no.nav.helse.sporhund.infrastructure.kafka.KafkaConfig
import no.nav.helse.sporhund.infrastructure.kafka.ReadTopics
import no.nav.helse.sporhund.infrastructure.kafka.testhelpers.TestcontainersKafka
import no.nav.security.mock.oauth2.MockOAuth2Server

fun main() {
    val clientId = "en-client-id"
    val issuerId = "LocalTestIssuer"
    val mockOAuth2Server = MockOAuth2Server().also { it.start() }
    val kafka = TestcontainersKafka("local-app")
    val postgres = TestcontainersDatabase("local-app")
    val valkey = TestcontainersValkey("local-app")
    val mockTexasServer = MockTexasServer()
    val mockTilgangsmaskinenServer = MockTilgangsmaskinenServer()
    val mockSprinterServer = MockSprinterServer()
    val mockDokarkivServer = MockDokarkivServer()

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
            mockTexasServer.stop()
            mockTilgangsmaskinenServer.stop()
            mockSprinterServer.stop()
            mockDokarkivServer.stop()
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
        personPseudoIdConfig = valkey.personPseudoIdConfig,
        populasjonstilgangskontrollConfig =
            PopulasjonstilgangskontrollConfig(
                scope = "test-scope",
                baseUrl = mockTilgangsmaskinenServer.baseUrl,
            ),
        accessTokenProviderConfig =
            AccessTokenProviderConfig(
                tokenEndpoint = mockTexasServer.tokenEndpoint,
                exchangeEndpoint = mockTexasServer.exchangeEndpoint,
            ),
        padm2Config =
            Padm2Config(
                baseUrl = "http://localhost",
                scope = "local-padm2-scope",
            ),
        sprinterConfig =
            SprinterConfig(
                baseUrl = mockSprinterServer.baseUrl,
            ),
        dokarkivConfig =
            DokarkivConfig(
                baseUrl = mockDokarkivServer.baseUrl,
                scope = "local-dokarkiv-scope",
            ),
        port = 8282,
    ) { addAdditionalRoutings(this) }
}
