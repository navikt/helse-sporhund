package no.nav.helse.sporhund.infrastructure.kafka

import no.nav.helse.sporhund.domain.Behandler
import no.nav.helse.sporhund.domain.ConversationRef
import no.nav.helse.sporhund.domain.HprNummer
import no.nav.helse.sporhund.domain.Identitetsnummer
import java.time.Instant

sealed interface SvarFraBehandler {
    val hprNummer: HprNummer
    val identitetsnummerSykmeldt: Identitetsnummer
    val behandler: Behandler
    val tekst: String
    val antallVedlegg: Int
    val tidspunktMottattNav: Instant
    val meldingId: String
    val journalpostId: String

    class MedConversationRef(
        val conversationRef: ConversationRef,
        override val hprNummer: HprNummer,
        override val identitetsnummerSykmeldt: Identitetsnummer,
        override val behandler: Behandler,
        override val tekst: String,
        override val antallVedlegg: Int,
        override val tidspunktMottattNav: Instant,
        override val meldingId: String,
        override val journalpostId: String,
    ) : SvarFraBehandler

    class UtenConversationRef(
        override val hprNummer: HprNummer,
        override val identitetsnummerSykmeldt: Identitetsnummer,
        override val behandler: Behandler,
        override val tekst: String,
        override val antallVedlegg: Int,
        override val tidspunktMottattNav: Instant,
        override val meldingId: String,
        override val journalpostId: String,
    ) : SvarFraBehandler
}
