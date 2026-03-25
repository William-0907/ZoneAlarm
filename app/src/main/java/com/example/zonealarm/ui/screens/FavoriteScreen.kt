package com.example.zonealarm.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zonealarm.AppBackground
import com.example.zonealarm.AppDarkBlue
import com.example.zonealarm.AppLightBlue
import com.example.zonealarm.AppPrimary
import com.example.zonealarm.data.AlarmEntity
import com.example.zonealarm.ui.viewmodels.AlarmViewModel
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(viewModel: AlarmViewModel) {
    val favorites by viewModel.alarms.collectAsState()
    var selectedAlarmIds by remember { mutableStateOf(setOf<Int>()) }
    val isSelectionMode = selectedAlarmIds.isNotEmpty()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { selectedAlarmIds = emptySet() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel selection", tint = AppLightBlue)
                    }
                    Text(
                        text = "${selectedAlarmIds.size} selected",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppLightBlue
                    )
                }
                IconButton(onClick = { showDeleteConfirmation = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete selected",
                        tint = Color.Red
                    )
                }
            } else {
                Text(
                    text = "FAVORITES",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = AppLightBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(thickness = 1.dp, color = AppLightBlue.copy(alpha = 0.2f))

        if (favorites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No favorites yet.", color = AppLightBlue.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(favorites, key = { it.id }) { alarm ->
                    val isSelected = selectedAlarmIds.contains(alarm.id)
                    FavoriteRow(
                        alarm = alarm,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onToggleSelection = {
                            selectedAlarmIds = if (isSelected) {
                                selectedAlarmIds - alarm.id
                            } else {
                                selectedAlarmIds + alarm.id
                            }
                        }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = AppLightBlue.copy(alpha = 0.1f))
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            containerColor = AppDarkBlue,
            titleContentColor = AppLightBlue,
            textContentColor = AppLightBlue,
            title = { Text("Delete Favorites") },
            text = { Text("Are you sure you want to delete ${selectedAlarmIds.size} favorites?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = favorites.filter { selectedAlarmIds.contains(it.id) }
                        toDelete.forEach { viewModel.deleteAlarm(it) }
                        selectedAlarmIds = emptySet()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel", color = AppLightBlue)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoriteRow(
    alarm: AlarmEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) AppPrimary.copy(alpha = 0.2f) else Color.Transparent)
            .combinedClickable(
                onClick = { if (isSelectionMode) onToggleSelection() },
                onLongClick = onToggleSelection
            )
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    colors = CheckboxDefaults.colors(checkedColor = AppPrimary, uncheckedColor = AppLightBlue)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column {
                Text(
                    text = alarm.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppLightBlue
                )
                val coords = String.format(Locale.US, "%.4f, %.4f", alarm.latitude, alarm.longitude)
                Text(text = coords, fontSize = 12.sp, color = AppLightBlue.copy(alpha = 0.6f))
            }
        }
        Text(text = "${(alarm.radius/1000).toInt()}km", color = AppLightBlue.copy(alpha = 0.7f), fontSize = 14.sp)
    }
}
