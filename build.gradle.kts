plugins {
    kotlin("jvm") version "2.3.21"
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
    implementation(libs.bundles.smiley4.ktor.openapi.tools)
    implementation(libs.bundles.db)
    implementation(libs.bundles.jackson)

    testRuntimeOnly(libs.logback.classic)

    testImplementation(kotlin("test"))
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.postgres)
}

kotlin {
    jvmToolchain(21)
}

tasks {
    test {
        useJUnitPlatform()
    }
    build {
        doLast {
            val erLokaltBygg = !System.getenv().containsKey("GITHUB_ACTION")
            val manglerPreCommitHook = !File(".git/hooks/pre-commit").isFile
            if (erLokaltBygg && manglerPreCommitHook) {
                println(
                    """
                    !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! ¯\_(⊙︿⊙)_/¯ !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    !            Hei du! Det ser ut til at du mangler en pre-commit-hook :/         !
                    ! Du kan installere den ved å kjøre './gradlew addKtlintFormatGitPreCommitHook' !
                    !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    """.trimIndent(),
                )
            }
        }
    }
    register<JavaExec>("runLocal") {
        group = "application"
        description = "Runs LocalApp locally"
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("LocalAppKt")
        environment("NAIS_CLUSTER_NAME", "local")
    }
}
