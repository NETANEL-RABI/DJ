package com.prodj.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(#121212) // רקע כהה מקצועי
            ) {
                DjConsoleScreen()
            }
        }
    }
}

@Composable
fun DjConsoleScreen() {
    val context = LocalContext.current
    // אתחול מנהל האודיו שכתבנו בשלבים הקודמים
    val audioManager = remember { DjAudioManager(context) }

    var isPlayingA by remember { mutableStateOf(false) }
    var isPlayingB by remember { mutableStateOf(false) }
    var crossfadeValue by remember { mutableStateOf(0.5f) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            text = "PRO DJ STATION",
            color = Color(#DEFF9A),
            fontSize = 28.sp,
            style = MaterialTheme.typography.headlineMedium
        )

        // אזור הנגנים (שמאל וימין)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // DECK A
            DjDeck(
                deckName = "DECK A",
                isPlaying = isPlayingA,
                onPlayClick = {
                    isPlayingA = audioManager.togglePlay("A")
                },
                onPitchChange = { speed ->
                    audioManager.setPitch("A", speed)
                }
            )

            // DECK B
            DjDeck(
                deckName = "DECK B",
                isPlaying = isPlayingB,
                onPlayClick = {
                    isPlayingB = audioManager.togglePlay("B")
                },
                onPitchChange = { speed ->
                    audioManager.setPitch("B", speed)
                }
            )
        }

        // מיקסר מרכזי - קרוספיידר
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Text("CROSSFADER", color = Color.White, fontSize = 14.sp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(300.dp)
            ) {
                Text("A", color = Color(#DEFF9A), modifier = Modifier.padding(end = 8.dp))
                Slider(
                    value = crossfadeValue,
                    onValueChange = {
                        crossfadeValue = it
                        audioManager.setCrossfade(it)
                    },
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(#DEFF9A),
                        activeTrackColor = Color(#DEFF9A)
                    )
                )
                Text("B", color = Color(#DEFF9A), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
fun DjDeck(
    deckName: String,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onPitchChange: (Float) -> Unit
) {
    var pitchSliderValue by remember { mutableStateOf(1.0f) }

    // אנימציית סיבוב חלקה לעיגול כאשר המוזיקה מתנגנת
    val infiniteTransition = rememberInfiniteTransition(label = "JogWheelRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color(#1A1A1A), shape = CircleShape)
            .padding(24.dp)
    ) {
        Text(deckName, color = Color.White, fontSize = 18.sp)

        Spacer(modifier = Modifier.height(16.dp))

        // עיגול ה-DJ (Jog Wheel) המסתובב
        Box(
            modifier = Modifier
                .size(150.dp)
                .rotate(if (isPlaying) rotation else 0f)
                .background(Color.Black, shape = CircleShape)
                .background(Color(#2A2A2A), shape = CircleShape), // טבעת פנימית
            contentAlignment = Alignment.Center
        ) {
            // נקודת מרכז זוהרת לעיצוב פרימיום
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Color(#DEFF9A), shape = CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // כפתור Play / Pause
        Button(
            onClick = onPlayClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(#DEFF9A))
        ) {
            Text(if (isPlaying) "PAUSE" else "PLAY", color = Color.Black)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // כוון מהירות (Pitch)
        Text("PITCH: ${String.format("%.2f", pitchSliderValue)}x", color = Color.Gray, fontSize = 12.sp)
        Slider(
            value = pitchSliderValue,
            onValueChange = {
                pitchSliderValue = it
                onPitchChange(it)
            },
            valueRange = 0.5f..1.5f,
            modifier = Modifier.width(120.dp)
        )
    }
}
