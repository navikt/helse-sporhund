package db

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
