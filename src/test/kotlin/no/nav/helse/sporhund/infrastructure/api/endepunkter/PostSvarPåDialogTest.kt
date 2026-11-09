package no.nav.helse.sporhund.infrastructure.api.endepunkter

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.domain.testhelpers.lagIdentitetsnummer
import no.nav.helse.sporhund.infrastructure.api.ApiDialogDetails
import no.nav.helse.sporhund.infrastructure.api.ApiSvarPaDialog
import no.nav.helse.sporhund.infrastructure.api.testhelpers.jsonClient
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class PostSvarPåDialogTest : EndepunktTest() {
    @Test
    fun `returnerer 201 med oppdatert dialog`() =
        testApp {
            val dialog = lagDialog()
            val pseudoId = lagreDialog(dialog)

            val client = jsonClient()
            val token = skrivetoken()

            val svar = ApiSvarPaDialog(melding = "Dette er et svar")

            val response =
                client.post(
                    "/api/personer/${pseudoId.value}/dialogmeldinger/${dialog.conversationRef.value}",
                ) {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(svar)
                }

            assertEquals(HttpStatusCode.Created, response.status)
            val oppdatert = response.body<ApiDialogDetails>()
            assertEquals(dialog.conversationRef.value, oppdatert.conversationRef)
            assertEquals(2, oppdatert.dialogmeldinger.size)
        }

    @Test
    fun `returnerer 404 for ukjent pseudoId`() =
        testApp {
            val client = jsonClient()
            val token = skrivetoken()

            val response =
                client.post(
                    "/api/personer/${UUID.randomUUID()}/dialogmeldinger/${UUID.randomUUID()}",
                ) {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(ApiSvarPaDialog(melding = "Svar"))
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
            assertIngentingSendt()
        }

    @Test
    fun `returnerer 404 for ukjent conversationRef`() =
        testApp {
            val pseudoId = personPseudoIdProvider.nyPersonPseudoId(lagIdentitetsnummer())

            val client = jsonClient()
            val token = skrivetoken()

            val response =
                client.post(
                    "/api/personer/${pseudoId.value}/dialogmeldinger/${UUID.randomUUID()}",
                ) {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(ApiSvarPaDialog(melding = "Svar"))
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
            assertIngentingSendt()
        }

    @Test
    fun `returnerer 401 uten token`() =
        testApp {
            val response =
                client.post(
                    "/api/personer/${UUID.randomUUID()}/dialogmeldinger/${UUID.randomUUID()}",
                ) {
                    contentType(ContentType.Application.Json)
                    setBody("{}")
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertIngentingSendt()
        }
}
