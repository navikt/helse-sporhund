package no.nav.helse.sporhund.infrastructure.kafka

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import no.nav.helse.sporhund.infrastructure.kafka.testhelpers.lagDialogmeldingFraBehandlerKafkaDto
import no.nav.helse.sporhund.infrastructure.kafka.testhelpers.lagDialogmeldingUtenForesporselssvar

class UtilsTest {
    @Test
    fun `erRelevant - conversationRef er gyldig UUID og har forespørselssvar`() {
        val dto =
            lagDialogmeldingFraBehandlerKafkaDto(
                conversationRef = "123e4567-e89b-12d3-a456-426614174000"
            )
        assertTrue(dto.erRelevant())
    }

    @Test
    fun `erRelevant - conversationRef er null og har forespørselssvar`() {
        val dto =
            lagDialogmeldingFraBehandlerKafkaDto(
                conversationRef = null
            )
        assertTrue(dto.erRelevant())
    }

    @Test
    fun `erRelevant - conversationRef er ikke UUID`() {
        val dto =
            lagDialogmeldingFraBehandlerKafkaDto(
                conversationRef = "ikke-en-uuid"
            )
        assertFalse(dto.erRelevant())
    }

    @Test
    fun `erRelevant - conversationRef er tom streng`() {
        val dto =
            lagDialogmeldingFraBehandlerKafkaDto(
                conversationRef = ""
            )
        assertFalse(dto.erRelevant())
    }

    @Test
    fun `erRelevant - mangler forespørselssvar`() {
        val dto =
            lagDialogmeldingFraBehandlerKafkaDto(
                dialogmelding = lagDialogmeldingUtenForesporselssvar()
            )
        assertFalse(dto.erRelevant())
    }

    @Test
    fun `erRelevant - conversationRef er null og mangler forespørselssvar`() {
        val dto =
            lagDialogmeldingFraBehandlerKafkaDto(
                conversationRef = null,
                dialogmelding = lagDialogmeldingUtenForesporselssvar()
            )
        assertFalse(dto.erRelevant())
    }
}
