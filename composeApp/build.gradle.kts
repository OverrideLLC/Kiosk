import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import androidx.room3.gradle.RoomExtension

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
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
            implementation(compose.components.uiToolingPreview)
            implementation(libs.material.kolor)
            implementation(libs.room3.runtime)
            implementation(libs.sqlite.bundled)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.datastore.preferences.core)

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

dependencies {
    add("kspDesktop", libs.room3.compiler)
}

extensions.configure<RoomExtension> {
    schemaDirectory("$projectDir/schemas")
}

compose.resources {
    publicResClass = true
    packageOfResClass = "app.kiosksync.resources"
    generateResClass = always
}

compose.desktop {
    application {
        mainClass = "app.kiosksync.MainKt"

        // Windows 10/11 may use a legacy ANSI code page for the JVM. The app
        // writes Spanish text and JSON, so force UTF-8 in the packaged runtime.
        jvmArgs += listOf("-Dfile.encoding=UTF-8")

        nativeDistributions {
            // Msi and Exe are produced by jpackage on Windows. Both require WiX
            // Toolset 3.x installed and on PATH; createDistributable does not.
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "OverrideKiosk"
            packageVersion = "1.1.3"
            description = "Override Kiosk: carrusel de imágenes para la red local"
            vendor = "Override"
            copyright = "© 2026 Override"
            // Shows the MIT license in the Windows installer wizard.
            licenseFile.set(rootProject.file("LICENSE"))

            // Keep the bundled runtime small. java.desktop, java.base,
            // java.logging and java.prefs are added by the Compose plugin.
            modules("java.naming", "jdk.unsupported")

            windows {
                iconFile.set(project.file("src/desktopMain/resources/icon.ico"))
                menuGroup = "Override Kiosk"
                shortcut = true
                dirChooser = true
                // No administrator rights: required for Windows Home.
                perUserInstall = true
                upgradeUuid = "c8945362-54e5-4a20-a7e8-0d58f5030508"
            }
        }
    }
}
