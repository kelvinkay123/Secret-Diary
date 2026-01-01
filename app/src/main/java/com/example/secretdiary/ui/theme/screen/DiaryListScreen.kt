package com.example.secretdiary.ui.theme.screen

import android.location.Geocoder
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    onEntryClick: (Int) -> Unit
) {
    // 1. Collect State
    val entriesState by viewModel.allEntries.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 2. UI State
    var searchQuery by remember { mutableStateOf("") }
    var showCalendar by remember { mutableStateOf(false) }
    var deleteEntryToConfirm by remember { mutableStateOf<DiaryEntry?>(null) }

    // NEW: Location State
    var currentLocationName by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    // Date Formatter
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }

    // --- NEW: Fetch Location Logic ---
    LaunchedEffect(Unit) {
        val locationHelper = LocationHelper(context)
        locationHelper.getCurrentLocation { location ->
            if (location != null) {
                // Geocoding must happen on a background thread (IO)
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            val city = address.locality ?: address.subAdminArea ?: "Unknown"
                            val country = address.countryCode ?: ""
                            // Update state on Main thread
                            withContext(Dispatchers.Main) {
                                currentLocationName = if (country.isNotEmpty()) "$city, $country" else city
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    // Derived States
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    val entryDates = remember(entriesState) {
        entriesState.map {
            Instant.ofEpochMilli(it.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }
    }

    val filteredList = remember(searchQuery, entriesState) {
        if (searchQuery.isBlank()) {
            entriesState
        } else {
            entriesState.filter { entry ->
                val dateStr = Instant.ofEpochMilli(entry.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .format(dateFormatter)

                entry.title.contains(searchQuery, ignoreCase = true) ||
                        entry.content.contains(searchQuery, ignoreCase = true) ||
                        dateStr.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Dialog Logic
    deleteEntryToConfirm?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleteEntryToConfirm = null },
            title = { Text("Delete Entry?") },
            text = { Text("Are you sure you want to delete \"${entry.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEntry(entry)
                        deleteEntryToConfirm = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteEntryToConfirm = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Diary") },
                actions = {
                    // --- NEW: Display Location in Top Right ---
                    AnimatedVisibility(visible = currentLocationName != null, enter = fadeIn()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place, // Ensure this icon is imported
                                contentDescription = "Location",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentLocationName ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Existing Calendar Toggle
                    IconButton(onClick = { showCalendar = !showCalendar }) {
                        Icon(
                            imageVector = if (showCalendar) Icons.Default.Close else Icons.Default.DateRange,
                            contentDescription = "Toggle Calendar"
                        )
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
                visible = showCalendar,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                EnhancedCalendarView(
                    entryDates = entryDates,
                    onDateSelected = { selectedDate ->
                        searchQuery = selectedDate.format(dateFormatter)
                        showCalendar = false
                    }
                )
            }

            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search diary...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
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
                    val emptyMessage = if (entriesState.isEmpty()) "Your diary is empty." else "No results found."
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
                            onDelete = { deleteEntryToConfirm = entry }
                        )
                    }
                }
            }
        }
    }
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = entry.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
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