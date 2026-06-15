package no.nav.helse.sporhund.infrastructure.api.endepunkter

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import no.nav.helse.sporhund.application.PersonPseudoIdProvider
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.domain.tilgangskontroll.Tilgang
import no.nav.helse.sporhund.infrastructure.api.ApiDialogmeldingOppgave
import no.nav.helse.sporhund.infrastructure.api.krevTilgang
import no.nav.helse.sporhund.infrastructure.api.mapping.tilApiDialogmeldingOppgave

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
        krevTilgang(Tilgang.Les) {
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
}
