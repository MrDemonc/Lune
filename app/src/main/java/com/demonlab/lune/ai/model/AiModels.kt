package com.demonlab.lune.ai.model

import androidx.compose.ui.graphics.Color
import com.demonlab.lune.tools.Song

enum class TimeOfDay(val labelEs: String, val labelEn: String) {
    MORNING("Mañana", "Morning"),
    AFTERNOON("Tarde", "Afternoon"),
    EVENING("Atardecer", "Evening"),
    NIGHT("Noche", "Night");

    companion object {
        fun current(): TimeOfDay {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 5..11 -> MORNING
                in 12..17 -> AFTERNOON
                in 18..22 -> EVENING
                else -> NIGHT
            }
        }
    }
}

enum class MixCategory {
    DAILY_FLOW,
    ENERGY,
    CHILL,
    FORGOTTEN_GEMS,
    TIME_OF_DAY,
    ARTIST_SPOTLIGHT,
    GENRE_DISCOVERY
}

data class SongInteraction(
    val songId: Long,
    var playCount: Int = 0,
    var fullCompletions: Int = 0,
    var fastSkips: Int = 0,
    var repeatCount: Int = 0,
    var lastPlayedTimestamp: Long = 0L,
    var morningPlays: Int = 0,
    var afternoonPlays: Int = 0,
    var eveningPlays: Int = 0,
    var nightPlays: Int = 0,
    var isFavorite: Boolean = false,
    var playlistAddCount: Int = 0
) {
    fun calculateAffinityScore(): Float {
        var score = 10f // Base baseline
        score += (playCount * 1.5f)
        score += (fullCompletions * 3.0f)
        score += (repeatCount * 4.0f)
        score -= (fastSkips * 2.5f)
        if (isFavorite) score += 8.0f
        score += (playlistAddCount * 3.0f)
        return score.coerceAtLeast(1f)
    }

    fun getTimeAffinity(timeOfDay: TimeOfDay): Float {
        val totalTimePlays = (morningPlays + afternoonPlays + eveningPlays + nightPlays).coerceAtLeast(1)
        val targetPlays = when (timeOfDay) {
            TimeOfDay.MORNING -> morningPlays
            TimeOfDay.AFTERNOON -> afternoonPlays
            TimeOfDay.EVENING -> eveningPlays
            TimeOfDay.NIGHT -> nightPlays
        }
        return (targetPlays.toFloat() / totalTimePlays.toFloat())
    }
}

data class AiMix(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: MixCategory,
    val songs: List<Song>,
    val gradientColors: List<Color>,
    val iconName: String = "auto_awesome",
    val description: String = ""
)
