package no.nav.helse.sporhund.application

import java.util.UUID

fun interface VedleggProvider {
    fun hentVedlegg(msgId: UUID): List<ByteArray>
}

