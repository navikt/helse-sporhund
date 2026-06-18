package no.nav.helse.sporhund.infrastructure.api

import com.github.navikt.tbd_libs.populasjonstilgang.api.PopulasjonstilgangskontrollProvider
import com.github.navikt.tbd_libs.populasjonstilgang.api.TilgangskontrollResultat
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.sporhund.application.PersonPseudoIdProvider
import no.nav.helse.sporhund.application.logg.Auditlogger.auditloggeManglendeTilgang
import no.nav.helse.sporhund.application.logg.loggError
import no.nav.helse.sporhund.application.logg.loggInfo
import no.nav.helse.sporhund.domain.Identitetsnummer
import no.nav.helse.sporhund.domain.Saksbehandler
import no.nav.helse.sporhund.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.sporhund.domain.tilgangskontroll.Tilgang

suspend fun RoutingContext.medPerson(
    personPseudoIdProvider: PersonPseudoIdProvider,
    populasjonstilgangskontrollProvider: PopulasjonstilgangskontrollProvider,
    block: suspend (identitetsnummer: Identitetsnummer, saksbehandler: Saksbehandler) -> Unit,
) {
    val pseudoId = call.personPseudoId()
    val identitetsnummer =
        personPseudoIdProvider.hentIdentitetsnummer(pseudoId) ?: return call.respond(HttpStatusCode.NotFound)
    val saksbehandler = call.saksbehandler()
    loggInfo(
        "Saksbehandler med Nav-ident=${saksbehandler.ident.value} gjør oppslag på person",
        "identitetsnummer" to identitetsnummer.value,
    )
    val result = populasjonstilgangskontrollProvider.kontrollerKjerneTilgang(call.accessToken(), identitetsnummer.value)

    when (result) {
        TilgangskontrollResultat.IdentIkkeFunnet -> {
            loggInfo("Personen ble ikke funnet", "identitetsnummer" to identitetsnummer.value)
            return call.respond(HttpStatusCode.NotFound)
        }

        is TilgangskontrollResultat.ManglerTilgang -> {
            auditloggeManglendeTilgang(saksbehandler, identitetsnummer)
            loggInfo(
                "Saksbehandler har ikke tilgang til personen",
                "identitetsnummer" to identitetsnummer.value,
                "tilgangSomMangler" to result.tilgangSomMangler.name,
            )
            return call.respond(HttpStatusCode.Forbidden)
        }

        TilgangskontrollResultat.Ok -> {
            loggInfo("Saksbehandler har tilgang til personen", "identitetsnummer" to identitetsnummer.value)
        }

        is TilgangskontrollResultat.UventetFeil -> {
            loggError(
                "En uventet feil oppsto",
                "identitetsnummer" to identitetsnummer.value,
                "menneskeligLesbarForklaring" to result.menneskeligLesbarForklaring,
            )
            return call.respond(HttpStatusCode.InternalServerError)
        }
    }
    block(identitetsnummer, saksbehandler)
}

suspend fun RoutingContext.krevTilgangOgRolle(
    påkrevdTilgang: Tilgang,
    påkrevdRolle: Brukerrolle,
    block: suspend () -> Unit,
) {
    if (påkrevdTilgang !in call.tilganger()) {
        loggInfo("Saksbehandler mangler tilgang", "tilgang" to påkrevdTilgang.name)
        return call.respond(HttpStatusCode.Forbidden)
    }
    if (påkrevdRolle !in call.brukerroller()) {
        loggInfo("Saksbehandler mangler rolle", "brukerrolle" to påkrevdRolle.name)
        return call.respond(HttpStatusCode.Forbidden)
    }
    block()
}
