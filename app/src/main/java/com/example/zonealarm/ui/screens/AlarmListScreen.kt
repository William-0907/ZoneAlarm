package com.example.zonealarm.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zonealarm.AppBackground
import com.example.zonealarm.AppDarkBlue
import com.example.zonealarm.AppLightBlue
import com.example.zonealarm.AppPrimary
import com.example.zonealarm.data.AlarmEntity
import com.example.zonealarm.ui.viewmodels.AlarmViewModel
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmListScreen(
    onAddClick: () -> Unit,
    onEditClick: (AlarmEntity) -> Unit,
    viewModel: AlarmViewModel = viewModel()
) {
    val alarms by viewModel.alarms.collectAsState()
    var selectedAlarmIds by remember { mutableStateOf(setOf<Int>()) }
    val isSelectionMode = selectedAlarmIds.isNotEmpty()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = AppLightBlue)
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
                            contentDescription = "Delete",
                            tint = Color.Red
                        )
                    }
                } else {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Black, color = AppLightBlue)) {
                                append("ZONE")
                            }
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Normal, color = AppLightBlue.copy(alpha = 0.6f))) {
                                append("ALARM")
                            }
                        },
                        fontSize = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 1.dp, color = AppLightBlue.copy(alpha = 0.2f))

            if (alarms.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No alarms. Tap + to add one.", color = AppLightBlue.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(alarms, key = { it.id }) { alarm ->
                        val isSelected = selectedAlarmIds.contains(alarm.id)
                        AlarmRow(
                            alarm = alarm,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onToggleSelection = {
                                selectedAlarmIds = if (isSelected) {
                                    selectedAlarmIds - alarm.id
                                } else {
                                    selectedAlarmIds + alarm.id
                                }
                            },
                            onToggleEnabled = { isEnabled ->
                                viewModel.updateAlarm(alarm.copy(isEnabled = isEnabled))
                            },
                            onClick = {
                                if (isSelectionMode) {
                                    selectedAlarmIds = if (isSelected) {
                                        selectedAlarmIds - alarm.id
                                    } else {
                                        selectedAlarmIds + alarm.id
                                    }
                                } else {
                                    onEditClick(alarm)
                                }
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = AppLightBlue.copy(alpha = 0.1f))
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !isSelectionMode,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
        ) {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = AppPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Alarm")
            }
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                containerColor = AppDarkBlue,
                titleContentColor = AppLightBlue,
                textContentColor = AppLightBlue,
                title = { Text("Delete Alarms") },
                text = { Text("Are you sure you want to delete ${selectedAlarmIds.size} alarms?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val toDelete = alarms.filter { selectedAlarmIds.contains(it.id) }
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmRow(
    alarm: AlarmEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) AppPrimary.copy(alpha = 0.2f) else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onToggleSelection
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
                colors = CheckboxDefaults.colors(checkedColor = AppPrimary, uncheckedColor = AppLightBlue)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = alarm.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = if (alarm.isEnabled) AppLightBlue else AppLightBlue.copy(alpha = 0.4f)
            )
            val coords = String.format(Locale.US, "%.4f, %.4f", alarm.latitude, alarm.longitude)
            Text(
                text = coords,
                fontSize = 12.sp,
                color = if (alarm.isEnabled) AppLightBlue.copy(alpha = 0.6f) else AppLightBlue.copy(alpha = 0.2f)
            )
        }

        if (!isSelectionMode) {
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = AppPrimary,
                    uncheckedThumbColor = AppLightBlue,
                    uncheckedTrackColor = AppDarkBlue,
                    uncheckedBorderColor = AppLightBlue
                )
            )
        }
    }
}
