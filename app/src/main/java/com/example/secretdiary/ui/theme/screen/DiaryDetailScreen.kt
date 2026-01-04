package com.example.secretdiary.ui.theme.screen

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import coil.compose.rememberAsyncImagePainter
import com.example.secretdiary.ui.theme.viewmodel.DiaryDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class AttachmentViewMode { GRID, LIST }

private data class AttachmentItem(
    val uri: String,
    val isVideo: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun DiaryDetailScreen(
    viewModel: DiaryDetailViewModel,
    entryId: Int,
    onNavigateBack: () -> Unit,
    savedStateHandle: SavedStateHandle?
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val entry by viewModel.entry.collectAsState()
    val location by viewModel.location.collectAsState()

    // Last captured media shown on this screen (photo OR video)
    var lastCapturedUri by remember { mutableStateOf<String?>(null) }
    var lastCapturedIsVideo by remember { mutableStateOf(false) }

    var viewMode by remember { mutableStateOf(AttachmentViewMode.GRID) }

    LaunchedEffect(entryId) {
        if (entryId != -1) viewModel.loadEntry(entryId)
    }

    // Reads result passed via savedStateHandle
    DisposableEffect(lifecycleOwner, savedStateHandle) {
        if (savedStateHandle == null) return@DisposableEffect onDispose { }

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val uriString = savedStateHandle.get<String>("media_uri")
                val isVideo = savedStateHandle.get<Boolean>("is_video") ?: false

                if (!uriString.isNullOrBlank()) {
                    viewModel.onMediaCaptured(uriString, isVideo)

                    lastCapturedUri = uriString
                    lastCapturedIsVideo = isVideo

                    savedStateHandle.remove<String>("media_uri")
                    savedStateHandle.remove<Boolean>("is_video")
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Parse attachments from content
    val attachments: List<AttachmentItem> = remember(entry?.content) {
        extractAttachmentsFromContent(entry?.content.orEmpty())
    }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diary Entry") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                // ✅ IMPORTANT: respect TopAppBar padding so nothing overlaps
                .padding(innerPadding)
                // ✅ Wider screen: smaller side padding
                .padding(horizontal = 8.dp)
        ) {
            // ✅ Extra breathing room below the app bar title (optional)
            Spacer(modifier = Modifier.height(12.dp))

            // ✅ TITLE heading
            Text(
                text = "Title",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            // ✅ Title input
            OutlinedTextField(
                value = entry?.title.orEmpty(),
                onValueChange = { viewModel.onTitleChange(it) },
                placeholder = { Text("Enter your title...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ✅ CONTENT heading
            Text(
                text = "Content",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            // ✅ Bigger content typing area
            OutlinedTextField(
                value = entry?.content.orEmpty(),
                onValueChange = { viewModel.onContentChange(it) },
                placeholder = { Text("Write your diary entry here...") },
                singleLine = false,
                maxLines = 50,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.saveEntry(); onNavigateBack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Save")
            }

            // Last captured preview (photo OR video)
            if (!lastCapturedUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Last captured:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (lastCapturedIsVideo) {
                    VideoThumbnail(
                        context = context,
                        uri = lastCapturedUri!!,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(12.dp)
                            )
                    )
                } else {
                    Image(
                        painter = rememberAsyncImagePainter(model = lastCapturedUri),
                        contentDescription = "Last image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(12.dp)
                            ),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Attachments header + toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attachments (${attachments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = {
                    viewMode =
                        if (viewMode == AttachmentViewMode.GRID) AttachmentViewMode.LIST
                        else AttachmentViewMode.GRID
                }) {
                    Icon(
                        imageVector = if (viewMode == AttachmentViewMode.GRID)
                            Icons.AutoMirrored.Filled.List
                        else
                            Icons.Default.GridView,
                        contentDescription = "Toggle view"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (attachments.isEmpty()) {
                Text(
                    text = "No attachments yet. Capture from Diary List screen.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                if (viewMode == AttachmentViewMode.GRID) {
                    AttachmentsGrid(items = attachments, onClick = { })
                } else {
                    AttachmentsList(items = attachments, onClick = { })
                }
            }

            if (location != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Location: ${location!!.latitude}, ${location!!.longitude}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// -------------------- UI: GRID --------------------
@Composable
private fun AttachmentsGrid(
    items: List<AttachmentItem>,
    onClick: (AttachmentItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            if (item.isVideo) {
                VideoTile(item, onClick)
            } else {
                ImageTile(item, onClick)
            }
        }
    }
}

@Composable
private fun ImageTile(item: AttachmentItem, onClick: (AttachmentItem) -> Unit) {
    Image(
        painter = rememberAsyncImagePainter(model = item.uri),
        contentDescription = "Image",
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .clickable { onClick(item) },
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun VideoTile(item: AttachmentItem, onClick: (AttachmentItem) -> Unit) {
    val context = LocalContext.current
    VideoThumbnail(
        context = context,
        uri = item.uri,
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
            .clickable { onClick(item) }
    )
}

// -------------------- UI: LIST --------------------
@Composable
private fun AttachmentsList(
    items: List<AttachmentItem>,
    onClick: (AttachmentItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items.size) { index ->
            val item = items[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .clickable { onClick(item) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.isVideo) {
                    val context = LocalContext.current
                    VideoThumbnail(
                        context = context,
                        uri = item.uri,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Image(
                        painter = rememberAsyncImagePainter(model = item.uri),
                        contentDescription = "Image",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.size(12.dp))

                Text(
                    text = if (item.isVideo) "Video attachment" else "Image attachment",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// -------------------- Video Thumbnail --------------------
@Composable
private fun VideoThumbnail(
    context: Context,
    uri: String,
    modifier: Modifier = Modifier
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = uri) {
        value = loadVideoThumbnail(context, uri)
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Video thumbnail",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Column(
            modifier = modifier.padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Videocam, contentDescription = "Video", modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text("Video", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private suspend fun loadVideoThumbnail(context: Context, uriString: String): Bitmap? {
    return withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            val uri = uriString.toUri()
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                retriever.setDataSource(pfd.fileDescriptor)
                retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }
}

// -------------------- Parsing helpers --------------------
private fun extractAttachmentsFromContent(content: String): List<AttachmentItem> {
    val out = mutableListOf<AttachmentItem>()

    Regex("""\[Image:\s*(.+?)]""").findAll(content).forEach { match ->
        val uri = match.groupValues[1].trim()
        if (uri.isNotBlank()) out.add(AttachmentItem(uri = uri, isVideo = false))
    }

    Regex("""\[Video:\s*(.+?)]""").findAll(content).forEach { match ->
        val uri = match.groupValues[1].trim()
        if (uri.isNotBlank()) out.add(AttachmentItem(uri = uri, isVideo = true))
    }

    return out.reversed()
}
