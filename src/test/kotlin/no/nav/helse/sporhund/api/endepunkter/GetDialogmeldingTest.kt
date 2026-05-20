package no.nav.helse.sporhund.api.endepunkter

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.helse.sporhund.api.ApiDialogDetails
import no.nav.helse.sporhund.api.testhelpers.jsonClient
import no.nav.helse.sporhund.api.testhelpers.utstedToken
import no.nav.helse.sporhund.domain.Dialog
import no.nav.helse.sporhund.domain.Dialogmelding
import no.nav.helse.sporhund.domain.testhelpers.lagBehandler
import no.nav.helse.sporhund.domain.testhelpers.lagBehandlerRef
import no.nav.helse.sporhund.domain.testhelpers.lagIdentitetsnummer
import no.nav.helse.sporhund.domain.testhelpers.lagNavIdent
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class GetDialogmeldingTest : EndepunktTest() {
    @Test
    fun `returnerer 200 med dialog for kjent conversationRef`() =
        testApplication {
            val identitetsnummer = lagIdentitetsnummer()
            val pseudoId = personPseudoIdProvider.nyPersonPseudoId(identitetsnummer)

            val dialog =
                Dialog.ny(
                    identitetsnummer,
                    Dialogmelding.FraNav.ny(lagNavIdent(), lagBehandler(), lagBehandlerRef(), "Forespørsel"),
                )
            transactionProvider.dialogRepository.lagre(dialog)

            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)

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
            val token = mockOAuth2Server.utstedToken(saksbehandler)

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
            val token = mockOAuth2Server.utstedToken(saksbehandler)

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
