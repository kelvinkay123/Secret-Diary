// FIX: Corrected package to match the file's directory location
package com.example.secretdiary.data.ui.screen

import android.annotation.SuppressLint
// FIX: Added all necessary imports for Layout components
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
// FIX: Import the AutoMirrored version of the ArrowBack icon
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Done
// FIX: Added all necessary imports for Material3 components
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import coil.compose.AsyncImage
// FIX: Ensured ViewModel import path is correct relative to the new package
import com.example.secretdiary.data.ui.viewmodel.DiaryDetailViewModel

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

    // Effect to load the diary entry when the screen is first composed
    LaunchedEffect(entryId) {
        viewModel.loadEntry(entryId)
    }

    // Effect to observe the result from CameraScreen
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getLiveData<String>("image_uri")?.observeForever { uri ->
            if (uri != null) {
                viewModel.onImageUriChange(uri)
                // Clear the value so it's not processed again on config/screen changes
                savedStateHandle.remove<String>("image_uri")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (entryId == -1) "New Entry" else "Edit Entry") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        // FIX: Use the AutoMirrored ArrowBack icon
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Text field for the diary entry title
            OutlinedTextField(
                value = entry?.title ?: "",
                onValueChange = { viewModel.onTitleChange(it) },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            // Text field for the diary entry content
            OutlinedTextField(
                value = entry?.content ?: "",
                onValueChange = { viewModel.onContentChange(it) },
                label = { Text("Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Takes up remaining space
            )

            // Button to launch the camera
            Button(onClick = onOpenCamera) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Open Camera")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Photo")
            }

            // Display the captured image if one exists
            if (imageUri.isNotEmpty()) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Captured image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp) // Set a fixed height for the preview
                )
            }
        }
    }
}
