package com.auratv.player.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.auratv.player.ui.theme.AuraColors

/**
 * Contenedor interactivo unificado para celulares táctiles y Android TV (D-Pad / control remoto).
 * Usa un único nodo focusable integrado en clickable para evitar conflictos de foco en Android TV.
 */
@Composable
fun FocusableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    shape: Shape = RoundedCornerShape(10.dp),
    containerColor: Color = AuraColors.BgSurfaceElevated,
    focusedContainerColor: Color = AuraColors.BgSurfaceHover,
    borderColor: Color = Color.Transparent,
    focusedBorderColor: Color = AuraColors.Emerald,
    borderWidth: Dp = 1.dp,
    focusedBorderWidth: Dp = 2.dp,
    content: @Composable BoxScope.(isFocused: Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val border = when {
        isFocused && enabled -> BorderStroke(focusedBorderWidth, focusedBorderColor)
        borderColor != Color.Transparent -> BorderStroke(borderWidth, borderColor)
        else -> null
    }

    val focusMod = if (focusRequester != null) {
        modifier.focusRequester(focusRequester)
    } else {
        modifier
    }

    Surface(
        shape = shape,
        color = if (isFocused && enabled) focusedContainerColor else containerColor,
        border = border,
        modifier = focusMod.clickable(
            enabled = enabled,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    ) {
        Box {
            content(isFocused)
        }
    }
}
