package no.nav.helse.sporhund.infrastructure.kafka

import kotlin.test.Test
import kotlin.test.assertEquals

class MaskertForesporselSvarTest {
    @Test
    fun `maskerer tekstNotatInnhold med enkel verdi`() {
        val input = """{"tekstNotatInnhold":"sensitiv tekst"}"""
        val expected = """{"tekstNotatInnhold":"***"}"""
        assertEquals(expected, input.maskertForesporselSvar())
    }

    @Test
    fun `maskerer tekstNotatInnhold med mellomrom rundt kolon`() {
        val input = """{"tekstNotatInnhold" : "sensitiv tekst"}"""
        val expected = """{"tekstNotatInnhold":"***"}"""
        assertEquals(expected, input.maskertForesporselSvar())
    }

    @Test
    fun `maskerer tekstNotatInnhold med escaped anførselstegn i verdi`() {
        val input = """{"tekstNotatInnhold":"Han sa \"hei\" til meg"}"""
        val expected = """{"tekstNotatInnhold":"***"}"""
        assertEquals(expected, input.maskertForesporselSvar())
    }

    @Test
    fun `maskerer tekstNotatInnhold med parentes i verdi`() {
        val input = """{"tekstNotatInnhold":"(This is not caught)"}"""
        val expected = """{"tekstNotatInnhold":"***"}"""
        assertEquals(expected, input.maskertForesporselSvar())
    }

    @Test
    fun `maskerer ikke andre felter`() {
        val input = """{"annetFelt":"skal ikke maskeres"}"""
        assertEquals(input, input.maskertForesporselSvar())
    }

    @Test
    fun `maskerer tekstNotatInnhold men beholder resten av JSON`() {
        val input = """{"id":"123","tekstNotatInnhold":"sensitiv tekst","annet":"verdi"}"""
        val expected = """{"id":"123","tekstNotatInnhold":"***","annet":"verdi"}"""
        assertEquals(expected, input.maskertForesporselSvar())
    }

    @Test
    fun `maskerer tekstNotatInnhold med tom streng som verdi`() {
        val input = """{"tekstNotatInnhold":""}"""
        val expected = """{"tekstNotatInnhold":"***"}"""
        assertEquals(expected, input.maskertForesporselSvar())
    }

    @Test
    fun `maskerer tekstNotatInnhold med backslash i verdi`() {
        val input = """{"tekstNotatInnhold":"linje1\\nlinje2"}"""
        val expected = """{"tekstNotatInnhold":"***"}"""
        assertEquals(expected, input.maskertForesporselSvar())
    }
}
