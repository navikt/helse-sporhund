package no.nav.helse.sporhund.api.endepunkter

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.sporhund.api.ApiDialogOppsummering
import no.nav.helse.sporhund.api.mapping.tilApiDialogmeldingerOversikt
import no.nav.helse.sporhund.api.personPseudoId
import no.nav.helse.sporhund.application.PersonPseudoIdProvider
import no.nav.helse.sporhund.application.TransactionProvider

fun Route.getDialogmeldingerRoute(
    personPseudoIdProvider: PersonPseudoIdProvider,
    transactionProvider: TransactionProvider,
) {
    get("/personer/{pseudoId}/dialogmeldinger", {
        operationId = "getDialogmeldinger"
        description = "Hent oversikt over alle dialoger"
        request {
            pathParameter<String>("pseudoId") {
                description = "Pseudonymisert person-ID"
                required = true
            }
        }
        response {
            HttpStatusCode.OK to {
                description = "Liste over dialoger"
                body<List<ApiDialogOppsummering>>()
            }
        }
    }) {
        val pseudoId = call.personPseudoId()
        val identitetsnummer = personPseudoIdProvider.hentIdentitetsnummer(pseudoId)
        if (identitetsnummer == null) {
            call.respond(emptyList<ApiDialogOppsummering>())
            return@get
        }
        val dialoger =
            transactionProvider.transaction {
                dialogRepository.finnDialoger(identitetsnummer)
            }
        call.respond(dialoger.map { it.tilApiDialogmeldingerOversikt() })
    }
}
