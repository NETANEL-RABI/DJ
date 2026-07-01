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
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- 1. מנוע שמע עצמאי וחכם (INTELLIGENT AUDIO ENGINE) ---
class DjAudioManager(context: Context) {
    val playerA: ExoPlayer = ExoPlayer.Builder(context).build()
    val playerB: ExoPlayer = ExoPlayer.Builder(context).build()
    private val samplerPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    init {
        playerA.repeatMode = Player.REPEAT_MODE_OFF
        playerB.repeatMode = Player.REPEAT_MODE_OFF
    }

    fun loadTrack(deck: String, audioUri: Uri) {
        val mediaItem = MediaItem.fromUri(audioUri)
        if (deck == "A") {
            playerA.setMediaItem(mediaItem)
            playerA.prepare()
        } else {
            playerB.setMediaItem(mediaItem)
            playerB.prepare()
        }
    }

    fun togglePlay(deck: String): Boolean {
        val player = if (deck == "A") playerA else playerB
        return if (player.isPlaying) {
            player.pause()
            false
        } else {
            player.play()
            true
        }
    }

    fun setPitch(deck: String, speed: Float) {
        val player = if (deck == "A") playerA else playerB
        player.playbackParameters = PlaybackParameters(speed, 1.0f)
    }

    fun setCrossfade(value: Float) {
        playerA.volume = 1.0f - value
        playerB.volume = value
    }

    fun playSample(context: Context, sampleName: String) {
        val sampleUrl = when (sampleName) {
            "AIRHORN" -> "https://www.soundjay.com/buttons/sounds/button-16.mp3"
            "SIREN" -> "https://www.soundjay.com/buttons/sounds/button-09.mp3"
            "LASER" -> "https://www.soundjay.com/mechanical/sounds/laser-beam-1.mp3"
            else -> "https://www.soundjay.com/buttons/sounds/button-3.mp3"
        }
        samplerPlayer.stop()
        samplerPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(sampleUrl)))
        samplerPlayer.prepare()
        samplerPlayer.play()
    }
    
    fun release() {
        playerA.release()
        playerB.release()
        samplerPlayer.release()
    }
}

// --- 2. האקטיביטי הראשי ---
class MainActivity : ComponentActivity() {
    private var audioManager: DjAudioManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF070707)) {
                DjConsoleScreen(onManagerCreated = { audioManager = it })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager?.release()
    }
}

// --- 3. מסך הקונסולה האוטונומי (ROBOTIC DJ SYSTEM) ---
@Composable
fun DjConsoleScreen(onManagerCreated: (DjAudioManager) -> Unit) {
    val context = LocalContext.current
    val audioManager = remember { DjAudioManager(context).also { onManagerCreated(it) } }
    val coroutineScope = rememberCoroutineScope()

    var isPlayingA by remember { mutableStateOf(false) }
    var isPlayingB by remember { mutableStateOf(false) }
    var crossfadeValue by remember { mutableStateOf(0.5f) }
    var isAutoDjActive by remember { mutableStateOf(false) }

    var trackNameA by remember { mutableStateOf("Tap LOAD for Track A") }
    var trackNameB by remember { mutableStateOf("Tap LOAD for Track B") }
    
    var currentActiveDeck by remember { mutableStateOf("A") }

    val launcherA = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { audioManager.loadTrack("A", it); trackNameA = "Track A Ready" }
    }

    val launcherB = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { audioManager.loadTrack("B", it); trackNameB = "Track B Ready" }
    }

    // --- הלב של המערכת: מאזין חכם שעושה טרנס לבד לגמרי ---
    LaunchedEffect(isAutoDjActive) {
        if (isAutoDjActive) {
            coroutineScope.launch {
                while (isAutoDjActive) {
                    delay(1000) // בודק את מצב השירים בכל שנייה
                    
                    if (currentActiveDeck == "A" && isPlayingA) {
                        val duration = audioManager.playerA.duration
                        val position = audioManager.playerA.currentPosition
                        
                        // אם נשארו פחות מ-10 שניות לסוף השיר בדק א' - תתחיל טרנס אוטומטי!
                        if (duration > 0 && (duration - position) <= 10000) {
                            if (!isPlayingB) {
                                isPlayingB = true
                                audioManager.playerB.play()
                            }
                            // מזיז את הקרוספיידר לבד מ-0 ל-1 באופן חלק
                            for (i in (crossfadeValue * 50).toInt()..50) {
                                if (!isAutoDjActive) break
                                crossfadeValue = i / 50f
                                audioManager.setCrossfade(crossfadeValue)
                                delay(100)
                            }
                            if (audioManager.playerA.isPlaying) {
                                audioManager.playerA.pause()
                                isPlayingA = false
                            }
                            currentActiveDeck = "B"
                        }
                    } else if (currentActiveDeck == "B" && isPlayingB) {
                        val duration = audioManager.playerB.duration
                        val position = audioManager.playerB.currentPosition
                        
                        // אם נשארו פחות מ-10 שניות לסוף השיר בדק ב' - תחל טרנס חזרה!
                        if (duration > 0 && (duration - position) <= 10000) {
                            if (!isPlayingA) {
                                isPlayingA = true
                                audioManager.playerA.play()
                            }
                            // מזיז את הקרוספיידר לבד מ-1 ל-0 באופן חלק
                            for (i in (crossfadeValue * 50).toInt() downTo 0) {
                                if (!isAutoDjActive) break
                                crossfadeValue = i / 50f
                                audioManager.setCrossfade(crossfadeValue)
                                delay(100)
                            }
                            if (audioManager.playerB.isPlaying) {
                                audioManager.playerB.pause()
                                isPlayingB = false
                            }
                            currentActiveDeck = "A"
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // באנר עליון משודרג
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("AI PRO DJ ROBOT v3.0", color = Color(0xFF00FFCC), fontSize = 18.sp)
                    Text(
                        text = if (isAutoDjActive) "🎯 LISTENING TO TRACK END..." else "💤 IDLE - MANUAL MODE",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
                
                Button(
                    onClick = { 
                        isAutoDjActive = !isAutoDjActive
                        if (isAutoDjActive && !isPlayingA && !isPlayingB) {
                            isPlayingA = true
                            audioManager.playerA.play()
                            currentActiveDeck = "A"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAutoDjActive) Color(0xFF00FFCC) else Color(0xFFFF007F)
                    ),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = if (isAutoDjActive) "🤖 FULL AUTO DJ: ACTIVE" else "🚀 LAUNCH AUTO TRANSITIONS",
                        color = Color.Black,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // חלק מרכזי: פטיפונים
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                DjDeck("DECK A", trackNameA, isPlayingA, { launcherA.launch("audio/*") }, { isPlayingA = audioManager.togglePlay("A"); currentActiveDeck = "A" }, { audioManager.setPitch("A", it) })
            }
            Box(modifier = Modifier.weight(1f)) {
                DjDeck("DECK B", trackNameB, isPlayingB, { launcherB.launch("audio/*") }, { isPlayingB = audioManager.togglePlay("B"); currentActiveDeck = "B" }, { audioManager.setPitch("B", it) })
            }
        }

        // סאמפלר אפקטים תחתון
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111111))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("LIVE DJ SAMPLER", color = Color.Gray, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("AIRHORN", "SIREN", "LASER", "DROP").forEach { sample ->
                        Button(
                            onClick = { audioManager.playSample(context, sample) },
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(2.dp)
                        ) {
                            Text(sample, color = Color(0xFF00FFCC), fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // קרוספיידר
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(320.dp).padding(bottom = 4.dp)) {
            Text("A", color = Color(0xFF00FFCC), fontSize = 14.sp)
            Slider(
                value = crossfadeValue,
                onValueChange = { if (!isAutoDjActive) { crossfadeValue = it; audioManager.setCrossfade(it) } },
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                colors = SliderDefaults.colors(thumbColor = Color(0xFFFF007F), activeTrackColor = Color(0xFF222222))
            )
            Text("B", color = Color(0xFF00FFCC), fontSize = 14.sp)
        }
    }
}

// --- 4. רכיב פטיפון משודרג ---
@Composable
fun DjDeck(
    deckName: String,
    trackName: String,
    isPlaying: Boolean,
    onBrowseClick: () -> Unit,
    onPlayClick: () -> Unit,
    onPitchChange: (Float) -> Unit
) {
    var pitch by remember { mutableStateOf(1.0f) }
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
        modifier = Modifier.background(Color(0xFF111111), shape = RoundedCornerShape(16.dp)).padding(10.dp)
    ) {
        Text(deckName, color = Color(0xFFFF007F), fontSize = 14.sp)
        Text(trackName, color = Color.Gray, fontSize = 10.sp, maxLines = 1)

        Spacer(modifier = Modifier.height(6.dp))

        // ויזואלייזר
        Row(modifier = Modifier.height(20.dp).width(90.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            for (i in 1..5) {
                val heightMultiplier = if (isPlaying) (i * 0.2f + waveAnim) % 1.0f else 0.1f
                Box(modifier = Modifier.weight(1f).height((20 * heightMultiplier).dp).background(Color(0xFF00FFCC), shape = RoundedCornerShape(1.dp)))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // תקליט
        Box(
            modifier = Modifier.size(100.dp).rotate(if (isPlaying) rotation else 0f).background(Color.Black, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(70.dp).background(Color(0xFF151515), shape = CircleShape))
            Box(modifier = Modifier.size(18.dp).background(Color(0xFFFF007F), shape = CircleShape))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(onClick = onBrowseClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                Text("LOAD", color = Color.White, fontSize = 10.sp)
            }
            Button(onClick = onPlayClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                Text(if (isPlaying) "PAUSE" else "PLAY", color = Color.Black, fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text("SPEED: ${String.format("%.1f", pitch)}x", color = Color.Gray, fontSize = 9.sp)
        Slider(value = pitch, onValueChange = { pitch = it; onPitchChange(it) }, valueRange = 0.5f..1.5f, colors = SliderDefaults.colors(thumbColor = Color(0xFF00FFCC)))
    }
}
