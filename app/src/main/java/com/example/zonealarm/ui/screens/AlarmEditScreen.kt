package com.example.zonealarm.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.zonealarm.AppBackground
import com.example.zonealarm.AppDarkBlue
import com.example.zonealarm.AppLightBlue
import com.example.zonealarm.AppPrimary
import com.example.zonealarm.data.AlarmEntity
import com.example.zonealarm.ui.viewmodels.AlarmViewModel
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolygonOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditScreen(
    alarm: AlarmEntity,
    onBack: () -> Unit,
    viewModel: AlarmViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var radiusMeters by remember { mutableFloatStateOf(alarm.radius) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

    val mapView = remember {
        MapView(context).apply {
            getMapAsync { map ->
                mapLibreMap = map
                map.setStyle("https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json") {
                    val pos = LatLng(alarm.latitude, alarm.longitude)
                    map.cameraPosition = CameraPosition.Builder()
                        .target(pos)
                        .zoom(13.0)
                        .build()
                    updateVisuals(map, pos, radiusMeters)
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { mapView.onStart() }
            override fun onResume(owner: LifecycleOwner) { mapView.onResume() }
            override fun onPause(owner: LifecycleOwner) { mapView.onPause() }
            override fun onStop(owner: LifecycleOwner) { mapView.onStop() }
            override fun onDestroy(owner: LifecycleOwner) { mapView.onDestroy() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                title = { Text(alarm.name, color = AppLightBlue, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AppLightBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
            }
            
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = AppBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Adjust Radius: ${radiusMeters.toInt()}m", color = AppLightBlue, fontWeight = FontWeight.Bold)
                    Slider(
                        value = radiusMeters,
                        onValueChange = { 
                            radiusMeters = it
                            mapLibreMap?.let { map -> 
                                updateVisuals(map, LatLng(alarm.latitude, alarm.longitude), radiusMeters)
                            }
                        },
                        valueRange = 250f..5000f,
                        colors = SliderDefaults.colors(
                            thumbColor = AppPrimary,
                            activeTrackColor = AppPrimary,
                            inactiveTrackColor = AppDarkBlue
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            viewModel.updateAlarm(alarm.copy(radius = radiusMeters))
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppPrimary)
                    ) {
                        Text("SAVE CHANGES", color = Color.White)
                    }
                }
            }
        }
    }
}

private fun updateVisuals(map: MapLibreMap, center: LatLng, radius: Float) {
    @Suppress("DEPRECATION")
    map.clear()
    @Suppress("DEPRECATION")
    map.addMarker(MarkerOptions().position(center))
    
    val points = mutableListOf<LatLng>()
    val radiusInDegrees = radius / 111320f 
    for (i in 0 until 360 step 5) {
        val rad = Math.toRadians(i.toDouble())
        val lat = center.latitude + (radiusInDegrees * cos(rad))
        val lng = center.longitude + (radiusInDegrees * sin(rad) / cos(Math.toRadians(center.latitude)))
        points.add(LatLng(lat, lng))
    }
    @Suppress("DEPRECATION")
    map.addPolygon(
        PolygonOptions()
            .addAll(points)
            .fillColor(AndroidColor.argb(80, 87, 96, 222))
            .strokeColor(AndroidColor.argb(150, 87, 96, 222))
    )
}
