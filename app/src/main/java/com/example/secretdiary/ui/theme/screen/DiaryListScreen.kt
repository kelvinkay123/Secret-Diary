package com.example.secretdiary.ui.theme.screen

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.secretdiary.data.location.LocationHelper
import com.example.secretdiary.ui.theme.calendar.EnhancedCalendarView
import com.example.secretdiary.ui.theme.viewmodel.DiaryEntry
import com.example.secretdiary.ui.theme.viewmodel.DiaryListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryListScreen(
    viewModel: DiaryListViewModel,
    onAddEntry: () -> Unit,
    onEntryClick: (Int) -> Unit,
    onOpenCamera: () -> Unit,
    onCreateEntryWithMedia: (String, Boolean) -> Unit,
    savedStateHandle: SavedStateHandle?
) {
    val entriesState by viewModel.allEntries.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val searchQuery = remember { mutableStateOf("") }
    val showCalendar = remember { mutableStateOf(false) }
    val deleteEntryToConfirm = remember { mutableStateOf<DiaryEntry?>(null) }
    val currentLocationName = remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }

    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = (results[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (results[Manifest.permission.ACCESS_COARSE_LOCATION] == true)

        if (granted) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Fetching location…") }
            fetchLocation(
                context = context,
                coroutineScope = coroutineScope,
                onLocationName = { currentLocationName.value = it },
                onError = { msg -> coroutineScope.launch { snackbarHostState.showSnackbar(msg) } }
            )
        } else {
            coroutineScope.launch { snackbarHostState.showSnackbar("Location permission denied.") }
        }
    }

    fun onLocationIconTap() {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        coroutineScope.launch { snackbarHostState.showSnackbar("Fetching location…") }
        fetchLocation(
            context = context,
            coroutineScope = coroutineScope,
            onLocationName = { currentLocationName.value = it },
            onError = { msg -> coroutineScope.launch { snackbarHostState.showSnackbar(msg) } }
        )
    }

    DisposableEffect(lifecycleOwner, savedStateHandle) {
        if (savedStateHandle == null) return@DisposableEffect onDispose { }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val uriString = savedStateHandle.get<String>("media_uri")
                val isVideo = savedStateHandle.get<Boolean>("is_video") ?: false

                if (!uriString.isNullOrBlank()) {
                    onCreateEntryWithMedia(uriString, isVideo)
                    savedStateHandle.remove<String>("media_uri")
                    savedStateHandle.remove<Boolean>("is_video")
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            fetchLocation(
                context = context,
                coroutineScope = coroutineScope,
                onLocationName = { currentLocationName.value = it },
                onError = { /* silent */ }
            )
        }
    }

    val showScrollToTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    val entryDates = remember(entriesState) {
        entriesState.map {
            Instant.ofEpochMilli(it.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }
    }

    val filteredList = remember(searchQuery.value, entriesState) {
        if (searchQuery.value.isBlank()) {
            entriesState
        } else {
            entriesState.filter { entry ->
                val dateStr = Instant.ofEpochMilli(entry.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .format(dateFormatter)

                entry.title.contains(searchQuery.value, ignoreCase = true) ||
                        entry.content.contains(searchQuery.value, ignoreCase = true) ||
                        dateStr.contains(searchQuery.value, ignoreCase = true)
            }
        }
    }

    deleteEntryToConfirm.value?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteEntryToConfirm.value = null },
            title = { Text("Delete Entry?") },
            text = { Text("Are you sure you want to delete \"${entry.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEntry(entry)
                        deleteEntryToConfirm.value = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteEntryToConfirm.value = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("My Diary") },
                actions = {
                    IconButton(onClick = { onLocationIconTap() }) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Get help location",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { showCalendar.value = !showCalendar.value }) {
                        Icon(
                            imageVector = if (showCalendar.value) Icons.Default.Close else Icons.Default.DateRange,
                            contentDescription = "Toggle Calendar"
                        )
                    }

                    AnimatedVisibility(visible = currentLocationName.value != null, enter = fadeIn()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = currentLocationName.value ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(
                    visible = showScrollToTop,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    FloatingActionButton(
                        onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
                        modifier = Modifier.padding(bottom = 12.dp),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Top")
                    }
                }

                FloatingActionButton(
                    onClick = onOpenCamera,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = "Camera")
                }

                FloatingActionButton(onClick = onAddEntry) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedVisibility(
                visible = showCalendar.value,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                EnhancedCalendarView(
                    entryDates = entryDates,
                    onDateSelected = { selectedDate ->
                        searchQuery.value = selectedDate.format(dateFormatter)
                        showCalendar.value = false
                    }
                )
            }

            TextField(
                value = searchQuery.value,
                onValueChange = { searchQuery.value = it },
                placeholder = { Text("Search diary...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.value.isNotEmpty()) {
                        IconButton(onClick = { searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true
            )

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val emptyMessage =
                        if (entriesState.isEmpty()) "Your diary is empty." else "No results found."
                    Text(emptyMessage)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredList, key = { it.id }) { entry ->
                        DiaryEntryItem(
                            entry = entry,
                            formatter = dateFormatter,
                            onClick = { onEntryClick(entry.id) },
                            onDelete = { deleteEntryToConfirm.value = entry }
                        )
                    }
                }
            }
        }
    }
}

/**
 * ✅ Shared fetch function (called on start + on icon tap)
 */
private fun fetchLocation(
    context: android.content.Context,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onLocationName: (String) -> Unit,
    onError: (String) -> Unit
) {
    val locationHelper = LocationHelper(context)
    locationHelper.getCurrentLocation { location ->
        if (location == null) {
            onError("Location unavailable.")
            return@getCurrentLocation
        }

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val city = address.locality ?: address.subAdminArea ?: "Unknown"
                    val country = address.countryCode ?: ""
                    val label = if (country.isNotEmpty()) "$city, $country" else city

                    withContext(Dispatchers.Main) { onLocationName(label) }
                } else {
                    withContext(Dispatchers.Main) { onError("Location unavailable.") }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { onError("Location unavailable.") }
            }
        }
    }
}

@Composable
private fun VideoPlayerView(
    videoUri: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val player = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri.toUri()))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = true
            }
        },
        update = { view ->
            view.player = player
        }
    )
}

@Composable
fun DiaryEntryItem(
    entry: DiaryEntry,
    formatter: DateTimeFormatter,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateString = remember(entry.timestamp) {
        Instant.ofEpochMilli(entry.timestamp)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(end = 40.dp)) {

                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 🎥 VIDEO
                if (entry.isVideo && !entry.mediaUri.isNullOrBlank()) {
                    VideoPlayerView(
                        videoUri = entry.mediaUri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    )

                    // 📷 IMAGE
                } else if (!entry.imageUri.isNullOrBlank()) {
                    AsyncImage(
                        model = entry.imageUri,
                        contentDescription = "Diary image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )

                    // 📝 TEXT ONLY
                } else {
                    Text(
                        text = entry.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
