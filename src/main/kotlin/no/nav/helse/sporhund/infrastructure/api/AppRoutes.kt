package no.nav.helse.sporhund.infrastructure.api

import com.github.navikt.tbd_libs.populasjonstilgang.api.PopulasjonstilgangskontrollProvider
import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import no.nav.helse.sporhund.application.PersonPseudoIdProvider
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.infrastructure.api.endepunkter.*

fun Routing.appRoutes(
    personPseudoIdProvider: PersonPseudoIdProvider,
    transactionProvider: TransactionProvider,
    populasjonstilgangskontrollProvider: PopulasjonstilgangskontrollProvider,
) {
    route("/api") {
        route("/openapi.json") {
            openApi()
        }
        route("/swagger") {
            swaggerUI("../openapi.json")
        }

        authenticate("oidc") {
            getDialogmeldingOppgaverRoute(personPseudoIdProvider, transactionProvider)
            getDialogmeldingerRoute(personPseudoIdProvider, populasjonstilgangskontrollProvider, transactionProvider)
            getDialogmeldingRoute(personPseudoIdProvider, transactionProvider, populasjonstilgangskontrollProvider)
            postNyDialogmeldingRoute(personPseudoIdProvider, populasjonstilgangskontrollProvider, transactionProvider)
            postSvarPåDialogRoute(personPseudoIdProvider, populasjonstilgangskontrollProvider, transactionProvider)
            patchDialogstatusRoute(personPseudoIdProvider, populasjonstilgangskontrollProvider, transactionProvider)
        }
    }
}
