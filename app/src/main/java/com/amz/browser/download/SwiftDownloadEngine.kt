package com.amz.browser.download

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amz.browser.download.engine.DownloadRequest
import com.amz.browser.download.engine.DownloadSnapshot
import com.amz.browser.download.engine.DownloadStatus
import com.amz.browser.download.engine.FileNames
import com.amz.browser.download.engine.HttpClients
import com.amz.browser.download.engine.DownloadEngine as SwiftEngine
import com.amz.browser.download.engine.DownloadHandle as SwiftHandle
import com.amz.browser.ui.theme.LocalAMzPalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Real download engine for AMz Browser: segmented, resumable, checksum-aware transfers
 * ported from SwiftDM (github.com/Rezerox39/swiftdm, MIT).
 */
class SwiftDownloadEngine(
    context: Context,
    var onToast: (String) -> Unit = {},
) : DownloadEngine {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val engine = SwiftEngine(client = HttpClients.shared)
    private val publisher = AmzDownloadPublisher(appContext)
    private val partsDir = File(appContext.filesDir, "amz-download-parts").apply { mkdirs() }

    private val handles = ConcurrentHashMap<String, SwiftHandle>()
    private val snapshots = MutableStateFlow<Map<String, DownloadSnapshot>>(emptyMap())
    private val notified = ConcurrentHashMap.newKeySet<String>()

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    override val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    override fun enqueue(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ) {
        val id = UUID.randomUUID().toString()
        val name = FileNames.fromContentDisposition(contentDisposition)
            ?: FileNames.fromUrl(url)
            ?: "download-${System.currentTimeMillis()}"
        val headers = buildMap {
            if (!userAgent.isNullOrBlank()) put("User-Agent", userAgent)
            if (!mimeType.isNullOrBlank()) put("Accept", mimeType)
        }
        val request = DownloadRequest(
            id = id,
            url = url,
            destinationDir = partsDir,
            segments = 8,
            fileNameOverride = FileNames.sanitize(name) ?: "download",
            headers = headers,
            mimeTypeHint = mimeType,
        )
        val handle = engine.start(request, publisherOverride = publisher)
        handles[id] = handle
        scope.launch {
            handle.state.collect { snapshot ->
                val all = snapshots.value + (id to snapshot)
                snapshots.value = all
                _downloads.value = all.values
                    .map { it.toItem() }
                    .sortedWith(compareByDescending<DownloadItem> { it.state.ordinal }.thenBy { it.id })
                when {
                    snapshot.status == DownloadStatus.Completed && notified.add(id) ->
                        onToast("Downloaded ${snapshot.fileName}")
                    snapshot.status == DownloadStatus.Failed && notified.add(id) ->
                        onToast("Download failed: ${snapshot.error?.message ?: "unknown error"}")
                }
            }
        }
        onToast("Starting ${request.fileNameOverride}")
    }

    fun pause(id: String) {
        scope.launch { handles[id]?.pause() }
    }

    fun resume(id: String) {
        scope.launch { handles[id]?.resume() }
    }

    fun cancel(id: String) {
        scope.launch {
            handles[id]?.cancel()
            handles.remove(id)
        }
    }

    fun clearCompleted() {
        val completed = snapshots.value.filter { it.value.status.isTerminal }.keys
        if (completed.isEmpty()) return
        snapshots.value = snapshots.value - completed
        completed.forEach(handles::remove)
        _downloads.value = snapshots.value.values.map { it.toItem() }
    }

    @Composable
    override fun Content() {
        val items by downloads.collectAsState()
        val palette = LocalAMzPalette.current
        if (items.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Download, null, tint = palette.textSecondary, modifier = Modifier.size(30.dp))
                Text("No downloads yet", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                Text("Downloads you start will appear here", color = palette.textSecondary, fontSize = 12.sp)
            }
            return
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.id }) { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = when (item.state) {
                            DownloadState.COMPLETED -> Icons.Rounded.CheckCircle
                            DownloadState.FAILED -> Icons.Rounded.Error
                            else -> Icons.Rounded.Download
                        },
                        contentDescription = null,
                        tint = when (item.state) {
                            DownloadState.COMPLETED -> palette.success
                            DownloadState.FAILED -> palette.destructive
                            else -> palette.accent
                        },
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.fileName,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(item.detail, color = palette.textSecondary, fontSize = 11.sp, maxLines = 1)
                        LinearProgressIndicator(
                            progress = { item.progress },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            color = palette.accent,
                            trackColor = palette.divider,
                            strokeCap = StrokeCap.Round
                        )
                    }
                    if (item.canPause) {
                        IconButton(onClick = { pause(item.id) }) {
                            Icon(Icons.Rounded.Pause, "Pause", tint = palette.textSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (item.canResume) {
                        IconButton(onClick = { resume(item.id) }) {
                            Icon(Icons.Rounded.PlayArrow, "Resume", tint = palette.accent, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (item.canCancel) {
                        IconButton(onClick = { cancel(item.id) }) {
                            Icon(Icons.Rounded.Close, "Cancel", tint = palette.textSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun DownloadSnapshot.toItem(): DownloadItem = DownloadItem(
    id = id,
    fileName = fileName.ifBlank { url },
    progress = overallFraction,
    state = when (status) {
        DownloadStatus.Idle, DownloadStatus.Probing -> DownloadState.QUEUED
        DownloadStatus.Downloading, DownloadStatus.Finalizing -> DownloadState.DOWNLOADING
        DownloadStatus.Paused -> DownloadState.PAUSED
        DownloadStatus.Completed -> DownloadState.COMPLETED
        DownloadStatus.Failed, DownloadStatus.Cancelled -> DownloadState.FAILED
    },
    detail = when {
        error != null -> error!!.message.orEmpty()
        status == DownloadStatus.Finalizing -> finalizeStep?.label ?: "Finalizing"
        status == DownloadStatus.Paused -> "Paused"
        status == DownloadStatus.Completed -> "Saved to Downloads"
        bytesPerSecond > 0 -> "${formatBytes(bytesPerSecond)}/s · $segmentCount segments"
        else -> status.name
    },
    canPause = status.isActive,
    canResume = status == DownloadStatus.Paused,
    canCancel = !status.isTerminal,
)

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> String.format("%.2f GB", bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
    bytes >= 1024 -> String.format("%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}
