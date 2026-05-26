package no.nav.helse.sporhund.infrastructure.api.endepunkter

import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import no.nav.helse.sporhund.domain.Dialogstatus
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.infrastructure.api.ApiDialogmeldingOppgave
import no.nav.helse.sporhund.infrastructure.api.ApiDialogmeldingStatus
import no.nav.helse.sporhund.infrastructure.api.testhelpers.jsonClient
import no.nav.helse.sporhund.infrastructure.api.testhelpers.utstedToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetDialogmeldingOppgaverTest : EndepunktTest() {
    @Test
    fun `returnerer 200 med tom liste når det ikke finnes noen åpne dialoger`() =
        testApplication {
            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)

            val response = client.get("/api/dialogmelding-oppgaver") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.body<List<ApiDialogmeldingOppgave>>().isEmpty())
        }

    @Test
    fun `returnerer åpen dialog som oppgave`() =
        testApplication {
            val dialog = lagDialog()
            transactionProvider.dialogRepository.lagre(dialog)

            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)

            val response = client.get("/api/dialogmelding-oppgaver") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            val oppgaver = response.body<List<ApiDialogmeldingOppgave>>()
            assertEquals(1, oppgaver.size)
            assertEquals(dialog.conversationRef.value, oppgaver.first().conversationRef)
        }

    @Test
    fun `inkluderer ikke lukket dialog`() =
        testApplication {
            val lukketDialog = lagDialog(status = Dialogstatus.DialogLukket)
            transactionProvider.dialogRepository.lagre(lukketDialog)

            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)

            val response = client.get("/api/dialogmelding-oppgaver") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.body<List<ApiDialogmeldingOppgave>>().isEmpty())
        }

    @Test
    fun `returnerer kun ikke-lukkede dialoger blant flere`() =
        testApplication {
            val åpenDialog = lagDialog()
            val lukketDialog = lagDialog(status = Dialogstatus.DialogLukket)
            transactionProvider.dialogRepository.lagre(åpenDialog)
            transactionProvider.dialogRepository.lagre(lukketDialog)

            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)

            val response = client.get("/api/dialogmelding-oppgaver") { bearerAuth(token) }

            assertEquals(HttpStatusCode.OK, response.status)
            val oppgaver = response.body<List<ApiDialogmeldingOppgave>>()
            assertEquals(1, oppgaver.size)
            assertEquals(åpenDialog.conversationRef.value, oppgaver.first().conversationRef)
        }

    @Test
    fun `status ForespørselSendt mappes til SENDT`() =
        testApplication {
            transactionProvider.dialogRepository.lagre(lagDialog())

            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)
            val oppgaver = client.get("/api/dialogmelding-oppgaver") { bearerAuth(token) }.body<List<ApiDialogmeldingOppgave>>()

            assertEquals(ApiDialogmeldingStatus.SENDT, oppgaver.first().status)
        }

    @Test
    fun `status PurringSendt mappes til PURRING_SENDT`() =
        testApplication {
            transactionProvider.dialogRepository.lagre(lagDialog(status = Dialogstatus.PurringSendt))

            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)
            val oppgaver = client.get("/api/dialogmelding-oppgaver") { bearerAuth(token) }.body<List<ApiDialogmeldingOppgave>>()

            assertEquals(ApiDialogmeldingStatus.PURRING_SENDT, oppgaver.first().status)
        }

    @Test
    fun `status SvarMottatt mappes til MOTTATT`() =
        testApplication {
            transactionProvider.dialogRepository.lagre(lagDialog(status = Dialogstatus.SvarMottatt))

            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)
            val oppgaver = client.get("/api/dialogmelding-oppgaver") { bearerAuth(token) }.body<List<ApiDialogmeldingOppgave>>()

            assertEquals(ApiDialogmeldingStatus.MOTTATT, oppgaver.first().status)
        }

    @Test
    fun `oppgave inneholder conversationRef og fristTidspunkt er etter sisteAktivitetTidspunkt`() =
        testApplication {
            val dialog = lagDialog()
            transactionProvider.dialogRepository.lagre(dialog)

            setupDefaultTestApp()

            val client = jsonClient()
            val token = mockOAuth2Server.utstedToken(saksbehandler)
            val oppgaver = client.get("/api/dialogmelding-oppgaver") { bearerAuth(token) }.body<List<ApiDialogmeldingOppgave>>()

            val oppgave = oppgaver.first()
            assertEquals(dialog.conversationRef.value, oppgave.conversationRef)
            assertTrue(oppgave.fristTidspunkt > oppgave.sisteAktivitetTidspunkt)
        }

    @Test
    fun `returnerer 401 uten token`() =
        testApplication {
            setupDefaultTestApp()

            val response = client.get("/api/dialogmelding-oppgaver")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
}
