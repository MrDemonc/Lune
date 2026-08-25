plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}

val appVersion: String = providers.gradleProperty("app.version").getOrElse("1.0.0")
val appName: String = providers.gradleProperty("app.name").getOrElse("Lune")
val appVendor: String = providers.gradleProperty("app.vendor").getOrElse("DemonLab")
val appLicense: String = providers.gradleProperty("app.license").getOrElse("GPLv3")

group = "com.demonlab.lune"
version = appVersion

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.animation)
    implementation(compose.foundation)
    implementation(compose.ui)
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    
    // Audio metadata tagging
    implementation("net.jthink:jaudiotagger:3.0.1")
    
    // JSON serialization
    implementation("com.google.code.gson:gson:2.11.0")
    
    // SQLite
    implementation("org.xerial:sqlite-jdbc:3.47.2.0")

    // D-Bus for Linux MPRIS2 media control
    implementation("com.github.hypfvieh:dbus-java-core:5.1.0")
    implementation("com.github.hypfvieh:dbus-java-transport-native-unixsocket:5.1.0")
    implementation("org.slf4j:slf4j-nop:2.0.13")
}

tasks.processResources {
    inputs.property("version", appVersion)
    inputs.property("name", appName)
    inputs.property("vendor", appVendor)
    inputs.property("license", appLicense)
    filesMatching("app.properties") {
        expand(
            "version" to appVersion,
            "name" to appName,
            "vendor" to appVendor,
            "license" to appLicense
        )
    }
}

compose.desktop {
    application {
        mainClass = "com.demonlab.lune.MainKt"
        jvmArgs += listOf(
            "-Dskiko.renderApi=OPENGL",
            "-Dskiko.vsync.enabled=true"
        )

        nativeDistributions {
            modules(
                "java.base",
                "java.desktop",
                "java.logging",
                "java.instrument",
                "java.management",
                "java.naming",
                "java.sql",
                "java.xml",
                "jdk.security.auth",
                "jdk.unsupported",
                "jdk.crypto.ec"
            )

            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.AppImage
            )
            packageName = appName
            packageVersion = appVersion
            description = "Lune Music Player for Linux"
            vendor = appVendor
            linux {
                iconFile.set(project.file("src/main/resources/icons/icon.png"))
                appCategory = "AudioVideo;Audio;Player;Music;"
            }
        }
    }
}
