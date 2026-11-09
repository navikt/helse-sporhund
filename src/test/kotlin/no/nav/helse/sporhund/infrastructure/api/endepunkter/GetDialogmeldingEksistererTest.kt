package no.nav.helse.sporhund.infrastructure.api.endepunkter

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.helse.sporhund.application.SessionContext
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.domain.testhelpers.lagFraBehandlerMelding
import no.nav.helse.sporhund.infrastructure.api.ApiDialogmeldingEksisterer
import no.nav.helse.sporhund.infrastructure.api.testhelpers.PREAUTORISERT_KLIENT_ID
import no.nav.helse.sporhund.infrastructure.api.testhelpers.jsonClient
import no.nav.helse.sporhund.infrastructure.api.testhelpers.setupTestApp
import no.nav.helse.sporhund.infrastructure.api.testhelpers.utstedMaskintoken
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class GetDialogmeldingEksistererTest : EndepunktTest() {
    @Test
    fun `svarer true når svaret er tatt inn`() =
        testApp {
            val msgId = UUID.randomUUID()
            lagreDialog(lagDialog().also { it.nyMelding(lagFraBehandlerMelding(msgId = msgId)) })

            val response = jsonClient().get(url(msgId.toString())) { bearerAuth(maskintoken()) }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ApiDialogmeldingEksisterer(eksisterer = true), response.body<ApiDialogmeldingEksisterer>())
        }

    @Test
    fun `svarer false for ukjent msgId`() =
        testApp {
            lagreDialog(lagDialog().also { it.nyMelding(lagFraBehandlerMelding()) })

            val response = jsonClient().get(url(UUID.randomUUID().toString())) { bearerAuth(maskintoken()) }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ApiDialogmeldingEksisterer(eksisterer = false), response.body<ApiDialogmeldingEksisterer>())
        }

    @Test
    fun `svarer false når id-en tilhører en utgående melding`() =
        testApp {
            val dialog = lagDialog()
            lagreDialog(dialog)
            val utgåendeId =
                dialog.meldinger
                    .single()
                    .id.value
                    .toString()

            val response = jsonClient().get(url(utgåendeId)) { bearerAuth(maskintoken()) }

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ApiDialogmeldingEksisterer(eksisterer = false), response.body<ApiDialogmeldingEksisterer>())
        }

    @Test
    fun `svarer 400 for ugyldig msgId`() =
        testApp {
            val forLang = "a".repeat(129)

            assertEquals(HttpStatusCode.BadRequest, client.get(url(forLang)) { bearerAuth(maskintoken()) }.status)
            assertEquals(HttpStatusCode.BadRequest, client.get(url("ugyldig%20tegn")) { bearerAuth(maskintoken()) }.status)
        }

    @Test
    fun `svarer 403 for maskintoken uten rollen`() =
        testApp {
            val response = client.get(url(UUID.randomUUID().toString())) { bearerAuth(maskintoken(roller = emptyList())) }

            assertEquals(HttpStatusCode.Forbidden, response.status)
        }

    @Test
    fun `avviser maskintoken fra app som ikke er forhåndsautorisert`() =
        testApp {
            val response = client.get(url(UUID.randomUUID().toString())) { bearerAuth(maskintoken(azp = "ukjent-klient")) }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `avviser saksbehandlertoken`() =
        testApp {
            val response = client.get(url(UUID.randomUUID().toString())) { bearerAuth(lesetoken()) }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `svarer 5xx og ikke false når databasen feiler`() =
        testApplication {
            setupTestApp(
                personPseudoIdProvider,
                object : TransactionProvider {
                    override fun <T> transaction(session: SessionContext.() -> T): T = error("Databasen er nede")
                },
                mockOAuth2Server,
                populasjonstilgangskontrollProvider,
                tilgangsgrupperTilTilganger,
                tilgangsgrupperTilBrukerroller,
            )

            val status = client.get(url(UUID.randomUUID().toString())) { bearerAuth(maskintoken()) }.status

            assertEquals(HttpStatusCode.InternalServerError, status)
        }

    @Test
    fun `avviser token som ikke er et app-token`() =
        testApp {
            val token = mockOAuth2Server.utstedMaskintoken(idtyp = null)

            assertEquals(HttpStatusCode.Unauthorized, client.get(url(UUID.randomUUID().toString())) { bearerAuth(token) }.status)
        }

    @Test
    fun `svarer 401 uten token`() =
        testApp {
            assertEquals(HttpStatusCode.Unauthorized, client.get(url(UUID.randomUUID().toString())).status)
        }

    private fun url(msgId: String) = "/api/syfo/v1/dialogmelding/$msgId/eksisterer"

    private fun maskintoken(
        roller: List<String> = listOf(DIALOGMELDING_EKSISTERER_ROLLE),
        azp: String = PREAUTORISERT_KLIENT_ID,
    ) = mockOAuth2Server.utstedMaskintoken(roller = roller, azp = azp)
}
