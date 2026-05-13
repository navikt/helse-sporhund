package no.nav.helse.sporhund.api.auth

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.auth.jwt.JWTAuthenticationProvider
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.request.uri
import no.nav.helse.sporhund.domain.NavIdent
import no.nav.helse.sporhund.domain.Saksbehandler
import no.nav.helse.sporhund.domain.SaksbehandlerOid
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.UUID

private val logger = LoggerFactory.getLogger("JWT-Auth")

fun JWTAuthenticationProvider.Config.configureJwtAuthentication(azureAdConfig: AzureAdConfig) {
    val jwkProvider = JwkProviderBuilder(URI(azureAdConfig.jwkProviderUri).toURL()).build()

    skipWhen { call -> call.request.uri.startsWith("/api/openapi.json") || call.request.uri.startsWith("/api/swagger") }

    verifier(jwkProvider, azureAdConfig.issuerUrl) {
        withAudience(azureAdConfig.clientId)
    }

    validate { credentials ->
        try {
            val saksbehandler = credentials.tilSaksbehandler()
            SaksbehandlerPrincipal(saksbehandler)
        } catch (ex: Exception) {
            logger.error("Feil ved validering av JWT", ex)
            null
        }
    }
}

private fun JWTCredential.tilSaksbehandler(): Saksbehandler =
    with(payload) {
        Saksbehandler(
            id = getClaim("oid").asString().let(UUID::fromString).let(::SaksbehandlerOid),
            navn = getClaim("name").asString(),
            epost = getClaim("preferred_username").asString(),
            ident = getClaim("NAVident").asString().let(::NavIdent),
        )
    }
