package com.example.zonealarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zonealarm.data.AlarmEntity
import com.example.zonealarm.ui.screens.AlarmEditScreen
import com.example.zonealarm.ui.screens.AlarmListScreen
import com.example.zonealarm.ui.screens.FavoritesScreen
import com.example.zonealarm.ui.screens.MapScreen
import com.example.zonealarm.ui.screens.SettingsScreen
import com.example.zonealarm.ui.theme.ZoneAlarmTheme
import com.example.zonealarm.ui.viewmodels.AlarmViewModel
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre

sealed class NavItem(val icon: ImageVector, val label: String) {
    object Alarms : NavItem(Icons.Default.Notifications, "Alarms")
    object Map : NavItem(Icons.Default.Map, "Map")
    object Favorites : NavItem(Icons.Default.Favorite, "Favorites")
    object Settings : NavItem(Icons.Default.Settings, "Settings")
}

// Custom Colors
val AppPrimary = Color(0xFF5760DE)
val AppDarkBlue = Color(0xFF2C2D38)
val AppBackground = Color(0xFF404154)
val AppSurface = Color(0xFFABAED9)
val AppLightBlue = Color(0xFFD3D5F0)

val AppDarkGrey = Color(0xFF1C1C1C)
val AppLightGrey = Color(0xFFF5F5F5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        enableEdgeToEdge()
        setContent {
            ZoneAlarmTheme {
                ZoneAlarmMainScreen()
            }
        }
    }
}

@Composable
fun ZoneAlarmMainScreen() {
    val viewModel: AlarmViewModel = viewModel()
    val tabs = listOf(NavItem.Alarms, NavItem.Map, NavItem.Favorites, NavItem.Settings)
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    
    // Manage "Edit Mode" state
    var editingAlarm by remember { mutableStateOf<AlarmEntity?>(null) }

    if (editingAlarm != null) {
        BackHandler { editingAlarm = null }
        AlarmEditScreen(
            alarm = editingAlarm!!,
            onBack = { editingAlarm = null },
            viewModel = viewModel
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = AppBackground,
            bottomBar = {
                NavigationBar(containerColor = AppBackground) {
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
                                selectedIconColor = AppLightBlue,
                                unselectedIconColor = AppSurface.copy(alpha = 0.7f),
                                indicatorColor = AppDarkBlue,
                                selectedTextColor = AppLightBlue,
                                unselectedTextColor = AppSurface.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.padding(innerPadding),
                userScrollEnabled = pagerState.currentPage != 1
            ) { page ->
                when (page) {
                    0 -> AlarmListScreen(
                        onAddClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                        onEditClick = { editingAlarm = it },
                        viewModel = viewModel
                    )
                    1 -> MapScreen(alarmViewModel = viewModel)
                    2 -> FavoritesScreen(viewModel = viewModel)
                    3 -> SettingsScreen()
                }
            }
        }
    }
}
