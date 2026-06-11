plugins {
    kotlin("jvm") version "2.4.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

group = "no.nav.helse"

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

dependencies {
    implementation(platform(libs.ktor.bom))
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

    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.server.content.negotiation)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.jackson)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.postgres)
    testImplementation(libs.mockOauth2Server)
}

kotlin {
    jvmToolchain(21)
}

tasks {
    test {
        useJUnitPlatform()
    }
    build {
        val erCiBygg = providers.environmentVariable("GITHUB_ACTIONS").orNull == "true"
        if (!erCiBygg) {
            dependsOn("addKtlintFormatGitPreCommitHook")
        }
    }

    register<JavaExec>("runLocal") {
        group = "application"
        description = "Runs LocalApp locally"
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("no.nav.helse.sporhund.LocalAppKt")
        environment("NAIS_CLUSTER_NAME", "local")
    }

    jar {
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = "no.nav.helse.sporhund.AppKt"
            attributes["Class-Path"] =
                configurations.runtimeClasspath.get().joinToString(separator = " ") {
                    it.name
                }
        }

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("${layout.buildDirectory.get()}/libs/${it.name}")
                if (!file.exists()) it.copyTo(file)
            }
        }
    }
}
