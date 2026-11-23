package com.auratv.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AvatarPreset(
    val icon: ImageVector,
    val startColor: Color,
    val endColor: Color,
    val label: String
)

object AvatarCatalog {
    val presets: List<AvatarPreset> = listOf(
        AvatarPreset(Icons.Rounded.LiveTv, Color(0xFF00E5A0), Color(0xFF007A5E), "Broadcast"),
        AvatarPreset(Icons.Rounded.Movie, Color(0xFF7C5CFF), Color(0xFF4226A8), "Cine"),
        AvatarPreset(Icons.Rounded.SportsSoccer, Color(0xFF00C2FF), Color(0xFF005B99), "Deportes"),
        AvatarPreset(Icons.Rounded.RocketLaunch, Color(0xFFFF6B6B), Color(0xFFA8243B), "Espacio"),
        AvatarPreset(Icons.Rounded.Headphones, Color(0xFFFFB84D), Color(0xFFCC6B00), "Música"),
        AvatarPreset(Icons.Rounded.SportsEsports, Color(0xFF00D4B4), Color(0xFF1F4E79), "Gaming"),
        AvatarPreset(Icons.Rounded.Pets, Color(0xFFFF7EB6), Color(0xFF9E2B6B), "Mascota"),
        AvatarPreset(Icons.Rounded.NightsStay, Color(0xFF5E7CE2), Color(0xFF1E2D6F), "Nocturno"),
        AvatarPreset(Icons.Rounded.AutoAwesome, Color(0xFFA855F7), Color(0xFFEC4899), "Aura"),
        AvatarPreset(Icons.Rounded.Explore, Color(0xFF2DD4BF), Color(0xFF0F766E), "Aventura"),
        AvatarPreset(Icons.Rounded.Bolt, Color(0xFFFACC15), Color(0xFFEA580C), "Energía"),
        AvatarPreset(Icons.Rounded.WbSunny, Color(0xFF38BDF8), Color(0xFF6366F1), "Solar")
    )

    fun get(index: Int): AvatarPreset {
        val safe = ((index % presets.size) + presets.size) % presets.size
        return presets[safe]
    }
}

@Composable
fun AvatarCircle(
    avatarIndex: Int,
    size: Dp = 88.dp,
    modifier: Modifier = Modifier
) {
    val preset = AvatarCatalog.get(avatarIndex)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(preset.startColor, preset.endColor)))
    ) {
        Icon(
            imageVector = preset.icon,
            contentDescription = preset.label,
            tint = Color.White,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}
