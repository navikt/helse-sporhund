package no.nav.helse.sporhund.api.endepunkter

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.helse.sporhund.api.ApiDialogOppsummering
import no.nav.helse.sporhund.api.testhelpers.FakePersonPseudoIdProvider
import no.nav.helse.sporhund.api.testhelpers.FakeTransactionProvider
import no.nav.helse.sporhund.api.testhelpers.jsonClient
import no.nav.helse.sporhund.api.testhelpers.setupTestApp
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
import kotlin.test.assertTrue

class GetDialogmeldingerTest : EndepunktTest() {
    @Test
    fun `returnerer 200 med dialoger for kjent pseudoId`() =
        testApplication {
            val identitetsnummer = lagIdentitetsnummer()
            val personPseudoIdProvider = FakePersonPseudoIdProvider()
            val pseudoId = personPseudoIdProvider.registrer(identitetsnummer)

            val transactionProvider = FakeTransactionProvider()
            val dialog =
                Dialog.ny(
                    identitetsnummer,
                    Dialogmelding.FraNav.ny(lagNavIdent(), lagBehandler(), lagBehandlerRef(), "Hei behandler"),
                )
            transactionProvider.dialogRepository.leggTil(dialog)

            setupTestApp(personPseudoIdProvider, transactionProvider, mockOAuth2Server)

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)

            val response = client.get("/api/personer/${pseudoId.value}/dialogmeldinger") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            val dialoger = response.body<List<ApiDialogOppsummering>>()
            assertEquals(1, dialoger.size)
            assertEquals(dialog.conversationRef.value, dialoger.first().conversationRef)
        }

    @Test
    fun `returnerer 200 med tom liste for ukjent pseudoId`() =
        testApplication {
            setupTestApp(FakePersonPseudoIdProvider(), FakeTransactionProvider(), mockOAuth2Server)

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)

            val response = client.get("/api/personer/${UUID.randomUUID()}/dialogmeldinger") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            val dialoger = response.body<List<ApiDialogOppsummering>>()
            assertTrue(dialoger.isEmpty())
        }

    @Test
    fun `returnerer 401 uten token`() =
        testApplication {
            setupTestApp(FakePersonPseudoIdProvider(), FakeTransactionProvider(), mockOAuth2Server)

            val response = client.get("/api/personer/${UUID.randomUUID()}/dialogmeldinger")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
}
