package no.nav.helse.sporhund.infrastructure.api.endepunkter

import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import no.nav.helse.sporhund.application.PersonPseudoIdProvider
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.infrastructure.api.ApiDialogOppsummering
import no.nav.helse.sporhund.infrastructure.api.mapping.tilApiDialogmeldingerOversikt
import no.nav.helse.sporhund.infrastructure.api.personPseudoId

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
