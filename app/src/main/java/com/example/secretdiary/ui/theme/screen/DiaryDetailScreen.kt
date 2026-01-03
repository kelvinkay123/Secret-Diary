package com.example.secretdiary.ui.theme.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.secretdiary.ui.theme.viewmodel.DiaryDetailViewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryDetailScreen(
    viewModel: DiaryDetailViewModel,
    entryId: Int,
    onNavigateBack: () -> Unit,
    onOpenCamera: () -> Unit,
    savedStateHandle: SavedStateHandle?
) {
    val entry by viewModel.entry.collectAsState()
    val imageUri by viewModel.imageUri.collectAsState()
    val location by viewModel.location.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasLocationPermission = isGranted
            if (isGranted) viewModel.loadLocation()
        }
    )

    // Load entry once (when editing)
    LaunchedEffect(entryId) {
        if (entryId != -1) viewModel.loadEntry(entryId)
    }

    /**
     * ✅ FIXED:
     * - Reads NEW keys: "media_uri" + "is_video"
     * - No observeForever leaks
     * - Triggers reliably when coming back from CameraScreen (ON_RESUME)
     */
    DisposableEffect(lifecycleOwner, savedStateHandle) {
        if (savedStateHandle == null) return@DisposableEffect onDispose { }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val uriString = savedStateHandle.get<String>("media_uri")
                val isVideo = savedStateHandle.get<Boolean>("is_video") ?: false

                if (!uriString.isNullOrBlank()) {
                    // ✅ this makes onMediaCaptured USED and also saves image/video correctly
                    viewModel.onMediaCaptured(uriString, isVideo)

                    savedStateHandle.remove<String>("media_uri")
                    savedStateHandle.remove<Boolean>("is_video")
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Auto-load location for new entries (only if not already set)
    LaunchedEffect(hasLocationPermission, entry?.latitude, location) {
        val hasAnyLocation = (entry?.latitude != null) || (location != null)
        if (hasLocationPermission && !hasAnyLocation) {
            viewModel.loadLocation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (entryId == -1) "New Entry" else "Edit Entry") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val isLocationSet = entry?.latitude != null || location != null

                    IconButton(
                        onClick = {
                            if (hasLocationPermission) {
                                viewModel.loadLocation()
                            } else {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Get Location",
                            modifier = Modifier.size(32.dp),
                            tint = if (isLocationSet)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Button(
                    onClick = onOpenCamera,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Open Camera")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Photo / Video")
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.saveEntry()
                    onNavigateBack()
                }
            ) {
                Icon(Icons.Default.Done, contentDescription = "Save Entry")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = entry?.title ?: "",
                onValueChange = { viewModel.onTitleChange(it) },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            val locationText = when {
                entry?.latitude != null -> "📍 ${entry?.latitude}, ${entry?.longitude}"
                location != null -> "📍 ${location?.latitude}, ${location?.longitude}"
                else -> null
            }

            if (locationText != null) {
                Text(
                    text = locationText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            OutlinedTextField(
                value = entry?.content ?: "",
                onValueChange = { viewModel.onContentChange(it) },
                label = { Text("What's on your mind?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // Preview last captured media
            imageUri?.let { uri ->
                if (uri.isNotBlank()) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Captured media",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }
            }
        }
    }
}
