package no.nav.helse.sporhund.infrastructure.clients.dokarkiv.testhelpers

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Enkel mock-server for Dokarkiv (journalføring).
 * Brukes i lokal kjøring og E2E-tester og returnerer alltid OK for:
 *   POST  /rest/journalpostapi/v1/journalpost (opprett journalpost)
 *   PATCH /rest/journalpostapi/v1/journalpost/{id}/feilregistrer/feilregistrerSakstilknytning
 *   PUT   /rest/journalpostapi/v1/journalpost/{id}/knyttTilAnnenSak
 *
 * Request-bodyene for opprettede og knyttede journalposter lagres, og det samme gjør ID-ene til feilregistrerte journalposter,
 * slik at testene kan sjekke hva som ble journalført og knyttet.
 */
class MockDokarkivServer {
    private val server = HttpServer.create(InetSocketAddress(0), 0)
    private val opprettedeJournalposter = CopyOnWriteArrayList<String>()
    private val feilregistrerteJournalposter = CopyOnWriteArrayList<String>()
    private val knyttedeJournalposter = CopyOnWriteArrayList<KnyttetJournalpost>()

    val baseUrl: String get() = "http://localhost:${server.address.port}"

    /** Request-bodyene (JSON) for alle journalposter som er opprettet med POST. */
    fun opprettedeJournalposter(): List<String> = opprettedeJournalposter.toList()

    /** ID-ene til journalposter som er feilregistrert med PATCH. */
    fun feilregistrerteJournalposter(): List<String> = feilregistrerteJournalposter.toList()

    /** Journalposter som er knyttet til annen sak med PUT, med request-bodyen (JSON). */
    fun knyttedeJournalposter(): List<KnyttetJournalpost> = knyttedeJournalposter.toList()

    data class KnyttetJournalpost(
        val journalpostId: String,
        val body: String,
    )

    init {
        server.createContext("/rest/journalpostapi/v1/journalpost") { exchange ->
            val requestBody = exchange.requestBody.use { it.readBytes() }.decodeToString()
            val (status, responseBody) =
                when {
                    exchange.requestMethod == "POST" -> {
                        opprettedeJournalposter.add(requestBody)
                        201 to """{"journalpostId":"mock-journalpostId","journalstatus":"FERDIGSTILT","melding":null,"journalpostferdigstilt":true,"dokumenter":[{"dokumentInfoId":"mock-dokumentInfoId"}]}"""
                    }

                    exchange.requestURI.path.endsWith("/feilregistrer/feilregistrerSakstilknytning") -> {
                        feilregistrerteJournalposter.add(exchange.journalpostId())
                        200 to "{}"
                    }

                    exchange.requestURI.path.endsWith("/knyttTilAnnenSak") -> {
                        knyttedeJournalposter.add(KnyttetJournalpost(exchange.journalpostId(), requestBody))
                        200 to """{"nyJournalpostId":"mock-ny-journalpostId"}"""
                    }

                    else -> {
                        200 to "{}"
                    }
                }
            val bytes = responseBody.toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.executor = null
        server.start()
    }

    private fun HttpExchange.journalpostId() = requestURI.path.removePrefix("/rest/journalpostapi/v1/journalpost/").substringBefore("/")

    fun stop() = server.stop(0)
}
