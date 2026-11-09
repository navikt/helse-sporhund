package no.nav.helse.sporhund.infrastructure.api.endepunkter

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.sporhund.domain.Dialogstatus
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.domain.testhelpers.lagIdentitetsnummer
import no.nav.helse.sporhund.infrastructure.api.ApiDialogDetails
import no.nav.helse.sporhund.infrastructure.api.ApiOppdaterDialogStatus
import no.nav.helse.sporhund.infrastructure.api.testhelpers.jsonClient
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class PatchDialogstatusRouteTest : EndepunktTest() {
    @Test
    fun `returnerer 200 og ferdigstiller dialog`() =
        testApp {
            val dialog = lagDialog()
            val pseudoId = lagreDialog(dialog)

            val client = jsonClient()
            val token = skrivetoken()

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
        testApp {
            val dialog = lagDialog(status = Dialogstatus.DialogLukket)
            val pseudoId = lagreDialog(dialog)

            val client = jsonClient()
            val token = skrivetoken()

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
        testApp {
            val dialog = lagDialog()
            lagreDialog(dialog)

            val client = jsonClient()
            val token = skrivetoken()

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
        testApp {
            val feilPseudoId = personPseudoIdProvider.nyPersonPseudoId(lagIdentitetsnummer())
            val dialog = lagDialog()
            lagreDialog(dialog)

            val client = jsonClient()
            val token = skrivetoken()

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
        testApp {
            val response =
                client.patch("/api/personer/${UUID.randomUUID()}/dialogmeldinger/${UUID.randomUUID()}") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"ferdigstilt\":true}")
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
}
