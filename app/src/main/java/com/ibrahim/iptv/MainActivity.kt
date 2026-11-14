package com.ibrahim.iptv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

private val Ink = Color(0xFF080A12)
private val Panel = Color(0xFF141827)
private val Violet = Color(0xFF9B83FF)
private val Muted = Color(0xFF9AA2B8)
private val White = Color(0xFFF7F7FC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = IptvRepository(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Ink, surface = Panel, primary = Violet,
                onBackground = White, onSurface = White
            )) {
                IbrahimApp(repo)
            }
        }
    }
}

data class TvEntry(
    val id: String,
    val title: String,
    val poster: String = "",
    val streamUrl: String = "",
    val group: String = "",
    val type: String = "vod"
)

@Composable
private fun IbrahimApp(repo: IptvRepository) {
    var page by remember { mutableStateOf("home") }
    var connected by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var server by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<TvEntry?>(null) }
    var entries by remember { mutableStateOf(emptyList<TvEntry>()) }
    var search by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val saved = repo.readCredentials()
        if (saved != null) {
            server = saved.first
            username = saved.second
            password = saved.third
            connected = true
            busy = true
            runCatching { repo.loadAll(server, username, password) }
                .onSuccess { entries = it }
                .onFailure { message = it.message ?: "Could not load provider" }
            busy = false
        }
    }

    Box(Modifier.fillMaxSize().background(Ink)) {
        if (selected != null) {
            TvPlayer(entry = selected!!, onBack = { selected = null })
        } else {
            Row(Modifier.fillMaxSize()) {
                SideRail(page = page, onSelect = { page = it })
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    TopHeader(onSearch = { page = "search" })
                    when (page) {
                        "settings" -> SettingsScreen(
                            server, { server = it }, username, { username = it },
                            password, { password = it }, busy, message,
                            onConnect = {
                                busy = true; message = ""
                                (repo).let { repository ->
                                    // Provider validation and catalog load run off the UI thread.
                                    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                        runCatching { repository.connect(server, username, password) }
                                            .onSuccess { entries = it; connected = true; message = "Provider connected successfully." }
                                            .onFailure { message = it.message ?: "Connection failed." }
                                        busy = false
                                    }
                                }
                            },
                            onDisconnect = { repo.clearCredentials(); connected = false; entries = emptyList(); message = "Provider disconnected." }
                        )
                        "search" -> {
                            SearchScreen(search, { search = it }, entries.filter { it.title.contains(search, true) }, { selected = it })
                        }
                        else -> HomeScreen(
                            entries = entries,
                            connected = connected,
                            busy = busy,
                            onConnect = { page = "settings" },
                            onPlay = { selected = it },
                            onBrowse = { page = it }
                        )
                    }
                }
            }
            if (message.isNotBlank() && page != "settings") {
                Text(message, color = Violet, modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp))
            }
        }
    }
}

@Composable
private fun SideRail(page: String, onSelect: (String) -> Unit) {
    val items = listOf(
        Triple("home", Icons.Default.Home, "Home"),
        Triple("live", Icons.Default.LiveTv, "Live TV"),
        Triple("movies", Icons.Default.Movie, "Movies"),
        Triple("series", Icons.Default.Tv, "Series"),
        Triple("search", Icons.Default.Search, "Search"),
        Triple("settings", Icons.Default.Settings, "Settings")
    )
    Column(
        Modifier.width(104.dp).fillMaxHeight().background(Color(0xFF0D101A))
            .padding(vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Text("I", color = Violet, fontSize = 34.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(10.dp))
        items.forEach { (id, icon, label) ->
            RailItem(icon, label, page == id) { onSelect(id) }
        }
    }
}

@Composable
private fun RailItem(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(
        Modifier.width(82.dp).clip(RoundedCornerShape(17.dp))
            .background(if (active) Color(0xFF292344) else if (focused) Color(0xFF282D40) else Color.Transparent)
            .border(if (focused) 2.dp else 1.dp, if (focused) Violet else Color.Transparent, RoundedCornerShape(17.dp))
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick).focusable().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = if (active || focused) Violet else Muted, modifier = Modifier.size(25.dp))
        Spacer(Modifier.height(5.dp))
        Text(label, fontSize = 10.sp, color = if (active || focused) White else Muted)
    }
}

@Composable
private fun TopHeader(onSearch: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 34.dp, vertical = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("IBRAHIM", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        Text(" IPTV", color = Violet, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.weight(1f))
        Text("STREAM YOUR WORLD", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.width(18.dp))
        IconButton(onClick = onSearch) { Icon(Icons.Default.Search, "Search", tint = White) }
    }
}

@Composable
private fun HomeScreen(
    entries: List<TvEntry>, connected: Boolean, busy: Boolean,
    onConnect: () -> Unit, onPlay: (TvEntry) -> Unit, onBrowse: (String) -> Unit
) {
    val live = entries.filter { it.type == "live" }
    val movies = entries.filter { it.type == "vod" }
    val series = entries.filter { it.type == "series" }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 34.dp, end = 30.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            HeroBanner(
                title = if (connected) "YOUR ENTERTAINMENT,\nALL IN ONE PLACE" else "WELCOME TO\nIBRAHIM IPTV",
                subtitle = if (connected) "Your channels and library are ready." else "Connect your IPTV provider to begin.",
                actionLabel = if (connected) "Browse live TV" else "Connect provider",
                onAction = { if (connected) onBrowse("live") else onConnect() }
            )
        }
        if (busy) item { Text("Loading your library…", color = Muted) }
        if (!connected) item {
            EmptyState("Connect your provider", "Add your authorized Xtream Codes account in Settings.", onConnect)
        } else {
            item { ContentRail("Continue exploring", movies.take(12), onPlay) }
            item { ContentRail("Live channels", live.take(16), onPlay) }
            item { ContentRail("Movies", movies.take(16), onPlay) }
            item { ContentRail("Series", series.take(16), onPlay) }
        }
    }
}

@Composable
private fun HeroBanner(title: String, subtitle: String, actionLabel: String, onAction: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(25.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF302451), Color(0xFF151B31), Color(0xFF101522))))
            .padding(30.dp)
    ) {
        Column(Modifier.align(Alignment.CenterStart).widthIn(max = 550.dp)) {
            Text("THE PREMIUM STREAMING EXPERIENCE", color = Violet, fontSize = 10.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            Text(title, fontSize = 33.sp, lineHeight = 39.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(9.dp))
            Text(subtitle, color = Muted, fontSize = 14.sp)
            Spacer(Modifier.height(18.dp))
            TvActionButton(actionLabel, onAction)
        }
        Text("IPTV", modifier = Modifier.align(Alignment.CenterEnd).padding(end = 35.dp),
            color = Color(0x337E6DD1), fontSize = 68.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun TvActionButton(label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Button(
        onClick = onClick,
        modifier = Modifier.onFocusChanged { focused = it.isFocused }
            .border(if (focused) 3.dp else 0.dp, if (focused) White else Color.Transparent, RoundedCornerShape(30.dp)),
        colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Color.White)
    ) { Text(label, fontWeight = FontWeight.Bold) }
}

@Composable
private fun ContentRail(title: String, items: List<TvEntry>, onPlay: (TvEntry) -> Unit) {
    if (items.isEmpty()) return
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("${items.size} items", color = Muted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(items) { entry -> PosterCard(entry, onPlay) }
        }
    }
}

@Composable
private fun PosterCard(entry: TvEntry, onPlay: (TvEntry) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(
        Modifier.width(142.dp).onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(14.dp))
            .border(if (focused) 3.dp else 1.dp, if (focused) Violet else Color(0xFF30364A), RoundedCornerShape(14.dp))
            .background(Panel).clickable { onPlay(entry) }.focusable()
    ) {
        Box(Modifier.fillMaxWidth().height(174.dp).background(Color(0xFF25243A)), contentAlignment = Alignment.Center) {
            if (entry.poster.isNotBlank()) {
                AsyncImage(model = entry.poster, contentDescription = entry.title, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Default.PlayCircle, null, tint = Violet, modifier = Modifier.size(42.dp))
            }
            if (focused) {
                Box(Modifier.align(Alignment.BottomEnd).padding(8.dp).background(Violet, RoundedCornerShape(20.dp)).padding(6.dp)) {
                    Icon(Icons.Default.PlayArrow, null, tint = White, modifier = Modifier.size(16.dp))
                }
            }
        }
        Text(entry.title, modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.LiveTv, null, tint = Violet, modifier = Modifier.size(38.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        TvActionButton("Open settings", onClick)
    }
}

@Composable
private fun SearchScreen(query: String, onQuery: (String) -> Unit, results: List<TvEntry>, onPlay: (TvEntry) -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp)) {
        Text("Search", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        BasicTextField(
            value = query, onValueChange = onQuery, singleLine = true,
            modifier = Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(12.dp)).padding(16.dp),
            textStyle = androidx.compose.ui.text.TextStyle(color = White, fontSize = 18.sp),
            decorationBox = { inner -> if (query.isEmpty()) Text("Search channels, movies and series…", color = Muted); inner() }
        )
        Spacer(Modifier.height(20.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) { items(results) { PosterCard(it, onPlay) } }
    }
}

@Composable
private fun SettingsScreen(
    server: String, onServer: (String) -> Unit,
    username: String, onUsername: (String) -> Unit,
    password: String, onPassword: (String) -> Unit,
    busy: Boolean, message: String, onConnect: () -> Unit, onDisconnect: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(38.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Settings", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("IPTV provider · Xtream Codes", color = Muted)
        TvInput("Server URL", server, onServer)
        TvInput("Username", username, onUsername)
        TvInput("Password", password, onPassword, secret = true)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TvActionButton(if (busy) "Connecting…" else "Connect provider", onConnect)
            TvActionButton("Disconnect", onDisconnect)
        }
        if (message.isNotBlank()) Text(message, color = Violet)
        Text("Use connection details from a provider you are authorized to access.", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun TvInput(label: String, value: String, onValue: (String) -> Unit, secret: Boolean = false) {
    Column(Modifier.fillMaxWidth(0.72f)) {
        Text(label, color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(5.dp))
        OutlinedTextField(value, onValue, modifier = Modifier.fillMaxWidth(),
            singleLine = true, visualTransformation = if (secret) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None)
    }
}

@Composable
private fun TvPlayer(entry: TvEntry, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(entry.streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(entry.streamUrl))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    Box(Modifier.fillMaxSize().background(Color.Black).onPreviewKeyEvent {
        if (it.type == KeyEventType.KeyDown && it.key == Key.Back) { onBack(); true } else false
    }) {
        AndroidView(factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
            modifier = Modifier.fillMaxSize())
        Text("‹  Back", color = White, modifier = Modifier.align(Alignment.TopStart).padding(22.dp)
            .background(Color(0x88000000), RoundedCornerShape(8.dp)).clickable { onBack() }.padding(12.dp))
    }
}
