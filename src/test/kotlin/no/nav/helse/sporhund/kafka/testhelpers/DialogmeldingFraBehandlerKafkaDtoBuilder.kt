package no.nav.helse.sporhund.kafka.testhelpers

import no.nav.helse.sporhund.kafka.Dialogmelding
import no.nav.helse.sporhund.kafka.DialogmeldingFraBehandlerKafkaDto
import no.nav.helse.sporhund.kafka.ForesporselFraSaksbehandlerForesporselSvar
import no.nav.helse.sporhund.kafka.TemaKode
import java.time.LocalDateTime
import java.util.UUID

fun lagDialogmeldingFraBehandlerKafkaDto(
    msgId: String = UUID.randomUUID().toString(),
    msgType: String = "DIALOG_NOTAT",
    navLogId: String = UUID.randomUUID().toString(),
    mottattTidspunkt: LocalDateTime = LocalDateTime.now(),
    conversationRef: String? = UUID.randomUUID().toString(),
    parentRef: String? = null,
    personIdentPasient: String = "12345678910",
    personIdentBehandler: String = "01010112345",
    legekontorOrgNr: String? = "123456789",
    legekontorHerId: String? = "12345",
    legekontorOrgName: String = "Testlegekontor",
    legehpr: String? = "1234567",
    dialogmelding: Dialogmelding = lagDialogmelding(),
    antallVedlegg: Int = 0,
    journalpostId: String = UUID.randomUUID().toString(),
    fellesformatXML: String = "<fellesformat/>",
): DialogmeldingFraBehandlerKafkaDto =
    DialogmeldingFraBehandlerKafkaDto(
        msgId = msgId,
        msgType = msgType,
        navLogId = navLogId,
        mottattTidspunkt = mottattTidspunkt,
        conversationRef = conversationRef,
        parentRef = parentRef,
        personIdentPasient = personIdentPasient,
        personIdentBehandler = personIdentBehandler,
        legekontorOrgNr = legekontorOrgNr,
        legekontorHerId = legekontorHerId,
        legekontorOrgName = legekontorOrgName,
        legehpr = legehpr,
        dialogmelding = dialogmelding,
        antallVedlegg = antallVedlegg,
        journalpostId = journalpostId,
        fellesformatXML = fellesformatXML,
    )

fun lagDialogmelding(
    id: String = UUID.randomUUID().toString(),
    tekstNotatInnhold: String = "Dette er en testmelding",
    navnHelsepersonell: String = "Ola Testlege",
    signaturDato: LocalDateTime = LocalDateTime.now(),
): Dialogmelding =
    Dialogmelding(
        id = id,
        innkallingMoterespons = null,
        foresporselFraSaksbehandlerForesporselSvar =
            ForesporselFraSaksbehandlerForesporselSvar(
                temaKode =
                    TemaKode(
                        kodeverkOID = "2.16.578.1.12.4.1.1.8127",
                        dn = "Forespørsel",
                        v = "DIALOG_FORESPORSEL",
                        arenaNotatKategori = "IJ",
                        arenaNotatKode = "FORESPORSEL",
                        arenaNotatTittel = "Forespørsel",
                    ),
                tekstNotatInnhold = tekstNotatInnhold,
                dokIdNotat = null,
                datoNotat = null,
            ),
        henvendelseFraLegeHenvendelse = null,
        navnHelsepersonell = navnHelsepersonell,
        signaturDato = signaturDato,
    )
