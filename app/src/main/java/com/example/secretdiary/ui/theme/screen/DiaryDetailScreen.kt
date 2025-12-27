package com.example.secretdiary.ui.theme.screen

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.lifecycle.SavedStateHandle
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
        }
    )

    LaunchedEffect(entryId) {
        if (entryId != -1) {
            viewModel.loadEntry(entryId)
        }
    }

    LaunchedEffect(savedStateHandle, hasLocationPermission) {
        savedStateHandle?.getLiveData<String>("image_uri")?.observeForever { uri ->
            if (uri != null) {
                viewModel.onImageUriChange(uri)
                if (hasLocationPermission) {
                    viewModel.loadLocation()
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
                savedStateHandle.remove<String>("image_uri")
            }
        }
        if (hasLocationPermission && entry?.latitude == null) {
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
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.saveEntry()
                onNavigateBack()
            }) {
                Icon(Icons.Default.Done, contentDescription = "Save Entry")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), // Make the column scrollable
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = entry?.title ?: "",
                onValueChange = { viewModel.onTitleChange(it) },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = entry?.content ?: "",
                onValueChange = { viewModel.onContentChange(it) },
                label = { Text("What's on your mind?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(onClick = onOpenCamera, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Open Camera")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Photo")
                }

                Button(
                    onClick = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                    modifier = Modifier.weight(1f),
                    enabled = entry?.latitude == null
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Get Location")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Get Location")
                }
            }

            val locationText = when {
                entry?.latitude != null -> "Location Saved: ${entry?.latitude}, ${entry?.longitude}"
                location != null -> "Current Location: ${location?.latitude}, ${location?.longitude}"
                else -> null
            }
            if (locationText != null) {
                Text(
                    text = locationText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            imageUri?.let { uri ->
                if (uri.isNotEmpty()) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Captured image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }
        }
    }
}
