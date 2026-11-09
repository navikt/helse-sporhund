package no.nav.helse.sporhund.infrastructure.clients.dokarkiv.testhelpers

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
 * Request-bodyene for opprettede journalposter lagres, slik at testene kan sjekke hva som ble journalført.
 */
class MockDokarkivServer {
    private val server = HttpServer.create(InetSocketAddress(0), 0)
    private val opprettedeJournalposter = CopyOnWriteArrayList<String>()

    val baseUrl: String get() = "http://localhost:${server.address.port}"

    /** Request-bodyene (JSON) for alle journalposter som er opprettet med POST. */
    fun opprettedeJournalposter(): List<String> = opprettedeJournalposter.toList()

    init {
        server.createContext("/rest/journalpostapi/v1/journalpost") { exchange ->
            val requestBody = exchange.requestBody.use { it.readBytes() }.decodeToString()
            val (status, responseBody) =
                when {
                    exchange.requestMethod == "POST" -> {
                        opprettedeJournalposter.add(requestBody)
                        201 to """{"journalpostId":"mock-journalpostId","journalstatus":"FERDIGSTILT","melding":null,"journalpostferdigstilt":true,"dokumenter":[{"dokumentInfoId":"mock-dokumentInfoId"}]}"""
                    }

                    exchange.requestURI.path.endsWith("/knyttTilAnnenSak") -> {
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

    fun stop() = server.stop(0)
}
