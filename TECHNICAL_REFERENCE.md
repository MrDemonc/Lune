# Lune — Guide Reference

In the latest versions of Lune, the code of the main class, Lune.kt, was split to reduce the amount of code within that class. Several functions were extracted and placed in new classes, creating a better code structure. Below, you can see how the code looks after splitting:

---

## Package Structure

```bash

com.demonlab.lune.ui/
├── activities/ → Lune.kt (Activity + MainScreen + ReusableSkipIcon)
├── components/ → SharedComponents.kt
├── data/ → Album.kt
├── player/ → PlayerComponents.kt
├── playlist/ → PlaylistViews.kt
├── screens/ → DetailViews.kt
├── search/ → SearchScreen.kt
├── sheets/ → BottomSheets.kt + PlaylistDialogs.kt
├── theme/ → LuneTheme
├── utils/ → Extensions.kt
└── viewmodels/ → MusicViewModel

```

---

## Block 1 — Utilities

**File:** `ui/utils/Extensions.kt`
**Package:** `com.demonlab.lune.ui.utils`

| Function                                                  | Type      | Description               |
| --------------------------------------------------------- | --------- | ------------------------- |
| `Vibrator.triggerLightVibration()`                        | Extension | Light haptic vibration    |
| `formatDuration(duration: Long): String`                  | Top-level | `mm:ss` format            |
| `formatDurationCompact(durationInMillis: Long): String`   | Top-level | Compact duration          |
| `formatLongDuration(durationInMillis: Long): String`      | Top-level | `h m s` format            |
| `Modifier.bounceClick(scaleDown: Float)`                  | Modifier  | Bounce animation on click |
| `Modifier.songSwipeGestures(enabled, onNext, onPrevious)` | Modifier  | Horizontal swipe on songs |

---

## Block 2 — Shared Components

**File:** `ui/components/SharedComponents.kt`
**Package:** `com.demonlab.lune.ui.components`

| Composable              | Description                                    |
| ----------------------- | ---------------------------------------------- |
| `ResponsiveText`        | Text that adapts to available width            |
| `SongItem`              | Song row (cover, title, artist, duration, fav) |
| `AlbumsListHeader`      | Album section header                           |
| `SongsListHeader`       | Song section header                            |
| `FolderFilterContent`   | Folder filter by tab                           |
| `WaveformVisualizer`    | Waveform visualizer                            |
| `OptionButton`          | Round options button                           |
| `ScrollToCurrentButton` | Floating "scroll to current song" button       |
| `VinylRecordAsyncCover` | Animated vinyl record cover                    |

---

## Block 3 — Player

**File:** `ui/player/PlayerComponents.kt`
**Package:** `com.demonlab.lune.ui.player`

| Composable             | Description                                          |
| ---------------------- | ---------------------------------------------------- |
| `AlbumStackedCarousel` | Stacked album carousel                               |
| `FullPlayer`           | Full-screen player                                   |
| `PlayerActionButton`   | Player action button                                 |
| `MiniPlayer`           | Mini bottom player bar                               |
| `ReusableSkipIcon`     | Skip next/previous icon                              |

---

## Block 4 — Detail Views

**File:** `ui/screens/DetailViews.kt`
**Package:** `com.demonlab.lune.ui.screens`

| Composable           | Description                                       |
| -------------------- | ------------------------------------------------- |
| `PlaylistDetailView` | Playlist detail view (collapsible header + songs) |
| `AlbumDetailView`    | Album detail view (collapsible header + songs)    |
| `FolderDetailView`   | Folder detail view (collapsible header + songs)   |

---

## Block 5 — Search

**File:** `ui/search/SearchScreen.kt`
**Package:** `com.demonlab.lune.ui.search`

| Name            | Type       | Description                                                               |
| --------------- | ---------- | ------------------------------------------------------------------------- |
| `SearchResults` | Data class | `songs`, `favoriteSongs`, `albumResults`, `playlistResults`, `tagResults` |
| `SearchScreen`  | Composable | Search screen with topbar + sectioned results                             |

---

## Block 6 — Playlists

**File:** `ui/playlist/PlaylistViews.kt`
**Package:** `com.demonlab.lune.ui.playlist`

| Composable                 | Description                            |
| -------------------------- | -------------------------------------- |
| `PlaylistPreviewCovers`    | Mini cover collage (1 or 4 images)     |
| `PlaylistListScreen`       | Full playlist list with create/options |
| `DeletePlaylistDialog`     | Delete confirmation dialog             |
| `PlaylistOptionsAndRename` | Options sheet + rename + delete        |
| `PlaylistOptionsSheet`     | Playlist options bottom sheet          |

---

## Block 7 — Bottom Sheets

**Files:** `ui/sheets/BottomSheets.kt`, `ui/sheets/PlaylistDialogs.kt`
**Package:** `com.demonlab.lune.ui.sheets`

| Composable                      | File               | Description                                |
| ------------------------------- | ------------------ | ------------------------------------------ |
| `SongOptionsBottomSheet`        | BottomSheets.kt    | Song options (add to playlist, edit, etc.) |
| `SortBottomSheet`               | BottomSheets.kt    | Sort songs                                 |
| `EqBottomSheet`                 | BottomSheets.kt    | Equalizer                                  |
| `QueueBottomSheet`              | BottomSheets.kt    | Play queue                                 |
| `PlayerOptionsBottomSheet`      | BottomSheets.kt    | Player options                             |
| `VisualizerSettingsBottomSheet` | BottomSheets.kt    | Visualizer settings                        |
| `EditSongBottomSheet`           | BottomSheets.kt    | Edit song metadata                         |
| `AddSongsToPlaylistDialog`      | PlaylistDialogs.kt | Add songs to playlist                      |
| `AddToPlaylistDialog`           | PlaylistDialogs.kt | Add song to playlist                       |
| `CreatePlaylistDialog`          | PlaylistDialogs.kt | Create new playlist                        |

---

## Block 8 — Album Data Class

**File:** `ui/data/Album.kt`
**Package:** `com.demonlab.lune.ui.data`

| Name        | Type       | Description                                                |
| ----------- | ---------- | ---------------------------------------------------------- |
| `Album`     | Data class | `id`, `name`, `artist`, `albumArtUri`, `coverUrl`, `songs` |
| `AlbumGrid` | Composable | Album grid                                                 |
| `AlbumCard` | Composable | Individual album card                                      |

---

## What remains in Lune.kt

**File:** `ui/activities/Lune.kt`
**Package:** `com.demonlab.lune.ui.activities`

| Name                    | Type                          | Description                                                                                                             |
| ----------------------- | ----------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| `Lune`                  | `class` (`AppCompatActivity`) | Main Activity                                                                                                           |
| `ACTION_VIEW_PLAYLISTS` | `const val`                   | Intent action for shortcut                                                                                              |
| `MainScreen`            | Composable                    | Main orchestrator: tabs, topbar, pager, search overlay, detail overlays, mini/full player, sheets, dialogs              |

---

## Notes

- **ReusableSkipIcon consolidated:** Centralized in `PlayerComponents.kt`.
- **MusicService.kt:** Has 6 deprecation warnings (Virtualizer) pending resolution.
- **All extracted functions** retain the same parameters and original behavior. No logic changes were made during refactoring.
