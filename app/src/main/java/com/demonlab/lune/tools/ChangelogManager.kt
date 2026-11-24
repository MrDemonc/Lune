package com.demonlab.lune.tools

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

object ChangelogManager {

    fun getChangelog(context: Context, versionCode: Int): List<String> {
        val fileName = "changelogs/$versionCode.txt"
        return try {
            context.assets.open(fileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                    lines
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .map { line ->
                            when {
                                line.startsWith("- ") -> line.removePrefix("- ").trim()
                                line.startsWith("• ") -> line.removePrefix("• ").trim()
                                line.startsWith("* ") -> line.removePrefix("* ").trim()
                                else -> line
                            }
                        }
                        .toList()
                }
            }
        } catch (e: Exception) {
            getLatestAvailableChangelog(context)
        }
    }

    private fun getLatestAvailableChangelog(context: Context): List<String> {
        return try {
            val files = context.assets.list("changelogs") ?: emptyArray()
            val latestCode = files
                .mapNotNull { it.removeSuffix(".txt").toIntOrNull() }
                .maxOrNull()
            if (latestCode != null) {
                context.assets.open("changelogs/$latestCode.txt").use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                        lines
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .map { line ->
                                when {
                                    line.startsWith("- ") -> line.removePrefix("- ").trim()
                                    line.startsWith("• ") -> line.removePrefix("• ").trim()
                                    line.startsWith("* ") -> line.removePrefix("* ").trim()
                                    else -> line
                                }
                            }
                            .toList()
                    }
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
