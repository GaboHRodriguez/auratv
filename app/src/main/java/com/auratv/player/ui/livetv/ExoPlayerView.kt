package com.auratv.player.ui.livetv

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.auratv.player.data.Channel
import com.auratv.player.ui.components.FocusableSurface
import com.auratv.player.ui.theme.AuraColors

class PlayerControllerHolder {
    var player: ExoPlayer? = null

    fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else p.play()
    }

    fun seekBy(deltaMs: Long) {
        val p = player ?: return
        val target = (p.currentPosition + deltaMs).coerceAtLeast(0L)
        p.seekTo(target)
    }

    fun seekToLive() {
        val p = player ?: return
        p.seekToDefaultPosition()
        p.play()
    }

    fun restartStream() {
        val p = player ?: return
        p.seekTo(0L)
        p.prepare()
        p.play()
    }

    fun stopPlayback() {
        val p = player ?: return
        p.playWhenReady = false
        p.stop()
        p.clearMediaItems()
    }
}

@OptIn(UnstableApi::class)
@Composable
fun ExoPlayerSurface(
    channel: Channel?,
    isMuted: Boolean,
    isPausedExternally: Boolean = false,
    controllerHolder: PlayerControllerHolder,
    onPlayingStateChanged: (Boolean) -> Unit,
    onNextChannel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isBuffering by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var retryCounter by remember { mutableIntStateOf(0) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        controllerHolder.player = exoPlayer
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    playbackError = null
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                onPlayingStateChanged(isPlaying)
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                playbackError = "Señal temporalmente no disponible (${error.errorCodeName})"
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            controllerHolder.player = null
            exoPlayer.release()
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(isPausedExternally) {
        if (isPausedExternally) {
            exoPlayer.pause()
        } else if (channel != null) {
            exoPlayer.play()
        }
    }

    LaunchedEffect(channel?.streamUrl, retryCounter, isPausedExternally) {
        if (channel == null || isPausedExternally) {
            if (channel == null) exoPlayer.stop() else exoPlayer.pause()
            playbackError = null
            isBuffering = false
            return@LaunchedEffect
        }

        playbackError = null
        isBuffering = true

        val headers = HashMap<String, String>()
        if (!channel.referer.isNullOrBlank()) {
            headers["Referer"] = channel.referer
        }
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(12_000)
            .setReadTimeoutMs(15_000)
            .setUserAgent(channel.userAgent ?: "Mozilla/5.0 (Linux; Android 14) AuraTV/1.0")
            .apply {
                if (headers.isNotEmpty()) setDefaultRequestProperties(headers)
            }

        val mediaSource = DefaultMediaSourceFactory(httpFactory)
            .createMediaSource(MediaItem.fromUri(channel.streamUrl))

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    keepScreenOn = true
                    isFocusable = false
                    isFocusableInTouchMode = false
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering && playbackError == null) {
            CircularProgressIndicator(
                color = AuraColors.Emerald,
                strokeWidth = 3.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
            )
        }

        val err = playbackError
        if (err != null && channel != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(AuraColors.BgSurface.copy(alpha = 0.92f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Icon(Icons.Rounded.WarningAmber, null, tint = AuraColors.Amber, modifier = Modifier.size(34.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${channel.number} · ${channel.name}",
                    color = AuraColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = err,
                    color = AuraColors.TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FocusableSurface(
                        onClick = { retryCounter++ },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = AuraColors.Emerald
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, null, tint = Color(0xFF041A12), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reintentar", color = Color(0xFF041A12), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    FocusableSurface(
                        onClick = onNextChannel,
                        shape = RoundedCornerShape(20.dp),
                        containerColor = AuraColors.BgSurfaceElevated,
                        borderColor = AuraColors.BorderSubtle
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.SkipNext, null, tint = AuraColors.TextPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Siguiente canal", color = AuraColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
