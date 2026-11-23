package com.auratv.player.ui.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auratv.player.data.UserProfile
import com.auratv.player.ui.components.AvatarCatalog
import com.auratv.player.ui.components.AvatarCircle
import com.auratv.player.ui.components.FocusableSurface
import com.auratv.player.ui.theme.AuraColors

private sealed class ProfileMode {
    data object Select : ProfileMode()
    data object EditGrid : ProfileMode()
    data object Create : ProfileMode()
    data class EditItem(val profile: UserProfile) : ProfileMode()
}

@Composable
fun ProfilesScreen(
    profiles: List<UserProfile>,
    activeProfile: UserProfile?,
    onSelectProfile: (UserProfile) -> Unit,
    onCreateProfile: (name: String, avatarIndex: Int) -> Unit,
    onUpdateProfile: (profileId: String, name: String, avatarIndex: Int) -> Unit,
    onDeleteProfile: (profileId: String) -> Unit,
    onExitApp: () -> Unit
) {
    var mode by remember { mutableStateOf<ProfileMode>(ProfileMode.Select) }

    when (val current = mode) {
        ProfileMode.Select, ProfileMode.EditGrid -> {
            ProfileGridView(
                profiles = profiles,
                isEditMode = current is ProfileMode.EditGrid,
                onProfileClick = { profile ->
                    if (current is ProfileMode.EditGrid) {
                        mode = ProfileMode.EditItem(profile)
                    } else {
                        onSelectProfile(profile)
                    }
                },
                onAddClick = { mode = ProfileMode.Create },
                onToggleEditMode = {
                    mode = if (current is ProfileMode.EditGrid) ProfileMode.Select else ProfileMode.EditGrid
                },
                onExitApp = onExitApp
            )
        }

        ProfileMode.Create -> {
            ProfileEditorView(
                title = "Creá un nuevo perfil",
                subtitle = "Elegí un avatar y nombre de usuario",
                initialName = "",
                initialAvatarIndex = profiles.size % AvatarCatalog.presets.size,
                activeProfile = activeProfile,
                confirmLabel = "Crear",
                canDelete = false,
                onCancel = { mode = ProfileMode.Select },
                onConfirm = { name, avatar ->
                    onCreateProfile(name, avatar)
                    mode = ProfileMode.Select
                },
                onDelete = {}
            )
        }

        is ProfileMode.EditItem -> {
            ProfileEditorView(
                title = "Editá el perfil",
                subtitle = "Cambiá el avatar y nombre de usuario",
                initialName = current.profile.name,
                initialAvatarIndex = current.profile.avatarIndex,
                activeProfile = activeProfile,
                confirmLabel = "Guardar",
                canDelete = profiles.size > 1,
                onCancel = { mode = ProfileMode.EditGrid },
                onConfirm = { name, avatar ->
                    onUpdateProfile(current.profile.id, name, avatar)
                    mode = ProfileMode.Select
                },
                onDelete = {
                    onDeleteProfile(current.profile.id)
                    mode = ProfileMode.Select
                }
            )
        }
    }
}

@Composable
private fun ProfileGridView(
    profiles: List<UserProfile>,
    isEditMode: Boolean,
    onProfileClick: (UserProfile) -> Unit,
    onAddClick: () -> Unit,
    onToggleEditMode: () -> Unit,
    onExitApp: () -> Unit
) {
    val firstProfileFocus = remember { FocusRequester() }

    LaunchedEffect(profiles.size) {
        if (profiles.isNotEmpty()) {
            kotlinx.coroutines.delay(80L)
            runCatching { firstProfileFocus.requestFocus() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraColors.BgObsidian)
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        // Brand Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Icon(
                imageVector = Icons.Rounded.LiveTv,
                contentDescription = null,
                tint = AuraColors.Emerald,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "AuraTV",
                color = AuraColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }

        // Center Profile Selector
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                text = if (isEditMode) "Editá tus perfiles" else "Elegí un perfil",
                color = AuraColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(32.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                items(profiles, key = { it.id }) { profile ->
                    val isFirst = profiles.firstOrNull()?.id == profile.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(116.dp)
                    ) {
                        FocusableSurface(
                            onClick = { onProfileClick(profile) },
                            focusRequester = if (isFirst) firstProfileFocus else null,
                            shape = CircleShape,
                            containerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            focusedBorderColor = AuraColors.Emerald,
                            focusedBorderWidth = 3.dp
                        ) {
                            Box(modifier = Modifier.padding(4.dp)) {
                                AvatarCircle(
                                    avatarIndex = profile.avatarIndex,
                                    size = 96.dp
                                )
                                if (isEditMode) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(AuraColors.BgSurfaceElevated)
                                            .border(1.5.dp, AuraColors.Emerald, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = "Editar",
                                            tint = AuraColors.TextPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = profile.name,
                            color = AuraColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // + Agregar perfil
                item(key = "add_profile") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(116.dp)
                    ) {
                        FocusableSurface(
                            onClick = onAddClick,
                            shape = CircleShape,
                            containerColor = AuraColors.BgSurfaceElevated,
                            focusedContainerColor = AuraColors.BgSurfaceHover,
                            borderColor = AuraColors.BorderSubtle,
                            focusedBorderColor = AuraColors.Emerald,
                            focusedBorderWidth = 3.dp
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(104.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Agregar perfil",
                                    tint = AuraColors.TextPrimary,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Agregar perfil",
                            color = AuraColors.TextSecondary,
                            fontSize = 14.sp,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Bottom Action + Exit + Version
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FocusableSurface(
                    onClick = onToggleEditMode,
                    shape = RoundedCornerShape(24.dp),
                    containerColor = if (isEditMode) AuraColors.Emerald else AuraColors.BgSurfaceElevated,
                    focusedContainerColor = if (isEditMode) AuraColors.EmeraldDark else AuraColors.BgSurfaceHover,
                    borderColor = if (isEditMode) AuraColors.Emerald else AuraColors.BorderSubtle
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 11.dp)
                    ) {
                        if (!isEditMode) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                tint = AuraColors.TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isEditMode) "Cancelar" else "EDITAR PERFILES",
                            color = if (isEditMode) Color(0xFF041A12) else AuraColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.7.sp
                        )
                    }
                }

                FocusableSurface(
                    onClick = onExitApp,
                    shape = RoundedCornerShape(24.dp),
                    containerColor = AuraColors.LiveRed.copy(alpha = 0.16f),
                    focusedContainerColor = AuraColors.LiveRed.copy(alpha = 0.30f),
                    borderColor = AuraColors.LiveRed.copy(alpha = 0.55f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PowerSettingsNew,
                            contentDescription = "Salir de AuraTV",
                            tint = AuraColors.LiveRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "SALIR",
                            color = AuraColors.LiveRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.7.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "AuraTV · TV Abierta Legal · v1.0.0",
                color = AuraColors.TextMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ProfileEditorView(
    title: String,
    subtitle: String,
    initialName: String,
    initialAvatarIndex: Int,
    activeProfile: UserProfile?,
    confirmLabel: String,
    canDelete: Boolean,
    onCancel: () -> Unit,
    onConfirm: (name: String, avatarIndex: Int) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var avatarIndex by remember { mutableIntStateOf(initialAvatarIndex) }
    val total = AvatarCatalog.presets.size

    fun prevAvatar() {
        avatarIndex = (avatarIndex - 1 + total) % total
    }

    fun nextAvatar() {
        avatarIndex = (avatarIndex + 1) % total
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraColors.BgObsidian)
            .padding(24.dp)
    ) {
        // Header: Brand left, active profile right
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LiveTv, null, tint = AuraColors.Emerald, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("AuraTV", color = AuraColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            if (activeProfile != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(activeProfile.name, color = AuraColors.TextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    AvatarCircle(activeProfile.avatarIndex, size = 34.dp)
                }
            }
        }

        // Center Editor Card
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 460.dp)
        ) {
            Text(title, color = AuraColors.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = AuraColors.TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(28.dp))

            // Horizontal Avatar Carousel
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FocusableSurface(
                    onClick = { prevAvatar() },
                    shape = CircleShape,
                    containerColor = AuraColors.BgSurfaceElevated
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Rounded.ChevronLeft, "Anterior", tint = AuraColors.TextPrimary)
                    }
                }

                FocusableSurface(
                    onClick = { prevAvatar() },
                    shape = CircleShape,
                    containerColor = Color.Transparent
                ) {
                    AvatarCircle(
                        avatarIndex = (avatarIndex - 1 + total) % total,
                        size = 64.dp,
                        modifier = Modifier.alpha(0.45f)
                    )
                }

                AvatarCircle(
                    avatarIndex = avatarIndex,
                    size = 104.dp,
                    modifier = Modifier.border(3.dp, AuraColors.Emerald, CircleShape)
                )

                FocusableSurface(
                    onClick = { nextAvatar() },
                    shape = CircleShape,
                    containerColor = Color.Transparent
                ) {
                    AvatarCircle(
                        avatarIndex = (avatarIndex + 1) % total,
                        size = 64.dp,
                        modifier = Modifier.alpha(0.45f)
                    )
                }

                FocusableSurface(
                    onClick = { nextAvatar() },
                    shape = CircleShape,
                    containerColor = AuraColors.BgSurfaceElevated
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Rounded.ChevronRight, "Siguiente", tint = AuraColors.TextPrimary)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 24) name = it },
                singleLine = true,
                placeholder = { Text("Nombre de usuario", color = AuraColors.TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AuraColors.Emerald,
                    unfocusedBorderColor = AuraColors.BorderSubtle,
                    focusedTextColor = AuraColors.TextPrimary,
                    unfocusedTextColor = AuraColors.TextPrimary,
                    cursorColor = AuraColors.Emerald
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Cancelar", color = AuraColors.TextPrimary, modifier = Modifier.padding(horizontal = 12.dp))
                }

                Button(
                    onClick = { onConfirm(name, avatarIndex) },
                    enabled = name.trim().isNotEmpty(),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuraColors.Emerald,
                        contentColor = Color(0xFF041A12),
                        disabledContainerColor = AuraColors.BgSurfaceElevated,
                        disabledContentColor = AuraColors.TextMuted
                    )
                ) {
                    Text(confirmLabel, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }

        if (canDelete) {
            FocusableSurface(
                onClick = onDelete,
                shape = RoundedCornerShape(20.dp),
                containerColor = AuraColors.BgSurfaceElevated,
                borderColor = AuraColors.LiveRed.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Rounded.DeleteOutline, null, tint = AuraColors.LiveRed, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ELIMINAR PERFIL", color = AuraColors.LiveRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
