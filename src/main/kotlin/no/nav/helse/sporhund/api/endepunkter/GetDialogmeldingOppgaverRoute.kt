package no.nav.helse.sporhund.api.endepunkter

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.sporhund.api.ApiDialogmeldingOppgave
import no.nav.helse.sporhund.api.mapping.tilApiDialogmeldingOppgave
import no.nav.helse.sporhund.application.PersonPseudoIdProvider
import no.nav.helse.sporhund.application.TransactionProvider

fun Route.getDialogmeldingOppgaverRoute(
    personPseudoIdProvider: PersonPseudoIdProvider,
    transactionProvider: TransactionProvider,
) {
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
        val dialoger =
            transactionProvider.transaction {
                dialogRepository.finnIkkeLukkedeDialoger()
            }
        call.respond(
            dialoger.map { dialog ->
                val personPseudoId = personPseudoIdProvider.nyPersonPseudoId(dialog.identitetsnummer)
                dialog.tilApiDialogmeldingOppgave(personPseudoId)
            },
        )
    }
}
