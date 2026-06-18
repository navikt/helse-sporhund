package no.nav.helse.sporhund.infrastructure.kafka

import no.nav.helse.sporhund.infrastructure.kafka.testhelpers.lagDialogmelding
import no.nav.helse.sporhund.infrastructure.kafka.testhelpers.lagDialogmeldingFraBehandlerKafkaDto
import no.nav.helse.sporhund.infrastructure.kafka.testhelpers.lagDialogmeldingUtenForesporselssvar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LoggingTest {
    @Test
    fun `medMaskertForesporselSvar - maskerer innholdet når feltet er satt`() {
        val dto = lagDialogmeldingFraBehandlerKafkaDto(dialogmelding = lagDialogmelding(tekstNotatInnhold = "sensitiv tekst"))
        assertNotNull(dto.dialogmelding.foresporselFraSaksbehandlerForesporselSvar, "Forutsetning: feltet skal være satt i test-dto")

        val maskert = dto.medMaskertForesporselSvar()

        assertNotNull(maskert.dialogmelding.foresporselFraSaksbehandlerForesporselSvar, "Feltet skal fremdeles være ikke-null slik at det synes i loggen at det fantes data")
        assertEquals("***", maskert.dialogmelding.foresporselFraSaksbehandlerForesporselSvar.tekstNotatInnhold)
    }

    @Test
    fun `medMaskertForesporselSvar - beholder null når feltet er null`() {
        val dto = lagDialogmeldingFraBehandlerKafkaDto(dialogmelding = lagDialogmeldingUtenForesporselssvar())

        val maskert = dto.medMaskertForesporselSvar()

        assertNull(maskert.dialogmelding.foresporselFraSaksbehandlerForesporselSvar)
    }

    @Test
    fun `medMaskertForesporselSvar - øvrige felter er uendret`() {
        val dto = lagDialogmeldingFraBehandlerKafkaDto()

        val maskert = dto.medMaskertForesporselSvar()

        assertEquals(dto.msgId, maskert.msgId)
        assertEquals(dto.conversationRef, maskert.conversationRef)
        assertEquals(dto.personIdentPasient, maskert.personIdentPasient)
        assertEquals(dto.dialogmelding.navnHelsepersonell, maskert.dialogmelding.navnHelsepersonell)
        assertEquals(dto.dialogmelding.foresporselFraSaksbehandlerForesporselSvar?.temaKode, maskert.dialogmelding.foresporselFraSaksbehandlerForesporselSvar?.temaKode)
    }

    @Test
    fun `maskertForesporselSvar - maskerer tekstNotatInnhold i compact JSON`() {
        val json = """{"tekstNotatInnhold":"sensitiv tekst","annetFelt":"uendret"}"""
        assertEquals("""{"tekstNotatInnhold":"***","annetFelt":"uendret"}""", json.maskertForesporselSvar())
    }

    @Test
    fun `maskertForesporselSvar - maskerer tekstNotatInnhold i pretty-printed JSON`() {
        val json = """{"tekstNotatInnhold" : "sensitiv tekst","annetFelt":"uendret"}"""
        assertEquals("""{"tekstNotatInnhold":"***","annetFelt":"uendret"}""", json.maskertForesporselSvar())
    }

    @Test
    fun `maskertForesporselSvar - gjør ingenting når feltet ikke finnes`() {
        val json = """{"annetFelt":"uendret"}"""
        assertEquals(json, json.maskertForesporselSvar())
    }

    @Test
    fun `maskertForesporselSvar - gjør ingenting når JSON er ugyldig`() {
        val ugyldig = "dette er ikke json"
        assertEquals(ugyldig, ugyldig.maskertForesporselSvar())
    }
}
