package no.nav.helse.sporhund.api

import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import no.nav.helse.sporhund.api.endepunkter.getDialogmeldingOppgaverRoute
import no.nav.helse.sporhund.api.endepunkter.getDialogmeldingRoute
import no.nav.helse.sporhund.api.endepunkter.getDialogmeldingerRoute
import no.nav.helse.sporhund.api.endepunkter.postNyDialogmeldingRoute
import no.nav.helse.sporhund.api.endepunkter.postSvarPåDialogRoute
import no.nav.helse.sporhund.application.PersonPseudoIdProvider
import no.nav.helse.sporhund.application.TransactionProvider

fun Routing.appRoutes(
    personPseudoIdProvider: PersonPseudoIdProvider,
    transactionProvider: TransactionProvider,
) {
    route("/api") {
        route("/openapi.json") {
            openApi()
        }
        route("/swagger") {
            swaggerUI("../openapi.json")
        }

        authenticate("oidc") {
            getDialogmeldingOppgaverRoute()
            getDialogmeldingerRoute(personPseudoIdProvider, transactionProvider)
            getDialogmeldingRoute(personPseudoIdProvider, transactionProvider)
            postNyDialogmeldingRoute(personPseudoIdProvider, transactionProvider)
            postSvarPåDialogRoute(personPseudoIdProvider, transactionProvider)
        }
    }
}
