package com.example.zonealarm

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zonealarm.data.AlarmDatabase
import com.example.zonealarm.ui.theme.ZoneAlarmTheme
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmActivity : ComponentActivity() {
    private var ringtone: Ringtone? = null
    private var alarmId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        alarmId = intent.getIntOf("ALARM_ID", -1)
        val alarmName = intent.getStringExtra("ALARM_NAME") ?: "Zone Alarm"
        
        // Play ringtone (User selected or default)
        val prefs = getSharedPreferences("ZoneAlarmPrefs", Context.MODE_PRIVATE)
        val ringtoneUriString = prefs.getString("selected_ringtone", null)
        val notificationUri: Uri = if (ringtoneUriString != null) {
            Uri.parse(ringtoneUriString)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
        
        try {
            ringtone = RingtoneManager.getRingtone(applicationContext, notificationUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        enableEdgeToEdge()
        setContent {
            ZoneAlarmTheme {
                AlarmScreen(
                    alarmName = alarmName,
                    onStopClick = {
                        stopAlarm()
                    }
                )
            }
        }
    }

    private fun stopAlarm() {
        ringtone?.stop()
        if (alarmId != -1) {
            val db = AlarmDatabase.getDatabase(this)
            val geofencingClient = LocationServices.getGeofencingClient(this)
            
            CoroutineScope(Dispatchers.IO).launch {
                // Turn off switch in DB
                db.alarmDao().setAlarmEnabled(alarmId, false)
                // Remove from active geofences
                geofencingClient.removeGeofences(listOf(alarmId.toString()))
            }
        }
        finish()
    }

    private fun Intent.getIntOf(key: String, default: Int): Int {
        return getIntExtra(key, default)
    }

    override fun onDestroy() {
        super.onDestroy()
        ringtone?.stop()
    }
}

@Composable
fun AlarmScreen(alarmName: String, onStopClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = AppPrimary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "ALARM!",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "You're almost there",
                fontSize = 24.sp,
                color = AppLightBlue
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = alarmName,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AppLightBlue
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Button(
                onClick = onStopClick,
                modifier = Modifier
                    .size(150.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text(
                    text = "STOP",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
