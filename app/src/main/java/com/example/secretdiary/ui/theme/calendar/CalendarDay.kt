package com.example.secretdiary.ui.theme.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

// 1. DATA MODEL
// Now actually used to pass data between the Grid and the Item
data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean = true,
    val hasEntry: Boolean = false
)

// 2. MAIN CALENDAR COMPONENT
@Composable
fun EnhancedCalendarView(
    entryDates: List<LocalDate>,
    onDateSelected: (LocalDate) -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    val daysInMonth = currentMonth.lengthOfMonth()
    val firstOfMonth = currentMonth.atDay(1)
    // 0 = Sunday, 1 = Monday, etc.
    val firstDayOfWeek = firstOfMonth.dayOfWeek.value % 7

    Card(
        modifier = Modifier.padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month & Navigation Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
                }
                Text(
                    text = "${currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days of the Week Row
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // The Calendar Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.height(260.dp)
            ) {
                // Fill empty slots for previous month days
                items(firstDayOfWeek) {
                    Spacer(modifier = Modifier.size(40.dp))
                }

                // Fill actual days of the month
                items(daysInMonth) { dayIndex ->
                    val date = currentMonth.atDay(dayIndex + 1)

                    // Create the CalendarDay object here
                    val calendarDay = CalendarDay(
                        date = date,
                        isCurrentMonth = true,
                        hasEntry = entryDates.contains(date)
                    )

                    CalendarDayItem(
                        day = calendarDay,
                        onClick = { onDateSelected(calendarDay.date) }
                    )
                }
            }
        }
    }
}

// 3. INDIVIDUAL DAY ITEM
@Composable
fun CalendarDayItem(
    day: CalendarDay, // Corrected: Now accepts the data class
    onClick: () -> Unit
) {
    val isToday = day.date == LocalDate.now()

    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Indicator for Diary Entry
        if (day.hasEntry) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(6.dp)
                    .background(
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        shape = CircleShape
                    )
            )
        }
    }
}