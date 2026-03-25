package com.example.zonealarm.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zonealarm.*
import com.example.zonealarm.ui.viewmodels.AlarmViewModel
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolygonOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private const val STREET_STYLE = "https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json"
private const val SATELLITE_STYLE = "https://demotiles.maplibre.org/style.json"

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(alarmViewModel: AlarmViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showNameDialog by remember { mutableStateOf(false) }
    var alarmName by remember { mutableStateOf("") }

    var permissionsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        
        // Request background location separately for Android 11+
        if (permissionsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBg = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!hasBg) {
                Toast.makeText(context, "Please allow 'All the time' location access in settings for background alarms to work.", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        val reqs = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Check if we already have fine location but not background
            if (permissionsGranted) {
                val hasBg = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (!hasBg) {
                    permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                }
            } else {
                permissionLauncher.launch(reqs.toTypedArray())
            }
        } else {
            permissionLauncher.launch(reqs.toTypedArray())
        }
    }

    val mapView = remember {
        MapView(context).apply {
            getMapAsync { map ->
                mapLibreMap = map
                val styleUrl = if (alarmViewModel.isSatellite) SATELLITE_STYLE else STREET_STYLE
                map.setStyle(styleUrl) { style ->
                    val initialPos = alarmViewModel.cameraPosition ?: CameraPosition.Builder()
                        .target(LatLng(13.7565, 121.0583))
                        .zoom(11.0)
                        .build()
                    map.cameraPosition = initialPos
                    
                    if (permissionsGranted) {
                        enableLocationComponent(map, style, context)
                    }

                    if (alarmViewModel.selectedPoint != null) {
                        if (alarmViewModel.isPinDropped) {
                            updateMapVisuals(map, alarmViewModel.selectedPoint!!, alarmViewModel.radiusMeters)
                        } else {
                            @Suppress("DEPRECATION")
                            map.addMarker(MarkerOptions().position(alarmViewModel.selectedPoint!!))
                        }
                    }
                }

                map.addOnMapClickListener { point ->
                    if (!alarmViewModel.isPinDropped) {
                        alarmViewModel.selectedPoint = point
                        @Suppress("DEPRECATION")
                        map.clear()
                        @Suppress("DEPRECATION")
                        map.addMarker(MarkerOptions().position(point).title("Selected Location"))
                    }
                    true
                }
                
                map.addOnCameraIdleListener {
                    alarmViewModel.cameraPosition = map.cameraPosition
                }
            }
        }
    }

    LaunchedEffect(alarmViewModel.isSatellite) {
        mapLibreMap?.let { map ->
            val styleUrl = if (alarmViewModel.isSatellite) SATELLITE_STYLE else STREET_STYLE
            map.setStyle(styleUrl) { style ->
                if (permissionsGranted) enableLocationComponent(map, style, context)
                if (alarmViewModel.selectedPoint != null) {
                    if (alarmViewModel.isPinDropped) {
                        updateMapVisuals(map, alarmViewModel.selectedPoint!!, alarmViewModel.radiusMeters)
                    } else {
                        @Suppress("DEPRECATION")
                        map.addMarker(MarkerOptions().position(alarmViewModel.selectedPoint!!))
                    }
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

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            containerColor = AppBackground,
            titleContentColor = AppLightBlue,
            textContentColor = AppLightBlue,
            title = { Text("Set Alarm Name") },
            text = {
                TextField(
                    value = alarmName,
                    onValueChange = { alarmName = it },
                    placeholder = { Text("e.g. Home, Market", color = AppLightBlue.copy(alpha = 0.5f)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = AppDarkBlue,
                        unfocusedContainerColor = AppDarkBlue,
                        focusedTextColor = AppLightBlue,
                        unfocusedTextColor = AppLightBlue,
                        cursorColor = AppPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (alarmName.isNotBlank() && alarmViewModel.selectedPoint != null) {
                            alarmViewModel.addAlarm(
                                name = alarmName,
                                latitude = alarmViewModel.selectedPoint!!.latitude,
                                longitude = alarmViewModel.selectedPoint!!.longitude,
                                radius = alarmViewModel.radiusMeters
                            )
                            Toast.makeText(context, "Alarm has been set", Toast.LENGTH_SHORT).show()
                            showNameDialog = false
                            alarmViewModel.isPinDropped = false
                            alarmViewModel.selectedPoint = null
                            @Suppress("DEPRECATION")
                            mapLibreMap?.clear()
                            alarmName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppPrimary)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel", color = AppLightBlue)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Top Search Bar
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter),
            elevation = CardDefaults.cardElevation(4.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = AppBackground)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search for places...", color = AppLightBlue.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AppLightBlue) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = AppLightBlue,
                    unfocusedTextColor = AppLightBlue
                ),
                singleLine = true
            )
        }

        // Satellite and My Location Controls
        Column(
            modifier = Modifier.padding(top = 80.dp, end = 16.dp).align(Alignment.TopEnd),
            horizontalAlignment = Alignment.End
        ) {
            Card(
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AppBackground)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Layers, null, modifier = Modifier.size(18.dp), tint = AppLightBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Satellite", fontSize = 12.sp, color = AppLightBlue)
                    Switch(
                        checked = alarmViewModel.isSatellite,
                        onCheckedChange = { alarmViewModel.isSatellite = it },
                        modifier = Modifier.scale(0.7f),
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = AppPrimary,
                            uncheckedBorderColor = AppLightBlue
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            FloatingActionButton(
                onClick = {
                    mapLibreMap?.locationComponent?.let {
                        if (it.isLocationComponentActivated) {
                            it.cameraMode = CameraMode.TRACKING
                        }
                    }
                },
                modifier = Modifier.size(40.dp),
                containerColor = AppBackground,
                contentColor = AppLightBlue
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My Location", modifier = Modifier.size(20.dp))
            }
        }

        // Location overlay
        if (alarmViewModel.selectedPoint != null && !alarmViewModel.isPinDropped) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp).padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppBackground),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Location Info", fontWeight = FontWeight.Bold, color = AppLightBlue)
                        val latStr = String.format(Locale.US, "%.5f", alarmViewModel.selectedPoint!!.latitude)
                        val lonStr = String.format(Locale.US, "%.5f", alarmViewModel.selectedPoint!!.longitude)
                        Text("Lat: $latStr, Lon: $lonStr", fontSize = 12.sp, color = AppLightBlue.copy(alpha = 0.8f))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        alarmViewModel.isPinDropped = true
                        mapLibreMap?.let { updateMapVisuals(it, alarmViewModel.selectedPoint!!, alarmViewModel.radiusMeters) }
                    },
                    modifier = Modifier.fillMaxWidth(0.7f),
                    colors = ButtonDefaults.buttonColors(containerColor = AppPrimary)
                ) {
                    Text("DROP PIN", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Detail Slider and Set Alarm
        if (alarmViewModel.isPinDropped && alarmViewModel.selectedPoint != null) {
            Card(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                elevation = CardDefaults.cardElevation(12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AppBackground)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Alarm Area", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = AppLightBlue)
                            Text("Adjust the radius below", color = AppLightBlue.copy(alpha = 0.6f), fontSize = 14.sp)
                        }
                        IconButton(onClick = { 
                            alarmViewModel.isPinDropped = false
                            alarmViewModel.selectedPoint = null
                            @Suppress("DEPRECATION")
                            mapLibreMap?.clear()
                        }) {
                            Icon(Icons.Default.Close, null, tint = AppLightBlue)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Radius: ${alarmViewModel.radiusMeters.toInt()} m", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppLightBlue)
                    Slider(
                        value = alarmViewModel.radiusMeters,
                        onValueChange = { 
                            alarmViewModel.radiusMeters = it
                            mapLibreMap?.let { map -> updateMapVisuals(map, alarmViewModel.selectedPoint!!, alarmViewModel.radiusMeters) }
                        },
                        valueRange = 250f..5000f,
                        colors = SliderDefaults.colors(
                            thumbColor = AppPrimary,
                            activeTrackColor = AppPrimary,
                            inactiveTrackColor = AppDarkBlue
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { showNameDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppPrimary)
                    ) {
                        Text("SET ALARM", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

private fun updateMapVisuals(map: MapLibreMap, center: LatLng, radius: Float) {
    @Suppress("DEPRECATION")
    map.clear()
    @Suppress("DEPRECATION")
    map.addMarker(MarkerOptions().position(center))
    
    val points = mutableListOf<LatLng>()
    val radiusInDegrees = radius / 111320f 
    
    for (i in 0 until 360 step 5) { // Optimized steps
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

@SuppressLint("MissingPermission")
private fun enableLocationComponent(map: MapLibreMap, loadedMapStyle: org.maplibre.android.maps.Style, context: Context) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

    val locationComponent = map.locationComponent
    val activationOptions = LocationComponentActivationOptions.builder(context, loadedMapStyle)
        .useDefaultLocationEngine(true)
        .build()
    
    locationComponent.activateLocationComponent(activationOptions)
    locationComponent.isLocationComponentEnabled = true
    locationComponent.cameraMode = CameraMode.TRACKING
    locationComponent.renderMode = RenderMode.COMPASS
}
