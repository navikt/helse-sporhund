package no.nav.helse.sporhund.api

import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import no.nav.helse.sporhund.api.endepunkter.getDialogmeldingOppgaverRoute
import no.nav.helse.sporhund.api.endepunkter.getDialogmeldingRoute
import no.nav.helse.sporhund.api.endepunkter.getDialogmeldingerRoute
import no.nav.helse.sporhund.api.endepunkter.postDialogmeldingRoute
import no.nav.helse.sporhund.api.endepunkter.postSvarPaDialogRoute
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.clients.personpseudoid.ValkeyPersonPseudoIdProvider

fun Routing.appRoutes(
    personPseudoIdProvider: ValkeyPersonPseudoIdProvider,
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
            postDialogmeldingRoute(personPseudoIdProvider, transactionProvider)
            postSvarPaDialogRoute(personPseudoIdProvider, transactionProvider)
        }
    }
}
