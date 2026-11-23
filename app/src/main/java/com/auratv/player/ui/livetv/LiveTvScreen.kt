package com.auratv.player.ui.livetv

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ScreenShare
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auratv.player.cast.AuraCastController
import com.auratv.player.cast.CastActionButton
import com.auratv.player.ui.components.FocusableSurface
import com.auratv.player.ui.sources.SourcesModal
import com.auratv.player.ui.theme.AuraColors
import com.auratv.player.viewmodel.AuraTvUiState
import com.auratv.player.viewmodel.AuraTvViewModel
import kotlinx.coroutines.delay

@Composable
fun LiveTvScreen(
    uiState: AuraTvUiState,
    viewModel: AuraTvViewModel,
    onExitApp: () -> Unit
) {
    val context = LocalContext.current
    val playerController = remember { PlayerControllerHolder() }
    val castController = remember { AuraCastController(context) }
    val castState by castController.state.collectAsState()

    var isPlaying by remember { mutableStateOf(true) }
    var controlsPing by remember { mutableIntStateOf(0) }
    var showZappingOsd by remember { mutableStateOf(false) }
    var zappingPing by remember { mutableIntStateOf(0) }

    val playPauseFocusRequester = remember { FocusRequester() }
    val exitConfirmFocusRequester = remember { FocusRequester() }

    val isAnyOverlayOpen = uiState.showEpgOverlay || uiState.showQuickGuide || uiState.showSourcesModal || uiState.showExitConfirmDialog

    DisposableEffect(castController) {
        castController.startListening()
        onDispose { castController.stopListening() }
    }

    // Sincronizar automáticamente el canal activo hacia Chromecast cuando está conectado
    LaunchedEffect(uiState.activeChannel?.id, castState.isConnected) {
        val ch = uiState.activeChannel ?: return@LaunchedEffect
        val progTitle = viewModel.currentProgramFor(ch)?.title
        if (castState.isConnected) {
            castController.castChannel(ch, progTitle)
        }
    }

    // Mostrar brevemente el banner estilo TV (OSD) al cambiar de canal
    LaunchedEffect(uiState.activeChannel?.id) {
        if (uiState.activeChannel != null && !uiState.showEpgOverlay) {
            showZappingOsd = true
            zappingPing++
        }
    }

    LaunchedEffect(zappingPing) {
        if (showZappingOsd) {
            delay(2600L)
            showZappingOsd = false
        }
    }

    // Cuando aparecen los controles del reproductor en TV, enfocar el botón central Play/Pausa
    LaunchedEffect(uiState.arePlayerControlsVisible, isAnyOverlayOpen, castState.isConnected) {
        if (uiState.arePlayerControlsVisible && !isAnyOverlayOpen && !castState.isConnected) {
            delay(80L)
            runCatching { playPauseFocusRequester.requestFocus() }
        }
    }

    // Auto-ocultamiento de controles del reproductor tras 4.5 s de inactividad
    LaunchedEffect(uiState.arePlayerControlsVisible, controlsPing, isAnyOverlayOpen, castState.isConnected) {
        if (uiState.arePlayerControlsVisible && !isAnyOverlayOpen && !castState.isConnected) {
            delay(4500L)
            viewModel.setPlayerControlsVisible(false)
        }
    }

    // Cuando se abre el diálogo de salida, enfocar el botón "Detener y salir"
    LaunchedEffect(uiState.showExitConfirmDialog) {
        if (uiState.showExitConfirmDialog) {
            delay(80L)
            runCatching { exitConfirmFocusRequester.requestFocus() }
        }
    }

    fun zapChannel(delta: Int) {
        viewModel.stepChannel(delta)
        showZappingOsd = true
        zappingPing++
        controlsPing++
    }

    fun stopAndExit() {
        playerController.stopPlayback()
        if (castState.isConnected) {
            castController.disconnect()
        }
        viewModel.stopPlayback()
        onExitApp()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 1. Reproductor HLS/DASH a pantalla completa en segundo plano
        // Solo acepta gestos táctiles y clic cuando NO hay overlays encima (evita robar foco en Android TV)
        var dragAccumulator by remember { mutableFloatStateOf(0f) }
        val videoInteractionModifier = if (!isAnyOverlayOpen) {
            Modifier
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { dragAccumulator = 0f },
                        onVerticalDrag = { _, dragAmount -> dragAccumulator += dragAmount },
                        onDragEnd = {
                            when {
                                dragAccumulator < -90f -> zapChannel(1)  // Deslizar arriba -> canal siguiente
                                dragAccumulator > 90f -> zapChannel(-1)  // Deslizar abajo -> canal anterior
                            }
                            dragAccumulator = 0f
                        }
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    viewModel.setPlayerControlsVisible(!uiState.arePlayerControlsVisible)
                    controlsPing++
                }
        } else {
            Modifier
        }

        ExoPlayerSurface(
            channel = uiState.activeChannel,
            isMuted = uiState.isMuted,
            isPausedExternally = castState.isConnected,
            controllerHolder = playerController,
            onPlayingStateChanged = { isPlaying = it },
            onNextChannel = { zapChannel(1) },
            modifier = Modifier
                .fillMaxSize()
                .then(videoInteractionModifier)
        )

        // 1b. Pantalla de estado cuando está transmitiendo a Chromecast
        if (castState.isConnected && !uiState.showEpgOverlay) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(AuraColors.BgObsidian.copy(alpha = 0.92f))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(AuraColors.BgSurface, RoundedCornerShape(18.dp))
                        .border(1.dp, AuraColors.Emerald.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 32.dp, vertical = 26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CastConnected,
                        contentDescription = null,
                        tint = AuraColors.Emerald,
                        modifier = Modifier.size(46.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Transmitiendo en ${castState.deviceName ?: "Chromecast"}",
                        color = AuraColors.Emerald,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    uiState.activeChannel?.let { ch ->
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ChannelLogoBox(channel = ch, size = 34.dp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "${ch.number} · ${ch.name}",
                                    color = AuraColors.TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = viewModel.currentProgramFor(ch)?.title ?: ch.category,
                                    color = AuraColors.TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        FocusableSurface(
                            onClick = { zapChannel(-1) },
                            shape = RoundedCornerShape(20.dp),
                            containerColor = AuraColors.BgSurfaceElevated,
                            borderColor = AuraColors.BorderSubtle
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                            ) {
                                Icon(Icons.Rounded.SkipPrevious, null, tint = AuraColors.TextPrimary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Anterior", color = AuraColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        FocusableSurface(
                            onClick = { castController.toggleRemotePlayPause() },
                            shape = CircleShape,
                            containerColor = AuraColors.Emerald
                        ) {
                            Box(modifier = Modifier.padding(12.dp)) {
                                Icon(
                                    imageVector = if (castState.isRemotePlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Pausar/Reanudar en TV",
                                    tint = Color(0xFF041A12),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        FocusableSurface(
                            onClick = { zapChannel(1) },
                            shape = RoundedCornerShape(20.dp),
                            containerColor = AuraColors.BgSurfaceElevated,
                            borderColor = AuraColors.BorderSubtle
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                            ) {
                                Text("Siguiente", color = AuraColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Rounded.SkipNext, null, tint = AuraColors.TextPrimary, modifier = Modifier.size(18.dp))
                            }
                        }

                        FocusableSurface(
                            onClick = { castController.toggleRemoteMute() },
                            shape = CircleShape,
                            containerColor = AuraColors.BgSurfaceElevated
                        ) {
                            Box(modifier = Modifier.padding(10.dp)) {
                                Icon(
                                    imageVector = if (castState.isRemoteMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = "Silenciar TV",
                                    tint = if (castState.isRemoteMuted) AuraColors.LiveRed else AuraColors.TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    FocusableSurface(
                        onClick = { castController.disconnect() },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = AuraColors.LiveRed.copy(alpha = 0.2f),
                        borderColor = AuraColors.LiveRed.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "Dejar de transmitir",
                            color = AuraColors.LiveRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // 1c. Banner OSD estilo TV al hacer zapping con controles ocultos
        AnimatedVisibility(
            visible = showZappingOsd && !uiState.arePlayerControlsVisible && !isAnyOverlayOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
        ) {
            uiState.activeChannel?.let { ch ->
                val prog = remember(ch.id, uiState.epgByKey) { viewModel.currentProgramFor(ch) }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(AuraColors.BgSurface.copy(alpha = 0.90f), RoundedCornerShape(14.dp))
                        .border(1.dp, AuraColors.Emerald.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    ChannelLogoBox(channel = ch, size = 34.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "CH ${ch.number} · ${ch.name}",
                            color = AuraColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = prog?.title ?: ch.category,
                            color = AuraColors.Emerald,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 2. Controles superpuestos del reproductor en vivo (cuando no hay overlays encima)
        AnimatedVisibility(
            visible = !isAnyOverlayOpen && (uiState.arePlayerControlsVisible || castState.isConnected),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.72f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.82f)
                            )
                        )
                    )
                    .padding(18.dp)
            ) {
                // Top Bar: Volver a Guía (izq) + Canal + Cast + Pantalla Inalámbrica (der)
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                ) {
                    FocusableSurface(
                        onClick = { viewModel.setShowEpgOverlay(true) },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = AuraColors.BgSurface.copy(alpha = 0.85f),
                        borderColor = AuraColors.BorderSubtle
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.ArrowBackIosNew, null, tint = AuraColors.Emerald, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Guía de TV", color = AuraColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.activeChannel?.let { ch ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(AuraColors.BgSurface.copy(alpha = 0.82f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                ChannelLogoBox(channel = ch, size = 26.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${ch.number} · ${ch.name}",
                                    color = AuraColors.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Botón oficial de Google Cast (Chromecast / Google TV)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .background(
                                    if (castState.isConnected) AuraColors.Emerald.copy(alpha = 0.25f) else AuraColors.BgSurface.copy(alpha = 0.85f),
                                    CircleShape
                                )
                                .border(
                                    1.dp,
                                    if (castState.isConnected) AuraColors.Emerald else AuraColors.BorderSubtle,
                                    CircleShape
                                )
                        ) {
                            CastActionButton(castState = castState)
                        }

                        // Botón auxiliar Miracast / Smart View (TVs sin Chromecast)
                        FocusableSurface(
                            onClick = { AuraCastController.openSystemCastSettings(context) },
                            shape = CircleShape,
                            containerColor = AuraColors.BgSurface.copy(alpha = 0.85f),
                            borderColor = AuraColors.BorderSubtle
                        ) {
                            Box(modifier = Modifier.padding(9.dp)) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ScreenShare,
                                    contentDescription = "Pantalla inalámbrica / Miracast",
                                    tint = AuraColors.TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Pestaña vertical izquierda: "Guía rápida"
                FocusableSurface(
                    onClick = { viewModel.setShowQuickGuide(true) },
                    shape = RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp),
                    containerColor = AuraColors.BgSurface.copy(alpha = 0.88f),
                    borderColor = AuraColors.Emerald.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ViewList, null, tint = AuraColors.Emerald, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Guía rápida", color = AuraColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Controles centrales únicos: Canal anterior | -10s | Play/Pausa | +10s | Canal siguiente
                if (!castState.isConnected) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        FocusableSurface(
                            onClick = { zapChannel(-1) },
                            shape = CircleShape,
                            containerColor = AuraColors.BgSurface.copy(alpha = 0.85f),
                            borderColor = AuraColors.BorderSubtle
                        ) {
                            Box(modifier = Modifier.padding(11.dp)) {
                                Icon(Icons.Rounded.SkipPrevious, "Canal anterior", tint = AuraColors.TextPrimary, modifier = Modifier.size(26.dp))
                            }
                        }

                        FocusableSurface(
                            onClick = {
                                playerController.seekBy(-10_000L)
                                controlsPing++
                            },
                            shape = CircleShape,
                            containerColor = AuraColors.BgSurface.copy(alpha = 0.85f)
                        ) {
                            Box(modifier = Modifier.padding(11.dp)) {
                                Icon(Icons.Rounded.Replay10, "-10 s", tint = AuraColors.TextPrimary, modifier = Modifier.size(24.dp))
                            }
                        }

                        FocusableSurface(
                            onClick = {
                                playerController.togglePlayPause()
                                controlsPing++
                            },
                            focusRequester = playPauseFocusRequester,
                            shape = CircleShape,
                            containerColor = AuraColors.Emerald,
                            focusedContainerColor = AuraColors.EmeraldDark
                        ) {
                            Box(modifier = Modifier.padding(15.dp)) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Reproducir o pausar",
                                    tint = Color(0xFF041A12),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        FocusableSurface(
                            onClick = {
                                playerController.seekBy(10_000L)
                                controlsPing++
                            },
                            shape = CircleShape,
                            containerColor = AuraColors.BgSurface.copy(alpha = 0.85f)
                        ) {
                            Box(modifier = Modifier.padding(11.dp)) {
                                Icon(Icons.Rounded.Forward10, "+10 s", tint = AuraColors.TextPrimary, modifier = Modifier.size(24.dp))
                            }
                        }

                        FocusableSurface(
                            onClick = { zapChannel(1) },
                            shape = CircleShape,
                            containerColor = AuraColors.BgSurface.copy(alpha = 0.85f),
                            borderColor = AuraColors.BorderSubtle
                        ) {
                            Box(modifier = Modifier.padding(11.dp)) {
                                Icon(Icons.Rounded.SkipNext, "Canal siguiente", tint = AuraColors.TextPrimary, modifier = Modifier.size(26.dp))
                            }
                        }
                    }
                }

                // Barra inferior: Ver en vivo, Reconectar, Info, Favorito, Silenciar, Fuentes y Salir
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        FocusableSurface(
                            onClick = {
                                playerController.seekToLive()
                                controlsPing++
                            },
                            shape = RoundedCornerShape(20.dp),
                            containerColor = AuraColors.BgSurface.copy(alpha = 0.88f),
                            borderColor = AuraColors.Emerald
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Icon(Icons.Rounded.RadioButtonChecked, null, tint = AuraColors.LiveRed, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Ver en vivo", color = AuraColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        FocusableSurface(
                            onClick = {
                                playerController.restartStream()
                                controlsPing++
                            },
                            shape = CircleShape,
                            containerColor = AuraColors.BgSurface.copy(alpha = 0.85f)
                        ) {
                            Box(modifier = Modifier.padding(8.dp)) {
                                Icon(Icons.Rounded.Replay, "Reconectar", tint = AuraColors.TextPrimary, modifier = Modifier.size(18.dp))
                            }
                        }

                        uiState.activeChannel?.let { ch ->
                            val nowProg = remember(ch.id, uiState.epgByKey) { viewModel.currentProgramFor(ch) }
                            Column {
                                Text(
                                    text = nowProg?.title ?: ch.name,
                                    color = AuraColors.TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${ch.category} · Deslizá ↑↓ para cambiar canal",
                                    color = AuraColors.TextSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        uiState.activeChannel?.let { ch ->
                            val fav = viewModel.isFavorite(ch)
                            FocusableSurface(
                                onClick = { viewModel.toggleFavorite(ch) },
                                shape = CircleShape,
                                containerColor = AuraColors.BgSurface.copy(alpha = 0.85f)
                            ) {
                                Box(modifier = Modifier.padding(9.dp)) {
                                    Icon(
                                        imageVector = if (fav) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                        contentDescription = "Favorito",
                                        tint = if (fav) AuraColors.Amber else AuraColors.TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        FocusableSurface(
                            onClick = { viewModel.toggleMute() },
                            shape = CircleShape,
                            containerColor = AuraColors.BgSurface.copy(alpha = 0.85f)
                        ) {
                            Box(modifier = Modifier.padding(9.dp)) {
                                Icon(
                                    imageVector = if (uiState.isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = "Silenciar",
                                    tint = if (uiState.isMuted) AuraColors.LiveRed else AuraColors.TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        FocusableSurface(
                            onClick = { viewModel.setShowSourcesModal(true) },
                            shape = RoundedCornerShape(18.dp),
                            containerColor = AuraColors.BgSurface.copy(alpha = 0.85f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Rounded.Public, null, tint = AuraColors.Emerald, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Fuentes", color = AuraColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        FocusableSurface(
                            onClick = { viewModel.setShowExitConfirmDialog(true) },
                            shape = CircleShape,
                            containerColor = AuraColors.LiveRed.copy(alpha = 0.22f),
                            focusedContainerColor = AuraColors.LiveRed.copy(alpha = 0.38f),
                            borderColor = AuraColors.LiveRed.copy(alpha = 0.6f)
                        ) {
                            Box(modifier = Modifier.padding(9.dp)) {
                                Icon(
                                    imageVector = Icons.Rounded.PowerSettingsNew,
                                    contentDescription = "Detener y salir",
                                    tint = AuraColors.LiveRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Mini-EPG lateral "Guía rápida"
        if (uiState.showQuickGuide) {
            QuickGuideDrawer(
                channels = uiState.filteredChannels,
                activeChannel = uiState.activeChannel,
                currentProgramFor = { viewModel.currentProgramFor(it) },
                onSelectChannel = { ch ->
                    viewModel.selectChannel(ch, hideEpg = true)
                    viewModel.setShowQuickGuide(false)
                    viewModel.setPlayerControlsVisible(false)
                },
                onClose = { viewModel.setShowQuickGuide(false) },
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }

        // 4. Grilla EPG Completa superpuesta
        if (uiState.showEpgOverlay) {
            EpgOverlay(
                channels = uiState.filteredChannels,
                activeChannel = uiState.activeChannel,
                activeProfile = uiState.activeProfile,
                selectedCategory = uiState.selectedCategory,
                selectedDayOffset = uiState.selectedDayOffset,
                searchQuery = uiState.searchQuery,
                favoriteCount = uiState.favoriteChannelIds.size,
                isMuted = uiState.isMuted,
                castState = castState,
                isLoadingChannels = uiState.isLoadingChannels,
                isLoadingEpg = uiState.isLoadingEpg,
                statusMessage = uiState.statusMessage,
                isFavorite = { viewModel.isFavorite(it) },
                programsFor = { ch, day -> viewModel.programsFor(ch, day) },
                dayBoundsMs = { viewModel.dayBoundsMs(it) },
                onHideGuide = {
                    viewModel.setShowEpgOverlay(false)
                    viewModel.setPlayerControlsVisible(true)
                    controlsPing++
                },
                onToggleMute = { viewModel.toggleMute() },
                onSelectCategory = { viewModel.selectCategory(it) },
                onSelectDayOffset = { viewModel.selectDayOffset(it) },
                onSearchChange = { viewModel.updateSearchQuery(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onPlayChannel = { ch, hide ->
                    viewModel.selectChannel(ch, hideEpg = hide)
                    viewModel.setPlayerControlsVisible(false)
                    controlsPing++
                },
                onRestartLive = { ch ->
                    viewModel.selectChannel(ch, hideEpg = true)
                    playerController.restartStream()
                    viewModel.setPlayerControlsVisible(false)
                    controlsPing++
                },
                onOpenSources = { viewModel.setShowSourcesModal(true) },
                onSwitchProfile = { viewModel.switchToProfiles() },
                onRequestExit = { viewModel.setShowExitConfirmDialog(true) }
            )
        }

        // 5. Modal de Fuentes Abiertas y Guías EPG
        if (uiState.showSourcesModal) {
            SourcesModal(
                playlists = uiState.allPlaylists,
                enabledPlaylistIds = uiState.enabledPlaylistIds,
                epgSources = uiState.allEpgSources,
                enabledEpgIds = uiState.enabledEpgIds,
                channelCounts = uiState.sourceChannelCounts,
                isLoading = uiState.isLoadingChannels,
                onTogglePlaylist = { viewModel.togglePlaylistSource(it) },
                onToggleEpg = { viewModel.toggleEpgSource(it) },
                onEnableRegionalPack = { viewModel.enableAllSpanishAndRegionalPresets() },
                onAddCustomPlaylist = { n, u -> viewModel.addCustomPlaylist(n, u) },
                onRemoveCustomPlaylist = { viewModel.removeCustomPlaylist(it) },
                onAddCustomEpg = { n, u -> viewModel.addCustomEpg(n, u) },
                onRemoveCustomEpg = { viewModel.removeCustomEpg(it) },
                onSyncAndClose = {
                    viewModel.setShowSourcesModal(false)
                    viewModel.syncSources()
                },
                onClose = { viewModel.setShowSourcesModal(false) }
            )
        }

        // 6. Diálogo de confirmación para detener reproducción y salir definitivamente
        if (uiState.showExitConfirmDialog) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.78f))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(AuraColors.BgSurface, RoundedCornerShape(18.dp))
                        .border(1.dp, AuraColors.BorderSubtle, RoundedCornerShape(18.dp))
                        .padding(horizontal = 28.dp, vertical = 24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PowerSettingsNew,
                        contentDescription = null,
                        tint = AuraColors.LiveRed,
                        modifier = Modifier.size(38.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "¿Salir de AuraTV?",
                        color = AuraColors.TextPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Se detendrá la reproducción actual y se cerrará la aplicación.",
                        color = AuraColors.TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FocusableSurface(
                            onClick = { viewModel.setShowExitConfirmDialog(false) },
                            shape = RoundedCornerShape(22.dp),
                            containerColor = AuraColors.BgSurfaceElevated,
                            borderColor = AuraColors.BorderSubtle
                        ) {
                            Text(
                                text = "Cancelar",
                                color = AuraColors.TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                            )
                        }

                        FocusableSurface(
                            onClick = { stopAndExit() },
                            focusRequester = exitConfirmFocusRequester,
                            shape = RoundedCornerShape(22.dp),
                            containerColor = AuraColors.LiveRed,
                            focusedContainerColor = Color(0xFFD93850)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Rounded.PowerSettingsNew, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Detener y salir",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
