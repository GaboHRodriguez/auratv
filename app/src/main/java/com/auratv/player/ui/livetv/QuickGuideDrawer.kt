package com.auratv.player.ui.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.auratv.player.data.Channel
import com.auratv.player.data.EpgProgram
import com.auratv.player.ui.components.FocusableSurface
import com.auratv.player.ui.theme.AuraColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun QuickGuideDrawer(
    channels: List<Channel>,
    activeChannel: Channel?,
    currentProgramFor: (Channel) -> EpgProgram?,
    onSelectChannel: (Channel) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val closeFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(80L)
        runCatching { closeFocusRequester.requestFocus() }
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(330.dp)
            .background(AuraColors.BgObsidian.copy(alpha = 0.94f))
            .padding(vertical = 14.dp, horizontal = 12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LiveTv, null, tint = AuraColors.Emerald, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Guía rápida (${channels.size})",
                    color = AuraColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            FocusableSurface(
                onClick = onClose,
                focusRequester = closeFocusRequester,
                shape = RoundedCornerShape(16.dp),
                containerColor = AuraColors.BgSurfaceElevated
            ) {
                Box(modifier = Modifier.padding(6.dp)) {
                    Icon(Icons.Rounded.Close, "Cerrar guía rápida", tint = AuraColors.TextPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = AuraColors.BorderSubtle)
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(channels, key = { it.id }) { channel ->
                val isActive = channel.id == activeChannel?.id
                val prog = remember(channel.id) { currentProgramFor(channel) }
                FocusableSurface(
                    onClick = { onSelectChannel(channel) },
                    shape = RoundedCornerShape(10.dp),
                    containerColor = if (isActive) AuraColors.BgSurfaceHover else AuraColors.BgSurface,
                    borderColor = if (isActive) AuraColors.Violet else AuraColors.BorderSubtle,
                    borderWidth = if (isActive) 2.dp else 1.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = channel.number.toString(),
                            color = if (isActive) AuraColors.Emerald else AuraColors.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(34.dp)
                        )
                        ChannelLogoBox(channel = channel, size = 36.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = channel.name,
                                color = AuraColors.TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val progTitle = prog?.title ?: channel.category
                            Text(
                                text = progTitle,
                                color = AuraColors.TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (prog != null) {
                                Text(
                                    text = "${timeFmt.format(Date(prog.startMs))} - ${timeFmt.format(Date(prog.endMs))}",
                                    color = AuraColors.TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelLogoBox(
    channel: Channel,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1C2533))
    ) {
        if (!channel.logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(size - 6.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
        } else {
            val initials = channel.name
                .split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercase() }
                .ifBlank { "TV" }
            Text(
                text = initials,
                color = AuraColors.Emerald,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
