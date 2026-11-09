package no.nav.helse.sporhund.application

import no.nav.helse.sporhund.application.kommando.KommandoKø

interface SessionContext {
    val outbox: Outbox
    val dialogRepository: DialogRepository
    val kommandoKø: KommandoKø
    val dialogmeldingPdfRepository: DialogmeldingPdfRepository
}
