package no.nav.helse.sporhund.infrastructure.api.testhelpers

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.navikt.tbd_libs.populasjonstilgang.api.PopulasjonstilgangskontrollProvider
import io.github.smiley4.ktoropenapi.OpenApi
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import no.nav.helse.sporhund.application.PersonPseudoIdProvider
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.application.VedleggProvider
import no.nav.helse.sporhund.application.tilgangskontroll.TilgangsgrupperTilTilganger
import no.nav.helse.sporhund.domain.NavIdent
import no.nav.helse.sporhund.domain.Saksbehandler
import no.nav.helse.sporhund.domain.SaksbehandlerOid
import no.nav.helse.sporhund.infrastructure.api.appRoutes
import no.nav.helse.sporhund.infrastructure.api.auth.AzureAdConfig
import no.nav.helse.sporhund.infrastructure.api.auth.configureJwtAuthentication
import no.nav.helse.sporhund.infrastructure.api.configureOpenApiPlugin
import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.*

const val TEST_CLIENT_ID = "test-client-id"
const val TEST_ISSUER_ID = "TestIssuer"

fun ApplicationTestBuilder.setupTestApp(
    personPseudoIdProvider: PersonPseudoIdProvider,
    transactionProvider: TransactionProvider,
    mockOAuth2Server: MockOAuth2Server,
    populasjonstilgangskontrollProvider: PopulasjonstilgangskontrollProvider,
    tilgangsgrupperTilTilganger: TilgangsgrupperTilTilganger,
    vedleggProvider: VedleggProvider = VedleggProvider { emptyList() },
) {
    val azureAdConfig =
        AzureAdConfig(
            clientId = TEST_CLIENT_ID,
            issuerUrl = mockOAuth2Server.issuerUrl(TEST_ISSUER_ID).toString(),
            jwkProviderUri = mockOAuth2Server.jwksUrl(TEST_ISSUER_ID).toString(),
        )

    application {
        install(OpenApi) { configureOpenApiPlugin() }
        install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
        authentication {
            jwt("oidc") {
                configureJwtAuthentication(azureAdConfig, tilgangsgrupperTilTilganger)
            }
        }
        routing {
            appRoutes(personPseudoIdProvider, transactionProvider, populasjonstilgangskontrollProvider, vedleggProvider)
        }
    }
}

fun ApplicationTestBuilder.jsonClient(): HttpClient =
    createClient {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
    }

fun lagTestSaksbehandler() =
    Saksbehandler(
        id = SaksbehandlerOid(UUID.randomUUID()),
        navn = "Test Saksbehandler",
        epost = "test.saksbehandler@nav.no",
        ident = NavIdent("T123456"),
    )

fun MockOAuth2Server.utstedToken(
    saksbehandler: Saksbehandler,
    groups: List<UUID> = emptyList(),
): String =
    issueToken(
        issuerId = TEST_ISSUER_ID,
        audience = TEST_CLIENT_ID,
        subject = saksbehandler.id.value.toString(),
        claims =
            mapOf(
                "NAVident" to saksbehandler.ident.value,
                "preferred_username" to saksbehandler.epost,
                "oid" to saksbehandler.id.value.toString(),
                "name" to saksbehandler.navn,
                "groups" to groups.map { it.toString() },
            ),
    ).serialize()

fun MockOAuth2Server.utstedTokenMedLesTilgang(
    saksbehandler: Saksbehandler,
    tilgangsgrupperTilTilganger: TilgangsgrupperTilTilganger,
): String = utstedToken(saksbehandler, tilgangsgrupperTilTilganger.lesetilgang)

fun MockOAuth2Server.utstedTokenMedSkrivTilgang(
    saksbehandler: Saksbehandler,
    tilgangsgrupperTilTilganger: TilgangsgrupperTilTilganger,
): String = utstedToken(saksbehandler, tilgangsgrupperTilTilganger.skrivetilgang)
