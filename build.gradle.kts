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
    implementation(libs.tbd.naisful.app)
    implementation(libs.tbd.kafka)
    implementation(libs.bundles.db)
    testImplementation(kotlin("test"))
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
}
