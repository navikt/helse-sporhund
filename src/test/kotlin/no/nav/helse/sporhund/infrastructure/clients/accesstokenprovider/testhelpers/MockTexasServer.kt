package no.nav.helse.sporhund.infrastructure.clients.accesstokenprovider.testhelpers

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress

/**
 * Enkel mock-server for NAIS Texas token-endepunkter.
 * Brukes i lokal kjøring for å unngå avhengighet til ekte Texas-sidecar.
 *
 * - POST /token       → maskin-til-maskin-token
 * - POST /exchange    → OBO-token
 */
class MockTexasServer {
    private val server = HttpServer.create(InetSocketAddress(0), 0)

    val tokenEndpoint: String get() = "http://localhost:${server.address.port}/token"
    val exchangeEndpoint: String get() = "http://localhost:${server.address.port}/exchange"

    init {
        server.createContext("/token") { exchange ->
            val response = """{"access_token":"mock-machine-token","expires_in":3600,"token_type":"Bearer"}"""
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(response.toByteArray()) }
        }
        server.createContext("/exchange") { exchange ->
            val response = """{"access_token":"mock-obo-token","expires_in":3600,"token_type":"Bearer"}"""
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(response.toByteArray()) }
        }
        server.executor = null
        server.start()
    }

    fun stop() = server.stop(0)
}
