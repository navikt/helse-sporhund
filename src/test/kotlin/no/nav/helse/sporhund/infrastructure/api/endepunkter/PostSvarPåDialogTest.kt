package no.nav.helse.sporhund.infrastructure.api.endepunkter

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import no.nav.helse.sporhund.application.NyDialogmeldingFraNav
import no.nav.helse.sporhund.application.OpprettUtgåendeJournalpost
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.domain.testhelpers.lagIdentitetsnummer
import no.nav.helse.sporhund.infrastructure.api.ApiDialogDetails
import no.nav.helse.sporhund.infrastructure.api.ApiSvarPaDialog
import no.nav.helse.sporhund.infrastructure.api.testhelpers.jsonClient
import no.nav.helse.sporhund.infrastructure.api.testhelpers.utstedToken
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class PostSvarPåDialogTest : EndepunktTest() {
    @Test
    fun `returnerer 201 med oppdatert dialog`() =
        testApp {
            val identitetsnummer = lagIdentitetsnummer()
            val pseudoId = personPseudoIdProvider.nyPersonPseudoId(identitetsnummer)

            val dialog = lagDialog(identitetsnummer = identitetsnummer)
            transactionProvider.dialogRepository.lagre(dialog)

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)

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
            assertOutboxContains<NyDialogmeldingFraNav>()
            assertOutboxContains<OpprettUtgåendeJournalpost>()
        }

    @Test
    fun `returnerer 404 for ukjent pseudoId`() =
        testApplication {
            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)

            val response =
                client.post(
                    "/api/personer/${UUID.randomUUID()}/dialogmeldinger/${UUID.randomUUID()}",
                ) {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(ApiSvarPaDialog(melding = "Svar"))
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEmptyOutbox()
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
                client.post(
                    "/api/personer/${pseudoId.value}/dialogmeldinger/${UUID.randomUUID()}",
                ) {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(ApiSvarPaDialog(melding = "Svar"))
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEmptyOutbox()
        }

    @Test
    fun `returnerer 401 uten token`() =
        testApplication {
            setupDefaultTestApp()

            val response =
                client.post(
                    "/api/personer/${UUID.randomUUID()}/dialogmeldinger/${UUID.randomUUID()}",
                ) {
                    contentType(ContentType.Application.Json)
                    setBody("{}")
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEmptyOutbox()
        }
}
