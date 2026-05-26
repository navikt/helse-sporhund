package no.nav.helse.sporhund.infrastructure.api.endepunkter

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import no.nav.helse.sporhund.domain.Dialogstatus
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.domain.testhelpers.lagIdentitetsnummer
import no.nav.helse.sporhund.infrastructure.api.ApiDialogDetails
import no.nav.helse.sporhund.infrastructure.api.ApiOppdaterDialogStatus
import no.nav.helse.sporhund.infrastructure.api.testhelpers.jsonClient
import no.nav.helse.sporhund.infrastructure.api.testhelpers.utstedToken
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class PatchDialogstatusRouteTest : EndepunktTest() {
    @Test
    fun `returnerer 200 og ferdigstiller dialog`() =
        testApplication {
            val identitetsnummer = lagIdentitetsnummer()
            val pseudoId = personPseudoIdProvider.nyPersonPseudoId(identitetsnummer)
            val dialog = lagDialog(identitetsnummer = identitetsnummer)
            transactionProvider.dialogRepository.lagre(dialog)

            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)

            val response =
                client.patch("/api/personer/${pseudoId.value}/dialogmeldinger/${dialog.conversationRef.value}") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(ApiOppdaterDialogStatus(ferdigstilt = true))
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.body<ApiDialogDetails>()
            assertEquals(dialog.conversationRef.value, body.conversationRef)

            val oppdatertDialog =
                transactionProvider.transaction {
                    dialogRepository.finnDialog(dialog.conversationRef)
                }
            assertEquals(Dialogstatus.DialogLukket, oppdatertDialog?.status)
        }

    @Test
    fun `returnerer 200 og gjenåpner dialog`() =
        testApplication {
            val identitetsnummer = lagIdentitetsnummer()
            val pseudoId = personPseudoIdProvider.nyPersonPseudoId(identitetsnummer)
            val dialog = lagDialog(identitetsnummer = identitetsnummer, status = Dialogstatus.DialogLukket)
            transactionProvider.dialogRepository.lagre(dialog)

            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)

            val response =
                client.patch("/api/personer/${pseudoId.value}/dialogmeldinger/${dialog.conversationRef.value}") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(ApiOppdaterDialogStatus(ferdigstilt = false))
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.body<ApiDialogDetails>()
            assertEquals(dialog.conversationRef.value, body.conversationRef)

            val oppdatertDialog =
                transactionProvider.transaction {
                    dialogRepository.finnDialog(dialog.conversationRef)
                }
            assertEquals(Dialogstatus.ForespørselSendt, oppdatertDialog?.status)
        }

    @Test
    fun `returnerer 404 for ukjent pseudoId`() =
        testApplication {
            val dialog = lagDialog()
            transactionProvider.dialogRepository.lagre(dialog)

            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)

            val response =
                client.patch("/api/personer/${UUID.randomUUID()}/dialogmeldinger/${dialog.conversationRef.value}") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(ApiOppdaterDialogStatus(ferdigstilt = true))
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `returnerer 404 hvis dialog tilhoerer annen person`() =
        testApplication {
            val riktigIdentitetsnummer = lagIdentitetsnummer()
            val feilPseudoId = personPseudoIdProvider.nyPersonPseudoId(lagIdentitetsnummer())
            val dialog = lagDialog(identitetsnummer = riktigIdentitetsnummer)
            transactionProvider.dialogRepository.lagre(dialog)

            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)

            val response =
                client.patch("/api/personer/${feilPseudoId.value}/dialogmeldinger/${dialog.conversationRef.value}") {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(ApiOppdaterDialogStatus(ferdigstilt = true))
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `returnerer 401 uten token`() =
        testApplication {
            setupDefaultTestApp()

            val response =
                client.patch("/api/personer/${UUID.randomUUID()}/dialogmeldinger/${UUID.randomUUID()}") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"ferdigstilt\":true}")
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
}
