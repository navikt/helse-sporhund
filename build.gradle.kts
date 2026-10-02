group = "no.nav.helse"

plugins {
    alias(libs.plugins.sykepenger.deployable)
}

sykepengerDeployable {
    mainClass = "no.nav.helse.sporhund.AppKt"
    imageName = "helse-sporhund"
}

dependencies {
    implementation(libs.tbd.naisful.app)
    implementation(libs.tbd.kafka)
    implementation(libs.tbd.personpseudoid)
    implementation(libs.tbd.retry)
    implementation(libs.bundles.smiley4.ktor.openapi.tools)
    implementation(libs.bundles.db)
    implementation(libs.cloud.sql.postgres.socket.factory)
    implementation(libs.bundles.jackson)

    implementation(libs.logback.classic)
    implementation(libs.logstash.logback.encoder)
    implementation(libs.logback.syslog4j)

    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.auth0.jwt)

    implementation(libs.tbd.populasjonstilgangskontroll.tilgangsmaskinen)
    implementation(libs.tbd.populasjonstilgangskontroll.api)

    implementation(libs.tbd.access.token.texas)
    implementation(libs.tbd.access.token.api)

    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.jackson)
    implementation(libs.ktor.client.content.negotiation)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.server.content.negotiation)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.jackson)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.postgres)
    testImplementation(libs.mockOauth2Server)
}

tasks {
    register<JavaExec>("runLocal") {
        group = "application"
        description = "Runs LocalApp locally"
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("no.nav.helse.sporhund.LocalAppKt")
        environment("NAIS_CLUSTER_NAME", "local")
    }
}
