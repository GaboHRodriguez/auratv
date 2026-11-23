package com.auratv.player.cast

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.ContextThemeWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.auratv.player.R
import com.auratv.player.data.Channel
import com.auratv.player.ui.components.FocusableSurface
import com.auratv.player.ui.theme.AuraColors
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.common.images.WebImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Proveedor oficial de Google Cast usando el Default Media Receiver.
 * Compatible con Chromecast, Google TV, Android TV con Chromecast integrado y pantallas compatibles.
 */
class AuraCastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        return CastOptions.Builder()
            .setReceiverApplicationId(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context) = null
}

data class CastUiState(
    val isCastSdkAvailable: Boolean = false,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val deviceName: String? = null,
    val isRemotePlaying: Boolean = true,
    val isRemoteMuted: Boolean = false
)

class AuraCastController(context: Context) {

    private val appContext = context.applicationContext
    private val castContext: CastContext? = runCatching {
        @Suppress("DEPRECATION")
        CastContext.getSharedInstance(appContext)
    }.getOrNull()

    private val _state = MutableStateFlow(
        CastUiState(isCastSdkAvailable = castContext != null)
    )
    val state: StateFlow<CastUiState> = _state.asStateFlow()

    private var lastRequestedChannel: Channel? = null
    private var lastRequestedProgramTitle: String? = null

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {
            _state.update { it.copy(isConnecting = true) }
        }

        override fun onSessionStarted(session: CastSession, sessionId: String) {
            _state.update {
                it.copy(
                    isConnected = true,
                    isConnecting = false,
                    deviceName = session.castDevice?.friendlyName ?: "Chromecast"
                )
            }
            lastRequestedChannel?.let { ch ->
                loadChannelOnSession(session, ch, lastRequestedProgramTitle)
            }
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {
            _state.update { it.copy(isConnected = false, isConnecting = false, deviceName = null) }
        }

        override fun onSessionEnding(session: CastSession) {
            _state.update { it.copy(isConnecting = false) }
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            _state.update { it.copy(isConnected = false, isConnecting = false, deviceName = null) }
        }

        override fun onSessionResuming(session: CastSession, sessionId: String) {
            _state.update { it.copy(isConnecting = true) }
        }

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            _state.update {
                it.copy(
                    isConnected = true,
                    isConnecting = false,
                    deviceName = session.castDevice?.friendlyName ?: "Chromecast"
                )
            }
        }

        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            _state.update { it.copy(isConnected = false, isConnecting = false, deviceName = null) }
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) {
            _state.update { it.copy(isConnected = false) }
        }
    }

    fun startListening() {
        val manager = castContext?.sessionManager ?: return
        runCatching {
            manager.addSessionManagerListener(sessionListener, CastSession::class.java)
            val current = manager.currentCastSession
            if (current != null && current.isConnected) {
                _state.update {
                    it.copy(
                        isConnected = true,
                        isConnecting = false,
                        deviceName = current.castDevice?.friendlyName ?: "Chromecast"
                    )
                }
            }
        }
    }

    fun stopListening() {
        runCatching {
            castContext?.sessionManager?.removeSessionManagerListener(sessionListener, CastSession::class.java)
        }
    }

    fun castChannel(channel: Channel, programTitle: String?) {
        lastRequestedChannel = channel
        lastRequestedProgramTitle = programTitle
        val session = castContext?.sessionManager?.currentCastSession ?: return
        if (session.isConnected) {
            loadChannelOnSession(session, channel, programTitle)
        }
    }

    private fun loadChannelOnSession(session: CastSession, channel: Channel, programTitle: String?) {
        val remoteClient = session.remoteMediaClient ?: return
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, "${channel.number} · ${channel.name}")
            putString(MediaMetadata.KEY_SUBTITLE, programTitle ?: channel.category)
            if (!channel.logoUrl.isNullOrBlank()) {
                runCatching { addImage(WebImage(Uri.parse(channel.logoUrl))) }
            }
        }

        val urlLower = channel.streamUrl.lowercase()
        val contentType = when {
            urlLower.contains(".mpd") -> "application/dash+xml"
            urlLower.contains(".mp4") -> "video/mp4"
            else -> "application/x-mpegurl"
        }

        val mediaInfo = MediaInfo.Builder(channel.streamUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
            .setContentType(contentType)
            .setMetadata(metadata)
            .build()

        val request = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .setAutoplay(true)
            .build()

        remoteClient.load(request)
        _state.update { it.copy(isRemotePlaying = true) }
    }

    fun toggleRemotePlayPause() {
        val remoteClient = castContext?.sessionManager?.currentCastSession?.remoteMediaClient ?: return
        if (remoteClient.isPlaying) {
            remoteClient.pause()
            _state.update { it.copy(isRemotePlaying = false) }
        } else {
            remoteClient.play()
            _state.update { it.copy(isRemotePlaying = true) }
        }
    }

    fun toggleRemoteMute() {
        val session = castContext?.sessionManager?.currentCastSession ?: return
        runCatching {
            val nextMute = !session.isMute
            session.isMute = nextMute
            _state.update { it.copy(isRemoteMuted = nextMute) }
        }
    }

    fun disconnect() {
        runCatching {
            castContext?.sessionManager?.endCurrentSession(true)
        }
        _state.update { it.copy(isConnected = false, isConnecting = false, deviceName = null) }
    }

    companion object {
        fun openSystemCastSettings(context: Context) {
            val intent = Intent(Settings.ACTION_CAST_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
        }
    }
}

/**
 * Botón nativo de Google Cast (Chromecast / Google TV) con fallback a Pantalla Inalámbrica / Miracast
 * en dispositivos sin Google Play Services.
 */
@Composable
fun CastActionButton(
    castState: CastUiState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (castState.isCastSdkAvailable) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.size(38.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    val themed = ContextThemeWrapper(ctx, R.style.Theme_AuraTV)
                    MediaRouteButton(themed).apply {
                        setAlwaysVisible(true)
                        runCatching {
                            CastButtonFactory.setUpMediaRouteButton(themed, this)
                        }
                    }
                },
                modifier = Modifier.size(38.dp)
            )
        }
    } else {
        FocusableSurface(
            onClick = { AuraCastController.openSystemCastSettings(context) },
            shape = CircleShape,
            containerColor = if (castState.isConnected) AuraColors.Emerald else AuraColors.BgSurfaceElevated,
            modifier = modifier
        ) {
            Box(modifier = Modifier.padding(8.dp)) {
                Icon(
                    imageVector = if (castState.isConnected) Icons.Rounded.CastConnected else Icons.Rounded.Cast,
                    contentDescription = "Transmitir a TV",
                    tint = if (castState.isConnected) Color(0xFF041A12) else AuraColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
