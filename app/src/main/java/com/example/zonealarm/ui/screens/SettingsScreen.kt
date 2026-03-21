package com.example.zonealarm.ui.screens

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zonealarm.*
import com.example.zonealarm.ui.viewmodels.AlarmViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: AlarmViewModel = viewModel()) {
    val context = LocalContext.current
    val history by viewModel.history.collectAsState()
    var showHistory by remember { mutableStateOf(false) }
    
    val prefs = remember { context.getSharedPreferences("ZoneAlarmPrefs", Context.MODE_PRIVATE) }
    var ringtoneName by remember { 
        mutableStateOf(prefs.getString("selected_ringtone_name", "Default System Alarm") ?: "Default System Alarm") 
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                val ringtone = RingtoneManager.getRingtone(context, uri)
                val name = ringtone.getTitle(context)
                
                prefs.edit()
                    .putString("selected_ringtone", uri.toString())
                    .putString("selected_ringtone_name", name)
                    .apply()
                
                ringtoneName = name
            }
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = AppLightBlue, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            SettingsItem(
                icon = Icons.Default.History,
                title = "Alarm History",
                subtitle = "View when your alarms were triggered",
                onClick = { showHistory = true }
            )
            
            SettingsItem(
                icon = Icons.Default.MusicNote,
                title = "Ringtone",
                subtitle = ringtoneName,
                onClick = {
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, 
                            prefs.getString("selected_ringtone", null)?.let { Uri.parse(it) })
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                    }
                    ringtonePickerLauncher.launch(intent)
                }
            )

            SettingsItem(
                icon = Icons.Default.NotificationsActive,
                title = "Vibration",
                subtitle = "Enabled",
                trailing = {
                    Switch(
                        checked = true,
                        onCheckedChange = {},
                        colors = SwitchDefaults.colors(checkedTrackColor = AppPrimary)
                    )
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "About",
                color = AppLightBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            SettingsItem(
                icon = Icons.Default.Info,
                title = "Version",
                subtitle = "1.0.0",
                onClick = {}
            )
        }
    }

    if (showHistory) {
        AlertDialog(
            onDismissRequest = { showHistory = false },
            containerColor = AppBackground,
            title = { Text("Alarm History", color = AppLightBlue) },
            text = {
                Box(modifier = Modifier.height(400.dp)) {
                    if (history.isEmpty()) {
                        Text("No history yet.", color = AppLightBlue.copy(alpha = 0.6f))
                    } else {
                        LazyColumn {
                            items(history) { item ->
                                HistoryItem(item.alarmName, item.timestamp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearHistory() }) {
                    Text("Clear All", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showHistory = false }) {
                    Text("Close", color = AppLightBlue)
                }
            }
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        onClick = { onClick?.invoke() },
        color = Color.Transparent,
        enabled = onClick != null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = AppLightBlue, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, color = AppLightBlue.copy(alpha = 0.6f), fontSize = 12.sp)
            }
            if (trailing != null) {
                trailing()
            } else if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = AppLightBlue.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun HistoryItem(name: String, timestamp: Long) {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(timestamp))
    
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(text = name, color = AppLightBlue, fontWeight = FontWeight.Bold)
        Text(text = dateString, color = AppLightBlue.copy(alpha = 0.6f), fontSize = 12.sp)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), color = AppDarkBlue)
    }
}
