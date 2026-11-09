package no.nav.helse.sporhund.infrastructure.api.endepunkter

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.sporhund.domain.Dialogstatus
import no.nav.helse.sporhund.domain.testhelpers.lagDialog
import no.nav.helse.sporhund.infrastructure.api.ApiDialogmeldingOppgave
import no.nav.helse.sporhund.infrastructure.api.ApiDialogmeldingStatus
import no.nav.helse.sporhund.infrastructure.api.testhelpers.jsonClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetDialogmeldingOppgaverTest : EndepunktTest() {
    @Test
    fun `returnerer 200 med tom liste når det ikke finnes noen åpne dialoger`() =
        testApp {
            val client = jsonClient()

            val response = client.get("/api/dialogmelding-oppgaver") { bearerAuth(lesetoken()) }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.body<List<ApiDialogmeldingOppgave>>().isEmpty())
        }

    @Test
    fun `returnerer åpen dialog som oppgave`() =
        testApp {
            val dialog = lagDialog()
            lagreDialog(dialog)

            val client = jsonClient()

            val response = client.get("/api/dialogmelding-oppgaver") { bearerAuth(lesetoken()) }

            assertEquals(HttpStatusCode.OK, response.status)
            val oppgaver = response.body<List<ApiDialogmeldingOppgave>>()
            assertEquals(1, oppgaver.size)
            assertEquals(dialog.conversationRef.value, oppgaver.first().conversationRef)
        }

    @Test
    fun `inkluderer ikke lukket dialog`() =
        testApp {
            val lukketDialog = lagDialog(status = Dialogstatus.DialogLukket)
            lagreDialog(lukketDialog)

            val client = jsonClient()

            val response = client.get("/api/dialogmelding-oppgaver") { bearerAuth(lesetoken()) }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.body<List<ApiDialogmeldingOppgave>>().isEmpty())
        }

    @Test
    fun `returnerer kun ikke-lukkede dialoger blant flere`() =
        testApp {
            val åpenDialog = lagDialog()
            val lukketDialog = lagDialog(status = Dialogstatus.DialogLukket)
            lagreDialog(åpenDialog)
            lagreDialog(lukketDialog)

            val client = jsonClient()

            val response = client.get("/api/dialogmelding-oppgaver") { bearerAuth(lesetoken()) }

            assertEquals(HttpStatusCode.OK, response.status)
            val oppgaver = response.body<List<ApiDialogmeldingOppgave>>()
            assertEquals(1, oppgaver.size)
            assertEquals(åpenDialog.conversationRef.value, oppgaver.first().conversationRef)
        }

    @Test
    fun `status ForespørselSendt mappes til SENDT`() =
        testApp {
            lagreDialog()

            val client = jsonClient()
            val oppgaver =
                client.get("/api/dialogmelding-oppgaver") { bearerAuth(lesetoken()) }.body<List<ApiDialogmeldingOppgave>>()

            assertEquals(ApiDialogmeldingStatus.SENDT, oppgaver.first().status)
        }

    @Test
    fun `status PurringSendt mappes til PURRING_SENDT`() =
        testApp {
            lagreDialog(lagDialog(status = Dialogstatus.PurringSendt))

            val client = jsonClient()
            val oppgaver =
                client.get("/api/dialogmelding-oppgaver") { bearerAuth(lesetoken()) }.body<List<ApiDialogmeldingOppgave>>()

            assertEquals(ApiDialogmeldingStatus.PURRING_SENDT, oppgaver.first().status)
        }

    @Test
    fun `status SvarMottatt mappes til MOTTATT`() =
        testApp {
            lagreDialog(lagDialog(status = Dialogstatus.SvarMottatt))

            val client = jsonClient()
            val oppgaver =
                client.get("/api/dialogmelding-oppgaver") { bearerAuth(lesetoken()) }.body<List<ApiDialogmeldingOppgave>>()

            assertEquals(ApiDialogmeldingStatus.MOTTATT, oppgaver.first().status)
        }

    @Test
    fun `oppgave inneholder conversationRef og fristTidspunkt er etter sisteAktivitetTidspunkt`() =
        testApp {
            val dialog = lagDialog()
            lagreDialog(dialog)

            val client = jsonClient()
            val oppgaver =
                client.get("/api/dialogmelding-oppgaver") { bearerAuth(lesetoken()) }.body<List<ApiDialogmeldingOppgave>>()

            val oppgave = oppgaver.first()
            assertEquals(dialog.conversationRef.value, oppgave.conversationRef)
            assertTrue(oppgave.fristTidspunkt > oppgave.sisteAktivitetTidspunkt)
        }

    @Test
    fun `returnerer 401 uten token`() =
        testApp {
            val response = client.get("/api/dialogmelding-oppgaver")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
}
