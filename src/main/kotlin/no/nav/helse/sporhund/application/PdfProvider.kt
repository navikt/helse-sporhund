package no.nav.helse.sporhund.application

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.helse.sporhund.domain.Behandler
import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.Fagområde
import no.nav.helse.sporhund.domain.Identitetsnummer
import no.nav.helse.sporhund.domain.NavIdent
import no.nav.helse.sporhund.domain.Søker
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

interface PdfProvider {
    fun genererPdf(meldingTilBehandlerPdfInput: MeldingTilBehandlerPdfInput): ByteArray

    fun genererPurringPdf(purringTilBehandlerPdfInput: PurringTilBehandlerPdfInput): ByteArray
}

data class MeldingTilBehandlerPdfInput(
    val conversationRef: String,
    val fra: Fra,
    val til: Til,
    val tidspunkt: LocalDateTime,
    val gjelder: Gjelder,
    val fagområde: String,
    val melding: String,
) {
    data class Fra(
        @field:JsonProperty("NAVIdent") @get:JsonProperty("NAVIdent") val NAVIdent: String,
        val navn: String,
    )

    data class Til(
        val navn: String,
        val kontor: Kontor,
    ) {
        companion object {
            fun lag(mottaker: Behandler) =
                Til(
                    navn = mottaker.navn.fulltNavn(),
                    kontor =
                        Kontor(
                            navn = mottaker.kontor.navn ?: "",
                            organisasjonsnummer = mottaker.kontor.organisasjonsnummer?.value ?: "",
                            adresse =
                                Kontor.Adresse(
                                    gate = mottaker.kontor.adresse?.veiadresse ?: "",
                                    postnummer = mottaker.kontor.adresse?.postnummer ?: "",
                                    poststed = mottaker.kontor.adresse?.poststed ?: "",
                                ),
                        ),
                )
        }

        data class Kontor(
            val navn: String,
            val organisasjonsnummer: String,
            val adresse: Adresse,
        ) {
            data class Adresse(
                val gate: String,
                val postnummer: String,
                val poststed: String,
            )
        }
    }

    data class Gjelder(
        val fødselsnummer: String,
        val navn: String,
    )

    companion object {
        fun fra(
            conversationRef: ConversationRef,
            tekst: String,
            avsenderIdent: NavIdent,
            avsenderNavn: String,
            mottaker: Behandler,
            gjelder: Identitetsnummer,
            søker: Søker,
            tidspunkt: Instant,
            fagområde: Fagområde,
        ) = MeldingTilBehandlerPdfInput(
            conversationRef = conversationRef.value.toString(),
            fra =
                Fra(
                    NAVIdent = avsenderIdent.value,
                    navn = avsenderNavn,
                ),
            til = Til.lag(mottaker),
            tidspunkt = tidspunkt.tilNorskTid(),
            gjelder = Gjelder(fødselsnummer = gjelder.value, navn = søker.navn.fulltNavn()),
            fagområde = fagområde.tekst(),
            melding = tekst,
        )
    }
}

/** Samme innhold som [MeldingTilBehandlerPdfInput], men uten avsender: purringen sendes av systemet. */
data class PurringTilBehandlerPdfInput(
    val conversationRef: String,
    val til: MeldingTilBehandlerPdfInput.Til,
    val tidspunkt: LocalDateTime,
    val gjelder: MeldingTilBehandlerPdfInput.Gjelder,
    val fagområde: String,
    val melding: String,
) {
    companion object {
        fun fra(
            conversationRef: ConversationRef,
            tekst: String,
            mottaker: Behandler,
            gjelder: Identitetsnummer,
            søker: Søker,
            tidspunkt: Instant,
            fagområde: Fagområde,
        ) = PurringTilBehandlerPdfInput(
            conversationRef = conversationRef.value.toString(),
            til = MeldingTilBehandlerPdfInput.Til.lag(mottaker),
            tidspunkt = tidspunkt.tilNorskTid(),
            gjelder = MeldingTilBehandlerPdfInput.Gjelder(fødselsnummer = gjelder.value, navn = søker.navn.fulltNavn()),
            fagområde = fagområde.tekst(),
            melding = tekst,
        )
    }
}

private fun Instant.tilNorskTid(): LocalDateTime = atZone(ZoneId.of("Europe/Oslo")).toLocalDateTime()

private fun Fagområde.tekst(): String =
    when (this) {
        Fagområde.EnkeltståendeBehandlingsdager -> "Enkeltstående behandlingsdager"
        Fagområde.Tilbakedatering -> "Tilbakedatering"
        Fagområde.Yrkesskade -> "Yrkesskade"
        Fagområde.Bestridelse -> "Bestridelse"
        Fagområde.UnntakFraArbeidsgiveransvar -> "Unntak fra arbeidsgiveransvar"
        Fagområde.Klage -> "Klage"
        Fagområde.Reisetilskudd -> "Reisetilskudd"
    }
