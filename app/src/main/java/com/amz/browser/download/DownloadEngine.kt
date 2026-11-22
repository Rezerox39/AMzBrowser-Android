package com.amz.browser.download

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.amz.browser.search.FakeSearchProvider
import com.amz.browser.search.SearchProvider
import com.amz.browser.ui.theme.LocalAMzPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class DownloadState { QUEUED, DOWNLOADING, COMPLETED, FAILED }

data class DownloadItem(
    val id: String,
    val fileName: String,
    val progress: Float,
    val state: DownloadState
)

interface DownloadEngine {
    fun enqueue(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    )
    val downloads: StateFlow<List<DownloadItem>>

    @Composable
    fun Content()
}

class NoOpDownloadEngine(
    var onToast: (String) -> Unit = {}
) : DownloadEngine {

    override val downloads: StateFlow<List<DownloadItem>> = MutableStateFlow(emptyList())

    override fun enqueue(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ) {
        onToast("Download engine not connected")
    }

    @Composable
    override fun Content() {
        val palette = LocalAMzPalette.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawRoundRect(
                    color = palette.textSecondary.copy(alpha = 0.35f),
                    cornerRadius = CornerRadius(24.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                    )
                )
            }
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Your download engine mounts here",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary
                )
                Text(
                    text = "Swap BrowserServices.downloadEngine in one line to connect your downloader.",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSecondary
                )
            }
        }
    }
}

object BrowserServices {
    // Swap in your own DownloadEngine implementation here in one line:
    var downloadEngine: DownloadEngine = NoOpDownloadEngine()

    // Swap in a real SearchProvider implementation for Card results (beta):
    var searchProvider: SearchProvider = FakeSearchProvider()
}
