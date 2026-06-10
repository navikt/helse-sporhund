package no.nav.helse.sporhund.infrastructure.api

import com.github.navikt.tbd_libs.populasjonstilgang.api.PopulasjonstilgangskontrollProvider
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import no.nav.helse.sporhund.application.PersonPseudoIdProvider
import no.nav.helse.sporhund.application.logg.loggError
import no.nav.helse.sporhund.application.logg.loggInfo
import no.nav.helse.sporhund.domain.Identitetsnummer

suspend fun RoutingContext.medPerson(
    personPseudoIdProvider: PersonPseudoIdProvider,
    populasjonstilgangskontrollProvider: PopulasjonstilgangskontrollProvider,
    block: suspend (identitetsnummer: Identitetsnummer) -> Unit,
) {
    val pseudoId = call.personPseudoId()
    val identitetsnummer =
        personPseudoIdProvider.hentIdentitetsnummer(pseudoId) ?: return call.respond(HttpStatusCode.NotFound)
    val saksbehandler = call.saksbehandler()
    loggInfo("Saksbehandler med Nav-ident=${saksbehandler.ident.value} gjør oppslag på person", "identitetsnummer" to identitetsnummer.value)
    loggInfo("Kall: ${call.request.httpMethod.value} ${call.request.uri}")
    val result = populasjonstilgangskontrollProvider.kontrollerTilgang(call.accessToken(), identitetsnummer.value)

    when (result) {
        TilgangskontrollResultat.IdentIkkeFunnet -> {
            loggInfo("Personen ble ikke funnet", "identitetsnummer" to identitetsnummer.value)
            return call.respond(HttpStatusCode.NotFound)
        }
        is TilgangskontrollResultat.ManglerTilgang -> {
            loggInfo("Saksbehandler har ikke tilgang til personen", "identitetsnummer" to identitetsnummer.value, "tilgangSomMangler" to result.tilgangSomMangler.name)
            return call.respond(HttpStatusCode.Forbidden)
        }
        TilgangskontrollResultat.Ok -> {
            loggInfo("Saksbehandler har tilgang til personen", "identitetsnummer" to identitetsnummer.value)
        }
        is TilgangskontrollResultat.UventetFeil -> {
            loggError("En uventet feil oppsto", "identitetsnummer" to identitetsnummer.value, "menneskeligLesbarForklaring" to result.menneskeligLesbarForklaring)
            return call.respond(HttpStatusCode.InternalServerError)
        }
    }
    block(identitetsnummer)
}
