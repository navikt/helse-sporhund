package no.nav.helse.sporhund.infrastructure.db

data class DbConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
) {
    // The PostgreSQL JDBC driver defaults to sslmode=prefer, which causes it to attempt SSL
    // negotiation even for localhost connections (e.g. Cloud SQL Auth Proxy). The proxy handles
    // SSL itself, so we explicitly disable it at the JDBC level to avoid PKIX certificate errors.
    val effectiveJdbcUrl: String = if ("sslmode=" in jdbcUrl) jdbcUrl else "$jdbcUrl?sslmode=disable"
}
