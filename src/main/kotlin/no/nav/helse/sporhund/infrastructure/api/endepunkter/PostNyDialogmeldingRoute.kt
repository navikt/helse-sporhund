package no.nav.helse.sporhund.infrastructure.api.endepunkter

import com.github.navikt.tbd_libs.populasjonstilgang.api.PopulasjonstilgangskontrollProvider
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.sporhund.application.OutboxMelding
import no.nav.helse.sporhund.application.PersonPseudoIdProvider
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.domain.BehandlerRef
import no.nav.helse.sporhund.domain.Dialog
import no.nav.helse.sporhund.domain.Dialogmelding
import no.nav.helse.sporhund.domain.Fagområde
import no.nav.helse.sporhund.domain.Identitetsnummer
import no.nav.helse.sporhund.domain.Navn
import no.nav.helse.sporhund.domain.Saksbehandler
import no.nav.helse.sporhund.domain.tilgangskontroll.Brukerrolle
import no.nav.helse.sporhund.domain.tilgangskontroll.Tilgang
import no.nav.helse.sporhund.infrastructure.api.ApiDialogDetails
import no.nav.helse.sporhund.infrastructure.api.ApiFagomrade
import no.nav.helse.sporhund.infrastructure.api.ApiNyDialogmelding
import no.nav.helse.sporhund.infrastructure.api.krevTilgangOgRolle
import no.nav.helse.sporhund.infrastructure.api.mapping.tilApiDialogDetails
import no.nav.helse.sporhund.infrastructure.api.mapping.tilBehandler
import no.nav.helse.sporhund.infrastructure.api.medPerson

fun Route.postNyDialogmeldingRoute(
    personPseudoIdProvider: PersonPseudoIdProvider,
    populasjonstilgangskontrollProvider: PopulasjonstilgangskontrollProvider,
    transactionProvider: TransactionProvider,
) {
    post("/personer/{pseudoId}/dialogmelding", {
        operationId = "postNyDialogmelding"
        description = "Send ny dialogmelding"
        request {
            pathParameter<String>("pseudoId") {
                description = "Pseudonymisert person-ID"
                required = true
            }
            body<ApiNyDialogmelding>()
        }
        response {
            HttpStatusCode.Created to {
                description = "Dialogmelding opprettet"
                body<ApiDialogDetails>()
            }
        }
    }) {
        krevTilgangOgRolle(påkrevdTilgang = Tilgang.Skriv, påkrevdRolle = Brukerrolle.Dialogmelding) {
            medPerson(personPseudoIdProvider, populasjonstilgangskontrollProvider) { identitetsnummer, saksbehandler ->
                val apiDialogmelding = call.receive<ApiNyDialogmelding>()
                val dialog =
                    transactionProvider.transaction {
                        val dialog = apiDialogmelding.tilDialog(identitetsnummer, saksbehandler)
                        dialogRepository.lagre(dialog)
                        val events = dialog.events()
                        val melding = dialog.nyesteMeldingFraNav()
                        events.forEach {
                            outbox.nyMelding(OutboxMelding.nyDialogmeldingFraNav(it))
                        }
                        outbox.nyMelding(OutboxMelding.opprettUtgåendeJournalpost(melding, dialog, saksbehandler))
                        return@transaction dialog
                    }

                call.respond(HttpStatusCode.Created, dialog.tilApiDialogDetails())
            }
        }
    }
}

private fun ApiNyDialogmelding.tilDialog(
    identitetsnummer: Identitetsnummer,
    saksbehandler: Saksbehandler,
): Dialog =
    Dialog.ny(
        identitetsnummer = identitetsnummer,
        søkernavn =
            Navn(
                fornavn = sokernavn.fornavn,
                mellomnavn = sokernavn.mellomnavn,
                etternavn = sokernavn.etternavn,
            ),
        melding =
            Dialogmelding.FraNav.ny(
                saksbehandler = saksbehandler.ident,
                behandler = tilBehandler(),
                behandlerRef = BehandlerRef(behandler.id),
                melding = melding,
            ),
        fagområde =
            when (fagomrade) {
                ApiFagomrade.ENKELTSTAENDE_BEHANDLINGSDAGER -> Fagområde.EnkeltståendeBehandlingsdager
                ApiFagomrade.TILBAKEDATERING -> Fagområde.Tilbakedatering
                ApiFagomrade.YRKESSKADE -> Fagområde.Yrkesskade
                ApiFagomrade.BESTRIDELSE -> Fagområde.Bestridelse
            },
    )
