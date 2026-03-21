package com.example.zonealarm.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.zonealarm.AppBackground
import com.example.zonealarm.AppDarkBlue
import com.example.zonealarm.AppLightBlue
import com.example.zonealarm.AppPrimary
import com.example.zonealarm.ui.viewmodels.AlarmViewModel
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolygonOptions
import org.maplibre.android.camera.CameraPosition
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
    var selectedPoint by remember { mutableStateOf<LatLng?>(null) }
    var isPinDropped by remember { mutableStateOf(false) }
    var radiusMeters by remember { mutableFloatStateOf(500f) }
    var searchQuery by remember { mutableStateOf("") }
    var isSatellite by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    
    var showNameDialog by remember { mutableStateOf(false) }
    var alarmName by remember { mutableStateOf("") }

    var permissionsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        if (permissionsGranted) {
            mapLibreMap?.let { map ->
                map.getStyle { style ->
                    enableLocationComponent(map, style, context)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val mapView = remember {
        MapView(context).apply {
            getMapAsync { map ->
                mapLibreMap = map
                val styleUrl = if (isSatellite) SATELLITE_STYLE else STREET_STYLE
                map.setStyle(styleUrl) { style ->
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(13.7565, 121.0583))
                        .zoom(11.0)
                        .build()
                    
                    if (permissionsGranted) {
                        enableLocationComponent(map, style, context)
                    }
                }

                map.addOnMapClickListener { point ->
                    if (!isPinDropped) {
                        selectedPoint = point
                        @Suppress("DEPRECATION")
                        map.clear()
                        @Suppress("DEPRECATION")
                        map.addMarker(MarkerOptions().position(point).title("Selected Location"))
                    }
                    true
                }
            }
        }
    }

    fun updateMapVisuals(map: MapLibreMap, center: LatLng, radius: Float) {
        @Suppress("DEPRECATION")
        map.clear()
        @Suppress("DEPRECATION")
        map.addMarker(MarkerOptions().position(center))
        
        val points = mutableListOf<LatLng>()
        val radiusInDegrees = radius / 111320f 
        
        for (i in 0 until 360 step 1) {
            val rad = Math.toRadians(i.toDouble())
            val lat = center.latitude + (radiusInDegrees * cos(rad))
            val lng = center.longitude + (radiusInDegrees * sin(rad) / cos(Math.toRadians(center.latitude)))
            points.add(LatLng(lat, lng))
        }
        
        @Suppress("DEPRECATION")
        map.addPolygon(
            PolygonOptions()
                .addAll(points)
                .fillColor(AndroidColor.argb(80, 87, 96, 222)) // Using AppPrimary color
                .strokeColor(AndroidColor.argb(150, 87, 96, 222))
        )
    }

    LaunchedEffect(isSatellite) {
        mapLibreMap?.let { map ->
            val styleUrl = if (isSatellite) SATELLITE_STYLE else STREET_STYLE
            map.setStyle(styleUrl) { style ->
                if (permissionsGranted) {
                    enableLocationComponent(map, style, context)
                }
                if (isPinDropped && selectedPoint != null) {
                    updateMapVisuals(map, selectedPoint!!, radiusMeters)
                } else if (selectedPoint != null) {
                    @Suppress("DEPRECATION")
                    map.addMarker(MarkerOptions().position(selectedPoint!!))
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
                        if (alarmName.isNotBlank() && selectedPoint != null) {
                            alarmViewModel.addAlarm(
                                name = alarmName,
                                latitude = selectedPoint!!.latitude,
                                longitude = selectedPoint!!.longitude,
                                radius = radiusMeters
                            )
                            Toast.makeText(context, "Alarm has been set", Toast.LENGTH_SHORT).show()
                            showNameDialog = false
                            isPinDropped = false
                            selectedPoint = null
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
            modifier = Modifier.fillMaxSize(),
            update = { /* handled by state */ }
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
                        checked = isSatellite,
                        onCheckedChange = { isSatellite = it },
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
                    if (permissionsGranted) {
                        mapLibreMap?.locationComponent?.let {
                            it.cameraMode = CameraMode.TRACKING
                        }
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
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
        if (selectedPoint != null && !isPinDropped) {
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
                        val latStr = String.format(Locale.US, "%.5f", selectedPoint!!.latitude)
                        val lonStr = String.format(Locale.US, "%.5f", selectedPoint!!.longitude)
                        Text("Lat: $latStr, Lon: $lonStr", fontSize = 12.sp, color = AppLightBlue.copy(alpha = 0.8f))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        isPinDropped = true
                        mapLibreMap?.let { updateMapVisuals(it, selectedPoint!!, radiusMeters) }
                    },
                    modifier = Modifier.fillMaxWidth(0.7f),
                    colors = ButtonDefaults.buttonColors(containerColor = AppPrimary)
                ) {
                    Text("DROP PIN", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Detail Slider and Set Alarm
        if (isPinDropped && selectedPoint != null) {
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
                        Row {
                            IconButton(onClick = { isFavorite = !isFavorite }) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) Color(0xFFFFD700) else AppLightBlue
                                )
                            }
                            IconButton(onClick = { 
                                isPinDropped = false
                                selectedPoint = null
                                @Suppress("DEPRECATION")
                                mapLibreMap?.clear()
                            }) {
                                Icon(Icons.Default.Close, null, tint = AppLightBlue)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Radius: ${radiusMeters.toInt()} m", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppLightBlue)
                    Slider(
                        value = radiusMeters,
                        onValueChange = { 
                            radiusMeters = it
                            mapLibreMap?.let { map -> updateMapVisuals(map, selectedPoint!!, radiusMeters) }
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

@SuppressLint("MissingPermission")
private fun enableLocationComponent(map: MapLibreMap, loadedMapStyle: org.maplibre.android.maps.Style, context: Context) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        return
    }

    val locationComponent = map.locationComponent
    
    val locationOptions = LocationComponentOptions.builder(context)
        .foregroundTintColor(AndroidColor.RED)
        .accuracyAlpha(0.2f)
        .accuracyColor(AndroidColor.RED)
        .build()

    val activationOptions = LocationComponentActivationOptions.builder(context, loadedMapStyle)
        .useDefaultLocationEngine(true)
        .locationComponentOptions(locationOptions)
        .build()
    
    locationComponent.activateLocationComponent(activationOptions)
    locationComponent.isLocationComponentEnabled = true
    locationComponent.cameraMode = CameraMode.TRACKING
    locationComponent.renderMode = RenderMode.COMPASS
}
