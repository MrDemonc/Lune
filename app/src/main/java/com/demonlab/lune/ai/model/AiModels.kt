package com.demonlab.lune.ai.model

import androidx.compose.ui.graphics.Color
import com.demonlab.lune.tools.Song
import java.util.Calendar

enum class TimeOfDay(val labelEs: String, val labelEn: String) {
    MORNING("Mañana", "Morning"),
    AFTERNOON("Tarde", "Afternoon"),
    EVENING("Atardecer", "Evening"),
    NIGHT("Noche", "Night");

    companion object {
        fun current(): TimeOfDay {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 5..11 -> MORNING
                in 12..17 -> AFTERNOON
                in 18..22 -> EVENING
                else -> NIGHT
            }
        }
    }
}

enum class DayType {
    WEEKDAY,
    WEEKEND;

    companion object {
        fun current(): DayType {
            val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            return if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) WEEKEND else WEEKDAY
        }
    }
}

enum class MixCategory {
    DAILY_FLOW,
    ENERGY,
    CHILL,
    DISCOVERY,
    FOCUS,
    ACOUSTIC_LIVE,
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
    var weekdayPlays: Int = 0,
    var weekendPlays: Int = 0,
    var isFavorite: Boolean = false,
    var playlistAddCount: Int = 0,
    var consecutiveSkips: Int = 0,
    var nextSongTransitions: MutableMap<Long, Int> = mutableMapOf()
) {
    fun calculateAffinityScore(): Float {
        var score = 10f // Base baseline
        score += (playCount * 1.5f)
        score += (fullCompletions * 3.5f)
        score += (repeatCount * 4.5f)
        score -= (fastSkips * 2.5f)
        score -= (consecutiveSkips * 3.0f)
        if (isFavorite) score += 9.0f
        score += (playlistAddCount * 3.5f)
        return score.coerceAtLeast(0.5f)
    }

    /**
     * Advanced dynamic score taking into account temporal context,
     * fatigue suppression, and recent novelty bonus.
     */
    fun calculateDynamicScore(now: Long, currentTime: TimeOfDay, isWeekend: Boolean): Float {
        val baseScore = calculateAffinityScore()
        
        // Time of day affinity factor (1.0x - 1.4x)
        val timeFactor = 1.0f + (getTimeAffinity(currentTime) * 0.40f)
        
        // Day type affinity factor
        val dayFactor = if (isWeekend) {
            val total = (weekdayPlays + weekendPlays).coerceAtLeast(1)
            1.0f + ((weekendPlays.toFloat() / total) * 0.25f)
        } else {
            val total = (weekdayPlays + weekendPlays).coerceAtLeast(1)
            1.0f + ((weekdayPlays.toFloat() / total) * 0.25f)
        }

        // Fatigue attenuation (if played very recently within 30 min, reduce score to avoid repetition)
        val timeSinceLastPlayed = now - lastPlayedTimestamp
        val fatigueMultiplier = when {
            lastPlayedTimestamp <= 0L -> 1.0f
            timeSinceLastPlayed < 30 * 60 * 1000L -> 0.20f // Played < 30 min ago
            timeSinceLastPlayed < 2 * 60 * 60 * 1000L -> 0.50f // Played < 2 hours ago
            timeSinceLastPlayed < 6 * 60 * 60 * 1000L -> 0.80f // Played < 6 hours ago
            else -> 1.0f
        }

        // Sweet-spot novelty bonus (played between 2 and 14 days ago)
        val noveltyBonus = if (timeSinceLastPlayed in (2L * 86400000L)..(14L * 86400000L)) {
            1.18f
        } else {
            1.0f
        }

        return (baseScore * timeFactor * dayFactor * fatigueMultiplier * noveltyBonus).coerceAtLeast(0.1f)
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

    fun recordTransitionTo(nextSongId: Long) {
        val count = nextSongTransitions[nextSongId] ?: 0
        nextSongTransitions[nextSongId] = count + 1
    }

    fun getTransitionProbabilityTo(nextSongId: Long): Float {
        if (nextSongTransitions.isEmpty()) return 0f
        val count = nextSongTransitions[nextSongId] ?: 0
        val total = nextSongTransitions.values.sum().coerceAtLeast(1)
        return count.toFloat() / total.toFloat()
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
