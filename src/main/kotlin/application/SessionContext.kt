package application

interface SessionContext {
    val outbox: Outbox
}
