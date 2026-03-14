package com.example.zonealarm

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.example.zonealarm.ui.theme.ZoneAlarmTheme
import kotlinx.coroutines.launch

import com.example.zonealarm.ui.screens.AlarmListScreen
import com.example.zonealarm.ui.screens.FavoritesScreen
import com.example.zonealarm.ui.screens.MapScreen
import com.example.zonealarm.ui.screens.SettingsScreen

sealed class NavItem(val icon: ImageVector, val label: String) {
    object Alarms : NavItem(Icons.Default.Notifications, "Alarms")
    object Map : NavItem(Icons.Default.Map, "Map")
    object Favorites : NavItem(Icons.Default.Favorite, "Favorites")
    object Settings : NavItem(Icons.Default.Settings, "Settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    val tabs = listOf(NavItem.Alarms, NavItem.Map, NavItem.Favorites, NavItem.Settings)

    // Pager state to control the swiping.
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Color.DarkGray,
            ) {
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
                            selectedIconColor = Color(0xFF1E3A8A),
                            unselectedIconColor = Color.Gray,
                            indicatorColor = Color(0xFFE0E7FF)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(innerPadding),
            userScrollEnabled = true
        ) { page ->
            when (page) {
                0 -> AlarmListScreen(onAddClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                })
                1 -> MapScreen()
                2 -> FavoritesScreen()
                3 -> SettingsScreen()
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ZoneAlarmMainScreenPreview() {
    ZoneAlarmTheme {
        ZoneAlarmMainScreen()
    }
}
