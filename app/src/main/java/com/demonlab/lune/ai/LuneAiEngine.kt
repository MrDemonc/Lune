package com.demonlab.lune.ai

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.demonlab.lune.R
import com.demonlab.lune.ai.model.AiMix
import com.demonlab.lune.ai.model.DayType
import com.demonlab.lune.ai.model.MixCategory
import com.demonlab.lune.ai.model.TimeOfDay
import com.demonlab.lune.ai.storage.AiTelemetryStorage
import com.demonlab.lune.tools.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

class LuneAiEngine private constructor(private val context: Context) {
    private val storage = AiTelemetryStorage(context)
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _aiMixes = MutableStateFlow<List<AiMix>>(emptyList())
    val aiMixes: StateFlow<List<AiMix>> = _aiMixes.asStateFlow()

    private var lastStartedSongId: Long? = null
    private var lastStartedTimeMs: Long = 0L
    private var previousPlayedSongId: Long? = null

    companion object {
        @Volatile
        private var INSTANCE: LuneAiEngine? = null

        fun getInstance(context: Context): LuneAiEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LuneAiEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 1. TELEMETRY & HABIT LEARNING
    // ─────────────────────────────────────────────────────────────

    fun onSongStarted(song: Song) {
        val now = System.currentTimeMillis()
        val timeOfDay = TimeOfDay.current()
        val isWeekend = DayType.current() == DayType.WEEKEND

        // Record Markov transition from previous track if applicable
        previousPlayedSongId?.let { prevId ->
            if (prevId != song.id && (now - lastStartedTimeMs > 40_000L)) {
                storage.recordInteraction(prevId) { prevInteraction ->
                    prevInteraction.recordTransitionTo(song.id)
                }
            }
        }

        // Check for immediate repeat loop
        val isReplay = lastStartedSongId == song.id && (now - lastStartedTimeMs < 35_000L)
        previousPlayedSongId = lastStartedSongId
        lastStartedSongId = song.id
        lastStartedTimeMs = now

        storage.recordInteraction(song.id) { interaction ->
            interaction.playCount++
            interaction.lastPlayedTimestamp = now
            if (isReplay) {
                interaction.repeatCount++
            }
            if (isWeekend) {
                interaction.weekendPlays++
            } else {
                interaction.weekdayPlays++
            }
            when (timeOfDay) {
                TimeOfDay.MORNING -> interaction.morningPlays++
                TimeOfDay.AFTERNOON -> interaction.afternoonPlays++
                TimeOfDay.EVENING -> interaction.eveningPlays++
                TimeOfDay.NIGHT -> interaction.nightPlays++
            }
            // Reset consecutive skips on clean start
            if (interaction.consecutiveSkips > 0) {
                interaction.consecutiveSkips = 0
            }
        }
    }

    fun onSongCompleted(song: Song) {
        storage.recordInteraction(song.id) { interaction ->
            interaction.fullCompletions++
            interaction.consecutiveSkips = 0
        }
    }

    fun onSongSkipped(song: Song, playedSeconds: Long, totalDurationSeconds: Long) {
        val isFastSkip = playedSeconds < 20 || (totalDurationSeconds > 0 && (playedSeconds.toFloat() / totalDurationSeconds.toFloat()) < 0.15f)
        if (isFastSkip) {
            storage.recordInteraction(song.id) { interaction ->
                interaction.fastSkips++
                interaction.consecutiveSkips++
            }
        }
    }

    fun onSongFavoriteToggled(song: Song, isFavorite: Boolean) {
        storage.recordInteraction(song.id) { interaction ->
            interaction.isFavorite = isFavorite
        }
    }

    fun onSongAddedToPlaylist(songId: Long) {
        storage.recordInteraction(songId) { interaction ->
            interaction.playlistAddCount++
        }
    }

    fun getSongAffinity(songId: Long): Float {
        val now = System.currentTimeMillis()
        val currentTime = TimeOfDay.current()
        val isWeekend = DayType.current() == DayType.WEEKEND
        return storage.getInteraction(songId).calculateDynamicScore(now, currentTime, isWeekend)
    }

    // ─────────────────────────────────────────────────────────────
    // 2. MUSICAL HEURISTICS & ACOUSTIC PROFILING
    // ─────────────────────────────────────────────────────────────

    data class AudioProfile(
        val energyScore: Float,       // 0.0 (calm/chill) to 1.0 (energetic/fast)
        val acousticScore: Float,     // 0.0 (electronic/produced) to 1.0 (raw acoustic)
        val isLiveOrAcoustic: Boolean,
        val isFocusFriendly: Boolean
    )

    fun profileSong(song: Song): AudioProfile {
        val titleLower = song.title.lowercase(Locale.getDefault())
        val genreLower = (song.genre ?: "").lowercase(Locale.getDefault())

        var energy = 0.50f
        var acoustic = 0.30f
        var isLiveOrAcoustic = false

        // Acoustic / Live heuristics
        if (titleLower.contains("acoustic") || titleLower.contains("acústic") || 
            titleLower.contains("unplugged") || titleLower.contains("piano version") ||
            genreLower.contains("acoustic") || genreLower.contains("folk")) {
            acoustic += 0.50f
            energy -= 0.25f
            isLiveOrAcoustic = true
        }

        if (titleLower.contains("live") || titleLower.contains("en vivo") || 
            titleLower.contains("en directo") || titleLower.contains("session")) {
            isLiveOrAcoustic = true
        }

        // High Energy heuristics
        if (titleLower.contains("remix") || titleLower.contains("extended") || 
            titleLower.contains("club") || titleLower.contains("speed up") ||
            genreLower.contains("rock") || genreLower.contains("metal") ||
            genreLower.contains("dance") || genreLower.contains("edm") ||
            genreLower.contains("electronic") || genreLower.contains("trap") ||
            genreLower.contains("hardcore")) {
            energy += 0.35f
            acoustic -= 0.20f
        }

        // Low Energy / Chill heuristics
        if (titleLower.contains("lofi") || titleLower.contains("lo-fi") ||
            titleLower.contains("slowed") || titleLower.contains("ambient") ||
            titleLower.contains("instrumental") || genreLower.contains("ambient") ||
            genreLower.contains("classical") || genreLower.contains("jazz") ||
            genreLower.contains("chill")) {
            energy -= 0.30f
            acoustic += 0.25f
        }

        // Duration dynamics
        if (song.duration > 300_000L) { // > 5 min usually more progressive/chill
            energy -= 0.08f
        } else if (song.duration in 120_000L..210_000L) { // 2-3.5 min usually high tempo radio hits
            energy += 0.08f
        }

        energy = energy.coerceIn(0.05f, 0.98f)
        acoustic = acoustic.coerceIn(0.05f, 0.98f)

        val isFocus = acoustic > 0.40f || energy in 0.20f..0.55f || titleLower.contains("instrumental") || titleLower.contains("lo-fi")

        return AudioProfile(
            energyScore = energy,
            acousticScore = acoustic,
            isLiveOrAcoustic = isLiveOrAcoustic,
            isFocusFriendly = isFocus
        )
    }

    private fun computeSongSimilarity(a: Song, b: Song): Float {
        if (a.id == b.id) return 1.0f

        var similarity = 0.0f

        // Same artist bonus
        if (a.artist.equals(b.artist, ignoreCase = true)) {
            similarity += 0.42f
        }

        // Same or compatible genre bonus
        val genreA = a.genre?.trim()?.lowercase(Locale.getDefault())
        val genreB = b.genre?.trim()?.lowercase(Locale.getDefault())
        if (!genreA.isNullOrEmpty() && !genreB.isNullOrEmpty()) {
            if (genreA == genreB) {
                similarity += 0.35f
            } else if (isCompatibleGenre(genreA, genreB)) {
                similarity += 0.22f
            }
        }

        // Audio profile harmonic distance
        val profileA = profileSong(a)
        val profileB = profileSong(b)
        val energyDelta = abs(profileA.energyScore - profileB.energyScore)
        val acousticDelta = abs(profileA.acousticScore - profileB.acousticScore)
        val profileHarmony = 1.0f - ((energyDelta * 0.6f) + (acousticDelta * 0.4f))
        similarity += (profileHarmony * 0.23f).coerceIn(0f, 0.23f)

        // Markov transition learned bonus
        val transitionWeight = storage.getInteraction(a.id).getTransitionProbabilityTo(b.id)
        if (transitionWeight > 0f) {
            similarity += (transitionWeight * 0.20f).coerceIn(0f, 0.20f)
        }

        return similarity.coerceIn(0f, 1f)
    }

    private fun isCompatibleGenre(g1: String, g2: String): Boolean {
        val rockFamily = setOf("rock", "alternative", "indie", "metal", "punk", "grunge", "hard rock")
        val popFamily = setOf("pop", "dance", "electropop", "synthpop", "indie pop", "disco")
        val electronicFamily = setOf("electronic", "edm", "house", "techno", "synthwave", "ambient", "trance")
        val urbanFamily = setOf("hip hop", "rap", "trap", "r&b", "soul", "reggaeton", "urban")
        val chillFamily = setOf("acoustic", "folk", "classical", "instrumental", "ambient", "lo-fi", "chill", "jazz")

        return (g1 in rockFamily && g2 in rockFamily) ||
               (g1 in popFamily && g2 in popFamily) ||
               (g1 in electronicFamily && g2 in electronicFamily) ||
               (g1 in urbanFamily && g2 in urbanFamily) ||
               (g1 in chillFamily && g2 in chillFamily)
    }

    // ─────────────────────────────────────────────────────────────
    // 3. SMART SHUFFLE (HARMONIC FLOW REORDERING)
    // ─────────────────────────────────────────────────────────────

    fun generateSmartShuffle(songs: List<Song>, startingSong: Song? = null): List<Song> {
        if (songs.size <= 2) return songs

        val remaining = songs.toMutableList()
        val result = mutableListOf<Song>()

        val seed = startingSong?.let { s -> remaining.find { it.id == s.id } }
            ?: remaining.maxByOrNull { getSongAffinity(it.id) }
            ?: remaining.first()

        result.add(seed)
        remaining.remove(seed)

        var current = seed
        val random = Random.Default

        while (remaining.isNotEmpty()) {
            val scoredCandidates = remaining.map { candidate ->
                val similarity = computeSongSimilarity(current, candidate)
                val affinity = getSongAffinity(candidate.id)
                val sameArtistPenalty = if (candidate.artist.equals(current.artist, ignoreCase = true)) -0.35f else 0f
                val noise = random.nextFloat() * 0.08f

                val finalScore = (similarity * 0.48f) + ((affinity / 45f).coerceIn(0f, 0.44f)) + sameArtistPenalty + noise
                candidate to finalScore
            }.sortedByDescending { it.second }

            val topSliceSize = minOf(3, scoredCandidates.size)
            val selected = scoredCandidates.take(topSliceSize).random(random).first

            result.add(selected)
            remaining.remove(selected)
            current = selected
        }

        return result
    }

    // ─────────────────────────────────────────────────────────────
    // 4. DISCOVERY MODE GENERATION (EXPLORING UNHEARD GEMS)
    // ─────────────────────────────────────────────────────────────

    fun generateDiscoveryQueue(allSongs: List<Song>, seedSong: Song? = null): List<Song> {
        if (allSongs.isEmpty()) return emptyList()

        // Partition songs into favorites/familiar vs unexplored
        val familiar = allSongs.filter { getSongAffinity(it.id) >= 12f }
        val unexplored = allSongs.filter { 
            val interaction = storage.getInteraction(it.id)
            interaction.playCount <= 1 && !interaction.isFavorite
        }

        if (unexplored.isEmpty()) {
            return generateSmartShuffle(allSongs, seedSong)
        }

        val seed = seedSong ?: familiar.maxByOrNull { getSongAffinity(it.id) } ?: allSongs.first()
        val result = mutableListOf<Song>(seed)

        // Find unexplored tracks that have high similarity to user's favorite tracks
        val rankedUnexplored = unexplored.map { candidate ->
            val maxSimToFavorites = familiar.maxOfOrNull { computeSongSimilarity(it, candidate) } ?: 0.3f
            candidate to maxSimToFavorites
        }.sortedByDescending { it.second }

        val familiarPool = (familiar - seed).toMutableList()
        val unexploredPool = rankedUnexplored.map { it.first }.toMutableList()

        // Interleave: 2 familiar tracks -> 1 high-confidence discovery track
        var flip = 0
        while (familiarPool.isNotEmpty() || unexploredPool.isNotEmpty()) {
            if (flip % 3 == 2 && unexploredPool.isNotEmpty()) {
                val nextDisc = unexploredPool.removeAt(0)
                result.add(nextDisc)
            } else if (familiarPool.isNotEmpty()) {
                val last = result.last()
                val nextFam = familiarPool.maxByOrNull { computeSongSimilarity(last, it) } ?: familiarPool.first()
                familiarPool.remove(nextFam)
                result.add(nextFam)
            } else if (unexploredPool.isNotEmpty()) {
                result.add(unexploredPool.removeAt(0))
            }
            flip++
        }

        return result
    }

    // ─────────────────────────────────────────────────────────────
    // 5. SMART NEXT PREDICTION (FOR AUTO-PLAY & INFINITE RADIO)
    // ─────────────────────────────────────────────────────────────

    fun predictNextSong(currentSong: Song, history: List<Song>, pool: List<Song>): Song? {
        val eligible = pool.filter { candidate ->
            candidate.id != currentSong.id && history.none { it.id == candidate.id }
        }
        if (eligible.isEmpty()) return pool.filter { it.id != currentSong.id }.randomOrNull()

        val recentArtists = history.takeLast(3).map { it.artist.lowercase(Locale.getDefault()) }

        return eligible.maxByOrNull { candidate ->
            val similarity = computeSongSimilarity(currentSong, candidate)
            val affinity = getSongAffinity(candidate.id)
            val isRecentArtist = candidate.artist.lowercase(Locale.getDefault()) in recentArtists
            val artistPenalty = if (isRecentArtist) -0.45f else 0f
            val markovBonus = storage.getInteraction(currentSong.id).getTransitionProbabilityTo(candidate.id) * 0.30f

            (similarity * 0.45f) + ((affinity / 40f).coerceIn(0f, 0.40f)) + artistPenalty + markovBonus
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 6. DEEP AI MIX GENERATION (EXPANDED DIVERSITY)
    // ─────────────────────────────────────────────────────────────

    fun refreshMixes(allSongs: List<Song>) {
        if (allSongs.isEmpty()) return

        engineScope.launch {
            val generated = mutableListOf<AiMix>()
            val currentTime = TimeOfDay.current()
            val dayType = DayType.current()
            val isWeekend = dayType == DayType.WEEKEND

            // 1. Daily AI Flow (Mix del Día)
            val dailySongs = allSongs
                .sortedByDescending { getSongAffinity(it.id) }
                .take(32)
            if (dailySongs.size >= 4) {
                generated.add(
                    AiMix(
                        id = "ai_daily_flow",
                        title = context.getString(R.string.ai_mix_daily_title),
                        subtitle = context.getString(R.string.ai_mix_daily_sub),
                        category = MixCategory.DAILY_FLOW,
                        songs = generateSmartShuffle(dailySongs),
                        gradientColors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899)),
                        iconName = "auto_awesome",
                        description = "Generado combinando afinidad en tiempo real, hábitos horarios y transiciones armónicas."
                    )
                )
            }

            // 2. Modo Descubrimiento (Deep Discovery Mix)
            val discoverySongs = generateDiscoveryQueue(allSongs).take(28)
            if (discoverySongs.size >= 4) {
                generated.add(
                    AiMix(
                        id = "ai_discovery_flow",
                        title = context.getString(R.string.ai_mix_discovery_title),
                        subtitle = context.getString(R.string.ai_mix_discovery_sub),
                        category = MixCategory.DISCOVERY,
                        songs = discoverySongs,
                        gradientColors = listOf(Color(0xFF0D9488), Color(0xFF059669), Color(0xFF10B981)),
                        iconName = "explore",
                        description = "Explora canciones de tu biblioteca con alta compatibilidad matemática con tus gustos."
                    )
                )
            }

            // 3. Energy Boost (Ritmo y Cardio)
            val energySongs = allSongs.filter { song ->
                val profile = profileSong(song)
                profile.energyScore > 0.62f || (song.duration < 230_000L && getSongAffinity(song.id) > 8f)
            }.sortedByDescending { getSongAffinity(it.id) }.take(25)

            if (energySongs.size >= 4) {
                generated.add(
                    AiMix(
                        id = "ai_energy_boost",
                        title = context.getString(R.string.ai_mix_energy_title),
                        subtitle = context.getString(R.string.ai_mix_energy_sub),
                        category = MixCategory.ENERGY,
                        songs = generateSmartShuffle(energySongs),
                        gradientColors = listOf(Color(0xFFF97316), Color(0xFFEF4444), Color(0xFFEC4899)),
                        iconName = "bolt",
                        description = "Selección de canciones con pulsos vivos y tempos energéticos según tu biblioteca."
                    )
                )
            }

            // 4. Chill & Focus / Relajación
            val chillSongs = allSongs.filter { song ->
                val profile = profileSong(song)
                profile.energyScore < 0.45f || profile.isFocusFriendly
            }.sortedByDescending { getSongAffinity(it.id) }.take(25)

            if (chillSongs.size >= 4) {
                generated.add(
                    AiMix(
                        id = "ai_chill_flow",
                        title = context.getString(R.string.ai_mix_chill_title),
                        subtitle = context.getString(R.string.ai_mix_chill_sub),
                        category = MixCategory.CHILL,
                        songs = generateSmartShuffle(chillSongs),
                        gradientColors = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF6366F1)),
                        iconName = "spa",
                        description = "Sonidos acústicos y texturas pausadas para momentos de tranquilidad."
                    )
                )
            }

            // 5. Contexto Temporal & Fin de Semana
            val timeName = when {
                isWeekend -> context.getString(R.string.ai_mix_weekend_title)
                currentTime == TimeOfDay.MORNING -> context.getString(R.string.ai_mix_morning_title)
                currentTime == TimeOfDay.AFTERNOON -> context.getString(R.string.ai_mix_afternoon_title)
                currentTime == TimeOfDay.EVENING -> context.getString(R.string.ai_mix_evening_title)
                else -> context.getString(R.string.ai_mix_night_title)
            }
            val timeSub = when {
                isWeekend -> context.getString(R.string.ai_mix_weekend_sub)
                currentTime == TimeOfDay.MORNING -> context.getString(R.string.ai_mix_morning_sub)
                currentTime == TimeOfDay.AFTERNOON -> context.getString(R.string.ai_mix_afternoon_sub)
                currentTime == TimeOfDay.EVENING -> context.getString(R.string.ai_mix_evening_sub)
                else -> context.getString(R.string.ai_mix_night_sub)
            }
            val timeGradients = when (currentTime) {
                TimeOfDay.MORNING -> listOf(Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF06B6D4))
                TimeOfDay.AFTERNOON -> listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFF06B6D4))
                TimeOfDay.EVENING -> listOf(Color(0xFFF97316), Color(0xFF8B5CF6), Color(0xFF4F46E5))
                TimeOfDay.NIGHT -> listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4C1D95))
            }

            val timeSongs = allSongs.sortedByDescending { song ->
                val interaction = storage.getInteraction(song.id)
                interaction.calculateDynamicScore(System.currentTimeMillis(), currentTime, isWeekend)
            }.take(25)

            if (timeSongs.size >= 4) {
                generated.add(
                    AiMix(
                        id = "ai_time_mix",
                        title = timeName,
                        subtitle = timeSub,
                        category = MixCategory.TIME_OF_DAY,
                        songs = generateSmartShuffle(timeSongs),
                        gradientColors = timeGradients,
                        iconName = if (currentTime == TimeOfDay.NIGHT) "bedtime" else "wb_sunny",
                        description = "Adaptado al contexto horario actual con base en tus hábitos pasados."
                    )
                )
            }

            // 6. Joyas Olvidadas (Forgotten Gems)
            val now = System.currentTimeMillis()
            val twoWeeksAgo = now - (14L * 24L * 60L * 60L * 1000L)
            val forgottenSongs = allSongs.filter { song ->
                val interaction = storage.getInteraction(song.id)
                (interaction.isFavorite || interaction.playCount >= 3) &&
                (interaction.lastPlayedTimestamp in 1..twoWeeksAgo || interaction.lastPlayedTimestamp == 0L)
            }.shuffled().take(22)

            if (forgottenSongs.size >= 4) {
                generated.add(
                    AiMix(
                        id = "ai_forgotten_gems",
                        title = context.getString(R.string.ai_mix_forgotten_title),
                        subtitle = context.getString(R.string.ai_mix_forgotten_sub),
                        category = MixCategory.FORGOTTEN_GEMS,
                        songs = forgottenSongs,
                        gradientColors = listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF047857)),
                        iconName = "history",
                        description = "Redescubre temas con alta puntuación en tu historial que han quedado atrás."
                    )
                )
            }

            // 7. Sesiones Acústicas / En Directo (si existen en la biblioteca)
            val acousticLiveSongs = allSongs.filter { profileSong(it).isLiveOrAcoustic }
            if (acousticLiveSongs.size >= 4) {
                generated.add(
                    AiMix(
                        id = "ai_acoustic_live",
                        title = context.getString(R.string.ai_mix_acoustic_title),
                        subtitle = context.getString(R.string.ai_mix_acoustic_sub),
                        category = MixCategory.ACOUSTIC_LIVE,
                        songs = generateSmartShuffle(acousticLiveSongs),
                        gradientColors = listOf(Color(0xFFD97706), Color(0xFFB45309), Color(0xFF78350F)),
                        iconName = "mic_external_on",
                        description = "Detección inteligente de pistas acústicas y grabaciones en vivo."
                    )
                )
            }

            // 8. Radio de Artista Destacado (Top Artist Spotlight)
            val topArtist = allSongs
                .groupBy { it.artist }
                .filter { it.key.isNotBlank() && it.value.size >= 2 }
                .maxByOrNull { entry ->
                    entry.value.sumOf { getSongAffinity(it.id).toDouble() }
                }

            if (topArtist != null) {
                val artistSongs = topArtist.value
                val kindredSongs = allSongs.filter { it.artist != topArtist.key && computeSongSimilarity(artistSongs.first(), it) > 0.40f }
                val spotlightPool = (artistSongs + kindredSongs.take(15)).distinctBy { it.id }

                if (spotlightPool.size >= 4) {
                    generated.add(
                        AiMix(
                            id = "ai_artist_${topArtist.key.hashCode()}",
                            title = context.getString(R.string.ai_mix_artist_radio_title, topArtist.key),
                            subtitle = context.getString(R.string.ai_mix_artist_radio_sub, topArtist.key),
                            category = MixCategory.ARTIST_SPOTLIGHT,
                            songs = generateSmartShuffle(spotlightPool),
                            gradientColors = listOf(Color(0xFFE11D48), Color(0xFFBE123C), Color(0xFF881337)),
                            iconName = "person",
                            description = "Sesión continua generada en torno a uno de tus artistas con mayor afinidad."
                        )
                    )
                }
            }

            _aiMixes.value = generated
        }
    }
}
