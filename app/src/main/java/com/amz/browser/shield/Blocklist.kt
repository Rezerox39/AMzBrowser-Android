package com.amz.browser.shield

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit

class BlocklistManager private constructor(private val context: Context) {

    @Volatile
    private var blockedHosts: HashSet<String> = HashSet()

    private val urlPathPatterns = listOf(
        "/pagead/",
        "/adserver",
        "doubleclick",
        "/ads/",
        "/ad-banner",
        "/popunder",
        "/popads",
        "googlesyndication",
        "googleadservices"
    )

    private val cachedHostsFile = File(context.filesDir, "amz_shield_hosts.txt")
    private val metaFile = File(context.filesDir, "amz_shield_updated_at.txt")

    suspend fun initializeAndRefreshIfNeeded() = withContext(Dispatchers.IO) {
        loadFromLocal()
        val lastUpdated = metaFile.takeIf { it.exists() }?.readText()?.toLongOrNull() ?: 0L
        val elapsedHours = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - lastUpdated)
        if (elapsedHours >= 24L) {
            runCatching { refreshFromRemoteLists() }
        }
    }

    private fun loadFromLocal() {
        val next = HashSet<String>(16_384)
        runCatching {
            context.assets.open("blocklist_hosts.txt").bufferedReader().useLines { lines ->
                lines.forEach { parseHostLine(it)?.let(next::add) }
            }
        }
        if (cachedHostsFile.exists()) {
            runCatching {
                cachedHostsFile.bufferedReader().useLines { lines ->
                    lines.forEach { parseHostLine(it)?.let(next::add) }
                }
            }
        }
        blockedHosts = next
    }

    private fun refreshFromRemoteLists() {
        val sources = listOf(
            "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=0&mimetype=plaintext",
            "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts"
        )
        val merged = HashSet<String>(65_536)
        sources.forEach { endpoint ->
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 12_000
                requestMethod = "GET"
            }
            try {
                if (conn.responseCode in 200..299) {
                    conn.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { parseHostLine(it)?.let(merged::add) }
                    }
                }
            } finally {
                conn.disconnect()
            }
        }
        if (merged.size > 500) {
            cachedHostsFile.writeText(merged.joinToString("
"))
            metaFile.writeText(System.currentTimeMillis().toString())
            val combined = HashSet(blockedHosts)
            combined.addAll(merged)
            blockedHosts = combined
        }
    }

    private fun parseHostLine(raw: String): String? {
        val trimmed = raw.substringBefore('#').trim().lowercase()
        if (trimmed.isEmpty()) return null
        val parts = trimmed.split(Regex("\s+"))
        val host = when {
            parts.size >= 2 && (parts[0] == "0.0.0.0" || parts[0] == "127.0.0.1") -> parts[1]
            parts.size == 1 -> parts[0]
            else -> return null
        }
        if (host == "localhost" || host == "0.0.0.0" || !host.contains('.')) return null
        return host
    }

    fun shouldBlock(rawUrl: String): Boolean {
        val lower = rawUrl.lowercase()
        for (i in urlPathPatterns.indices) {
            if (lower.contains(urlPathPatterns[i])) return true
        }
        val host = runCatching { URI(rawUrl).host?.lowercase() }.getOrNull() ?: return false
        val snapshot = blockedHosts
        if (snapshot.contains(host)) return true

        var dot = host.indexOf('.')
        while (dot != -1) {
            val parent = host.substring(dot + 1)
            if (snapshot.contains(parent)) return true
            dot = host.indexOf('.', dot + 1)
        }
        return false
    }

    companion object {
        @Volatile
        private var instance: BlocklistManager? = null

        fun getInstance(context: Context): BlocklistManager =
            instance ?: synchronized(this) {
                instance ?: BlocklistManager(context.applicationContext).also { instance = it }
            }
    }
}
