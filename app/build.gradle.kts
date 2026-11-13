import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.Properties
import groovy.json.JsonSlurper


val keystorePropertiesFile = layout.projectDirectory.file("../keystore.properties").asFile
val keystoreProperties = Properties()

if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.demonlab.lune"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.demonlab.lune"
        minSdk = 24
        targetSdk = 37
        versionCode = 15
        versionName = "1.5.7"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val storePath = keystoreProperties.getProperty("storeFile")
    val storePassword = keystoreProperties.getProperty("storePassword")
    val keyAlias = keystoreProperties.getProperty("keyAlias")
    val keyPassword = keystoreProperties.getProperty("keyPassword")

    val keystoreFile = storePath?.takeIf { it.isNotBlank() }?.let {
        rootProject.file(it).takeIf { f -> f.exists() && f.length() > 0 }
            ?: file(it).takeIf { f -> f.exists() && f.length() > 0 }
    }

    val isSigningConfigured = keystoreFile != null &&
        !storePassword.isNullOrBlank() &&
        !keyAlias.isNullOrBlank() &&
        !keyPassword.isNullOrBlank()

    signingConfigs {
        create("release") {
            if (isSigningConfigured) {
                storeFile = keystoreFile
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (isSigningConfigured) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    packaging {
        resources {
            excludes.add("**/baseline.prof")
            excludes.add("**/baseline.profm")
        }
        jniLibs {
            keepDebugSymbols.add("**/libandroidx.graphics.path.so")
        }
    }
}

base {
    archivesName = "Lune"
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.media)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.animation.graphics)
    implementation(libs.coil.compose)
    implementation(libs.androidx.documentfile)

    implementation(libs.gson)
    implementation(libs.jaudiotagger)
    implementation(libs.androidx.room3.runtime)
    ksp(libs.androidx.room3.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

androidComponents {
    onVariants { variant ->
        val variantName = variant.name
        val capitalizedName = variantName.replaceFirstChar { it.uppercase() }

        tasks.matching {
            it.name == "compile${capitalizedName}ArtProfile" ||
                    it.name == "merge${capitalizedName}ArtProfile"
        }.configureEach {
            enabled = false
        }
    }
}

val copyChangelogs = tasks.register("copyChangelogs") {
    val changelogDir = rootProject.file("fastlane/metadata/android/en-US/changelog")
    val assetsChangelogDir = layout.projectDirectory.dir("src/main/assets/changelogs").asFile

    inputs.dir(changelogDir)
    outputs.dir(assetsChangelogDir)

    doLast {
        val latestFile = changelogDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".txt") }
            ?.mapNotNull { file ->
                file.name.removeSuffix(".txt").toIntOrNull()?.let { code -> code to file }
            }
            ?.maxByOrNull { it.first }
            ?.second

        assetsChangelogDir.mkdirs()
        assetsChangelogDir.listFiles()?.forEach { file ->
            if (latestFile == null || file.name != latestFile.name) {
                file.delete()
            }
        }

        if (latestFile != null) {
            latestFile.copyTo(File(assetsChangelogDir, latestFile.name), overwrite = true)
        }
    }
}

val updateContributors = tasks.register("updateContributors") {
    val contributorsFile = layout.projectDirectory.file("src/main/assets/contributors.json").asFile
    val avatarsDir = layout.projectDirectory.dir("src/main/assets/avatars").asFile
    outputs.file(contributorsFile)
    outputs.dir(avatarsDir)

    doLast {
        try {
            val url = URI("https://api.github.com/repos/MrDemonc/Lune/contributors").toURL()
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Lune-Android-Build")
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                contributorsFile.parentFile.mkdirs()
                contributorsFile.writeText(responseText)
                println("Successfully updated contributors from GitHub (${contributorsFile.length()} bytes)")

                avatarsDir.mkdirs()
                val parsed = JsonSlurper().parseText(responseText) as? List<*>
                parsed?.filterIsInstance<Map<String, *>>()?.forEach { userMap ->
                    val login = userMap["login"] as? String ?: return@forEach
                    val avatarUrl = userMap["avatar_url"] as? String ?: return@forEach
                    val targetFile = File(avatarsDir, "$login.png")
                    if (!targetFile.exists()) {
                        try {
                            val imgConn = URI(avatarUrl).toURL().openConnection() as HttpURLConnection
                            imgConn.setRequestProperty("User-Agent", "Lune-Android-Build")
                            imgConn.connectTimeout = 5000
                            imgConn.readTimeout = 5000
                            if (imgConn.responseCode == 200) {
                                targetFile.outputStream().use { out ->
                                    imgConn.inputStream.copyTo(out)
                                }
                                println("Downloaded avatar for contributor: $login")
                            }
                        } catch (e: Exception) {
                            println("Could not download avatar for $login: ${e.message}")
                        }
                    }
                }
            } else {
                println("Could not update contributors (HTTP ${connection.responseCode}). Keeping existing cached contributors.")
            }
        } catch (e: Exception) {
            println("Skipping contributors update (offline or network error: ${e.message}). Keeping existing cached contributors.")
        }
    }
}

tasks.named("preBuild") {
    dependsOn(copyChangelogs, updateContributors)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
