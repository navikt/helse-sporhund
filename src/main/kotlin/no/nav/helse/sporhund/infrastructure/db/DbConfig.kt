package no.nav.helse.sporhund.infrastructure.db

data class DbConfig(
    val gcpProjectId: String,
    val databaseRegion: String,
    val databaseInstance: String,
    val databaseName: String,
    val username: String,
    val password: String,
    private val jdbcUrlOverride: String? = null,
) {
    val jdbcUrl: String =
        jdbcUrlOverride ?: String.format(
            "jdbc:postgresql:///%s?cloudSqlInstance=%s:%s:%s&socketFactory=com.google.cloud.sql.postgres.SocketFactory",
            databaseName,
            gcpProjectId,
            databaseRegion,
            databaseInstance,
        )
}
