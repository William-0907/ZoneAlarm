package com.example.zonealarm.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zonealarm.data.AlarmHistoryEntity
import com.example.zonealarm.ui.viewmodels.AlarmViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AlarmHistoryScreen(viewModel: AlarmViewModel = viewModel()) {
    val history by viewModel.history.collectAsState()
    var selectedHistoryIds by remember { mutableStateOf(setOf<Int>()) }
    val isSelectionMode = selectedHistoryIds.isNotEmpty()
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var detailItem by remember { mutableStateOf<AlarmHistoryEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    if (isSelectionMode) {
                        Text("${selectedHistoryIds.size} selected", color = MaterialTheme.colorScheme.onBackground)
                    } else {
                        Text("Alarm History", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { selectedHistoryIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = Color.Red)
                        }
                    } else if (history.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearHistory() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear All", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No history recorded yet.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
            ) {
                items(history, key = { it.id }) { item ->
                    val isSelected = selectedHistoryIds.contains(item.id)
                    HistoryCard(
                        item = item,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelectionMode) {
                                selectedHistoryIds = if (isSelected) selectedHistoryIds - item.id else selectedHistoryIds + item.id
                            } else {
                                detailItem = item
                            }
                        },
                        onLongClick = {
                            selectedHistoryIds = selectedHistoryIds + item.id
                        }
                    )
                }
            }
        }
    }

    if (detailItem != null) {
        AlertDialog(
            onDismissRequest = { detailItem = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(detailItem!!.alarmName, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
                val timeSdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val date = Date(detailItem!!.timestamp)
                
                Column {
                    Text("Triggered on:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(sdf.format(date), color = MaterialTheme.colorScheme.onSurface)
                    Text(timeSdf.format(date), color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Event Type:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(detailItem!!.transitionType, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Location logging is active. View map in Alarms tab.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            },
            confirmButton = {
                TextButton(onClick = { detailItem = null }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryCard(
    item: AlarmHistoryEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val sdf = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
    val dateString = sdf.format(Date(item.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.alarmName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isSelected) {
                    Icon(Icons.Default.Info, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = item.transitionType,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = dateString, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
}
