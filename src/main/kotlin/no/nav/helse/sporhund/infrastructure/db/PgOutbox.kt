package no.nav.helse.sporhund.infrastructure.db

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Session
import no.nav.helse.sporhund.application.KnyttInnkommendeJournalpost
import no.nav.helse.sporhund.application.NyDialogmeldingFraNav
import no.nav.helse.sporhund.application.OpprettUtgåendeJournalpost
import no.nav.helse.sporhund.application.Outbox
import no.nav.helse.sporhund.application.OutboxMelding
import no.nav.helse.sporhund.application.OutboxMeldingId
import no.nav.helse.sporhund.domain.*
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass

class PgOutbox(
    private val session: Session,
) : Outbox {
    override fun nyMelding(melding: OutboxMelding) {
        val dto =
            when (melding) {
                is NyDialogmeldingFraNav ->
                    NyDialogmeldingFraNavDto(
                        outboxMeldingId = melding.id.value,
                        conversationRef = melding.nyDialogmeldingFraNavEvent.conversationRef.value,
                        behandlerRef = melding.nyDialogmeldingFraNavEvent.behandlerRef.value,
                        identitetsnummer = melding.nyDialogmeldingFraNavEvent.identitetsnummer.value,
                        meldingId = melding.nyDialogmeldingFraNavEvent.meldingId.value,
                        tekst = melding.nyDialogmeldingFraNavEvent.tekst,
                        erPurring = melding.nyDialogmeldingFraNavEvent.erPurring,
                    )
                is OpprettUtgåendeJournalpost ->
                    OpprettUtgåendeJournalpostDto(
                        outboxMeldingId = melding.id.value,
                        conversationRef = melding.conversationRef.value,
                        meldingId = melding.meldingId.value,
                        tekst = melding.tekst,
                        avsenderIdent = melding.avsender.ident.value,
                        avsenderNavn = melding.avsender.navn,
                        mottaker = BehandlerDto.fra(melding.mottaker),
                        gjelder = melding.gjelder.value,
                        søkerFornavn = melding.søkernavn.fornavn,
                        søkerMellomnavn = melding.søkernavn.mellomnavn,
                        søkerEtternavn = melding.søkernavn.etternavn,
                        tidspunkt = melding.tidspunkt,
                        fagområde =
                            when (melding.fagområde) {
                                Fagområde.EnkeltståendeBehandlingsdager -> "EnkeltståendeBehandlingsdager"
                                Fagområde.Tilbakedatering -> "Tilbakedatering"
                                Fagområde.Yrkesskade -> "Yrkesskade"
                                Fagområde.Bestridelse -> "Bestridelse"
                            },
                        dialogtype =
                            when (melding.dialogtype) {
                                Dialogtype.Journalnotat -> "Journalnotat"
                                Dialogtype.MedisinskeOpplysninger -> "MedisinskeOpplysninger"
                                Dialogtype.EkstraUttalelserFraLege -> "EkstraUttalelserFraLege"
                                Dialogtype.SpesialistErklæring -> "SpesialistErklæring"
                                Dialogtype.UtvidetSpesialistErklæring -> "UtvidetSpesialistErklæring"
                            },
                    )
                is KnyttInnkommendeJournalpost ->
                    KnyttInnkommendeJournalpostDto(
                        outboxMeldingId = melding.id.value,
                        journalpostId = melding.journalpostId,
                        conversationRef = melding.conversationRef.value,
                        identitetsnummer = melding.identitetsnummer.value,
                    )
            }
        asSQL(
            """
            INSERT INTO outbox(id, event, opprettet, sendt_tidspunkt)
            VALUES (:id, :event::jsonb, now(), null)
            """.trimIndent(),
            "id" to melding.id.value,
            "event" to objectMapper.writeValueAsString(dto),
        ).update(session)
    }

    override fun <T : OutboxMelding> meldinger(type: KClass<T>): List<T> =
        asSQL(
            """SELECT id, event FROM outbox WHERE sendt_tidspunkt IS NULL FOR UPDATE SKIP LOCKED""",
        ).list(session) { row ->
            when (val dto = objectMapper.readValue<OutboxMeldingDto>(row.string("event"))) {
                is NyDialogmeldingFraNavDto ->
                    NyDialogmeldingFraNav(
                        id = OutboxMeldingId(dto.outboxMeldingId),
                        nyDialogmeldingFraNavEvent =
                            NyDialogmeldingFraNavEvent(
                                conversationRef = ConversationRef(dto.conversationRef),
                                behandlerRef = BehandlerRef(dto.behandlerRef),
                                identitetsnummer = Identitetsnummer.fraString(dto.identitetsnummer),
                                meldingId = DialogmeldingId(dto.meldingId),
                                tekst = dto.tekst,
                                erPurring = dto.erPurring,
                            ),
                    )
                is OpprettUtgåendeJournalpostDto ->
                    OpprettUtgåendeJournalpost(
                        id = OutboxMeldingId(dto.outboxMeldingId),
                        conversationRef = ConversationRef(dto.conversationRef),
                        meldingId = DialogmeldingId(dto.meldingId),
                        tekst = dto.tekst,
                        avsender =
                            Saksbehandler(
                                id = SaksbehandlerOid(UUID.fromString("00000000-0000-0000-0000-000000000000")),
                                navn = dto.avsenderNavn,
                                epost = "",
                                ident = NavIdent(dto.avsenderIdent),
                            ),
                        mottaker = dto.mottaker.tilDomene(),
                        gjelder = Identitetsnummer.fraString(dto.gjelder),
                        søkernavn =
                            Navn(
                                fornavn = dto.søkerFornavn,
                                mellomnavn = dto.søkerMellomnavn,
                                etternavn = dto.søkerEtternavn,
                            ),
                        tidspunkt = dto.tidspunkt,
                        fagområde =
                            when (dto.fagområde) {
                                "EnkeltståendeBehandlingsdager" -> Fagområde.EnkeltståendeBehandlingsdager
                                "Tilbakedatering" -> Fagområde.Tilbakedatering
                                "Yrkesskade" -> Fagområde.Yrkesskade
                                "Bestridelse" -> Fagområde.Bestridelse
                                else -> error("Ukjent fagområde: ${dto.fagområde}")
                            },
                        dialogtype =
                            when (dto.dialogtype) {
                                "Journalnotat" -> Dialogtype.Journalnotat
                                "MedisinskeOpplysninger" -> Dialogtype.MedisinskeOpplysninger
                                "EkstraUttalelserFraLege" -> Dialogtype.EkstraUttalelserFraLege
                                "SpesialistErklæring" -> Dialogtype.SpesialistErklæring
                                "UtvidetSpesialistErklæring" -> Dialogtype.UtvidetSpesialistErklæring
                                else -> error("Ukjent dialogtype: ${dto.dialogtype}")
                            },
                    )
                is KnyttInnkommendeJournalpostDto ->
                    KnyttInnkommendeJournalpost(
                        id = OutboxMeldingId(dto.outboxMeldingId),
                        journalpostId = dto.journalpostId,
                        conversationRef = ConversationRef(dto.conversationRef),
                        identitetsnummer = Identitetsnummer.fraString(dto.identitetsnummer),
                    )
            }
        }.filterIsInstance(type.java)

    override fun meldingSendt(id: OutboxMeldingId) {
        asSQL(
            """UPDATE outbox SET sendt_tidspunkt = :tidspunkt WHERE id = :id""",
            "tidspunkt" to Instant.now(),
            "id" to id.value,
        ).update(session)
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes(
        JsonSubTypes.Type(value = NyDialogmeldingFraNavDto::class, name = "NyDialogmeldingFraNav"),
        JsonSubTypes.Type(value = OpprettUtgåendeJournalpostDto::class, name = "OpprettUtgåendeJournalpost"),
        JsonSubTypes.Type(value = KnyttInnkommendeJournalpostDto::class, name = "KnyttInnkommendeJournalpost"),
    )
    private sealed interface OutboxMeldingDto {
        val outboxMeldingId: UUID
    }

    private data class NyDialogmeldingFraNavDto(
        override val outboxMeldingId: UUID,
        val conversationRef: UUID,
        val behandlerRef: String,
        val identitetsnummer: String,
        val meldingId: UUID,
        val tekst: String?,
        val erPurring: Boolean = false,
    ) : OutboxMeldingDto

    private data class OpprettUtgåendeJournalpostDto(
        override val outboxMeldingId: UUID,
        val conversationRef: UUID,
        val meldingId: UUID,
        val tekst: String,
        val avsenderIdent: String,
        val avsenderNavn: String,
        val mottaker: BehandlerDto,
        val gjelder: String,
        val søkerFornavn: String,
        val søkerMellomnavn: String?,
        val søkerEtternavn: String,
        val tidspunkt: Instant,
        val fagområde: String,
        val dialogtype: String,
    ) : OutboxMeldingDto

    private data class KnyttInnkommendeJournalpostDto(
        override val outboxMeldingId: UUID,
        val journalpostId: String,
        val conversationRef: UUID,
        val identitetsnummer: String,
    ) : OutboxMeldingDto

    private data class BehandlerDto(
        val hprNummer: String,
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String,
        val kontorNavn: String?,
        val organisasjonsnummer: String?,
        val adresseGate: String?,
        val adressePostnummer: String?,
        val adressePoststed: String?,
    ) {
        fun tilDomene() =
            Behandler(
                hprNummer = HprNummer(hprNummer),
                navn = Navn(fornavn = fornavn, mellomnavn = mellomnavn, etternavn = etternavn),
                kontor =
                    Kontor(
                        navn = kontorNavn,
                        organisasjonsnummer = organisasjonsnummer?.let { Organisasjonsnummer(it) },
                        adresse =
                            if (adresseGate != null || adressePostnummer != null || adressePoststed != null) {
                                Adresse(veiadresse = adresseGate, postnummer = adressePostnummer, poststed = adressePoststed)
                            } else {
                                null
                            },
                    ),
                telefonnummer = null,
            )

        companion object {
            fun fra(behandler: Behandler) =
                BehandlerDto(
                    hprNummer = behandler.hprNummer.value,
                    fornavn = behandler.navn.fornavn,
                    mellomnavn = behandler.navn.mellomnavn,
                    etternavn = behandler.navn.etternavn,
                    kontorNavn = behandler.kontor.navn,
                    organisasjonsnummer = behandler.kontor.organisasjonsnummer?.value,
                    adresseGate = behandler.kontor.adresse?.veiadresse,
                    adressePostnummer = behandler.kontor.adresse?.postnummer,
                    adressePoststed = behandler.kontor.adresse?.poststed,
                )
        }
    }
}
