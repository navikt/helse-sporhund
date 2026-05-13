package no.nav.helse.sporhund.db

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Session
import no.nav.helse.sporhund.application.DialogRepository
import no.nav.helse.sporhund.domain.Behandler
import no.nav.helse.sporhund.domain.BehandlerRef
import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.Dialog
import no.nav.helse.sporhund.domain.Dialogmelding
import no.nav.helse.sporhund.domain.DialogmeldingId
import no.nav.helse.sporhund.domain.HprNummer
import no.nav.helse.sporhund.domain.Identitetsnummer
import no.nav.helse.sporhund.domain.NavIdent
import no.nav.helse.sporhund.domain.Organisasjonsnummer
import no.nav.helse.sporhund.domain.Telefonnummer
import java.time.Instant
import java.util.*

class PgDialogRepository(
    private val session: Session,
) : DialogRepository {
    override fun lagre(dialog: Dialog) {
        asSQL(
            """
                INSERT INTO dialog(conversation_ref, identitetsnummer, json)
                VALUES(:conversation_ref, :identitetsnummer, :json::jsonb)
                ON CONFLICT (conversation_ref)
                DO UPDATE SET json = excluded.json
            """,
            "conversation_ref" to dialog.conversationRef.value,
            "identitetsnummer" to dialog.identitetsnummer.value,
            "json" to DialogDto.fraDialog(dialog).let { objectMapper.writeValueAsString(it) },
        ).update(session)
    }

    override fun finnDialog(conversationRef: ConversationRef): Dialog? =
        asSQL(
            """
                SELECT json FROM dialog WHERE conversation_ref = :conversation_ref
            """,
            "conversation_ref" to conversationRef.value,
        ).single(session) {
            objectMapper.readValue<DialogDto>(it.string("json")).tilDomene()
        }

    private data class DialogDto(
        val conversationRef: UUID,
        val identitetsnummer: IdentitetsnummerDto,
        val meldinger: List<DialogmeldingDto>,
    ) {
        fun tilDomene(): Dialog =
            Dialog.fraLagring(
                conversationRef = ConversationRef(conversationRef),
                identitetsnummer = identitetsnummer.tilDomene(),
                meldinger = meldinger.map { it.tilDomene() },
            )

        companion object {
            fun fraDialog(dialog: Dialog): DialogDto =
                DialogDto(
                    conversationRef = dialog.conversationRef.value,
                    identitetsnummer = IdentitetsnummerDto.fraIdentitetsnummer(dialog.identitetsnummer),
                    meldinger = dialog.meldinger.map { DialogmeldingDto.fraDialogmelding(it) },
                )
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes(
        JsonSubTypes.Type(value = IdentitetsnummerDto.Fødselsnummer::class, name = "FODSELSNUMMER"),
        JsonSubTypes.Type(value = IdentitetsnummerDto.DNummer::class, name = "DNUMMER"),
    )
    private sealed interface IdentitetsnummerDto {
        val value: String

        fun tilDomene(): Identitetsnummer

        data class Fødselsnummer(
            override val value: String,
        ) : IdentitetsnummerDto {
            override fun tilDomene() =
                no.nav.helse.sporhund.domain
                    .Fødselsnummer(value)
        }

        data class DNummer(
            override val value: String,
        ) : IdentitetsnummerDto {
            override fun tilDomene() =
                no.nav.helse.sporhund.domain
                    .DNummer(value)
        }

        companion object {
            fun fraIdentitetsnummer(identitetsnummer: Identitetsnummer): IdentitetsnummerDto =
                when (identitetsnummer) {
                    is no.nav.helse.sporhund.domain.Fødselsnummer -> Fødselsnummer(identitetsnummer.value)
                    is no.nav.helse.sporhund.domain.DNummer -> DNummer(identitetsnummer.value)
                }
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes(
        JsonSubTypes.Type(value = DialogmeldingDto.FraNav::class, name = "FRA_NAV"),
        JsonSubTypes.Type(value = DialogmeldingDto.FraBehandler::class, name = "FRA_BEHANDLER"),
    )
    private sealed interface DialogmeldingDto {
        fun tilDomene(): Dialogmelding

        data class FraNav(
            val id: UUID,
            val tidspunkt: Instant,
            val melding: String,
            val saksbehandler: String,
            val behandlerRef: String,
            val behandler: BehandlerDto,
        ) : DialogmeldingDto {
            override fun tilDomene(): Dialogmelding.FraNav =
                Dialogmelding.FraNav.fraLagring(
                    id = DialogmeldingId(id),
                    tidspunkt = tidspunkt,
                    melding = melding,
                    saksbehandler = NavIdent(saksbehandler),
                    behandler = behandler.tilDomene(),
                    behandlerRef = BehandlerRef(behandlerRef),
                )
        }

        data class FraBehandler(
            val id: UUID,
            val tidspunkt: Instant,
            val melding: String,
            val behandler: BehandlerDto,
            val antallVedlegg: Int,
        ) : DialogmeldingDto {
            override fun tilDomene(): Dialogmelding.FraBehandler =
                Dialogmelding.FraBehandler(
                    id = DialogmeldingId(id),
                    tidspunkt = tidspunkt,
                    melding = melding,
                    behandler = behandler.tilDomene(),
                    antallVedlegg = antallVedlegg,
                )
        }

        companion object {
            fun fraDialogmelding(dialogmelding: Dialogmelding): DialogmeldingDto =
                when (dialogmelding) {
                    is Dialogmelding.FraNav -> {
                        FraNav(
                            id = dialogmelding.id.value,
                            tidspunkt = dialogmelding.tidspunkt,
                            melding = dialogmelding.melding,
                            saksbehandler = dialogmelding.saksbehandler.value,
                            behandlerRef = dialogmelding.behandlerRef.value,
                            behandler = BehandlerDto.fraBehandler(dialogmelding.behandler),
                        )
                    }

                    is Dialogmelding.FraBehandler -> {
                        FraBehandler(
                            id = dialogmelding.id.value,
                            tidspunkt = dialogmelding.tidspunkt,
                            melding = dialogmelding.melding,
                            behandler = BehandlerDto.fraBehandler(dialogmelding.behandler),
                            antallVedlegg = dialogmelding.antallVedlegg,
                        )
                    }

                    else -> {
                        error("Ukjent dialogmeldingstype: ${dialogmelding::class}")
                    }
                }
        }
    }

    private data class BehandlerDto(
        val hprNummer: String,
        val navn: String,
        val kontor: String,
        val kontorOrganisasjonsnummer: String,
        val telefonnummer: String?,
    ) {
        fun tilDomene(): Behandler =
            Behandler(
                hprNummer = HprNummer(hprNummer),
                navn = navn,
                kontor = kontor,
                kontorOrganisasjonsnummer = Organisasjonsnummer(kontorOrganisasjonsnummer),
                telefonnummer = if (telefonnummer != null) Telefonnummer(telefonnummer) else null,
            )

        companion object {
            fun fraBehandler(behandler: Behandler): BehandlerDto =
                BehandlerDto(
                    hprNummer = behandler.hprNummer.value,
                    navn = behandler.navn,
                    kontor = behandler.kontor,
                    kontorOrganisasjonsnummer = behandler.kontorOrganisasjonsnummer.value,
                    telefonnummer = behandler.telefonnummer?.value,
                )
        }
    }
}
