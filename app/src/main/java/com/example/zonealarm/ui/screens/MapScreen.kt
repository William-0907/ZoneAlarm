package com.example.zonealarm.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.location.Address
import android.location.Geocoder
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zonealarm.ui.viewmodels.AlarmViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolygonOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private const val STREET_STYLE = "https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json"
private const val OUTDOOR_STYLE = "https://tiles.openfreemap.org/styles/liberty"

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(alarmViewModel: AlarmViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<Address>>(emptyList()) }
    var showSuggestions by remember { mutableStateOf(false) }
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
    }

    val mapView = remember {
        MapView(context).apply {
            getMapAsync { map ->
                mapLibreMap = map
                map.uiSettings.isCompassEnabled = false
                
                val styleUrl = if (alarmViewModel.isSatellite) OUTDOOR_STYLE else STREET_STYLE
                map.setStyle(styleUrl) { style ->
                    // Improved Zoom: Default to 14.0 instead of 6.5
                    val initialPos = alarmViewModel.cameraPosition ?: CameraPosition.Builder()
                        .target(LatLng(14.5995, 120.9842)) // Default to Manila instead of Luzon center
                        .zoom(14.0)
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
                        map.animateCamera(CameraUpdateFactory.newLatLng(point))
                    }
                    true
                }
                
                map.addOnCameraIdleListener {
                    alarmViewModel.cameraPosition = map.cameraPosition
                }
            }
        }
    }

    LaunchedEffect(searchQuery) {
        val query = searchQuery.trim()
        if (query.length < 2) {
            suggestions = emptyList()
            showSuggestions = false
            return@LaunchedEffect
        }
        delay(300) 
        try {
            val geocoder = Geocoder(context)
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(query, 10)
                withContext(Dispatchers.Main) {
                    if (results != null) {
                        suggestions = results
                        showSuggestions = results.isNotEmpty()
                    } else {
                        suggestions = emptyList()
                        showSuggestions = false
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                suggestions = emptyList()
                showSuggestions = false
            }
        }
    }

    fun selectLocation(address: Address) {
        val latLng = LatLng(address.latitude, address.longitude)
        mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.0))
        if (!alarmViewModel.isPinDropped) {
            alarmViewModel.selectedPoint = latLng
            @Suppress("DEPRECATION")
            mapLibreMap?.clear()
            @Suppress("DEPRECATION")
            mapLibreMap?.addMarker(MarkerOptions().position(latLng).title(address.featureName ?: "Selected Location"))
        }
        showSuggestions = false
        keyboardController?.hide()
    }

    LaunchedEffect(alarmViewModel.isSatellite) {
        mapLibreMap?.let { map ->
            val styleUrl = if (alarmViewModel.isSatellite) OUTDOOR_STYLE else STREET_STYLE
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
            override fun onDestroy(owner: LifecycleOwner) { 
                mapView.onDestroy()
                mapLibreMap = null 
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { 
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("Set Alarm Name") },
            text = {
                TextField(
                    value = alarmName,
                    onValueChange = { alarmName = it },
                    placeholder = { Text("e.g. Home, Market", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary
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
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            Card(
                elevation = CardDefaults.cardElevation(8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        if (it.isBlank()) {
                            showSuggestions = false
                            suggestions = emptyList()
                        }
                    },
                    placeholder = { Text("Search for places...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                searchQuery = ""
                                suggestions = emptyList()
                                showSuggestions = false
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { 
                        if (suggestions.isNotEmpty()) selectLocation(suggestions[0])
                        keyboardController?.hide()
                    })
                )
            }

            AnimatedVisibility(
                visible = showSuggestions && suggestions.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(suggestions) { address ->
                            val mainText = address.featureName ?: address.thoroughfare ?: address.locality ?: ""
                            val subText = address.getAddressLine(0) ?: ""
                            
                            ListItem(
                                headlineContent = { Text(mainText, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text(subText, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), fontSize = 12.sp) },
                                leadingContent = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier.clickable { selectLocation(address) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(bottom = if (alarmViewModel.selectedPoint != null) 220.dp else 16.dp, end = 16.dp)
                .align(Alignment.BottomEnd),
            horizontalAlignment = Alignment.End
        ) {
            FloatingActionButton(
                onClick = {
                    mapLibreMap?.animateCamera(CameraUpdateFactory.bearingTo(0.0))
                },
                modifier = Modifier.size(56.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Explore, contentDescription = "Reset North")
            }

            Spacer(modifier = Modifier.height(16.dp))

            FloatingActionButton(
                onClick = {
                    mapLibreMap?.locationComponent?.let {
                        if (it.isLocationComponentActivated) {
                            it.cameraMode = CameraMode.TRACKING
                            it.zoomWhileTracking(15.0)
                        } else {
                           permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    }
                },
                modifier = Modifier.size(56.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My Location")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                elevation = CardDefaults.cardElevation(4.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).clickable {
                        alarmViewModel.isSatellite = !alarmViewModel.isSatellite
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (alarmViewModel.isSatellite) Icons.Default.Layers else Icons.Default.LayersClear,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (alarmViewModel.isSatellite) "3D Map" else "2D Map", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = alarmViewModel.isSatellite,
                        onCheckedChange = { alarmViewModel.isSatellite = it },
                        modifier = Modifier.scale(0.7f),
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        if (alarmViewModel.selectedPoint != null && !alarmViewModel.isPinDropped) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp).padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Selected Location", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        val latStr = String.format(Locale.US, "%.5f", alarmViewModel.selectedPoint!!.latitude)
                        val lonStr = String.format(Locale.US, "%.5f", alarmViewModel.selectedPoint!!.longitude)
                        Text("Lat: $latStr, Lon: $lonStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                alarmViewModel.isPinDropped = true
                                mapLibreMap?.let { updateMapVisuals(it, alarmViewModel.selectedPoint!!, alarmViewModel.radiusMeters) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("DROP PIN HERE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (alarmViewModel.isPinDropped && alarmViewModel.selectedPoint != null) {
            Card(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
                elevation = CardDefaults.cardElevation(12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Alarm Area", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Adjust the radius below", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
                        }
                        IconButton(onClick = { 
                            alarmViewModel.isPinDropped = false
                            alarmViewModel.selectedPoint = null
                            @Suppress("DEPRECATION")
                            mapLibreMap?.clear()
                        }) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Radius: ${alarmViewModel.radiusMeters.toInt()} m", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Slider(
                        value = alarmViewModel.radiusMeters,
                        onValueChange = { 
                            alarmViewModel.radiusMeters = it
                            mapLibreMap?.let { map -> updateMapVisuals(map, alarmViewModel.selectedPoint!!, alarmViewModel.radiusMeters) }
                        },
                        valueRange = 100f..5000f
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { showNameDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SET ALARM", fontWeight = FontWeight.Bold)
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
