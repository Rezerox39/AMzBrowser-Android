package com.amz.browser.ui.screens

import com.amz.browser.R
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amz.browser.browser.TabManagerViewModel
import com.amz.browser.browser.WebViewFactory
import com.amz.browser.data.AppSettings
import com.amz.browser.data.SettingsRepo
import com.amz.browser.data.ShieldStats
import com.amz.browser.download.BrowserServices
import com.amz.browser.download.NoOpDownloadEngine
import com.amz.browser.download.SwiftDownloadEngine
import com.amz.browser.search.OmniboxResolver
import com.amz.browser.search.SearchEngine
import com.amz.browser.search.SuggestionsClient
import com.amz.browser.shield.ShieldInjector
import com.amz.browser.ui.components.AuroraBackground
import com.amz.browser.ui.components.GlassSurface
import com.amz.browser.ui.components.ProvideTiltSensor
import com.amz.browser.ui.components.TiltCard
import com.amz.browser.ui.components.jellyClickable
import com.amz.browser.ui.theme.AMzAccents
import com.amz.browser.ui.theme.AMzTheme
import com.amz.browser.ui.theme.AnimationSpeed
import com.amz.browser.ui.theme.LocalAMzPalette
import com.amz.browser.ui.theme.ThemeMode
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URI
import java.util.Calendar

private data class FavoritePreset(
    val name: String,
    val iconRes: Int,
    val color: Long,
    val url: String
)

private val DefaultFavorites = listOf(
    FavoritePreset("Apple", R.drawable.ic_fav_apple, 0xFF1D1D1FL, "https://www.apple.com"),
    FavoritePreset("Wikipedia", R.drawable.ic_fav_wikipedia, 0xFF8E8E93L, "https://en.wikipedia.org"),
    FavoritePreset("YouTube", R.drawable.ic_fav_youtube, 0xFFFF3B30L, "https://www.youtube.com"),
    FavoritePreset("GitHub", R.drawable.ic_fav_github, 0xFF2C2C30L, "https://github.com"),
    FavoritePreset("Maps", R.drawable.ic_fav_maps, 0xFF30D158L, "https://www.openstreetmap.org"),
    FavoritePreset("News", R.drawable.ic_fav_news, 0xFFFF9F0AL, "https://news.ycombinator.com"),
    FavoritePreset("Music", R.drawable.ic_fav_music, 0xFFFF2D55L, "https://music.apple.com"),
    FavoritePreset("Example", R.drawable.ic_fav_example, 0xFF0A84FFL, "https://example.com")
)

private fun siteIconFor(host: String): Int? = when {
    host.contains("music.apple.com") -> R.drawable.ic_site_music
    host.contains("apple.com") -> R.drawable.ic_site_apple
    host.contains("wikipedia.org") -> R.drawable.ic_site_wikipedia
    host.contains("youtube.com") || host.contains("youtu.be") -> R.drawable.ic_site_youtube
    host.contains("github.com") -> R.drawable.ic_site_github
    host.contains("openstreetmap.org") -> R.drawable.ic_site_maps
    host.contains("news.ycombinator.com") -> R.drawable.ic_site_news
    host.contains("example.com") -> R.drawable.ic_site_example
    else -> null
}

@Composable
fun AMzBrowserRoot(
    initialUrl: String?,
    onRequestFileChooser: (ValueCallback<Array<Uri>>, Array<String>) -> Unit,
    tabManager: TabManagerViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember(context) { SettingsRepo(context) }

    val settings by repo.settings.collectAsState(initial = AppSettings())
    val shieldStats by repo.shieldStats.collectAsState(initial = ShieldStats())
    val bookmarks by repo.bookmarks.collectAsState(initial = emptyList())
    val history by repo.history.collectAsState(initial = emptyList())
    val persistedTabs by repo.persistedTabs.collectAsState(initial = emptyList())

    val tabs by tabManager.tabs.collectAsState()
    val activeTabId by tabManager.activeTabId.collectAsState()
    val activeTab = tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()

    var showTabs by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showLibrary by remember { mutableStateOf(false) }
    var libraryTab by remember { mutableIntStateOf(0) } // 0=Bookmarks, 1=History, 2=Downloads
    var showFind by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    var blockedRedirectUrl by remember { mutableStateOf<String?>(null) }

    fun showToast(msg: String) {
        toastMessage = msg
        scope.launch {
            delay(1800)
            if (toastMessage == msg) toastMessage = null
        }
    }

    LaunchedEffect(Unit) {
        when (val engine = BrowserServices.downloadEngine) {
            is NoOpDownloadEngine -> engine.onToast = { showToast(it) }
            is SwiftDownloadEngine -> engine.onToast = { showToast(it) }
        }
    }

    LaunchedEffect(persistedTabs) {
        tabManager.restoreTabsIfFresh(persistedTabs)
    }

    LaunchedEffect(tabs) {
        repo.saveTabs(tabManager.toPersistedTabs())
    }

    fun navigate(rawInput: String) {
        val (resolvedUrl, _) = OmniboxResolver.resolve(rawInput, settings.searchEngine, settings.httpsOnly)
        if (resolvedUrl.isBlank()) return
        blockedRedirectUrl = null
        tabManager.updateTab(activeTab.id) { it.copy(url = resolvedUrl) }
        val wv = tabManager.ensureWebView(
            tabId = activeTab.id,
            context = context,
            isDarkTheme = settings.themeMode == ThemeMode.DARK,
            textZoom = settings.textZoom,
            isAdShieldEnabled = { settings.adShield },
            isHttpsOnlyEnabled = { settings.httpsOnly },
            onAdBlocked = { scope.launch { repo.incrementShieldCounters(ads = 1) } },
            onPopupBlocked = { scope.launch { repo.incrementShieldCounters(popups = 1) } },
            onRedirectBlocked = { target ->
                blockedRedirectUrl = target
                scope.launch { repo.incrementShieldCounters(redirects = 1) }
            },
            onPageFinished = { _, url, title, blocked, isPriv ->
                if (!isPriv) scope.launch { repo.recordHistory(title, url) }
                // Shield counters are always visible in the Shield Report; avoid a
                // toast on every page load / search.
            },
            onRequestFileChooser = onRequestFileChooser
        )
        wv.loadUrl(resolvedUrl)
    }

    LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) navigate(initialUrl)
    }

    BackHandler(enabled = showMenu || showLibrary || showTabs || showFind || (activeTab.webView?.canGoBack() == true)) {
        when {
            showMenu -> showMenu = false
            showLibrary -> showLibrary = false
            showTabs -> showTabs = false
            showFind -> {
                showFind = false
                findQuery = ""
                activeTab.webView?.clearMatches()
            }
            activeTab.webView?.canGoBack() == true -> activeTab.webView.goBack()
        }
    }

    AMzTheme(
        themeMode = settings.themeMode,
        accentHex = settings.accentHex,
        glassBlurEnabled = settings.glassBlur,
        speedFactor = settings.animSpeed.factor
    ) {
        ProvideTiltSensor(enabled = settings.gyroTilt) {
            val palette = LocalAMzPalette.current
            val hazeState = remember { HazeState() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.background)
            ) {
                if (activeTab.url.isBlank()) {
                    AuroraBackground()
                }
                // Main viewport (Start Page or live WebView)
                Box(modifier = Modifier.fillMaxSize()) {
                    if (activeTab.url.isBlank()) {
                        AMzStartPage(
                            userName = settings.userName,
                            isPrivate = activeTab.isPrivate,
                            stats = shieldStats,
                            history = history,
                            gyroEnabled = settings.gyroTilt,
                            hazeState = hazeState,
                            onOpenUrl = { navigate(it) }
                        )
                    } else {
                        val wv = remember(activeTab.id) {
                            tabManager.ensureWebView(
                                tabId = activeTab.id,
                                context = context,
                                isDarkTheme = palette.isDark,
                                textZoom = settings.textZoom,
                                isAdShieldEnabled = { settings.adShield },
                                isHttpsOnlyEnabled = { settings.httpsOnly },
                                onAdBlocked = { scope.launch { repo.incrementShieldCounters(ads = 1) } },
                                onPopupBlocked = { scope.launch { repo.incrementShieldCounters(popups = 1) } },
                                onRedirectBlocked = { target ->
                                    blockedRedirectUrl = target
                                    scope.launch { repo.incrementShieldCounters(redirects = 1) }
                                },
                                onPageFinished = { _, url, title, blocked, isPriv ->
                                    if (!isPriv) scope.launch { repo.recordHistory(title, url) }
                                    // Shield counters are always visible in the Shield Report; avoid a
                // toast on every page load / search.
                                },
                                onRequestFileChooser = onRequestFileChooser
                            )
                        }
                        AndroidView(
                            factory = { wv },
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(bottom = 112.dp)
                        )
                    }
                }

                // Find in Page Bar
                AnimatedVisibility(
                    visible = showFind,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    GlassSurface(cornerRadius = 22.dp, hazeState = hazeState) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = findQuery,
                                onValueChange = {
                                    findQuery = it
                                    activeTab.webView?.findAllAsync(it)
                                },
                                singleLine = true,
                                textStyle = TextStyle(color = palette.textPrimary, fontSize = 16.sp),
                                cursorBrush = SolidColor(palette.accent),
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { activeTab.webView?.findNext(false) }) { Text("Prev") }
                            TextButton(onClick = { activeTab.webView?.findNext(true) }) { Text("Next") }
                            TextButton(onClick = {
                                showFind = false
                                findQuery = ""
                                activeTab.webView?.clearMatches()
                            }) { Text("Done") }
                        }
                    }
                }

                // Redirect Blocked Snackbar — anchored to the top so search never shifts it
                AnimatedVisibility(
                    visible = blockedRedirectUrl != null,
                    enter = fadeIn() + slideInVertically { -it / 2 },
                    exit = fadeOut() + slideOutVertically { -it / 2 },
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    blockedRedirectUrl?.let { blocked ->
                    GlassSurface(
                        cornerRadius = 28.dp,
                        hazeState = hazeState,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Redirect blocked", color = palette.textPrimary, fontSize = 14.sp)
                            TextButton(onClick = {
                                blockedRedirectUrl = null
                                activeTab.webView?.loadUrl(blocked)
                            }) {
                                Text("Allow", color = palette.accent, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                }

                // Floating Navigation Panel
                AMzFloatingNavPanel(
                    currentUrl = activeTab.url,
                    progress = activeTab.progress,
                    canGoBack = activeTab.canGoBack,
                    canGoForward = activeTab.canGoForward,
                    tabCount = tabs.size,
                    searchEngine = settings.searchEngine,
                    hazeState = hazeState,
                    onSubmit = { navigate(it) },
                    onBack = { activeTab.webView?.goBack() },
                    onForward = { activeTab.webView?.goForward() },
                    onReload = { activeTab.webView?.reload() },
                    onShare = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, activeTab.url.ifBlank { "AMz Browser" })
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share link"))
                    },
                    onTabs = {
                        tabManager.captureThumbnail(activeTab.id)
                        showTabs = true
                    },
                    onMenu = { showMenu = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(12.dp)
                )

                // Tab Switcher Overlay
                if (showTabs) {
                    AMzTabSwitcherOverlay(
                        tabs = tabs,
                        activeTabId = activeTab.id,
                        gyroEnabled = settings.gyroTilt,
                        hazeState = hazeState,
                        onSelect = {
                            tabManager.selectTab(it)
                            showTabs = false
                        },
                        onCloseTab = { tabManager.closeTab(it) },
                        onNewTab = {
                            tabManager.openNewTab(isPrivate = activeTab.isPrivate)
                            showTabs = false
                        },
                        onDone = { showTabs = false }
                    )
                }

                // Quick Menu Bottom Sheet
                if (showMenu) {
                    AMzQuickMenuSheet(
                        settings = settings,
                        isBookmarked = bookmarks.any { it.url == activeTab.url && activeTab.url.isNotBlank() },
                        isPrivate = activeTab.isPrivate,
                        isDesktop = activeTab.desktopMode,
                        isReader = activeTab.readerMode,
                        hazeState = hazeState,
                        onDismiss = { showMenu = false },
                        onToggleBookmark = {
                            if (activeTab.url.isBlank()) {
                                showToast("Open a page first")
                            } else {
                                scope.launch {
                                    val added = repo.toggleBookmark(activeTab.title, activeTab.url)
                                    showToast(if (added) "Bookmarked" else "Bookmark removed")
                                }
                            }
                        },
                        onFind = {
                            showMenu = false
                            showFind = true
                        },
                        onToggleReader = {
                            activeTab.webView?.evaluateJavascript(ShieldInjector.READER_MODE_SCRIPT, null)
                            tabManager.updateTab(activeTab.id) { it.copy(readerMode = !it.readerMode) }
                            showMenu = false
                        },
                        onToggleDesktop = {
                            val nextDesktop = !activeTab.desktopMode
                            tabManager.updateTab(activeTab.id) { it.copy(desktopMode = nextDesktop) }
                            activeTab.webView?.apply {
                                this.settings.userAgentString = if (nextDesktop) WebViewFactory.DESKTOP_USER_AGENT else null
                                this.settings.useWideViewPort = nextDesktop
                                this.settings.loadWithOverviewMode = nextDesktop
                                reload()
                            }
                            showToast(if (nextDesktop) "Desktop site" else "Mobile site")
                        },
                        onOpenLibrary = { tabIdx ->
                            showMenu = false
                            libraryTab = tabIdx
                            showLibrary = true
                        },
                        onTogglePrivate = {
                            tabManager.openNewTab(isPrivate = !activeTab.isPrivate)
                            showMenu = false
                        },
                        onUpdateSettings = { updater -> scope.launch { repo.update(updater) } },
                        onClearData = {
                            scope.launch { repo.clearHistory() }
                            showToast("Browsing data cleared")
                        }
                    )
                }

                // Library Overlay (Bookmarks | History | Downloads slot)
                if (showLibrary) {
                    AMzLibraryOverlay(
                        selectedTab = libraryTab,
                        bookmarks = bookmarks,
                        history = history,
                        onSelectTab = { libraryTab = it },
                        onOpenUrl = {
                            showLibrary = false
                            navigate(it)
                        },
                        onClear = {
                            scope.launch {
                                if (libraryTab == 0) repo.clearBookmarks()
                                if (libraryTab == 1) repo.clearHistory()
                            }
                            showToast("Cleared")
                        },
                        onDone = { showLibrary = false }
                    )
                }

                // First-Launch Setup Overlay
                if (!settings.setupDone) {
                    AMzSetupOverlay(
                        settings = settings,
                        hazeState = hazeState,
                        onUpdateSettings = { updater -> scope.launch { repo.update(updater) } },
                        onFinish = {
                            scope.launch { repo.update { it[SettingsRepo.SETUP_DONE] = true } }
                        }
                    )
                }

                // Floating Glass Toast
                AnimatedVisibility(
                    visible = toastMessage != null,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 12.dp)
                ) {
                    GlassSurface(cornerRadius = 999.dp, hazeState = hazeState) {
                        Text(
                            text = toastMessage.orEmpty(),
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AMzStartPage(
    userName: String,
    isPrivate: Boolean,
    stats: ShieldStats,
    history: List<com.amz.browser.data.HistoryEntry>,
    gyroEnabled: Boolean,
    hazeState: HazeState,
    onOpenUrl: (String) -> Unit
) {
    val palette = LocalAMzPalette.current
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when {
        isPrivate -> "Private Browsing"
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 180.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Column {
                Text(
                    text = if (!isPrivate && userName.isNotBlank()) "$greeting,\n$userName" else greeting,
                    style = MaterialTheme.typography.displayLarge,
                    color = palette.textPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isPrivate) "History is not saved in private tabs." else "Where to next?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textSecondary
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Favorites", style = MaterialTheme.typography.headlineSmall, color = palette.textPrimary)
                DefaultFavorites.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        row.forEach { fav ->
                            DelayedReveal(DefaultFavorites.indexOf(fav)) {
                                TiltCard(
                                    gyroEnabled = gyroEnabled,
                                    maxDegrees = 12f,
                                    modifier = Modifier.width(74.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.jellyClickable { onOpenUrl(fav.url) }
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color(fav.color))
                                        ) {
                                            Image(
                                                painter = painterResource(fav.iconRes),
                                                contentDescription = fav.name,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = fav.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = palette.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Shield Report", style = MaterialTheme.typography.headlineSmall, color = palette.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TiltCard(gyroEnabled = gyroEnabled, modifier = Modifier.weight(1f)) {
                        GlassSurface(cornerRadius = 22.dp, hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stats.adsBlocked.toString(),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text("Ads & trackers blocked", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                            }
                        }
                    }
                    TiltCard(gyroEnabled = gyroEnabled, modifier = Modifier.weight(1f)) {
                        GlassSurface(cornerRadius = 22.dp, hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = (stats.popupsStopped + stats.redirectsStopped).toString(),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text("Pop-ups & redirects", style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                            }
                        }
                    }
                }
            }
        }

        if (history.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Recently Visited", style = MaterialTheme.typography.headlineSmall, color = palette.textPrimary)
                    GlassSurface(cornerRadius = 22.dp, hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            history.take(4).forEach { entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenUrl(entry.url) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.Icon(
                                        Icons.Rounded.History,
                                        contentDescription = null,
                                        tint = palette.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(entry.title, color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(entry.url, style = MaterialTheme.typography.labelMedium, color = palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AMzFloatingNavPanel(
    currentUrl: String,
    progress: Int,
    canGoBack: Boolean,
    canGoForward: Boolean,
    tabCount: Int,
    searchEngine: SearchEngine,
    hazeState: HazeState,
    onSubmit: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onShare: () -> Unit,
    onTabs: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAMzPalette.current
    val host = remember(currentUrl) { runCatching { URI(currentUrl).host?.removePrefix("www.") }.getOrNull().orEmpty() }
    val isHttps = currentUrl.startsWith("https://", true)
    val siteIcon = remember(host) { siteIconFor(host) }

    var query by remember { mutableStateOf(host) }
    var editing by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }

    // Keep the text stable while the user types; only follow the page URL when not editing.
    LaunchedEffect(host) {
        if (!editing) query = host
    }

    LaunchedEffect(query, editing, searchEngine) {
        if (!editing || query.length < 2) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        val requested = query
        delay(220)
        if (!editing || query != requested) return@LaunchedEffect
        val result = SuggestionsClient.fetch(requested, searchEngine)
        if (editing && query == requested) suggestions = result
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (editing && query.isNotBlank()) {
            GlassSurface(
                cornerRadius = 22.dp,
                hazeState = hazeState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                editing = false
                                onSubmit(query)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Search for \"$query\"", color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    suggestions.forEach { term ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    query = term
                                    editing = false
                                    onSubmit(term)
                                }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(term, color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        GlassSurface(cornerRadius = 34.dp, hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(palette.fill)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        if (!editing && siteIcon != null) {
                            Image(
                                painter = painterResource(siteIcon),
                                contentDescription = null,
                                modifier = Modifier.size(19.dp)
                            )
                        } else {
                            androidx.compose.material3.Icon(
                                imageVector = if (isHttps && !editing) Icons.Rounded.Lock else Icons.Rounded.Search,
                                contentDescription = null,
                                tint = if (isHttps && !editing) palette.success else palette.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = {
                                query = it
                                editing = true
                            },
                            singleLine = true,
                            textStyle = TextStyle(color = palette.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                            cursorBrush = SolidColor(palette.accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                editing = false
                                onSubmit(query)
                            }),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                if (query.isEmpty()) {
                                    Text("Search or enter website", color = palette.textSecondary, fontSize = 16.sp)
                                }
                                inner()
                            }
                        )
                        if (currentUrl.isNotBlank()) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .jellyClickable { onReload() }
                            ) {
                                androidx.compose.material3.Icon(Icons.Rounded.Refresh, contentDescription = "Reload", tint = palette.textSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    if (progress in 1..99) {
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            color = palette.accent,
                            trackColor = Color.Transparent,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(2.5.dp)
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(44.dp).alpha(if (canGoBack) 1f else 0.4f).jellyClickable(canGoBack) { onBack() }, contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = palette.accent)
                    }
                    Box(Modifier.size(44.dp).alpha(if (canGoForward) 1f else 0.4f).jellyClickable(canGoForward) { onForward() }, contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Forward", tint = palette.accent)
                    }
                    Box(Modifier.size(44.dp).jellyClickable { onShare() }, contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(Icons.Rounded.Share, contentDescription = "Share", tint = palette.accent)
                    }
                    Box(Modifier.size(44.dp).jellyClickable { onTabs() }, contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(Icons.Rounded.Tab, contentDescription = "Tabs", tint = palette.accent)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 4.dp)
                                .size(17.dp)
                                .clip(CircleShape)
                                .background(palette.accent)
                        ) {
                            Text(tabCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(Modifier.size(44.dp).jellyClickable { onMenu() }, contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(Icons.Rounded.MoreHoriz, contentDescription = "Menu", tint = palette.accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun AMzTabSwitcherOverlay(
    tabs: List<com.amz.browser.browser.BrowserTab>,
    activeTabId: String,
    gyroEnabled: Boolean,
    hazeState: HazeState,
    onSelect: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: () -> Unit,
    onDone: () -> Unit
) {
    val palette = LocalAMzPalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(
            text = "${tabs.size} ${if (tabs.size == 1) "Tab" else "Tabs"}",
            style = MaterialTheme.typography.displayLarge,
            color = palette.textPrimary
        )
        Spacer(Modifier.height(14.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(tabs, key = { it.id }) { tab ->
                TiltCard(gyroEnabled = gyroEnabled) {
                    GlassSurface(
                        cornerRadius = 20.dp,
                        hazeState = hazeState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clickable { onSelect(tab.id) }
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(palette.accent.copy(alpha = if (tab.id == activeTabId) 0.25f else 0.12f))
                            ) {
                                tab.thumbnail?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = tab.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(palette.card.copy(alpha = 0.85f))
                                        .clickable { onCloseTab(tab.id) }
                                ) {
                                    androidx.compose.material3.Icon(Icons.Rounded.Close, contentDescription = "Close", tint = palette.textPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(
                                text = tab.title,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onNewTab,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
            ) {
                androidx.compose.material3.Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("New Tab")
            }
            TextButton(onClick = onDone) {
                Text("Done", color = palette.accent, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DelayedReveal(
    index: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 45L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(380)) + slideInVertically { it / 2 }
    ) {
        content()
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalAMzPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
        Text(label, color = palette.textPrimary, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun AMzQuickMenuSheet(
    settings: AppSettings,
    isBookmarked: Boolean,
    isPrivate: Boolean,
    isDesktop: Boolean,
    isReader: Boolean,
    hazeState: HazeState,
    onDismiss: () -> Unit,
    onToggleBookmark: () -> Unit,
    onFind: () -> Unit,
    onToggleReader: () -> Unit,
    onToggleDesktop: () -> Unit,
    onOpenLibrary: (Int) -> Unit,
    onTogglePrivate: () -> Unit,
    onUpdateSettings: ((androidx.datastore.preferences.core.MutablePreferences) -> Unit) -> Unit,
    onClearData: () -> Unit
) {
    val palette = LocalAMzPalette.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.36f))
            .clickable(onClick = onDismiss)
    ) {
        GlassSurface(
            cornerRadius = 34.dp,
            hazeState = hazeState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(8.dp)
                .fillMaxWidth()
                .clickable(enabled = false) {}
        ) {
            LazyColumn(
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        MenuTile(if (isBookmarked) "Saved" else "Bookmark", if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, isBookmarked, Modifier.weight(1f), onToggleBookmark)
                        MenuTile("Find", Icons.Rounded.Search, false, Modifier.weight(1f), onFind)
                        MenuTile("Reader", Icons.Rounded.MenuBook, isReader, Modifier.weight(1f), onToggleReader)
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        MenuTile("Desktop", Icons.Rounded.Computer, isDesktop, Modifier.weight(1f), onToggleDesktop)
                        MenuTile("Downloads", Icons.Rounded.Download, false, Modifier.weight(1f)) { onOpenLibrary(2) }
                        MenuTile("History", Icons.Rounded.History, false, Modifier.weight(1f)) { onOpenLibrary(1) }
                    }
                }
                item {
                    SettingSwitchRow("Ad Shield", Icons.Rounded.Shield, settings.adShield) { v ->
                        onUpdateSettings { it[SettingsRepo.AD_SHIELD] = v }
                    }
                }
                item {
                    SettingSwitchRow("HTTPS-Only", Icons.Rounded.Lock, settings.httpsOnly) { v ->
                        onUpdateSettings { it[SettingsRepo.HTTPS_ONLY] = v }
                    }
                }
                item {
                    SettingSwitchRow("Private Mode", Icons.Rounded.VisibilityOff, isPrivate) { onTogglePrivate() }
                }
                item {
                    SettingSwitchRow("Glass Blur", Icons.Rounded.BlurOn, settings.glassBlur) { v ->
                        onUpdateSettings { it[SettingsRepo.GLASS_BLUR] = v }
                    }
                }
                item {
                    SettingSwitchRow("Gyro Tilt", Icons.Rounded.Sensors, settings.gyroTilt) { v ->
                        onUpdateSettings { it[SettingsRepo.GYRO_TILT] = v }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AMzAccents.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .clickable { onUpdateSettings { it[SettingsRepo.ACCENT_HEX] = hex } }
                            )
                        }
                    }
                }
                item {
                    TextButton(onClick = onClearData) {
                        Text("Clear Browsing Data", color = palette.destructive)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val palette = LocalAMzPalette.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (active) palette.accent else palette.card)
            .jellyClickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        androidx.compose.material3.Icon(icon, contentDescription = label, tint = if (active) Color.White else palette.accent)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (active) Color.White else palette.textPrimary)
    }
}

@Composable
private fun AMzLibraryOverlay(
    selectedTab: Int,
    bookmarks: List<com.amz.browser.data.BookmarkEntry>,
    history: List<com.amz.browser.data.HistoryEntry>,
    onSelectTab: (Int) -> Unit,
    onOpenUrl: (String) -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit
) {
    val palette = LocalAMzPalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
    ) {
        Text("Library", style = MaterialTheme.typography.displayLarge, color = palette.textPrimary)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(palette.fill)
                .padding(3.dp)
        ) {
            listOf("Bookmarks", "History", "Downloads").forEachIndexed { idx, label ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selectedTab == idx) palette.card else Color.Transparent)
                        .clickable { onSelectTab(idx) }
                        .padding(vertical = 8.dp)
                ) {
                    Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = palette.textPrimary)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(bookmarks) { bm ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(palette.card)
                                .clickable { onOpenUrl(bm.url) }
                                .padding(14.dp)
                        ) {
                            Text(bm.title, color = palette.textPrimary, fontWeight = FontWeight.Medium)
                            Text(bm.url, style = MaterialTheme.typography.labelMedium, color = palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                1 -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(history) { h ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(palette.card)
                                .clickable { onOpenUrl(h.url) }
                                .padding(14.dp)
                        ) {
                            Text(h.title, color = palette.textPrimary, fontWeight = FontWeight.Medium)
                            Text(h.url, style = MaterialTheme.typography.labelMedium, color = palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                else -> BrowserServices.downloadEngine.Content()
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (selectedTab != 2) {
                TextButton(onClick = onClear) { Text("Clear", color = palette.destructive) }
            } else {
                Spacer(Modifier.width(1.dp))
            }
            TextButton(onClick = onDone) { Text("Done", color = palette.accent, fontWeight = FontWeight.SemiBold, fontSize = 17.sp) }
        }
    }
}

@Composable
private fun AMzSetupOverlay(
    settings: AppSettings,
    hazeState: HazeState,
    onUpdateSettings: ((androidx.datastore.preferences.core.MutablePreferences) -> Unit) -> Unit,
    onFinish: () -> Unit
) {
    val palette = LocalAMzPalette.current
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf(settings.userName) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        AuroraBackground()
        TiltCard(gyroEnabled = settings.gyroTilt) {
            GlassSurface(
                cornerRadius = 34.dp,
                hazeState = hazeState,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (step) {
                        0 -> {
                            Text("Welcome to AMz Browser", style = MaterialTheme.typography.headlineSmall, color = palette.textPrimary)
                            Text("What should we call you?", color = palette.textSecondary)
                            BasicTextField(
                                value = name,
                                onValueChange = { name = it },
                                singleLine = true,
                                textStyle = TextStyle(color = palette.textPrimary, fontSize = 17.sp),
                                cursorBrush = SolidColor(palette.accent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(palette.fill)
                                    .padding(16.dp)
                            )
                        }
                        1 -> {
                            Text("Appearance", style = MaterialTheme.typography.headlineSmall, color = palette.textPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AMzAccents.forEach { hex ->
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(hex))
                                            .clickable { onUpdateSettings { it[SettingsRepo.ACCENT_HEX] = hex } }
                                    )
                                }
                            }
                        }
                        else -> {
                            Text("How it feels", style = MaterialTheme.typography.headlineSmall, color = palette.textPrimary)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Glass Blur", color = palette.textPrimary)
                                Switch(checked = settings.glassBlur, onCheckedChange = { v -> onUpdateSettings { it[SettingsRepo.GLASS_BLUR] = v } })
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Gyro Tilt", color = palette.textPrimary)
                                Switch(checked = settings.gyroTilt, onCheckedChange = { v -> onUpdateSettings { it[SettingsRepo.GYRO_TILT] = v } })
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (step == 0) onUpdateSettings { it[SettingsRepo.USER_NAME] = name.trim() }
                            if (step < 2) step++ else onFinish()
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (step == 2) "Start Browsing" else "Continue", modifier = Modifier.padding(vertical = 6.dp))
                    }
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(onClick = onFinish) { Text("Skip", color = palette.textSecondary) }
                    }
                }
            }
        }
    }
}
