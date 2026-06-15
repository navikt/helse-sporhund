package no.nav.helse.sporhund.infrastructure.api.endepunkter

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.domain.testhelpers.lagIdentitetsnummer
import no.nav.helse.sporhund.infrastructure.api.ApiDialogDetails
import no.nav.helse.sporhund.infrastructure.api.testhelpers.jsonClient
import no.nav.helse.sporhund.infrastructure.api.testhelpers.utstedTokenMedLesTilgang
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class GetDialogmeldingTest : EndepunktTest() {
    @Test
    fun `returnerer 200 med dialog for kjent conversationRef`() =
        testApplication {
            val identitetsnummer = lagIdentitetsnummer()
            val pseudoId = personPseudoIdProvider.nyPersonPseudoId(identitetsnummer)

            val dialog = lagDialog(identitetsnummer = identitetsnummer)
            transactionProvider.dialogRepository.lagre(dialog)

            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedTokenMedLesTilgang(saksbehandler, tilgangsgrupperTilTilganger)

            val response =
                client.get(
                    "/api/personer/${pseudoId.value}/dialogmeldinger/${dialog.conversationRef.value}",
                ) { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            val details = response.body<ApiDialogDetails>()
            assertEquals(dialog.conversationRef.value, details.conversationRef)
            assertEquals(1, details.dialogmeldinger.size)
        }

    @Test
    fun `returnerer 404 for ukjent pseudoId`() =
        testApplication {
            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedTokenMedLesTilgang(saksbehandler, tilgangsgrupperTilTilganger)

            val response =
                client.get(
                    "/api/personer/${UUID.randomUUID()}/dialogmeldinger/${UUID.randomUUID()}",
                ) { bearerAuth(token) }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `returnerer 404 for ukjent conversationRef`() =
        testApplication {
            val identitetsnummer = lagIdentitetsnummer()
            val pseudoId = personPseudoIdProvider.nyPersonPseudoId(identitetsnummer)

            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedTokenMedLesTilgang(saksbehandler, tilgangsgrupperTilTilganger)

            val response =
                client.get(
                    "/api/personer/${pseudoId.value}/dialogmeldinger/${UUID.randomUUID()}",
                ) { bearerAuth(token) }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `returnerer 401 uten token`() =
        testApplication {
            setupDefaultTestApp()

            val response = client.get("/api/personer/${UUID.randomUUID()}/dialogmeldinger/${UUID.randomUUID()}")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
}
