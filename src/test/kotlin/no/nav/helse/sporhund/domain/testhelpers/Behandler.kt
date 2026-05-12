package no.nav.helse.sporhund.domain.testhelpers

import no.nav.helse.sporhund.domain.Behandler
import no.nav.helse.sporhund.domain.BehandlerRef
import no.nav.helse.sporhund.domain.HprNummer
import no.nav.helse.sporhund.domain.Organisasjonsnummer
import java.util.UUID
import kotlin.random.Random

fun lagBehandlerRef() = BehandlerRef(UUID.randomUUID().toString())

fun lagHprNummer() = HprNummer(Random.nextInt(1_000_000, 9_999_999).toString())

fun lagOrganisasjonsnummer() = Organisasjonsnummer(Random.nextInt(100_000_000, 999_999_999).toString())

fun lagBehandler(): Behandler {
    val hprNummer = lagHprNummer()
    return Behandler(
        hprNummer = hprNummer,
        navn = "Behandler ${hprNummer.value}",
        kontor = "Kontor ${hprNummer.value}",
        kontorOrganisasjonsnummer = lagOrganisasjonsnummer(),
    )
}
