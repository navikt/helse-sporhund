package no.nav.helse.sporhund.infrastructure.api.endepunkter

import com.github.navikt.tbd_libs.populasjonstilgang.api.PopulasjonstilgangskontrollProvider
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.helse.sporhund.application.PersonPseudoIdProvider
import no.nav.helse.sporhund.application.TransactionProvider
import no.nav.helse.sporhund.application.VedleggProvider
import no.nav.helse.sporhund.application.logg.Auditlogger.auditlogge
import no.nav.helse.sporhund.application.logg.teamLogs
import no.nav.helse.sporhund.domain.Dialogmelding
import no.nav.helse.sporhund.infrastructure.api.medPerson
import java.util.*

fun Route.getVedleggRoute(
    personPseudoIdProvider: PersonPseudoIdProvider,
    populasjonstilgangskontrollProvider: PopulasjonstilgangskontrollProvider,
    transactionProvider: TransactionProvider,
    vedleggProvider: VedleggProvider,
) {
    get("/personer/{pseudoId}/vedlegg/{msgId}/{index}", {
        operationId = "getVedlegg"
        description = "Hent ett vedlegg for en dialogmelding fra behandler"
        request {
            pathParameter<String>("pseudoId") {
                description = "Pseudonymisert person-ID"
                required = true
            }
            pathParameter<String>("msgId") {
                description = "ID til meldingen fra behandler"
                required = true
            }
            pathParameter<Int>("index") {
                description = "0-basert indeks for vedlegget"
                required = true
            }
        }
        response {
            HttpStatusCode.OK to {
                description = "Vedlegget som binærdata"
            }
            HttpStatusCode.NotFound to {
                description = "Melding eller vedlegg ikke funnet"
            }
        }
    }) {
        medPerson(personPseudoIdProvider, populasjonstilgangskontrollProvider) { identitetsnummer, saksbehandler ->
            auditlogge(
                saksbehandler,
                identitetsnummer,
                "Saksbehandler henter ut vedlegg i dialogmelding for en person",
            )
            val msgId = requireNotNull(call.parameters["msgId"])
            val index =
                call.parameters["index"]?.toIntOrNull()
                    ?: return@medPerson call.respond(HttpStatusCode.BadRequest)

            val msgUuid =
                runCatching { UUID.fromString(msgId) }.getOrElse {
                    teamLogs.error("msgId er ikke en UUID, msgId=$msgId")
                    return@medPerson call.respond(HttpStatusCode.NotFound)
                }

            val melding =
                transactionProvider.transaction {
                    dialogRepository
                        .finnDialoger(identitetsnummer)
                        .flatMap { it.meldinger }
                        .filterIsInstance<Dialogmelding.FraBehandler>()
                        .firstOrNull { it.id.value == msgId }
                }

            if (melding == null) {
                teamLogs.error("Melding med id $msgId ikke funnet for person med identitetsnummer $identitetsnummer")
                return@medPerson call.respond(HttpStatusCode.NotFound)
            }

            val bytes =
                vedleggProvider.hentVedlegg(msgUuid).getOrNull(index)
                    ?: return@medPerson call.respond(HttpStatusCode.NotFound)

            call.respondBytes(bytes, ContentType.Application.Pdf)
        }
    }
}
