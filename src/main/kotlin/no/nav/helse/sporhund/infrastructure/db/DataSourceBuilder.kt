package no.nav.helse.sporhund.infrastructure.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.flywaydb.core.Flyway
import java.time.Duration

internal class DataSourceBuilder(
    private val dbConfig: DbConfig,
) {
    private val hikariConfig =
        HikariConfig().apply {
            jdbcUrl = dbConfig.jdbcUrl
            username = dbConfig.username
            password = dbConfig.password
            // Cloud SQL's certificate is signed by Google's internal CA, not trusted by the JVM
            // default truststore. Connections go via GCP private VPC so disabling SSL at the JDBC
            // level is safe. This also overrides sslmode=require if present in the injected URL.
            addDataSourceProperty("sslmode", "disable")
            maximumPoolSize = 20
            minimumIdle = 2
            idleTimeout = Duration.ofMinutes(1).toMillis()
            maxLifetime = idleTimeout * 5
            initializationFailTimeout = Duration.ofMinutes(1).toMillis()
            connectionTimeout = Duration.ofSeconds(5).toMillis()
            leakDetectionThreshold = Duration.ofSeconds(30).toMillis()
            metricRegistry =
                PrometheusMeterRegistry(
                    PrometheusConfig.DEFAULT,
                    PrometheusRegistry.defaultRegistry,
                    Clock.SYSTEM,
                )
        }

    fun build(): HikariDataSource = HikariDataSource(hikariConfig)

    fun migrate() {
        HikariDataSource(hikariConfig).use { dataSource ->
            Flyway
                .configure()
                .dataSource(dataSource)
                .lockRetryCount(-1)
                .validateMigrationNaming(true)
                .load()
                .migrate()
        }
    }
}
