package no.nav.helse.sporhund.infrastructure.api

import com.github.navikt.tbd_libs.populasjonstilgang.api.PopulasjonstilgangskontrollProvider
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import no.nav.helse.sporhund.application.PersonPseudoIdProvider
import no.nav.helse.sporhund.domain.Identitetsnummer

suspend fun RoutingContext.medPerson(
    personPseudoIdProvider: PersonPseudoIdProvider,
    populasjonstilgangskontrollProvider: PopulasjonstilgangskontrollProvider,
    block: suspend (identitetsnummer: Identitetsnummer) -> Unit
) {
    val pseudoId = call.personPseudoId()
    val identitetsnummer =
        personPseudoIdProvider.hentIdentitetsnummer(pseudoId) ?: return call.respond(HttpStatusCode.NotFound)
    val result = populasjonstilgangskontrollProvider.kontrollerTilgang(call.accessToken(), identitetsnummer.value)

    when (result) {
        TilgangskontrollResultat.IdentIkkeFunnet -> return call.respond(HttpStatusCode.NotFound)
        is TilgangskontrollResultat.ManglerTilgang -> return call.respond(HttpStatusCode.Forbidden)
        TilgangskontrollResultat.Ok -> Unit
        is TilgangskontrollResultat.UventetFeil -> return call.respond(HttpStatusCode.InternalServerError)
    }
    block(identitetsnummer)
}
