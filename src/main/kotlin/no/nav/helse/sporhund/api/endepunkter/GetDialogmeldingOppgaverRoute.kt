package no.nav.helse.sporhund.api.endepunkter

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.sporhund.api.ApiDialogmeldingOppgave
import no.nav.helse.sporhund.api.MockStore

fun Route.getDialogmeldingOppgaverRoute() {
    get("/dialogmelding-oppgaver", {
        operationId = "getDialogmeldingOppgaver"
        description = "Hent dialogmelding-oppgaver"
        response {
            HttpStatusCode.OK to {
                description = "Liste over dialogmelding-oppgaver"
                body<List<ApiDialogmeldingOppgave>>()
            }
        }
    }) {
        call.respond(MockStore.hentOppgaver())
    }
}
