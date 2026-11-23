package com.auratv.player.ui.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auratv.player.data.EpgSource
import com.auratv.player.data.PlaylistSource
import com.auratv.player.ui.components.FocusableSurface
import com.auratv.player.ui.theme.AuraColors

@Composable
fun SourcesModal(
    playlists: List<PlaylistSource>,
    enabledPlaylistIds: Set<String>,
    epgSources: List<EpgSource>,
    enabledEpgIds: Set<String>,
    channelCounts: Map<String, Int>,
    isLoading: Boolean,
    onTogglePlaylist: (String) -> Unit,
    onToggleEpg: (String) -> Unit,
    onEnableRegionalPack: () -> Unit,
    onAddCustomPlaylist: (name: String, url: String) -> Unit,
    onRemoveCustomPlaylist: (String) -> Unit,
    onAddCustomEpg: (name: String, url: String) -> Unit,
    onRemoveCustomEpg: (String) -> Unit,
    onSyncAndClose: () -> Unit,
    onClose: () -> Unit
) {
    var activeTab by remember { mutableStateOf("PLAYLISTS") } // PLAYLISTS | EPG | CUSTOM
    var regionFilter by remember { mutableStateOf("Todas") }
    var customName by remember { mutableStateOf("") }
    var customUrl by remember { mutableStateOf("") }
    var customIsEpg by remember { mutableStateOf(false) }
    val syncFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(80L)
        runCatching { syncFocusRequester.requestFocus() }
    }

    val regions = remember(playlists) {
        listOf("Todas") + playlists.map { it.regionOrCategory }.distinct()
    }
    val visiblePlaylists = remember(playlists, regionFilter) {
        if (regionFilter == "Todas") playlists else playlists.filter { it.regionOrCategory == regionFilter }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AuraColors.BgSurface, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            // Header
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Public, null, tint = AuraColors.Emerald, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Fuentes Abiertas y Guías EPG",
                            color = AuraColors.TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${enabledPlaylistIds.size} listas M3U activas · ${enabledEpgIds.size} guías XMLTV activas",
                            color = AuraColors.TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FocusableSurface(
                        onClick = onSyncAndClose,
                        focusRequester = syncFocusRequester,
                        shape = RoundedCornerShape(20.dp),
                        containerColor = AuraColors.Emerald,
                        focusedContainerColor = AuraColors.EmeraldDark
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, null, tint = Color(0xFF041A12), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isLoading) "Sincronizando..." else "Aplicar y Sincronizar",
                                color = Color(0xFF041A12),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    FocusableSurface(
                        onClick = onClose,
                        shape = RoundedCornerShape(20.dp),
                        containerColor = AuraColors.BgSurfaceElevated
                    ) {
                        Box(modifier = Modifier.padding(9.dp)) {
                            Icon(Icons.Rounded.Close, "Cerrar", tint = AuraColors.TextPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Section Tabs
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SourceTabChip("Listas M3U (${playlists.size})", activeTab == "PLAYLISTS") { activeTab = "PLAYLISTS" }
                SourceTabChip("Guías EPG (${epgSources.size})", activeTab == "EPG") { activeTab = "EPG" }
                SourceTabChip("+ Agregar URL Propia", activeTab == "CUSTOM") { activeTab = "CUSTOM" }
                Spacer(Modifier.weight(1f))
                if (activeTab == "PLAYLISTS") {
                    FocusableSurface(
                        onClick = onEnableRegionalPack,
                        shape = RoundedCornerShape(18.dp),
                        containerColor = AuraColors.Violet.copy(alpha = 0.25f),
                        borderColor = AuraColors.Violet
                    ) {
                        Text(
                            text = "Activar Pack AR + LatAm + ES",
                            color = AuraColors.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = AuraColors.BorderSubtle)
            Spacer(Modifier.height(12.dp))

            when (activeTab) {
                "PLAYLISTS" -> {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 10.dp)
                    ) {
                        items(regions) { region ->
                            val selected = region == regionFilter
                            FocusableSurface(
                                onClick = { regionFilter = region },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = if (selected) AuraColors.Emerald else AuraColors.BgSurfaceElevated
                            ) {
                                Text(
                                    text = region,
                                    color = if (selected) Color(0xFF041A12) else AuraColors.TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(visiblePlaylists, key = { it.id }) { source ->
                            val enabled = source.id in enabledPlaylistIds
                            val loadedCount = channelCounts[source.id]
                            FocusableSurface(
                                onClick = { onTogglePlaylist(source.id) },
                                shape = RoundedCornerShape(10.dp),
                                containerColor = if (enabled) AuraColors.BgSurfaceElevated else AuraColors.BgObsidian,
                                borderColor = if (enabled) AuraColors.Emerald.copy(alpha = 0.6f) else AuraColors.BorderSubtle
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (enabled) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (enabled) AuraColors.Emerald else AuraColors.TextMuted,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = source.name,
                                            color = AuraColors.TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${source.provider} · ${source.regionOrCategory} · ${source.url}",
                                            color = AuraColors.TextMuted,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    val badge = when {
                                        loadedCount != null -> "$loadedCount ch"
                                        source.estimatedChannels > 0 -> "~${source.estimatedChannels} ch"
                                        else -> "Propia"
                                    }
                                    Text(
                                        text = badge,
                                        color = if (enabled) AuraColors.Emerald else AuraColors.TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (source.isCustom) {
                                        Spacer(Modifier.width(10.dp))
                                        FocusableSurface(
                                            onClick = { onRemoveCustomPlaylist(source.id) },
                                            shape = RoundedCornerShape(8.dp),
                                            containerColor = AuraColors.LiveRed.copy(alpha = 0.2f)
                                        ) {
                                            Box(modifier = Modifier.padding(6.dp)) {
                                                Icon(Icons.Rounded.DeleteOutline, "Eliminar", tint = AuraColors.LiveRed, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "EPG" -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(epgSources, key = { it.id }) { epg ->
                            val enabled = epg.id in enabledEpgIds
                            FocusableSurface(
                                onClick = { onToggleEpg(epg.id) },
                                shape = RoundedCornerShape(10.dp),
                                containerColor = if (enabled) AuraColors.BgSurfaceElevated else AuraColors.BgObsidian,
                                borderColor = if (enabled) AuraColors.Violet else AuraColors.BorderSubtle
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (enabled) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (enabled) AuraColors.Violet else AuraColors.TextMuted,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(epg.name, color = AuraColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                        Text("${epg.region} · ${epg.url}", color = AuraColors.TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    if (epg.isCustom) {
                                        FocusableSurface(
                                            onClick = { onRemoveCustomEpg(epg.id) },
                                            shape = RoundedCornerShape(8.dp),
                                            containerColor = AuraColors.LiveRed.copy(alpha = 0.2f)
                                        ) {
                                            Box(modifier = Modifier.padding(6.dp)) {
                                                Icon(Icons.Rounded.DeleteOutline, "Eliminar", tint = AuraColors.LiveRed, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Agregá tu propia lista M3U/M3U8 o guía XMLTV (.xml / .xml.gz) legal y autorizada:",
                            color = AuraColors.TextSecondary,
                            fontSize = 13.sp
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SourceTabChip("Lista M3U / M3U8", !customIsEpg) { customIsEpg = false }
                            SourceTabChip("Guía EPG XMLTV", customIsEpg) { customIsEpg = true }
                        }

                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            singleLine = true,
                            label = { Text("Nombre de la fuente") },
                            placeholder = { Text("Ej: Mis Canales Abiertos") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuraColors.Emerald,
                                unfocusedBorderColor = AuraColors.BorderSubtle,
                                focusedTextColor = AuraColors.TextPrimary,
                                unfocusedTextColor = AuraColors.TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = customUrl,
                            onValueChange = { customUrl = it },
                            singleLine = true,
                            label = { Text("URL HTTPS / HTTP") },
                            placeholder = { Text(if (customIsEpg) "https://.../epg.xml.gz" else "https://.../lista.m3u8") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuraColors.Emerald,
                                unfocusedBorderColor = AuraColors.BorderSubtle,
                                focusedTextColor = AuraColors.TextPrimary,
                                unfocusedTextColor = AuraColors.TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (customIsEpg) {
                                    onAddCustomEpg(customName, customUrl)
                                } else {
                                    onAddCustomPlaylist(customName, customUrl)
                                }
                                customName = ""
                                customUrl = ""
                                activeTab = if (customIsEpg) "EPG" else "PLAYLISTS"
                            },
                            enabled = customName.isNotBlank() && customUrl.trim().startsWith("http", ignoreCase = true),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AuraColors.Emerald,
                                contentColor = Color(0xFF041A12)
                            )
                        ) {
                            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Guardar fuente", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FocusableSurface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        containerColor = if (selected) AuraColors.Emerald else AuraColors.BgSurfaceElevated,
        focusedContainerColor = if (selected) AuraColors.EmeraldDark else AuraColors.BgSurfaceHover
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF041A12) else AuraColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}
