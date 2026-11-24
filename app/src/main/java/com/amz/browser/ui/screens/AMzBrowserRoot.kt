package com.amz.browser.ui.screens

import com.amz.browser.R
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import kotlin.math.abs
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amz.browser.browser.TabManagerViewModel
import com.amz.browser.browser.WebViewFactory
import com.amz.browser.data.AppSettings
import com.amz.browser.data.PersistedTab
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
import com.amz.browser.ui.components.FingerTracker
import com.amz.browser.ui.components.GlassSurface
import com.amz.browser.ui.components.LocalTiltController
import com.amz.browser.ui.components.ProvideTiltSensor
import com.amz.browser.ui.components.SheetScrim
import com.amz.browser.ui.components.TiltCard
import com.amz.browser.ui.components.TiltDebugOverlay
import com.amz.browser.ui.components.tiltDebugLongPress
import com.amz.browser.ui.components.tiltParallax
import com.amz.browser.ui.components.jellyClickable
import com.amz.browser.ui.theme.AMzAccents
import com.amz.browser.ui.theme.AMzTheme
import com.amz.browser.ui.theme.AnimationSpeed
import com.amz.browser.ui.theme.LocalAMzPalette
import com.amz.browser.ui.theme.ThemeMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URI
import java.util.Calendar
import java.util.concurrent.atomic.AtomicIntegerArray

@Immutable
private data class FavoritePreset(
    val name: String,
    val iconRes: Int,
    val color: Long,
    val url: String
)

private val DefaultFavorites = listOf(
    FavoritePreset("Apple", R.drawable.ic_fav_apple, 0xFF1D1D1FL, "https://www.apple.com"),
    FavoritePreset("Wikipedia", R.drawable.ic_fav_wikipedia, 0xFF3F3F46L, "https://en.wikipedia.org"),
    FavoritePreset("YouTube", R.drawable.ic_fav_youtube, 0xFFFF3B30L, "https://www.youtube.com"),
    FavoritePreset("GitHub", R.drawable.ic_fav_github, 0xFF2C2C30L, "https://github.com"),
    FavoritePreset("Maps", R.drawable.ic_fav_maps, 0xFF30D158L, "https://www.openstreetmap.org"),
    FavoritePreset("News", R.drawable.ic_fav_news, 0xFFFF9F0AL, "https://news.ycombinator.com"),
    FavoritePreset("Music", R.drawable.ic_fav_music, 0xFFFF2D55L, "https://music.apple.com"),
    FavoritePreset("Mail", R.drawable.ic_fav_mail, 0xFF0A84FFL, "https://mail.google.com")
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
    host.contains("mail.google.com") -> R.drawable.ic_fav_mail
    else -> null
}

/**
 * Watches drag direction without consuming events, so the WebView and lazy
 * lists still scroll normally. Dragging down reveals the nav panel; dragging
 * up hides it while reading a long page.
 */
private fun Modifier.panelScrollWatcher(onScrollDown: (Boolean) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val down = awaitPointerEvent(PointerEventPass.Initial)
                val pointerId = down.changes.firstOrNull { it.pressed }?.id ?: continue
                val startY = down.changes.first { it.id == pointerId }.position.y
                var signaled = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                    if (!change.pressed) break
                    val dy = change.position.y - startY
                    if (!signaled && abs(dy) > 24f) {
                        onScrollDown(dy > 0f)
                        signaled = true
                    }
                }
            }
        }
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
    var showTiltDebug by remember { mutableStateOf(false) }
    var panelExpanded by remember { mutableStateOf(true) }
    var panelHeightPx by remember { mutableIntStateOf(0) }

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

    var lastPersistedTabs by remember { mutableStateOf<List<PersistedTab>?>(null) }

    LaunchedEffect(persistedTabs) {
        tabManager.restoreTabsIfFresh(persistedTabs)
    }

    // Tab state changes on every progress tick and page event. Debounce the
    // write and skip identical snapshots so DataStore does not re-emit (and
    // recompose the entire tree) dozens of times during a single load.
    LaunchedEffect(tabs) {
        delay(650)
        val snapshot = tabManager.toPersistedTabs()
        if (snapshot == lastPersistedTabs) return@LaunchedEffect
        lastPersistedTabs = snapshot
        repo.saveTabs(snapshot)
    }

    // A search results page can block hundreds of ad/tracker requests. Writing
    // one DataStore transaction per request caused a recomposition storm that
    // looked like flicker. Batch the counters and flush periodically instead.
    val pendingShield = remember { AtomicIntegerArray(3) }
    fun queueShield(ads: Int = 0, popups: Int = 0, redirects: Int = 0) {
        pendingShield.addAndGet(0, ads)
        pendingShield.addAndGet(1, popups)
        pendingShield.addAndGet(2, redirects)
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1200)
            val ads = pendingShield.getAndSet(0, 0)
            val popups = pendingShield.getAndSet(1, 0)
            val redirects = pendingShield.getAndSet(2, 0)
            if (ads + popups + redirects > 0) {
                repo.incrementShieldCounters(ads, popups, redirects)
            }
        }
    }

    fun ensureActiveWebView(requestedUrl: String = activeTab.url): WebView {
        val webView = tabManager.ensureWebView(
            tabId = activeTab.id,
            context = context,
            isDarkTheme = settings.themeMode == ThemeMode.DARK,
            textZoom = settings.textZoom,
            isAdShieldEnabled = { settings.adShield },
            isHttpsOnlyEnabled = { settings.httpsOnly },
            onAdBlocked = { queueShield(ads = 1) },
            onPopupBlocked = { queueShield(popups = 1) },
            onRedirectBlocked = { target ->
                blockedRedirectUrl = target
                queueShield(redirects = 1)
            },
            onPageFinished = { _, url, title, _, isPriv ->
                if (!isPriv) scope.launch { repo.recordHistory(title, url) }
            },
            onRequestFileChooser = onRequestFileChooser
        )
        // Idempotent: a recomposition or a re-attach must never reload the page.
        if (requestedUrl.isNotBlank() && webView.url != requestedUrl) {
            webView.loadUrl(requestedUrl)
        }
        return webView
    }

    fun navigate(rawInput: String) {
        val (resolvedUrl, _) = OmniboxResolver.resolve(rawInput, settings.searchEngine, settings.httpsOnly)
        if (resolvedUrl.isBlank()) return
        blockedRedirectUrl = null
        tabManager.updateTab(activeTab.id) { it.copy(url = resolvedUrl) }
        ensureActiveWebView(resolvedUrl)
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
            val baseHazeState = remember { HazeState() }
            // Never sample the live WebView for backdrop blur: it produces grey
            // and black frames when the surface is reallocated. When a page is
            // loaded we hand null to every GlassSurface, so only the app-drawn
            // aurora is ever blurred.
            val hazeState: HazeState? = if (activeTab.url.isBlank()) baseHazeState else null
            val fingerTracker = remember { FingerTracker() }

            val density = LocalDensity.current
            val measuredPanelHeight = with(density) { panelHeightPx.toDp() }
            val panelShift by animateDpAsState(
                targetValue = if (panelExpanded) 0.dp else measuredPanelHeight + 12.dp,
                animationSpec = tween(220),
                label = "panel collapse"
            )
            val visiblePanelHeight = (measuredPanelHeight - panelShift).coerceAtLeast(0.dp)
            val contentBottomInset by animateDpAsState(
                targetValue = visiblePanelHeight + 12.dp,
                animationSpec = tween(220),
                label = "content bottom inset"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(palette.background)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val pressed = event.changes.filter { it.pressed }
                                fingerTracker.position = pressed.firstOrNull()?.position ?: Offset.Unspecified
                            }
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = contentBottomInset)
                        .panelScrollWatcher { scrollingDown -> panelExpanded = !scrollingDown }
                ) {
                    if (activeTab.url.isBlank()) {
                        // Only the app-drawn aurora is a blur source.
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .hazeSource(baseHazeState)
                        ) {
                            AuroraBackground(fingerTracker = fingerTracker)
                        }
                        AMzStartPage(
                            userName = settings.userName,
                            isPrivate = activeTab.isPrivate,
                            stats = shieldStats,
                            history = history,
                            gyroEnabled = settings.gyroTilt,
                            hazeState = hazeState,
                            onOpenUrl = { navigate(it) },
                            showTiltDebug = showTiltDebug,
                            onToggleTiltDebug = { showTiltDebug = !showTiltDebug }
                        )
                    } else {
                        val wv = activeTab.webView
                        if (wv == null) {
                            // ensureWebView writes to the tab StateFlow, so it must
                            // never be called from a remember initializer during
                            // composition (that schedules another recomposition).
                            LaunchedEffect(activeTab.id) {
                                ensureActiveWebView()
                            }
                            Box(Modifier.fillMaxSize())
                        } else {
                            // Stable lambdas keep AndroidView from detaching/recreating
                            // the WebView on unrelated recompositions.
                            val factory: () -> WebView = remember(wv, activeTab.id) {
                                {
                                    wv.also { view ->
                                        // A tab switch can reuse this instance; detach
                                        // it from the old Compose container first.
                                        (view.parent as? ViewGroup)?.removeView(view)
                                        view.setBackgroundColor(palette.background.toArgb())
                                    }
                                }
                            }
                            val updateView: (WebView) -> Unit = remember(
                                wv,
                                palette.background,
                                settings.textZoom
                            ) {
                                { view ->
                                    view.setBackgroundColor(palette.background.toArgb())
                                    if (view.settings.textZoom != settings.textZoom) {
                                        view.settings.textZoom = settings.textZoom
                                    }
                                }
                            }
                            key(activeTab.id) {
                                AndroidView(
                                    factory = factory,
                                    update = updateView,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // Solid edge fades drawn as siblings: never alpha/blur an
                // ancestor of the WebView. They do not intercept touch input.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(palette.background.copy(alpha = 0.92f), Color.Transparent)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, palette.background.copy(alpha = 0.96f))
                            )
                        )
                )

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
                        .onGloballyPositioned { coordinates ->
                            val height = coordinates.size.height
                            if (panelHeightPx != height) panelHeightPx = height
                        }
                        .offset(y = panelShift)
                )

                // Tab Switcher Overlay
                if (showTabs) {
                    AMzTabSwitcherOverlay(
                        tabs = tabs,
                        activeTabId = activeTab.id,
                        gyroEnabled = settings.gyroTilt,
                        hazeState = hazeState,
                        onSelect = { id ->
                            // selectTab() captures the outgoing tab exactly once.
                            tabManager.selectTab(id)
                            showTabs = false
                        },
                        onCloseTab = { id ->
                            if (id == activeTab.id) tabManager.captureThumbnail(id)
                            tabManager.closeTab(id)
                        },
                        onNewTab = {
                            tabManager.captureThumbnail(activeTab.id)
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
                        },
                        showTiltDebug = showTiltDebug,
                        onToggleTiltDebug = { showTiltDebug = !showTiltDebug }
                    )
                }

                // Hidden tilt HUD: long-press the title for 3 s to toggle.
                if (showTiltDebug) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(12.dp)
                    ) {
                        TiltDebugOverlay(LocalTiltController.current)
                    }
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
    hazeState: HazeState?,
    onOpenUrl: (String) -> Unit,
    showTiltDebug: Boolean,
    onToggleTiltDebug: () -> Unit
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
        // The root wrapper already reserves the measured panel height; this is
        // just breathing room above the bottom fade.
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item(key = "greeting") {
            Column {
                Text(
                    text = if (!isPrivate && userName.isNotBlank()) "$greeting,\n$userName" else greeting,
                    style = MaterialTheme.typography.displayLarge,
                    color = palette.textPrimary,
                    modifier = Modifier
                        .tiltDebugLongPress { onToggleTiltDebug() }
                        .then(tiltParallax(4.dp))
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (isPrivate) "History is not saved in private tabs." else "Where to next?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textSecondary,
                    modifier = tiltParallax(8.dp)
                )
            }
        }

        item(key = "favorites") {
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

        item(key = "shield") {
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

        item(key = "recent") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Recently Visited", style = MaterialTheme.typography.headlineSmall, color = palette.textPrimary)
                GlassSurface(cornerRadius = 22.dp, hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
                    if (history.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            androidx.compose.material3.Icon(
                                Icons.Rounded.History,
                                contentDescription = null,
                                tint = palette.accent.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Nothing here yet",
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                "Pages you visit will show up here.",
                                color = palette.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            history.take(3).forEach { entry ->
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
                                        Text(entry.title.ifBlank { entry.url }, color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

/** Idle / Editing / Loaded drive every visible part of the omnibox. */
@Immutable
private enum class OmniboxState { Idle, Editing, Loaded }

@Composable
private fun AMzFloatingNavPanel(
    currentUrl: String,
    progress: Int,
    canGoBack: Boolean,
    canGoForward: Boolean,
    tabCount: Int,
    searchEngine: SearchEngine,
    hazeState: HazeState?,
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
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val host = remember(currentUrl) { runCatching { URI(currentUrl).host?.removePrefix("www.") }.getOrNull().orEmpty() }
    val isHttps = currentUrl.startsWith("https://", true)
    val siteIcon = remember(host) { siteIconFor(host) }
    val isLoading = progress in 1..99

    var focused by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf(OmniboxState.Idle) }
    var fieldValue by remember { mutableStateOf(TextFieldValue(host)) }
    var submittedText by remember { mutableStateOf<String?>(null) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }

    // The only text the user sees: typed text while editing/loading, otherwise
    // the host name derived from the WebView URL callbacks.
    val displayText by remember(host, currentUrl, fieldValue.text, submittedText, state, isLoading) {
        derivedStateOf {
            when {
                state == OmniboxState.Editing -> fieldValue.text
                submittedText != null -> submittedText!!
                else -> host.ifBlank { currentUrl }
            }
        }
    }

    LaunchedEffect(displayText) {
        if (state != OmniboxState.Editing && fieldValue.text != displayText) {
            fieldValue = TextFieldValue(displayText)
        }
    }
    LaunchedEffect(progress, currentUrl) {
        if (progress >= 100 && currentUrl.isNotBlank()) submittedText = null
    }

    fun stopEditing() {
        state = OmniboxState.Idle
        suggestions = emptyList()
        keyboard?.hide()
        focusRequester.freeFocus()
    }

    fun submit(text: String) {
        val input = text.trim()
        if (input.isBlank()) return
        submittedText = input
        stopEditing()
        onSubmit(input)
    }

    LaunchedEffect(fieldValue.text, state, focused, searchEngine) {
        val requested = fieldValue.text
        // The card is bound to focus, never to text alone.
        if (state != OmniboxState.Editing || !focused || requested.length < 2) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(220)
        if (state != OmniboxState.Editing || !focused || fieldValue.text != requested) return@LaunchedEffect
        val result = SuggestionsClient.fetch(requested, searchEngine)
        if (state == OmniboxState.Editing && focused && fieldValue.text == requested) {
            suggestions = result
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (state == OmniboxState.Editing && focused && fieldValue.text.isNotBlank()) {
            GlassSurface(
                cornerRadius = 22.dp,
                hazeState = hazeState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(6.dp)
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { submit(fieldValue.text) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Search for \"${fieldValue.text}\"", color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    suggestions.forEach { term ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { submit(term) }
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
                        if (state != OmniboxState.Editing && siteIcon != null) {
                            Image(
                                painter = painterResource(siteIcon),
                                contentDescription = null,
                                modifier = Modifier.size(19.dp)
                            )
                        } else {
                            androidx.compose.material3.Icon(
                                imageVector = if (isHttps && state != OmniboxState.Editing) Icons.Rounded.Lock else Icons.Rounded.Search,
                                contentDescription = null,
                                tint = if (isHttps && state != OmniboxState.Editing) palette.success else palette.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value = if (state == OmniboxState.Editing) fieldValue else TextFieldValue(displayText),
                            onValueChange = {
                                fieldValue = it
                                state = OmniboxState.Editing
                            },
                            singleLine = true,
                            textStyle = TextStyle(color = palette.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                            cursorBrush = SolidColor(palette.accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { submit(fieldValue.text) }),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .onFocusChanged { focus ->
                                    focused = focus.isFocused
                                    if (focus.isFocused && state != OmniboxState.Editing) {
                                        state = OmniboxState.Editing
                                        fieldValue = fieldValue.copy(
                                            selection = TextRange(0, fieldValue.text.length)
                                        )
                                    } else if (!focus.isFocused && state == OmniboxState.Editing) {
                                        state = OmniboxState.Idle
                                        suggestions = emptyList()
                                    }
                                },
                            decorationBox = { inner ->
                                if (displayText.isEmpty()) {
                                    Text("Search or enter website", color = palette.textSecondary, fontSize = 16.sp)
                                }
                                inner()
                            }
                        )
                        if (state == OmniboxState.Editing) {
                            TextButton(onClick = { stopEditing() }) {
                                Text("Cancel", color = palette.accent, fontSize = 13.sp)
                            }
                        }
                        if (!isLoading && state != OmniboxState.Editing && currentUrl.isNotBlank()) {
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

                    if (isLoading) {
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
                                .size(18.dp)
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
    hazeState: HazeState?,
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
                val thumbnail = remember(tab.thumbnail) { tab.thumbnail?.asImageBitmap() }
                TiltCard(gyroEnabled = gyroEnabled) {
                    GlassSurface(
                        cornerRadius = 20.dp,
                        hazeState = hazeState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(tab.id) }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(3f / 4f)
                                    .background(
                                        palette.accent.copy(
                                            alpha = if (tab.id == activeTabId) 0.25f else 0.12f
                                        )
                                    )
                            ) {
                                thumbnail?.let { bmp ->
                                    Image(
                                        bitmap = bmp,
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
                                    androidx.compose.material3.Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Close",
                                        tint = palette.textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val host = remember(tab.url) {
                                    runCatching { URI(tab.url).host?.removePrefix("www.") }.getOrNull().orEmpty()
                                }
                                val icon = remember(host) { siteIconFor(host) }
                                if (icon != null) {
                                    Image(
                                        painter = painterResource(icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    androidx.compose.material3.Icon(
                                        Icons.Rounded.Tab,
                                        contentDescription = null,
                                        tint = palette.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = tab.title.ifBlank { host.ifBlank { "New Tab" } },
                                    color = palette.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = palette.fill
            )
        )
    }
}

@Composable
private fun MenuSectionLabel(text: String) {
    val palette = LocalAMzPalette.current
    Text(
        text,
        color = palette.accent,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun <T> SettingChoiceRow(
    label: String,
    options: List<T>,
    selected: T,
    labelFor: (T) -> String,
    onSelect: (T) -> Unit
) {
    val palette = LocalAMzPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = palette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(palette.fill)
                .padding(3.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            options.forEach { option ->
                val active = option == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (active) palette.accent else Color.Transparent)
                        .clickable { onSelect(option) }
                ) {
                    Text(
                        labelFor(option),
                        color = if (active) Color.White else palette.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSliderRow(
    label: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    suffix: String,
    onChange: (Int) -> Unit
) {
    val palette = LocalAMzPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("$label  $value$suffix", color = palette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = palette.accent,
                inactiveTrackColor = palette.fill
            )
        )
    }
}

@Composable
private fun AMzQuickMenuSheet(
    settings: AppSettings,
    isBookmarked: Boolean,
    isPrivate: Boolean,
    isDesktop: Boolean,
    isReader: Boolean,
    hazeState: HazeState?,
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
    val density = LocalDensity.current
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.85f
    var dragY by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        SheetScrim(onDismiss = onDismiss)
        GlassSurface(
            cornerRadius = 34.dp,
            hazeState = hazeState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(8.dp)
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .offset(y = with(density) { dragY.toDp() })
                .clickable(enabled = false) {}
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { _, delta ->
                                    val next = dragY + delta.y
                                    // Rubber-band: resistance in both directions.
                                    dragY = if (next > 0f) next * 0.72f else next * 0.28f
                                },
                                onDragEnd = {
                                    if (dragY > 110f) onDismiss()
                                    dragY = 0f
                                },
                                onDragCancel = { dragY = 0f }
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(palette.textSecondary.copy(alpha = 0.55f))
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 56.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "browsing") {
                        MenuSectionLabel("Browsing")
                    }
                    item(key = "actions_one") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            MenuTile(if (isBookmarked) "Saved" else "Bookmark", if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, isBookmarked, Modifier.weight(1f), onToggleBookmark)
                            MenuTile("Find", Icons.Rounded.Search, false, Modifier.weight(1f), onFind)
                            MenuTile("Reader", Icons.Rounded.MenuBook, isReader, Modifier.weight(1f), onToggleReader)
                        }
                    }
                    item(key = "actions_two") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            MenuTile("Desktop", Icons.Rounded.Computer, isDesktop, Modifier.weight(1f), onToggleDesktop)
                            MenuTile("Downloads", Icons.Rounded.Download, false, Modifier.weight(1f)) { onOpenLibrary(2) }
                            MenuTile("History", Icons.Rounded.History, false, Modifier.weight(1f)) { onOpenLibrary(1) }
                        }
                    }
                    item(key = "privacy") {
                        MenuSectionLabel("Privacy")
                    }
                    item(key = "adshield") {
                        SettingSwitchRow("Ad Shield", Icons.Rounded.Shield, settings.adShield) { v ->
                            onUpdateSettings { it[SettingsRepo.AD_SHIELD] = v }
                        }
                    }
                    item(key = "https") {
                        SettingSwitchRow("HTTPS-Only", Icons.Rounded.Lock, settings.httpsOnly) { v ->
                            onUpdateSettings { it[SettingsRepo.HTTPS_ONLY] = v }
                        }
                    }
                    item(key = "private") {
                        SettingSwitchRow("Private Mode", Icons.Rounded.VisibilityOff, isPrivate) { onTogglePrivate() }
                    }
                    item(key = "appearance") {
                        MenuSectionLabel("Appearance")
                    }
                    item(key = "glass") {
                        SettingSwitchRow("Glass Blur", Icons.Rounded.BlurOn, settings.glassBlur) { v ->
                            onUpdateSettings { it[SettingsRepo.GLASS_BLUR] = v }
                        }
                    }
                    item(key = "gyro") {
                        SettingSwitchRow("Gyro Tilt", Icons.Rounded.Sensors, settings.gyroTilt) { v ->
                            onUpdateSettings { it[SettingsRepo.GYRO_TILT] = v }
                        }
                    }
                    item(key = "search_engine") {
                        SettingChoiceRow(
                            label = "Search Engine",
                            options = SearchEngine.values().toList(),
                            selected = settings.searchEngine,
                            labelFor = { it.label }
                        ) { engine ->
                            onUpdateSettings { it[SettingsRepo.SEARCH_ENGINE] = engine.name }
                        }
                    }
                    item(key = "text_size") {
                        SettingSliderRow("Text Size", settings.textZoom, 75f..200f, 5, "%") { zoom ->
                            onUpdateSettings { it[SettingsRepo.TEXT_ZOOM] = zoom }
                        }
                    }
                    item(key = "theme") {
                        SettingChoiceRow(
                            label = "Theme",
                            options = ThemeMode.values().toList(),
                            selected = settings.themeMode,
                            labelFor = { mode ->
                                when (mode) {
                                    ThemeMode.AUTO -> "Auto"
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                }
                            }
                        ) { mode ->
                            onUpdateSettings { it[SettingsRepo.THEME_MODE] = mode.name }
                        }
                    }
                    item(key = "anim_speed") {
                        SettingChoiceRow(
                            label = "Animation Speed",
                            options = AnimationSpeed.values().toList(),
                            selected = settings.animSpeed,
                            labelFor = { speed ->
                                when (speed) {
                                    AnimationSpeed.CALM -> "Calm"
                                    AnimationSpeed.FLUID -> "Fluid"
                                    AnimationSpeed.SNAPPY -> "Snappy"
                                }
                            }
                        ) { speed ->
                            onUpdateSettings { it[SettingsRepo.ANIM_SPEED] = speed.name }
                        }
                    }
                    item(key = "accent") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Accent", color = palette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
                    }
                    item(key = "clear") {
                        TextButton(onClick = onClearData) {
                            Text("Clear Browsing Data", color = palette.destructive)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, palette.card.copy(alpha = 0.96f))
                            )
                        )
                )
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
    hazeState: HazeState?,
    onUpdateSettings: ((androidx.datastore.preferences.core.MutablePreferences) -> Unit) -> Unit,
    onFinish: () -> Unit,
    showTiltDebug: Boolean = false,
    onToggleTiltDebug: () -> Unit = {}
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
        AuroraBackground(fingerTracker = null)
        TiltCard(gyroEnabled = settings.gyroTilt) {
            GlassSurface(
                cornerRadius = 34.dp,
                hazeState = hazeState,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    when (step) {
                        0 -> {
                            Text(
                                "Welcome to AMz Browser",
                                style = MaterialTheme.typography.headlineSmall,
                                color = palette.textPrimary,
                                modifier = Modifier
                                    .tiltDebugLongPress { onToggleTiltDebug() }
                                    .then(tiltParallax(4.dp))
                            )
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
                            Text("Theme", color = palette.textSecondary, fontSize = 14.sp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(palette.fill),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ThemeMode.values().forEach { mode ->
                                    val active = settings.themeMode == mode
                                    val label = when (mode) {
                                        ThemeMode.AUTO -> "Auto"
                                        ThemeMode.LIGHT -> "Light"
                                        ThemeMode.DARK -> "Dark"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(if (active) palette.accent else Color.Transparent)
                                            .clickable {
                                                onUpdateSettings { it[SettingsRepo.THEME_MODE] = mode.name }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            label,
                                            color = if (active) Color.White else palette.textSecondary,
                                            fontSize = 14.sp,
                                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                            modifier = Modifier.padding(vertical = 10.dp)
                                        )
                                    }
                                }
                            }
                            Text("Accent", color = palette.textSecondary, fontSize = 14.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AMzAccents.forEach { hex ->
                                    val active = settings.accentHex == hex
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(hex))
                                            .clickable { onUpdateSettings { it[SettingsRepo.ACCENT_HEX] = hex } },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (active) {
                                            androidx.compose.material3.Icon(
                                                Icons.Rounded.Close,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
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

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                if (step == 0) onUpdateSettings { it[SettingsRepo.USER_NAME] = name.trim() }
                                if (step < 2) step++ else onFinish()
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (step == 2) "Start Browsing" else "Continue",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TextButton(onClick = onFinish) {
                                Text("Skip", color = palette.textSecondary, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
