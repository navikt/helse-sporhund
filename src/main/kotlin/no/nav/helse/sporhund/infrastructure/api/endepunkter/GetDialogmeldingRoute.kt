package no.nav.helse.sporhund.infrastructure.api.endepunkter

import com.github.navikt.tbd_libs.populasjonstilgang.api.PopulasjonstilgangskontrollProvider
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.sporhund.application.PersonPseudoIdProvider
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.application.logg.Auditlogger.auditlogge
import no.nav.helse.sporhund.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.sporhund.domain.tilgangskontroll.Tilgang
import no.nav.helse.sporhund.infrastructure.api.ApiDialogDetails
import no.nav.helse.sporhund.infrastructure.api.conversationRef
import no.nav.helse.sporhund.infrastructure.api.krevTilgangOgRolle
import no.nav.helse.sporhund.infrastructure.api.mapping.tilApiDialogDetails
import no.nav.helse.sporhund.infrastructure.api.medPerson

fun Route.getDialogmeldingRoute(
    personPseudoIdProvider: PersonPseudoIdProvider,
    transactionProvider: TransactionProvider,
    populasjonstilgangskontrollProvider: PopulasjonstilgangskontrollProvider,
) {
    get("/personer/{pseudoId}/dialogmeldinger/{conversationRef}", {
        operationId = "getDialogmelding"
        description = "Hent en enkelt dialog med alle meldinger"
        request {
            pathParameter<String>("pseudoId") {
                description = "Pseudonymisert person-ID"
                required = true
            }
            pathParameter<String>("conversationRef") {
                description = "ID til dialogen"
                required = true
            }
        }
        response {
            HttpStatusCode.OK to {
                description = "Full dialog med alle meldinger"
                body<ApiDialogDetails>()
            }
            HttpStatusCode.NotFound to {
                description = "Dialog ikke funnet"
            }
        }
    }) {
        krevTilgangOgRolle(påkrevdTilgang = Tilgang.Les, påkrevdRolle = Brukerrolle.Dialogmelding) {
            medPerson(personPseudoIdProvider, populasjonstilgangskontrollProvider) { identitetsnummer, saksbehandler ->
                auditlogge(
                    saksbehandler,
                    identitetsnummer,
                    "Saksbehandler gjør oppslag på person for å hente ut dialogmeldinger",
                )
                val conversationRef = call.conversationRef()
                val dialog =
                    transactionProvider.transaction {
                        dialogRepository.finnDialog(conversationRef)
                    }
                if (dialog != null) {
                    call.respond(dialog.tilApiDialogDetails())
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}
