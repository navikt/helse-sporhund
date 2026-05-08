package no.nav.helse.sporhund.application

interface SessionContext {
    val outbox: Outbox
}
