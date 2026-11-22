package com.amz.browser.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class SearchEngine(val label: String, val searchPrefix: String) {
    GOOGLE("Google", "https://www.google.com/search?q="),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q="),
    BING("Bing", "https://www.bing.com/search?q=")
}

data class ResultCard(
    val id: String,
    val site: String,
    val url: String,
    val title: String,
    val snippet: String,
    val body: String
)

interface SearchProvider {
    suspend fun search(query: String): List<ResultCard>
}

class FakeSearchProvider : SearchProvider {
    override suspend fun search(query: String): List<ResultCard> {
        delay(300)
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        return listOf(
            ResultCard(
                id = "1",
                site = "en.wikipedia.org",
                url = "https://en.wikipedia.org/wiki/Special:Search?search=$encoded",
                title = "$query — Overview",
                snippet = "Concise encyclopedia summary and key ideas for $query.",
                body = "Detailed overview card for $query. Plug in a real SearchProvider to replace this demo provider."
            ),
            ResultCard(
                id = "2",
                site = "github.com",
                url = "https://github.com/search?q=$encoded",
                title = "$query — Open Source Projects",
                snippet = "Repositories, documentation, and libraries matching $query.",
                body = "Browse community repositories and technical references for $query."
            )
        )
    }
}

object OmniboxResolver {
    fun resolve(raw: String, engine: SearchEngine, httpsOnly: Boolean): Pair<String, Boolean> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return "" to false
        val hasSpace = trimmed.any { it.isWhitespace() }
        val hasScheme = trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)
        val isLocalhost = trimmed.startsWith("localhost", true)
        val looksLikeHost = !hasSpace && (hasScheme || isLocalhost || trimmed.contains('.'))

        if (looksLikeHost) {
            val url = when {
                hasScheme && httpsOnly && trimmed.startsWith("http://", true) && !trimmed.startsWith("http://localhost", true) ->
                    "https://" + trimmed.substring(7)
                hasScheme -> trimmed
                isLocalhost -> "http://$trimmed"
                else -> "https://$trimmed"
            }
            return url to false
        }
        val encoded = URLEncoder.encode(trimmed, "UTF-8")
        return (engine.searchPrefix + encoded) to true
    }
}

object SuggestionsClient {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(query: String, engine: SearchEngine): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        runCatching {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val endpoint = when (engine) {
                SearchEngine.DUCKDUCKGO -> "https://duckduckgo.com/ac/?q=$encoded&type=list"
                SearchEngine.GOOGLE, SearchEngine.BING ->
                    "https://suggestqueries.google.com/complete/search?client=firefox&q=$encoded"
            }
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 2500
                readTimeout = 2500
                requestMethod = "GET"
            }
            try {
                if (conn.responseCode !in 200..299) return@withContext emptyList()
                val text = conn.inputStream.bufferedReader().readText()
                val root = json.parseToJsonElement(text).jsonArray
                if (root.size >= 2) {
                    root[1].jsonArray.mapNotNull { it.jsonPrimitive.content }.take(5)
                } else {
                    emptyList()
                }
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(emptyList())
    }
}
