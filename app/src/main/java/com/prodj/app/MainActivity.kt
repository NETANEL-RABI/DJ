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

// --- 1. מנוע השמע (AUDIO ENGINE) ---
class DjAudioManager(context: Context) {
    private var playerA: ExoPlayer? = ExoPlayer.Builder(context).build()
    private var playerB: ExoPlayer? = PlayerBBuilder(context)

    private fun PlayerBBuilder(context: Context): ExoPlayer {
        return ExoPlayer.Builder(context).build()
    }

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
}

// --- 2. האקטיביטי הראשי ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF121212)
            ) {
                DjConsoleScreen()
            }
        }
    }
}

// --- 3. מסך הקונסולה הראשי ---
@Composable
fun DjConsoleScreen() {
    val context = LocalContext.current
    val audioManager = remember { DjAudioManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var isPlayingA by remember { mutableStateOf(false) }
    var isPlayingB by remember { mutableStateOf(false) }
    var crossfadeValue by remember { mutableStateOf(0.5f) }
    var isAutoMixActive by remember { mutableStateOf(false) }

    var trackNameA by remember { mutableStateOf("No Track Loaded") }
    var trackNameB by remember { mutableStateOf("No Track Loaded") }

    val launcherA = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            audioManager.loadTrack("A", it)
            trackNameA = "Track Loaded (A)"
        }
    }

    val launcherB = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            audioManager.loadTrack("B", it)
            trackNameB = "Track Loaded (B)"
        }
    }

    LaunchedEffect(isAutoMixActive) {
        if (isAutoMixActive) {
            coroutineScope.launch {
                if (!isPlayingA) isPlayingA = audioManager.togglePlay("A")
                while (isAutoMixActive) {
                    delay(15000)
                    if (!isAutoMixActive) break
                    if (!isPlayingB) isPlayingB = audioManager.togglePlay("B")
                    for (i in 0..50) {
                        if (!isAutoMixActive) break
                        crossfadeValue = i / 50f
                        audioManager.setCrossfade(crossfadeValue)
                        delay(100)
                    }
                    if (isPlayingA) isPlayingA = audioManager.togglePlay("A")
                    delay(15000)
                    if (!isAutoMixActive) break
                    if (!isPlayingA) isPlayingA = audioManager.togglePlay("A")
                    for (i in 50 downTo 0) {
                        if (!isAutoMixActive) break
                        crossfadeValue = i / 50f
                        audioManager.setCrossfade(crossfadeValue)
                        delay(100)
                    }
                    if (isPlayingB) isPlayingB = audioManager.togglePlay("B")
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(
            onClick = { isAutoMixActive = !isAutoMixActive },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAutoMixActive) Color(0xFF00FFCC) else Color(0xFF333333)
            )
        ) {
            Text(if (isAutoMixActive) "AUTOMIX: ACTIVE" else "START AUTOMIX", color = Color.Black)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DjDeck(
                deckName = "DECK A",
                trackName = trackNameA,
                isPlaying = isPlayingA,
                onBrowseClick = { launcherA.launch("audio/*") },
                onPlayClick = { isPlayingA = audioManager.togglePlay("A") },
                onPitchChange = { audioManager.setPitch("A", it) }
            )

            DjDeck(
                deckName = "DECK B",
                trackName = trackNameB,
                isPlaying = isPlayingB,
                onBrowseClick = { launcherB.launch("audio/*") },
                onPlayClick = { isPlayingB = audioManager.togglePlay("B") },
                onPitchChange = { audioManager.setPitch("B", it) }
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CROSSFADER", color = Color.White, fontSize = 14.sp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(300.dp)
            ) {
                Text("A", color = Color(0xFFDEFF9A))
                Slider(
                    value = crossfadeValue,
                    onValueChange = {
                        if (!isAutoMixActive) {
                            crossfadeValue = it
                            audioManager.setCrossfade(it)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFDEFF9A),
                        activeTrackColor = Color(0xFFDEFF9A)
                    )
                )
                Text("B", color = Color(0xFFDEFF9A))
            }
        }
    }
}

// --- 4. רכיב נגן בודד ---
@Composable
fun DjDeck(
    deckName: String,
    trackName: String,
    isPlaying: Boolean,
    onBrowseClick: () -> Unit,
    onPlayClick: () -> Unit,
    onPitchChange: (Float) -> Unit
) {
    var pitchSliderValue by remember { mutableStateOf(1.0f) }
    val infiniteTransition = rememberInfiniteTransition(label = "Spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing)),
        label = "Rotate"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color(0xFF1A1A1A), shape = CircleShape)
            .padding(20.dp)
    ) {
        Text(deckName, color = Color.White, fontSize = 16.sp)
        Text(trackName, color = Color.Gray, fontSize = 10.sp, maxLines = 1)

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .size(130.dp)
                .rotate(if (isPlaying) rotation else 0f)
                .background(Color.Black, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(25.dp)
                    .background(Color(0xFFDEFF9A), shape = CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onBrowseClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
            ) {
                Text("LOAD", color = Color.White, fontSize = 12.sp)
            }
            Button(
                onClick = onPlayClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDEFF9A))
            ) {
                Text(if (isPlaying) "PAUSE" else "PLAY", color = Color.Black, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = pitchSliderValue,
            onValueChange = {
                pitchSliderValue = it
                onPitchChange(it)
            },
            valueRange = 0.5f..1.5f,
            modifier = Modifier.width(100.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFDEFF9A),
                activeTrackColor = Color(0xFFDEFF9A)
            )
        )
    }
}
