package com.demonlab.lune.data

import java.io.InputStream
import java.util.Properties

object AppConfig {
    private val properties = Properties()

    init {
        try {
            val stream: InputStream? = Thread.currentThread().contextClassLoader.getResourceAsStream("app.properties")
                ?: AppConfig::class.java.getResourceAsStream("/app.properties")
            if (stream != null) {
                properties.load(stream)
            }
        } catch (e: Exception) {
            println("AppConfig warning: Could not load app.properties: ${e.message}")
        }
    }

    val version: String
        get() = properties.getProperty("version", "1.0.0")

    val name: String
        get() = properties.getProperty("name", "Lune")

    val vendor: String
        get() = properties.getProperty("vendor", "DemonLab")

    val license: String
        get() = properties.getProperty("license", "GPLv3")
}
