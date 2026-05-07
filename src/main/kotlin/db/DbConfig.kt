package db

data class DbConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
)
