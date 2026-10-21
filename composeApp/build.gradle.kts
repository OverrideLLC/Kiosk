import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)

                // Ktor Server (Receiver)
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cio)
                implementation(libs.ktor.server.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)

                // Ktor Client (Emitter)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.content.negotiation)

                // JmDNS (Discovery)
                implementation(libs.jmdns)

                // Windows low-level keyboard hook (kiosk lock)
                implementation(libs.jna.platform)
            }
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "app.kiosksync.resources"
    generateResClass = always
}

compose.desktop {
    application {
        mainClass = "app.kiosksync.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "KioskSync"
            packageVersion = "1.0.0"
            description = "KioskSync Digital Signage Carousel"
            vendor = "KioskSync"
        }
    }
}
