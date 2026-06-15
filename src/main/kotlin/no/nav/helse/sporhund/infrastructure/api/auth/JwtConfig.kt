package no.nav.helse.sporhund.infrastructure.api.auth

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import no.nav.helse.sporhund.application.tilgangskontroll.TilgangsgrupperTilTilganger
import no.nav.helse.sporhund.domain.NavIdent
import no.nav.helse.sporhund.domain.Saksbehandler
import no.nav.helse.sporhund.domain.SaksbehandlerOid
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.*

private val logger = LoggerFactory.getLogger("JWT-Auth")

fun JWTAuthenticationProvider.Config.configureJwtAuthentication(
    azureAdConfig: AzureAdConfig,
    tilgangsgrupperTilTilganger: TilgangsgrupperTilTilganger,
) {
    val jwkProvider = JwkProviderBuilder(URI(azureAdConfig.jwkProviderUri).toURL()).build()

    skipWhen { call -> call.request.uri.startsWith("/api/openapi.json") || call.request.uri.startsWith("/api/swagger") }

    verifier(jwkProvider, azureAdConfig.issuerUrl) {
        withAudience(azureAdConfig.clientId)
    }

    validate { credentials ->
        try {
            val saksbehandler = credentials.tilSaksbehandler()
            val accessToken =
                accessToken()
                    ?: return@validate null
            SaksbehandlerPrincipal(
                saksbehandler = saksbehandler,
                accessToken = accessToken,
                tilganger = tilgangsgrupperTilTilganger.finnTilgangerFraTilgangsgrupper(credentials.groupsAsUuids()),
            )
        } catch (ex: Exception) {
            logger.error("Feil ved validering av JWT", ex)
            null
        }
    }
}

private fun ApplicationCall.accessToken(): String? =
    request.headers[HttpHeaders.Authorization]
        ?.removePrefix("Bearer ")
        ?.trim()

private fun JWTCredential.groupsAsUuids(): List<UUID> =
    payload
        .getClaim("groups")
        ?.asList(String::class.java)
        ?.map(UUID::fromString)
        .orEmpty()

private fun JWTCredential.tilSaksbehandler(): Saksbehandler =
    with(payload) {
        Saksbehandler(
            id = getClaim("oid").asString().let(UUID::fromString).let(::SaksbehandlerOid),
            navn = getClaim("name").asString(),
            epost = getClaim("preferred_username").asString(),
            ident = getClaim("NAVident").asString().let(::NavIdent),
        )
    }
