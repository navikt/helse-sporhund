package no.nav.helse.sporhund.infrastructure.api.endepunkter

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.domain.testhelpers.lagIdentitetsnummer
import no.nav.helse.sporhund.infrastructure.api.ApiDialogDetails
import no.nav.helse.sporhund.infrastructure.api.testhelpers.jsonClient
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class GetDialogmeldingTest : EndepunktTest() {
    @Test
    fun `returnerer 200 med dialog for kjent conversationRef`() =
        testApp {
            val dialog = lagDialog()
            val pseudoId = lagreDialog(dialog)

            val client = jsonClient()

            val response =
                client.get(
                    "/api/personer/${pseudoId.value}/dialogmeldinger/${dialog.conversationRef.value}",
                ) { bearerAuth(lesetoken()) }

            assertEquals(HttpStatusCode.OK, response.status)
            val details = response.body<ApiDialogDetails>()
            assertEquals(dialog.conversationRef.value, details.conversationRef)
            assertEquals(1, details.dialogmeldinger.size)
        }

    @Test
    fun `returnerer 404 for ukjent pseudoId`() =
        testApp {
            val client = jsonClient()

            val response =
                client.get(
                    "/api/personer/${UUID.randomUUID()}/dialogmeldinger/${UUID.randomUUID()}",
                ) { bearerAuth(lesetoken()) }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `returnerer 404 for ukjent conversationRef`() =
        testApp {
            val identitetsnummer = lagIdentitetsnummer()
            val pseudoId = personPseudoIdProvider.nyPersonPseudoId(identitetsnummer)

            val client = jsonClient()

            val response =
                client.get(
                    "/api/personer/${pseudoId.value}/dialogmeldinger/${UUID.randomUUID()}",
                ) { bearerAuth(lesetoken()) }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `returnerer 401 uten token`() =
        testApp {
            val response = client.get("/api/personer/${UUID.randomUUID()}/dialogmeldinger/${UUID.randomUUID()}")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
}
