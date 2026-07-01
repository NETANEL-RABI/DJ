package com.prodj.app

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- 1. מנוע השמע המקצועי (ADVANCED AUDIO ENGINE) ---
class DjAudioManager(context: Context) {
    private var playerA: ExoPlayer? = ExoPlayer.Builder(context).build()
    private var playerB: ExoPlayer? = ExoPlayer.Builder(context).build()
    private var samplerPlayer: ExoPlayer? = ExoPlayer.Builder(context).build()

    fun loadTrack(deck: String, audioUri: Uri) {
        val mediaItem = MediaItem.fromUri(audioUri)
        if (deck == "A") {
            playerA?.setMediaItem(mediaItem)
            playerA?.prepare()
        } else {
            playerB?.setMediaItem(mediaItem)
            playerB?.prepare()
        }
    }

    fun togglePlay(deck: String): Boolean {
        val player = if (deck == "A") playerA else playerB
        return player?.let {
            if (it.isPlaying) {
                it.pause()
                false
            } else {
                it.play()
                true
            }
        } ?: false
    }

    fun setPitch(deck: String, speed: Float) {
        val player = if (deck == "A") playerA else playerB
        player?.playbackParameters = PlaybackParameters(speed, 1.0f)
    }

    fun setCrossfade(value: Float) {
        playerA?.volume = 1.0f - value
        playerB?.volume = value
    }

    fun setEqualizer(deck: String, band: String, value: Float) {
        val player = if (deck == "A") playerA else playerB
        if (band == "LOW") { player?.volume = value }
    }

    fun playSample(context: Context, sampleName: String) {
        val sampleUrl = when (sampleName) {
            "AIRHORN" -> "https://www.soundjay.com/buttons/sounds/button-16.mp3"
            "SIREN" -> "https://www.soundjay.com/buttons/sounds/button-09.mp3"
            "LASER" -> "https://www.soundjay.com/mechanical/sounds/laser-beam-1.mp3"
            else -> "https://www.soundjay.com/buttons/sounds/button-3.mp3"
        }
        samplerPlayer?.stop()
        samplerPlayer?.setMediaItem(MediaItem.fromUri(Uri.parse(sampleUrl)))
        samplerPlayer?.prepare()
        samplerPlayer?.play()
    }
}

// --- 2. האקטיביטי הראשי ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0A0A0A)) {
                DjConsoleScreen()
            }
        }
    }
}

// --- 3. מסך הקונסולה (PREMIUM CLUB UI) ---
@Composable
fun DjConsoleScreen() {
    val context = LocalContext.current
    val audioManager = remember { DjAudioManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var isPlayingA by remember { mutableStateOf(false) }
    var isPlayingB by remember { mutableStateOf(false) }
    var crossfadeValue by remember { mutableStateOf(0.5f) }
    var isAutoMixActive by remember { mutableStateOf(false) }

    var trackNameA by remember { mutableStateOf("Tap LOAD to choose MP3") }
    var trackNameB by remember { mutableStateOf("Tap LOAD to choose MP3") }

    val launcherA = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { audioManager.loadTrack("A", it); trackNameA = "Track Loaded (A)" }
    }

    val launcherB = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { audioManager.loadTrack("B", it); trackNameB = "Track Loaded (B)" }
    }

    // לוגיקת הטרנזקשן והאוטומיקס העצמאי
    LaunchedEffect(isAutoMixActive) {
        if (isAutoMixActive) {
            coroutineScope.launch {
                if (!isPlayingA) isPlayingA = audioManager.togglePlay("A")
                while (isAutoMixActive) {
                    delay(12000) // השיר מנגן 12 שניות לבד
                    if (!isAutoMixActive) break
                    
                    // מפעיל את השיר השני לקראת המעבר העצמאי
                    if (!isPlayingB) isPlayingB = audioManager.togglePlay("B")
                    
                    // ביצוע טרנס (Transition) אוטומטי חלק במשך 3 שניות על הקרוספיידר
                    for (i in 0..30) {
                        if (!isAutoMixActive) break
                        crossfadeValue = i / 30f
                        audioManager.setCrossfade(crossfadeValue)
                        delay(100)
                    }
                    if (isPlayingA) isPlayingA = audioManager.togglePlay("A")
                    
                    delay(12000) // השיר השני מנגן 12 שניות
                    if (!isAutoMixActive) break
                    if (!isPlayingA) isPlayingA = audioManager.togglePlay("A")
                    
                    // טרנס חזרה מ-B ל-A
                    for (i in 30 downTo 0) {
                        if (!isAutoMixActive) break
                        crossfadeValue = i / 30f
                        audioManager.setCrossfade(crossfadeValue)
                        delay(100)
                    }
                    if (isPlayingB) isPlayingB = audioManager.togglePlay("B")
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // כפתור ה-DJ העצמאי הגדול בראש המסך
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("PRO DJ STATION v2.5", color = Color.White, fontSize = 18.sp)
                    Text("Auto-Transition Mode", color = Color.Gray, fontSize = 12.sp)
                }
                
                // כפתור ה-DJ העצמאי הזוהר
                Button(
                    onClick = { isAutoMixActive = !isAutoMixActive },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAutoMixActive) Color(0xFF00FFCC) else Color(0xFFFF007F)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(50.dp)
                ) {
                    Text(
                        text = if (isAutoMixActive) "🤖 AUTO DJ: ACTIVE" else "🚀 START AUTO DJ", 
                        color = Color.Black,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // חלק מרכזי: DECK A ו- DECK B
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                DjDeck("DECK A", trackNameA, isPlayingA, { launcherA.launch("audio/*") }, { isPlayingA = audioManager.togglePlay("A") }, { audioManager.setPitch("A", it) }, { b, v -> audioManager.setEqualizer("A", b, v) })
            }
            Box(modifier = Modifier.weight(1f)) {
                DjDeck("DECK B", trackNameB, isPlayingB, { launcherB.launch("audio/*") }, { isPlayingB = audioManager.togglePlay("B") }, { audioManager.setPitch("B", it) }, { b, v -> audioManager.setEqualizer("B", b, v) })
            }
        }

        // חלק תחתון: SAMPLER PADS
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text("SAMPLER FX", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val samples = listOf("AIRHORN", "SIREN", "LASER", "DROP")
                samples.forEach { sample ->
                    Button(
                        onClick = { audioManager.playSample(context, sample) },
                        modifier = Modifier.weight(1f).height(45.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(sample, color = Color(0xFF00FFCC), fontSize = 11.sp)
                    }
                }
            }
        }

        // קרוספיידר ראשי
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(320.dp)) {
                Text("A", color = Color(0xFF00FFCC), fontSize = 16.sp)
                Slider(
                    value = crossfadeValue,
                    onValueChange = { if (!isAutoMixActive) { crossfadeValue = it; audioManager.setCrossfade(it) } },
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFFF007F), activeTrackColor = Color(0xFF333333))
                )
                Text("B", color = Color(0xFF00FFCC), fontSize = 16.sp)
            }
        }
    }
}

// --- 4. רכיב נגן פרימיום ---
@Composable
fun DjDeck(
    deckName: String,
    trackName: String,
    isPlaying: Boolean,
    onBrowseClick: () -> Unit,
    onPlayClick: () -> Unit,
    onPitchChange: (Float) -> Unit,
    onEqChange: (String, Float) -> Unit
) {
    var pitch by remember { mutableStateOf(1.0f) }
    var lowEq by remember { mutableStateOf(1.0f) }
    var midEq by remember { mutableStateOf(1.0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "Vinyl")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing)), label = "Rotate"
    )
    val waveAnim by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(animation = tween(400, easing = FastOutLinearInEasing), repeatMode = RepeatMode.Reverse), label = "Wave"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.background(Color(0xFF141414), shape = RoundedCornerShape(16.dp)).padding(12.dp)
    ) {
        Text(deckName, color = Color(0xFFFF007F), fontSize = 14.sp)
        Text(trackName, color = Color.Gray, fontSize = 10.sp, maxLines = 1)

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.height(24.dp).width(100.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            for (i in 1..5) {
                val heightMultiplier = if (isPlaying) (i * 0.2f + waveAnim) % 1.0f else 0.1f
                Box(modifier = Modifier.weight(1f).height((24 * heightMultiplier).dp).background(Color(0xFF00FFCC), shape = RoundedCornerShape(1.dp)))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier.size(110.dp).rotate(if (isPlaying) rotation else 0f).background(Color.Black, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(80.dp).background(Color(0xFF111111), shape = CircleShape))
            Box(modifier = Modifier.size(20.dp).background(Color(0xFFFF007F), shape = CircleShape))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = onBrowseClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                Text("LOAD", color = Color.White, fontSize = 11.sp)
            }
            Button(onClick = onPlayClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                Text(if (isPlaying) "PAUSE" else "PLAY", color = Color.Black, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text("PITCH: ${String.format("%.1f", pitch)}x", color = Color.Gray, fontSize = 10.sp)
        Slider(value = pitch, onValueChange = { pitch = it; onPitchChange(it) }, valueRange = 0.5f..1.5f, colors = SliderDefaults.colors(thumbColor = Color(0xFF00FFCC)))

        Text("BASS (LOW)", color = Color.Gray, fontSize = 10.sp)
        Slider(value = lowEq, onValueChange = { lowEq = it; onEqChange("LOW", it) }, valueRange = 0.0f..1.0f, colors = SliderDefaults.colors(thumbColor = Color(0xFFFF007F)))

        Text("MID", color = Color.Gray, fontSize = 10.sp)
        Slider(value = midEq, onValueChange = { midEq = it; onEqChange("MID", it) }, valueRange = 0.0f..1.0f, colors = SliderDefaults.colors(thumbColor = Color(0xFFFF007F)))
    }
}
