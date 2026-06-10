package no.nav.helse.sporhund.infrastructure.clients.dokarkiv.testhelpers

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress

/**
 * Enkel mock-server for Dokarkiv (journalføring).
 * Brukes i lokal kjøring og returnerer alltid OK for:
 *   POST /rest/journalpostapi/v1/journalpost (opprett journalpost)
 *   PATCH /rest/journalpostapi/v1/journalpost/{id}/feilregistrer/feilregistrerSakstilknytning
 *   PUT   /rest/journalpostapi/v1/journalpost/{id}/knyttTilAnnenSak
 */
class MockDokarkivServer {
    private val server = HttpServer.create(InetSocketAddress(0), 0)

    val baseUrl: String get() = "http://localhost:${server.address.port}"

    init {
        server.createContext("/rest/journalpostapi/v1/journalpost") { exchange ->
            exchange.requestBody.use { it.readBytes() }
            val responseBody = """{"journalpostId":"mock-journalpostId","journalstatus":"FERDIGSTILT","melding":null,"journalpostferdigstilt":true}""".toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(201, responseBody.size.toLong())
            exchange.responseBody.use { it.write(responseBody) }
        }
        server.executor = null
        server.start()
    }

    fun stop() = server.stop(0)
}
