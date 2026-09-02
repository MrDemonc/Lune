package com.demonlab.lune.ai

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.demonlab.lune.ai.model.AiMix
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
import kotlin.random.Random
import kotlin.math.abs
import kotlin.math.sqrt

class LuneAiEngine private constructor(private val context: Context) {
    private val storage = AiTelemetryStorage(context)
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _aiMixes = MutableStateFlow<List<AiMix>>(emptyList())
    val aiMixes: StateFlow<List<AiMix>> = _aiMixes.asStateFlow()

    private var lastStartedSongId: Long? = null
    private var lastStartedTimeMs: Long = 0L

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
    // 1. TELEMETRY RECORDING & HABIT LEARNING
    // ─────────────────────────────────────────────────────────────

    fun onSongStarted(song: Song) {
        val now = System.currentTimeMillis()
        val timeOfDay = TimeOfDay.current()

        // Check for immediate replay
        val isReplay = lastStartedSongId == song.id && (now - lastStartedTimeMs < 30_000L)
        lastStartedSongId = song.id
        lastStartedTimeMs = now

        storage.recordInteraction(song.id) { interaction ->
            interaction.playCount++
            interaction.lastPlayedTimestamp = now
            if (isReplay) {
                interaction.repeatCount++
            }
            when (timeOfDay) {
                TimeOfDay.MORNING -> interaction.morningPlays++
                TimeOfDay.AFTERNOON -> interaction.afternoonPlays++
                TimeOfDay.EVENING -> interaction.eveningPlays++
                TimeOfDay.NIGHT -> interaction.nightPlays++
            }
        }
    }

    fun onSongCompleted(song: Song) {
        storage.recordInteraction(song.id) { interaction ->
            interaction.fullCompletions++
        }
    }

    fun onSongSkipped(song: Song, playedSeconds: Long, totalDurationSeconds: Long) {
        // A skip under 20 seconds or under 15% of the track is considered a fast skip
        val isFastSkip = playedSeconds < 20 || (totalDurationSeconds > 0 && (playedSeconds.toFloat() / totalDurationSeconds.toFloat()) < 0.15f)
        if (isFastSkip) {
            storage.recordInteraction(song.id) { interaction ->
                interaction.fastSkips++
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
        return storage.getInteraction(songId).calculateAffinityScore()
    }

    // ─────────────────────────────────────────────────────────────
    // 2. MUSICAL FEATURE EXTRACTION & SIMILARITY
    // ─────────────────────────────────────────────────────────────

    private data class SongFeatureVector(
        val genreHash: Int,
        val artistHash: Int,
        val durationNormalized: Float,
        val affinityScore: Float
    )

    private fun extractVector(song: Song): SongFeatureVector {
        val interaction = storage.getInteraction(song.id)
        val genre = song.genre?.trim()?.lowercase(Locale.getDefault()) ?: "general"
        val artist = song.artist.trim().lowercase(Locale.getDefault())
        val durationNorm = (song.duration.toFloat() / 300_000f).coerceIn(0.2f, 2.0f) // Normalized to 5 min base
        val affinity = interaction.calculateAffinityScore()

        return SongFeatureVector(
            genreHash = genre.hashCode(),
            artistHash = artist.hashCode(),
            durationNormalized = durationNorm,
            affinityScore = affinity
        )
    }

    private fun computeSongSimilarity(a: Song, b: Song): Float {
        if (a.id == b.id) return 1.0f

        var similarity = 0.0f

        // Same artist bonus
        if (a.artist.equals(b.artist, ignoreCase = true)) {
            similarity += 0.45f
        }

        // Same or compatible genre bonus
        val genreA = a.genre?.trim()?.lowercase(Locale.getDefault())
        val genreB = b.genre?.trim()?.lowercase(Locale.getDefault())
        if (!genreA.isNullOrEmpty() && !genreB.isNullOrEmpty()) {
            if (genreA == genreB) {
                similarity += 0.40f
            } else if (isCompatibleGenre(genreA, genreB)) {
                similarity += 0.25f
            }
        }

        // Duration / pacing similarity
        val durationDiff = abs(a.duration - b.duration).toFloat() / 60_000f
        val durationFactor = (1.0f - (durationDiff * 0.15f)).coerceIn(0f, 0.15f)
        similarity += durationFactor

        return similarity.coerceIn(0f, 1f)
    }

    private fun isCompatibleGenre(g1: String, g2: String): Boolean {
        val rockFamily = setOf("rock", "alternative", "indie", "metal", "punk", "grunge", "hard rock")
        val popFamily = setOf("pop", "dance", "electropop", "synthpop", "indie pop", "disco")
        val electronicFamily = setOf("electronic", "edm", "house", "techno", "synthwave", "ambient", "trance")
        val urbanFamily = setOf("hip hop", "rap", "trap", "r&b", "soul", "reggaeton", "urban")
        val chillFamily = setOf("acoustic", "folk", "classical", "instrumental", "ambient", "lo-fi", "chill")

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

        // Pick starting track (current song or highest affinity seed)
        val seed = startingSong?.let { s -> remaining.find { it.id == s.id } }
            ?: remaining.maxByOrNull { getSongAffinity(it.id) }
            ?: remaining.first()

        result.add(seed)
        remaining.remove(seed)

        var current = seed
        val random = Random.Default

        while (remaining.isNotEmpty()) {
            // Find top candidate matching harmonic flow
            // Criteria: High similarity to current, high user affinity, no immediate artist repeat
            val scoredCandidates = remaining.map { candidate ->
                val similarity = computeSongSimilarity(current, candidate)
                val affinity = getSongAffinity(candidate.id)
                val sameArtistPenalty = if (candidate.artist.equals(current.artist, ignoreCase = true)) -0.35f else 0f
                val noise = random.nextFloat() * 0.10f // Subtle non-determinism for variety

                val finalScore = (similarity * 0.45f) + ((affinity / 50f).coerceIn(0f, 0.45f)) + sameArtistPenalty + noise
                candidate to finalScore
            }.sortedByDescending { it.second }

            // Pick from top 3 candidates with soft probability
            val topSliceSize = minOf(3, scoredCandidates.size)
            val selected = scoredCandidates.take(topSliceSize).random(random).first

            result.add(selected)
            remaining.remove(selected)
            current = selected
        }

        return result
    }

    // ─────────────────────────────────────────────────────────────
    // 4. SMART NEXT PREDICTION (FOR AUTO-PLAY & INFINITE RADIO)
    // ─────────────────────────────────────────────────────────────

    fun predictNextSong(currentSong: Song, history: List<Song>, pool: List<Song>): Song? {
        val eligible = pool.filter { candidate ->
            candidate.id != currentSong.id && history.none { it.id == candidate.id }
        }
        if (eligible.isEmpty()) return pool.filter { it.id != currentSong.id }.randomOrNull()

        val recentArtistIds = history.takeLast(3).map { it.artist.lowercase(Locale.getDefault()) }

        return eligible.maxByOrNull { candidate ->
            val similarity = computeSongSimilarity(currentSong, candidate)
            val affinity = getSongAffinity(candidate.id)
            val isRecentArtist = candidate.artist.lowercase(Locale.getDefault()) in recentArtistIds
            val artistPenalty = if (isRecentArtist) -0.40f else 0f

            (similarity * 0.50f) + ((affinity / 40f).coerceIn(0f, 0.40f)) + artistPenalty
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 5. AI MIX GENERATION
    // ─────────────────────────────────────────────────────────────

    fun refreshMixes(allSongs: List<Song>) {
        if (allSongs.isEmpty()) return

        engineScope.launch {
            val generated = mutableListOf<AiMix>()
            val currentTime = TimeOfDay.current()

            // 1. Daily AI Flow (Mix del Día)
            val dailySongs = allSongs
                .sortedByDescending { getSongAffinity(it.id) }
                .take(30)
            if (dailySongs.size >= 4) {
                generated.add(
                    AiMix(
                        id = "ai_daily_flow",
                        title = "Mix del Día",
                        subtitle = "Tus canciones esenciales con transiciones fluidas",
                        category = MixCategory.DAILY_FLOW,
                        songs = generateSmartShuffle(dailySongs),
                        gradientColors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899)),
                        iconName = "auto_awesome",
                        description = "Generado por IA combinando tus hábitos de escucha recientes y canciones más afines."
                    )
                )
            }

            // 2. Energy Boost (Ritmo y Enfoque Activo)
            val energySongs = allSongs.filter { song ->
                val genre = song.genre?.lowercase(Locale.getDefault()) ?: ""
                val isUpbeatGenre = genre.contains("rock") || genre.contains("dance") || 
                                    genre.contains("pop") || genre.contains("metal") ||
                                    genre.contains("electronic") || genre.contains("trap") ||
                                    genre.contains("hip hop")
                isUpbeatGenre || (song.duration < 240_000L && getSongAffinity(song.id) > 8f)
            }.sortedByDescending { getSongAffinity(it.id) }.take(25)

            if (energySongs.size >= 4) {
                generated.add(
                    AiMix(
                        id = "ai_energy_boost",
                        title = "Energy Boost",
                        subtitle = "Ritmos dinámicos para motivarte y entrenar",
                        category = MixCategory.ENERGY,
                        songs = generateSmartShuffle(energySongs),
                        gradientColors = listOf(Color(0xFFF97316), Color(0xFFEF4444), Color(0xFFEC4899)),
                        iconName = "bolt",
                        description = "Selección de canciones con pulsos vivos y tempos energéticos según tu biblioteca."
                    )
                )
            }

            // 3. Chill & Relax (Calma y Concentración)
            val chillSongs = allSongs.filter { song ->
                val genre = song.genre?.lowercase(Locale.getDefault()) ?: ""
                val isChillGenre = genre.contains("acoustic") || genre.contains("ambient") ||
                                   genre.contains("folk") || genre.contains("jazz") ||
                                   genre.contains("classical") || genre.contains("lo-fi") ||
                                   genre.contains("chill") || genre.contains("r&b")
                isChillGenre || (song.duration > 210_000L && getSongAffinity(song.id) > 6f)
            }.sortedByDescending { getSongAffinity(it.id) }.take(25)

            if (chillSongs.size >= 4) {
                generated.add(
                    AiMix(
                        id = "ai_chill_flow",
                        title = "Chill & Relax",
                        subtitle = "Melodías suaves para relajarte y concentrarte",
                        category = MixCategory.CHILL,
                        songs = generateSmartShuffle(chillSongs),
                        gradientColors = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6), Color(0xFF6366F1)),
                        iconName = "spa",
                        description = "Sonidos acústicos y texturas pausadas para momentos de tranquilidad."
                    )
                )
            }

            // 4. Mix Temporal (Mañana / Tarde / Noche)
            val timeName = when (currentTime) {
                TimeOfDay.MORNING -> "Mix de la Mañana"
                TimeOfDay.AFTERNOON -> "Mix de la Tarde"
                TimeOfDay.EVENING -> "Mix del Atardecer"
                TimeOfDay.NIGHT -> "Mix Nocturno"
            }
            val timeSub = when (currentTime) {
                TimeOfDay.MORNING -> "Despierta con la mejor selección para empezar tu día"
                TimeOfDay.AFTERNOON -> "El acompañamiento perfecto para tu jornada"
                TimeOfDay.EVENING -> "Música cálida para desconectar al final del día"
                TimeOfDay.NIGHT -> "Ambiente envolvente para la noche"
            }
            val timeGradients = when (currentTime) {
                TimeOfDay.MORNING -> listOf(Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF06B6D4))
                TimeOfDay.AFTERNOON -> listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFF06B6D4))
                TimeOfDay.EVENING -> listOf(Color(0xFFF97316), Color(0xFF8B5CF6), Color(0xFF4F46E5))
                TimeOfDay.NIGHT -> listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4C1D95))
            }

            val timeSongs = allSongs.sortedByDescending { song ->
                val interaction = storage.getInteraction(song.id)
                interaction.calculateAffinityScore() * (1f + interaction.getTimeAffinity(currentTime))
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

            // 5. Joyas Olvidadas (Forgotten Gems)
            val now = System.currentTimeMillis()
            val twoWeeksAgo = now - (14L * 24L * 60L * 60L * 1000L)
            val forgottenSongs = allSongs.filter { song ->
                val interaction = storage.getInteraction(song.id)
                (interaction.isFavorite || interaction.playCount >= 3) &&
                (interaction.lastPlayedTimestamp in 1..twoWeeksAgo || interaction.lastPlayedTimestamp == 0L)
            }.shuffled().take(20)

            if (forgottenSongs.size >= 4) {
                generated.add(
                    AiMix(
                        id = "ai_forgotten_gems",
                        title = "Joyas Olvidadas",
                        subtitle = "Canciones que te encantan pero hace tiempo no escuchas",
                        category = MixCategory.FORGOTTEN_GEMS,
                        songs = forgottenSongs,
                        gradientColors = listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF047857)),
                        iconName = "history",
                        description = "Redescubre temas con alta puntuación en tu historial que han quedado atrás."
                    )
                )
            }

            // 6. Radio de Artista Destacado (Top Artist Spotlight)
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
                            title = "Radio: ${topArtist.key}",
                            subtitle = "Lo mejor de ${topArtist.key} y artistas afines",
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
