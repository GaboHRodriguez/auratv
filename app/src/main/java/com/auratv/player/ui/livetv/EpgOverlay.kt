package com.auratv.player.ui.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.auratv.player.cast.CastActionButton
import com.auratv.player.cast.CastUiState
import com.auratv.player.data.Channel
import com.auratv.player.data.EpgProgram
import com.auratv.player.data.LegalSourceCatalog
import com.auratv.player.data.ProgramState
import com.auratv.player.data.UserProfile
import com.auratv.player.ui.components.AvatarCircle
import com.auratv.player.ui.components.FocusableSurface
import com.auratv.player.ui.theme.AuraColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val SLOT_MS = 30L * 60L * 1000L
private const val VISIBLE_SLOTS = 4 // 2 horas visibles por ventana de grilla

@Composable
fun EpgOverlay(
    channels: List<Channel>,
    activeChannel: Channel?,
    activeProfile: UserProfile?,
    selectedCategory: String,
    selectedDayOffset: Int,
    searchQuery: String,
    favoriteCount: Int,
    isMuted: Boolean,
    castState: CastUiState,
    isLoadingChannels: Boolean,
    isLoadingEpg: Boolean,
    statusMessage: String?,
    isFavorite: (Channel) -> Boolean,
    programsFor: (Channel, Int) -> List<EpgProgram>,
    dayBoundsMs: (Int) -> Pair<Long, Long>,
    onHideGuide: () -> Unit,
    onToggleMute: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectDayOffset: (Int) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onPlayChannel: (Channel, Boolean) -> Unit,
    onRestartLive: (Channel) -> Unit,
    onOpenSources: () -> Unit,
    onSwitchProfile: () -> Unit,
    onRequestExit: () -> Unit
) {
    var showDateDropdown by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var expandedChannelId by remember { mutableStateOf<String?>(null) }
    var expandedProgram by remember { mutableStateOf<EpgProgram?>(null) }
    val guideFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(80L)
        runCatching { guideFocusRequester.requestFocus() }
    }

    val nowMs = System.currentTimeMillis()
    val currentHalfHourMs = (nowMs / SLOT_MS) * SLOT_MS
    var windowStartMs by remember { mutableLongStateOf(currentHalfHourMs) }

    // Al cambiar de día, ajusta la ventana de tiempo al día elegido
    LaunchedEffect(selectedDayOffset) {
        val (dayStart, _) = dayBoundsMs(selectedDayOffset)
        windowStartMs = if (selectedDayOffset == 0) {
            (System.currentTimeMillis() / SLOT_MS) * SLOT_MS
        } else {
            dayStart + 18L * 60L * 60L * 1000L // 18:00 por defecto en otros días
        }
    }

    val windowEndMs = windowStartMs + VISIBLE_SLOTS * SLOT_MS
    val isAtLiveWindow = selectedDayOffset == 0 && kotlin.math.abs(windowStartMs - currentHalfHourMs) <= SLOT_MS
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dayFmt = remember { SimpleDateFormat("EEE dd/MM", Locale("es", "AR")) }

    fun dayLabel(offset: Int): String = when (offset) {
        -1 -> "Ayer"
        0 -> "Hoy"
        else -> {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
            dayFmt.format(cal.time).replaceFirstChar { it.uppercase() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraColors.BgObsidian.copy(alpha = 0.90f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // === TOP BAR ===
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AuraColors.BgSurface)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FocusableSurface(
                        onClick = onHideGuide,
                        focusRequester = guideFocusRequester,
                        shape = RoundedCornerShape(20.dp),
                        containerColor = AuraColors.BgSurfaceElevated,
                        borderColor = AuraColors.BorderSubtle
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.ArrowBackIosNew, null, tint = AuraColors.Emerald, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ocultar Guía", color = AuraColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Text(
                        text = "Guía de TV",
                        color = AuraColors.Emerald,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (isLoadingChannels || isLoadingEpg) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = AuraColors.Emerald,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isLoadingChannels) "Cargando canales..." else "Cargando EPG...",
                                color = AuraColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    } else if (statusMessage != null) {
                        Text(
                            text = "${channels.size} canales",
                            color = AuraColors.TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FocusableSurface(
                        onClick = { showSearchBar = !showSearchBar },
                        shape = CircleShape,
                        containerColor = if (showSearchBar) AuraColors.Emerald else AuraColors.BgSurfaceElevated
                    ) {
                        Box(modifier = Modifier.padding(8.dp)) {
                            Icon(
                                Icons.Rounded.Search,
                                "Buscar",
                                tint = if (showSearchBar) Color(0xFF041A12) else AuraColors.TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    FocusableSurface(
                        onClick = onOpenSources,
                        shape = RoundedCornerShape(20.dp),
                        containerColor = AuraColors.BgSurfaceElevated,
                        borderColor = AuraColors.BorderSubtle
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Icon(Icons.Rounded.Public, null, tint = AuraColors.Emerald, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Fuentes", color = AuraColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    CastActionButton(castState = castState)

                    FocusableSurface(
                        onClick = onToggleMute,
                        shape = CircleShape,
                        containerColor = AuraColors.BgSurfaceElevated
                    ) {
                        Box(modifier = Modifier.padding(8.dp)) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = "Audio",
                                tint = if (isMuted) AuraColors.LiveRed else AuraColors.TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (activeProfile != null) {
                        FocusableSurface(
                            onClick = onSwitchProfile,
                            shape = CircleShape,
                            containerColor = Color.Transparent
                        ) {
                            AvatarCircle(activeProfile.avatarIndex, size = 34.dp)
                        }
                    }

                    FocusableSurface(
                        onClick = onRequestExit,
                        shape = RoundedCornerShape(20.dp),
                        containerColor = AuraColors.LiveRed.copy(alpha = 0.16f),
                        focusedContainerColor = AuraColors.LiveRed.copy(alpha = 0.30f),
                        borderColor = AuraColors.LiveRed.copy(alpha = 0.55f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        ) {
                            Icon(Icons.Rounded.PowerSettingsNew, "Salir", tint = AuraColors.LiveRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Salir", color = AuraColors.LiveRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (showSearchBar) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AuraColors.BgSurface)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        singleLine = true,
                        placeholder = { Text("Buscar por nombre o número de canal...", color = AuraColors.TextMuted, fontSize = 13.sp) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                FocusableSurface(
                                    onClick = { onSearchChange("") },
                                    shape = CircleShape,
                                    containerColor = Color.Transparent
                                ) {
                                    Icon(Icons.Rounded.Close, "Limpiar", tint = AuraColors.TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraColors.Emerald,
                            unfocusedBorderColor = AuraColors.BorderSubtle,
                            focusedTextColor = AuraColors.TextPrimary,
                            unfocusedTextColor = AuraColors.TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // === FILTERS & DATE ROW ===
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AuraColors.BgObsidian)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Date Selector ("Hoy" dropdown: Ayer, Hoy, +5 días)
                Box {
                    FocusableSurface(
                        onClick = { showDateDropdown = true },
                        shape = RoundedCornerShape(18.dp),
                        containerColor = AuraColors.BgSurfaceElevated,
                        borderColor = AuraColors.BorderSubtle
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Icon(Icons.Rounded.CalendarToday, null, tint = AuraColors.Emerald, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(dayLabel(selectedDayOffset), color = AuraColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = AuraColors.TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = showDateDropdown,
                        onDismissRequest = { showDateDropdown = false },
                        modifier = Modifier.background(AuraColors.BgSurfaceElevated)
                    ) {
                        (-1..5).forEach { offset ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = dayLabel(offset),
                                        color = if (offset == selectedDayOffset) AuraColors.Emerald else AuraColors.TextPrimary,
                                        fontWeight = if (offset == selectedDayOffset) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSelectDayOffset(offset)
                                    showDateDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Category Filter Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(LegalSourceCatalog.categoryTabs) { cat ->
                        val isFavPill = cat == "Favoritos"
                        val enabled = !isFavPill || favoriteCount > 0
                        val selected = cat == selectedCategory
                        FocusableSurface(
                            onClick = { onSelectCategory(cat) },
                            enabled = enabled,
                            shape = RoundedCornerShape(18.dp),
                            containerColor = when {
                                selected -> AuraColors.Emerald
                                !enabled -> AuraColors.BgSurface
                                else -> AuraColors.BgSurfaceElevated
                            },
                            borderColor = if (selected) AuraColors.Emerald else AuraColors.BorderSubtle
                        ) {
                            Text(
                                text = if (isFavPill && favoriteCount > 0) "Favoritos ($favoriteCount)" else cat,
                                color = when {
                                    selected -> Color(0xFF041A12)
                                    !enabled -> AuraColors.TextMuted
                                    else -> AuraColors.TextPrimary
                                },
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // === TIMELINE HEADER ===
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AuraColors.BgSurface)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "CANALES",
                    color = AuraColors.TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(168.dp)
                )

                FocusableSurface(
                    onClick = { windowStartMs -= SLOT_MS },
                    shape = CircleShape,
                    containerColor = AuraColors.BgSurfaceElevated
                ) {
                    Box(modifier = Modifier.padding(4.dp)) {
                        Icon(Icons.Rounded.ChevronLeft, "Anterior 30 min", tint = AuraColors.TextPrimary, modifier = Modifier.size(18.dp))
                    }
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                ) {
                    for (slot in 0 until VISIBLE_SLOTS) {
                        val slotTime = windowStartMs + slot * SLOT_MS
                        val isCurrentSlot = nowMs in slotTime until (slotTime + SLOT_MS)
                        Text(
                            text = timeFmt.format(Date(slotTime)),
                            color = if (isCurrentSlot) AuraColors.Emerald else AuraColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isCurrentSlot) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                FocusableSurface(
                    onClick = { windowStartMs += SLOT_MS },
                    shape = CircleShape,
                    containerColor = AuraColors.BgSurfaceElevated
                ) {
                    Box(modifier = Modifier.padding(4.dp)) {
                        Icon(Icons.Rounded.ChevronRight, "Siguiente 30 min", tint = AuraColors.TextPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            HorizontalDivider(color = AuraColors.BorderSubtle)

            // === VIRTUALIZED EPG CHANNEL ROWS ===
            if (channels.isEmpty() && !isLoadingChannels) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No hay canales para este filtro", color = AuraColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text("Probá cambiando de categoría o activando más fuentes abiertas.", color = AuraColors.TextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 72.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(channels, key = { it.id }) { channel ->
                        val isActive = channel.id == activeChannel?.id
                        val fav = isFavorite(channel)
                        val rowPrograms = remember(channel.id, selectedDayOffset, windowStartMs) {
                            programsFor(channel, selectedDayOffset).filter {
                                it.endMs > windowStartMs && it.startMs < windowEndMs
                            }
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(68.dp)
                                    .background(if (isActive) AuraColors.BgSurfaceHover.copy(alpha = 0.7f) else Color.Transparent)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                // Active left vertical emerald bar
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (isActive) AuraColors.Emerald else Color.Transparent)
                                )
                                Spacer(Modifier.width(6.dp))

                                // Channel info cell
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.width(166.dp)
                                ) {
                                    FocusableSurface(
                                        onClick = { onPlayChannel(channel, true) },
                                        shape = RoundedCornerShape(8.dp),
                                        containerColor = Color.Transparent,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(4.dp)
                                        ) {
                                            Text(
                                                text = channel.number.toString(),
                                                color = if (isActive) AuraColors.Emerald else AuraColors.TextSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.width(30.dp)
                                            )
                                            ChannelLogoBox(channel = channel, size = 34.dp)
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = channel.name,
                                                color = AuraColors.TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }

                                    FocusableSurface(
                                        onClick = { onToggleFavorite(channel) },
                                        shape = CircleShape,
                                        containerColor = Color.Transparent
                                    ) {
                                        Box(modifier = Modifier.padding(5.dp)) {
                                            Icon(
                                                imageVector = if (fav) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                                contentDescription = "Gestionar canal favorito",
                                                tint = if (fav) AuraColors.Amber else AuraColors.TextMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.width(8.dp))

                                // Program timeline cells proportional to duration
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        if (rowPrograms.isEmpty()) {
                                            FocusableSurface(
                                                onClick = { onPlayChannel(channel, true) },
                                                shape = RoundedCornerShape(8.dp),
                                                containerColor = AuraColors.BgSurface,
                                                borderColor = AuraColors.BorderSubtle,
                                                modifier = Modifier.weight(1f).fillMaxHeight()
                                            ) {
                                                Column(
                                                    verticalArrangement = Arrangement.Center,
                                                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp)
                                                ) {
                                                    Text(channel.name, color = AuraColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(channel.category, color = AuraColors.TextMuted, fontSize = 10.sp, maxLines = 1)
                                                }
                                            }
                                        } else {
                                            val totalWindow = (windowEndMs - windowStartMs).coerceAtLeast(1L)
                                            for (prog in rowPrograms) {
                                                val clippedStart = prog.startMs.coerceAtLeast(windowStartMs)
                                                val clippedEnd = prog.endMs.coerceAtMost(windowEndMs)
                                                val weight = ((clippedEnd - clippedStart).toFloat() / totalWindow.toFloat()).coerceAtLeast(0.12f)
                                                val isLive = prog.stateAt(nowMs) == ProgramState.LIVE
                                                val isSelectedProg = expandedChannelId == channel.id && expandedProgram?.id == prog.id

                                                FocusableSurface(
                                                    onClick = {
                                                        if (isSelectedProg) {
                                                            expandedChannelId = null
                                                            expandedProgram = null
                                                        } else {
                                                            expandedChannelId = channel.id
                                                            expandedProgram = prog
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    containerColor = when {
                                                        isSelectedProg -> AuraColors.Violet.copy(alpha = 0.28f)
                                                        isLive -> AuraColors.BgSurfaceElevated
                                                        else -> AuraColors.BgSurface
                                                    },
                                                    borderColor = when {
                                                        isSelectedProg -> AuraColors.Violet
                                                        isLive -> AuraColors.Emerald.copy(alpha = 0.55f)
                                                        else -> AuraColors.BorderSubtle
                                                    },
                                                    modifier = Modifier.weight(weight).fillMaxHeight()
                                                ) {
                                                    Column(
                                                        verticalArrangement = Arrangement.Center,
                                                        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = prog.title,
                                                            color = AuraColors.TextPrimary,
                                                            fontSize = 12.sp,
                                                            fontWeight = if (isLive) FontWeight.Bold else FontWeight.Medium,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Spacer(Modifier.height(2.dp))
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            if (isLive) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(6.dp)
                                                                        .clip(CircleShape)
                                                                        .background(AuraColors.Emerald)
                                                                )
                                                                Spacer(Modifier.width(5.dp))
                                                            }
                                                            Text(
                                                                text = prog.category,
                                                                color = if (isLive) AuraColors.Emerald else AuraColors.TextMuted,
                                                                fontSize = 10.sp,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Live Time Marker vertical indicator
                                    if (nowMs in windowStartMs..windowEndMs) {
                                        val fraction = (nowMs - windowStartMs).toFloat() / (windowEndMs - windowStartMs).toFloat()
                                        Row(modifier = Modifier.fillMaxSize()) {
                                            if (fraction > 0.005f) {
                                                Spacer(Modifier.weight(fraction))
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .fillMaxHeight()
                                                    .background(AuraColors.Emerald)
                                            )
                                            val rem = (1f - fraction).coerceAtLeast(0.001f)
                                            Spacer(Modifier.weight(rem))
                                        }
                                    }
                                }
                            }

                            // === INLINE EXPANDABLE PROGRAM DETAIL DRAWER ===
                            val progDetail = expandedProgram
                            if (expandedChannelId == channel.id && progDetail != null) {
                                InlineProgramDetailDrawer(
                                    channel = channel,
                                    program = progDetail,
                                    nowMs = nowMs,
                                    timeFmt = timeFmt,
                                    onPlay = { onPlayChannel(channel, true) },
                                    onRestart = { onRestartLive(channel) },
                                    onClose = {
                                        expandedChannelId = null
                                        expandedProgram = null
                                    }
                                )
                            }

                            HorizontalDivider(color = AuraColors.BorderSubtle.copy(alpha = 0.45f))
                        }
                    }
                }
            }
        }

        // Floating "Ir a la hora actual" pill when scrolled away from live time
        if (!isAtLiveWindow) {
            FocusableSurface(
                onClick = {
                    onSelectDayOffset(0)
                    windowStartMs = (System.currentTimeMillis() / SLOT_MS) * SLOT_MS
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = AuraColors.Emerald,
                focusedContainerColor = AuraColors.EmeraldDark,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Rounded.Schedule, null, tint = Color(0xFF041A12), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ir a la hora actual", color = Color(0xFF041A12), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InlineProgramDetailDrawer(
    channel: Channel,
    program: EpgProgram,
    nowMs: Long,
    timeFmt: SimpleDateFormat,
    onPlay: () -> Unit,
    onRestart: () -> Unit,
    onClose: () -> Unit
) {
    val state = program.stateAt(nowMs)
    val durationLabel = if (program.durationMinutes >= 60) {
        val h = program.durationMinutes / 60
        val m = program.durationMinutes % 60
        if (m == 0) "${h} h" else "${h} h ${m} min"
    } else {
        "${program.durationMinutes} min"
    }

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .background(AuraColors.BgSurface)
            .border(1.dp, AuraColors.Violet.copy(alpha = 0.6f))
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        // Poster / Thumbnail
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 136.dp, height = 82.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AuraColors.BgSurfaceElevated)
        ) {
            val img = program.iconUrl ?: channel.logoUrl
            if (!img.isNullOrBlank()) {
                AsyncImage(
                    model = img,
                    contentDescription = program.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ChannelLogoBox(channel = channel, size = 48.dp)
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = program.title,
                    color = AuraColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!program.rating.isNullOrBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = program.rating,
                        color = AuraColors.Emerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(AuraColors.Emerald.copy(alpha = 0.16f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = "${timeFmt.format(Date(program.startMs))} - ${timeFmt.format(Date(program.endMs))}  |  $durationLabel  ·  ${program.category}  ·  ${channel.sourceName}",
                color = AuraColors.TextSecondary,
                fontSize = 12.sp
            )

            if (program.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = program.description,
                    color = AuraColors.TextMuted,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state == ProgramState.LIVE) {
                    FocusableSurface(
                        onClick = onPlay,
                        shape = RoundedCornerShape(20.dp),
                        containerColor = AuraColors.Emerald,
                        focusedContainerColor = AuraColors.EmeraldDark
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFF041A12), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reproducir", color = Color(0xFF041A12), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    FocusableSurface(
                        onClick = onRestart,
                        shape = RoundedCornerShape(20.dp),
                        containerColor = AuraColors.BgSurfaceElevated,
                        borderColor = AuraColors.BorderSubtle
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.Replay, null, tint = AuraColors.TextPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reiniciar", color = AuraColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    FocusableSurface(
                        onClick = onPlay,
                        shape = RoundedCornerShape(20.dp),
                        containerColor = AuraColors.BgSurfaceElevated,
                        borderColor = AuraColors.Emerald
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = AuraColors.Emerald, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ir al canal en vivo", color = AuraColors.TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        FocusableSurface(
            onClick = onClose,
            shape = CircleShape,
            containerColor = AuraColors.BgSurfaceElevated
        ) {
            Box(modifier = Modifier.padding(6.dp)) {
                Icon(Icons.Rounded.Close, "Cerrar detalle", tint = AuraColors.TextPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}
