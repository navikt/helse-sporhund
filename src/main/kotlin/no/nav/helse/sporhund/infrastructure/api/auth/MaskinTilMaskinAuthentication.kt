package no.nav.helse.sporhund.infrastructure.api.auth

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.auth.jwt.*
import no.nav.helse.sporhund.application.logg.loggWarn
import java.net.URI

data class MaskinTilMaskinConfig(
    val preautoriserteKlientIder: Set<String>,
)

data class MaskinPrincipal(
    val klientId: String,
    val roller: Set<String>,
)

/**
 * Godtar bare app-tokens (client_credentials) fra apper som er forhåndsautorisert i accessPolicy.
 * Rollen sjekkes i ruta, slik at et gyldig token uten rollen gir 403 og ikke 401.
 */
fun JWTAuthenticationProvider.Config.configureMaskinTilMaskinAuthentication(
    azureAdConfig: AzureAdConfig,
    maskinTilMaskinConfig: MaskinTilMaskinConfig,
) {
    val jwkProvider = JwkProviderBuilder(URI(azureAdConfig.jwkProviderUri).toURL()).build()

    verifier(jwkProvider, azureAdConfig.issuerUrl) {
        withAudience(azureAdConfig.clientId)
        withClaim("idtyp", "app")
    }

    validate { credentials ->
        val klientId = credentials.payload.getClaim("azp").asString()
        if (klientId == null || klientId !in maskinTilMaskinConfig.preautoriserteKlientIder) {
            loggWarn("Avviser maskintoken fra app som ikke er forhåndsautorisert", "azp" to klientId)
            return@validate null
        }
        MaskinPrincipal(
            klientId = klientId,
            roller =
                credentials.payload
                    .getClaim("roles")
                    ?.asList(String::class.java)
                    .orEmpty()
                    .toSet(),
        )
    }
}
