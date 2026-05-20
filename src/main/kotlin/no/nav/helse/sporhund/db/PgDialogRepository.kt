package no.nav.helse.sporhund.db

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.module.kotlin.readValue
import kotliquery.Session
import no.nav.helse.sporhund.application.DialogRepository
import no.nav.helse.sporhund.domain.*
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

    override fun finnDialoger(identitetsnummer: Identitetsnummer): List<Dialog> =
        asSQL(
            """
                SELECT json FROM dialog WHERE identitetsnummer = :identitetsnummer
            """,
            "identitetsnummer" to identitetsnummer.value,
        ).list(session) {
            objectMapper.readValue<DialogDto>(it.string("json")).tilDomene()
        }

    override fun finnIkkeLukkedeDialoger(): List<Dialog> =
        asSQL(
            """
            SELECT json FROM dialog WHERE json -> 'status' != '"DialogLukket"'
            """.trimIndent(),
        ).list(session) {
            objectMapper.readValue<DialogDto>(it.string("json")).tilDomene()
        }

    private data class DialogDto(
        val conversationRef: UUID,
        val identitetsnummer: IdentitetsnummerDto,
        val søkernavn: NavnDto,
        val meldinger: List<DialogmeldingDto>,
        val status: DialogstatusDto,
        val dialogtype: DialogtypeDto,
        val fagområde: FagområdeDto,
    ) {
        fun tilDomene(): Dialog =
            Dialog.fraLagring(
                conversationRef = ConversationRef(conversationRef),
                identitetsnummer = identitetsnummer.tilDomene(),
                meldinger = meldinger.map { it.tilDomene() },
                status = status.tilDomene(),
                dialogtype = dialogtype.tilDomene(),
                fagområde = fagområde.tilDomene(),
                søkernavn = søkernavn.tilDomene(),
            )

        companion object {
            fun fraDialog(dialog: Dialog): DialogDto =
                DialogDto(
                    conversationRef = dialog.conversationRef.value,
                    identitetsnummer = IdentitetsnummerDto.fraIdentitetsnummer(dialog.identitetsnummer),
                    meldinger = dialog.meldinger.map { DialogmeldingDto.fraDialogmelding(it) },
                    status = DialogstatusDto.fraDialogstatus(dialog.status),
                    dialogtype = DialogtypeDto.fraDialogtype(dialog.dialogtype),
                    fagområde = FagområdeDto.fraDialogtype(dialog.fagområde),
                    søkernavn = NavnDto(fornavn = dialog.søkernavn.fornavn, mellomnavn = dialog.søkernavn.mellomnavn, etternavn = dialog.søkernavn.etternavn),
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

    private enum class DialogstatusDto {
        ForespørselSendt,
        SvarMottatt,
        PurringSendt,
        DialogLukket, ;

        fun tilDomene(): Dialogstatus =
            when (this) {
                ForespørselSendt -> Dialogstatus.ForespørselSendt
                SvarMottatt -> Dialogstatus.SvarMottatt
                PurringSendt -> Dialogstatus.PurringSendt
                DialogLukket -> Dialogstatus.DialogLukket
            }

        companion object {
            fun fraDialogstatus(dialogstatus: Dialogstatus): DialogstatusDto =
                when (dialogstatus) {
                    Dialogstatus.ForespørselSendt -> ForespørselSendt
                    Dialogstatus.SvarMottatt -> SvarMottatt
                    Dialogstatus.PurringSendt -> PurringSendt
                    Dialogstatus.DialogLukket -> DialogLukket
                }
        }
    }

    private enum class FagområdeDto {
        EnkeltståendeBehandlingsdager,
        Tilbakedatering,
        Yrkesskade,
        Bestridelse,
        ;

        fun tilDomene(): Fagområde =
            when (this) {
                EnkeltståendeBehandlingsdager -> Fagområde.EnkeltståendeBehandlingsdager
                Tilbakedatering -> Fagområde.Tilbakedatering
                Yrkesskade -> Fagområde.Yrkesskade
                Bestridelse -> Fagområde.Bestridelse
            }

        companion object {
            fun fraDialogtype(fagområde: Fagområde): FagområdeDto =
                when (fagområde) {
                    Fagområde.EnkeltståendeBehandlingsdager -> EnkeltståendeBehandlingsdager
                    Fagområde.Tilbakedatering -> Tilbakedatering
                    Fagområde.Yrkesskade -> Yrkesskade
                    Fagområde.Bestridelse -> Bestridelse
                }
        }
    }

    private enum class DialogtypeDto {
        Journalnotat,
        MedisinskeOpplysninger,
        EkstraUttalelserFraLege,
        SpesialistErklæring,
        UtvidetSpesialistErklæring,
        ;

        fun tilDomene(): Dialogtype =
            when (this) {
                Journalnotat -> Dialogtype.Journalnotat
                MedisinskeOpplysninger -> Dialogtype.MedisinskeOpplysninger
                EkstraUttalelserFraLege -> Dialogtype.EkstraUttalelserFraLege
                SpesialistErklæring -> Dialogtype.SpesialistErklæring
                UtvidetSpesialistErklæring -> Dialogtype.UtvidetSpesialistErklæring
            }

        companion object {
            fun fraDialogtype(dialogtype: Dialogtype): DialogtypeDto =
                when (dialogtype) {
                    Dialogtype.Journalnotat -> Journalnotat
                    Dialogtype.MedisinskeOpplysninger -> MedisinskeOpplysninger
                    Dialogtype.EkstraUttalelserFraLege -> EkstraUttalelserFraLege
                    Dialogtype.SpesialistErklæring -> SpesialistErklæring
                    Dialogtype.UtvidetSpesialistErklæring -> UtvidetSpesialistErklæring
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

    private data class NavnDto(
        val fornavn: String,
        val mellomnavn: String?,
        val etternavn: String,
    ) {
        fun tilDomene() = Navn(fornavn = fornavn, mellomnavn = mellomnavn, etternavn = etternavn)
    }

    private data class BehandlerDto(
        val hprNummer: String,
        val navn: NavnDto,
        val kontor: KontorDto,
        val telefonnummer: String?,
    ) {
        fun tilDomene(): Behandler =
            Behandler(
                hprNummer = HprNummer(hprNummer),
                navn = navn.tilDomene(),
                kontor = kontor.tilDomene(),
                telefonnummer = if (telefonnummer != null) Telefonnummer(telefonnummer) else null,
            )

        data class KontorDto(
            val navn: String?,
            val organisasjonsnummer: String?,
            val adresse: AdresseDto?,
        ) {
            fun tilDomene() =
                Kontor(
                    navn = navn,
                    organisasjonsnummer = organisasjonsnummer?.let { Organisasjonsnummer(it) },
                    adresse = adresse?.tilDomene(),
                )
        }

        data class AdresseDto(
            val veiadresse: String?,
            val postnummer: String?,
            val poststed: String?,
        ) {
            fun tilDomene() = Adresse(veiadresse = veiadresse, postnummer = postnummer, poststed = poststed)
        }

        companion object {
            fun fraBehandler(behandler: Behandler): BehandlerDto =
                BehandlerDto(
                    hprNummer = behandler.hprNummer.value,
                    navn =
                        NavnDto(
                            fornavn = behandler.navn.fornavn,
                            mellomnavn = behandler.navn.mellomnavn,
                            etternavn = behandler.navn.etternavn,
                        ),
                    kontor =
                        KontorDto(
                            navn = behandler.kontor.navn,
                            organisasjonsnummer = behandler.kontor.organisasjonsnummer?.value,
                            adresse =
                                behandler.kontor.adresse?.let {
                                    AdresseDto(veiadresse = it.veiadresse, postnummer = it.postnummer, poststed = it.poststed)
                                },
                        ),
                    telefonnummer = behandler.telefonnummer?.value,
                )
        }
    }
}
