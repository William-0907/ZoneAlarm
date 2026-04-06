package com.example.zonealarm

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zonealarm.data.AlarmEntity
import com.example.zonealarm.ui.screens.AlarmEditScreen
import com.example.zonealarm.ui.screens.AlarmHistoryScreen
import com.example.zonealarm.ui.screens.AlarmListScreen
import com.example.zonealarm.ui.screens.MapScreen
import com.example.zonealarm.ui.screens.SettingsScreen
import com.example.zonealarm.ui.theme.ZoneAlarmTheme
import com.example.zonealarm.ui.viewmodels.AlarmViewModel
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre

sealed class NavItem(val icon: ImageVector, val label: String) {
    object Alarms : NavItem(Icons.Default.Notifications, "Alarms")
    object Map : NavItem(Icons.Default.Map, "Map")
    object History : NavItem(Icons.Default.History, "History")
    object Settings : NavItem(Icons.Default.Settings, "Settings")
}

// Custom Colors - Dark Mode defaults
val AppPrimary = Color(0xFF5760DE)
val AppDarkBlue = Color(0xFF2C2D38)
val AppBackground = Color(0xFF404154)
val AppLightBlue = Color(0xFFE6E8FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        enableEdgeToEdge()
        setContent {
            val viewModel: AlarmViewModel = viewModel()
            ZoneAlarmTheme(darkTheme = viewModel.isDarkMode) {
                PermissionChecker()
                ZoneAlarmMainScreen(viewModel)
            }
        }
    }

    @Composable
    private fun PermissionChecker() {
        var showOverlayDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            if (!Settings.canDrawOverlays(this@MainActivity)) {
                showOverlayDialog = true
            }
        }

        if (showOverlayDialog) {
            AlertDialog(
                onDismissRequest = { showOverlayDialog = false },
                title = { Text("Permission Required") },
                text = { Text("ZoneAlarm needs 'Display over other apps' permission to show the alarm screen when you enter a zone, even if the app is closed.") },
                confirmButton = {
                    TextButton(onClick = {
                        showOverlayDialog = false
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            "package:$packageName".toUri()
                        )
                        startActivity(intent)
                    }) {
                        Text("Grant Permission")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOverlayDialog = false }) {
                        Text("Later")
                    }
                }
            )
        }
    }
}

@Composable
fun ZoneAlarmMainScreen(viewModel: AlarmViewModel) {
    val tabs = listOf(NavItem.Alarms, NavItem.Map, NavItem.History, NavItem.Settings)
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    
    var editingAlarm by remember { mutableStateOf<AlarmEntity?>(null) }

    val currentAlarm = editingAlarm
    if (currentAlarm != null) {
        BackHandler { editingAlarm = null }
        AlarmEditScreen(
            alarm = currentAlarm,
            onBack = { editingAlarm = null },
            viewModel = viewModel
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                    tabs.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                indicatorColor = MaterialTheme.colorScheme.surface,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.padding(innerPadding),
                userScrollEnabled = pagerState.currentPage != 1,
                beyondViewportPageCount = tabs.size // FIX: Keep all screens in memory to eliminate lag when switching
            ) { page ->
                when (page) {
                    0 -> AlarmListScreen(
                        onAddClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                        onEditClick = { editingAlarm = it },
                        viewModel = viewModel
                    )
                    1 -> MapScreen(alarmViewModel = viewModel)
                    2 -> AlarmHistoryScreen(viewModel = viewModel)
                    3 -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
