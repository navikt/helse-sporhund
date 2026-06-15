package no.nav.helse.sporhund.infrastructure.api.endepunkter

import com.github.navikt.tbd_libs.populasjonstilgang.api.PopulasjonstilgangskontrollProvider
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import no.nav.helse.sporhund.application.PersonPseudoIdProvider
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.sporhund.domain.tilgangskontroll.Tilgang
import no.nav.helse.sporhund.infrastructure.api.ApiDialogOppsummering
import no.nav.helse.sporhund.infrastructure.api.krevTilgangOgRolle
import no.nav.helse.sporhund.infrastructure.api.mapping.tilApiDialogmeldingerOversikt
import no.nav.helse.sporhund.infrastructure.api.medPerson

fun Route.getDialogmeldingerRoute(
    personPseudoIdProvider: PersonPseudoIdProvider,
    populasjonstilgangskontrollProvider: PopulasjonstilgangskontrollProvider,
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
        krevTilgangOgRolle(påkrevdTilgang = Tilgang.Les, påkrevdRolle = Brukerrolle.Dialogmelding) {
            medPerson(personPseudoIdProvider, populasjonstilgangskontrollProvider) { identitetsnummer, _ ->
                val dialoger =
                    transactionProvider.transaction {
                        dialogRepository.finnDialoger(identitetsnummer)
                    }
                call.respond(dialoger.map { dialog -> dialog.tilApiDialogmeldingerOversikt() })
            }
        }
    }
}
