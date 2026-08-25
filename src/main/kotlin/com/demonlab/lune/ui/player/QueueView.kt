package com.demonlab.lune.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.demonlab.lune.data.L10n
import com.demonlab.lune.tools.PlaybackManager
import com.demonlab.lune.ui.components.CoverArtwork
import com.demonlab.lune.ui.utils.formatDuration

@Composable
fun QueueDialog(
    playbackManager: PlaybackManager,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${L10n.queue} (${playbackManager.activeQueue.size})", fontWeight = FontWeight.Bold)
                if (playbackManager.activeQueue.isNotEmpty()) {
                    TextButton(onClick = { playbackManager.clearQueue() }) {
                        Text(L10n.clear, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        text = {
            if (playbackManager.activeQueue.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(L10n.queueEmpty, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                ) {
                    itemsIndexed(playbackManager.activeQueue) { index, song ->
                        val isCurrent = index == playbackManager.currentIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable { playbackManager.playQueueIndex(index) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CoverArtwork(
                                coverPath = song.coverPath,
                                modifier = Modifier.size(36.dp),
                                shape = RoundedCornerShape(6.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formatDuration(song.duration),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(
                                onClick = { playbackManager.removeFromQueue(index) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(L10n.close)
            }
        }
    )
}
