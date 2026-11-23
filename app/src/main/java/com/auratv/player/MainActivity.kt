package com.auratv.player

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.auratv.player.data.AppScreen
import com.auratv.player.ui.livetv.LiveTvScreen
import com.auratv.player.ui.profiles.ProfilesScreen
import com.auratv.player.ui.theme.AuraTvTheme
import com.auratv.player.viewmodel.AuraTvViewModel

class MainActivity : AppCompatActivity() {

    private val viewModel: AuraTvViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveFullscreen()

        setContent {
            AuraTvTheme {
                val uiState by viewModel.uiState.collectAsState()

                BackHandler(enabled = uiState.currentScreen == AppScreen.LIVE_TV) {
                    when {
                        uiState.showExitConfirmDialog -> viewModel.setShowExitConfirmDialog(false)
                        uiState.showSourcesModal -> viewModel.setShowSourcesModal(false)
                        uiState.showQuickGuide -> viewModel.setShowQuickGuide(false)
                        uiState.showEpgOverlay -> viewModel.setShowExitConfirmDialog(true)
                        uiState.arePlayerControlsVisible -> viewModel.setPlayerControlsVisible(false)
                        else -> viewModel.setShowEpgOverlay(true)
                    }
                }

                when (uiState.currentScreen) {
                    AppScreen.PROFILES -> {
                        ProfilesScreen(
                            profiles = uiState.profiles,
                            activeProfile = uiState.activeProfile,
                            onSelectProfile = { viewModel.selectProfile(it) },
                            onCreateProfile = { name, avatar -> viewModel.addProfile(name, avatar) },
                            onUpdateProfile = { id, name, avatar -> viewModel.updateProfile(id, name, avatar) },
                            onDeleteProfile = { id -> viewModel.deleteProfile(id) },
                            onExitApp = { exitApplication() }
                        )
                    }

                    AppScreen.LIVE_TV -> {
                        LiveTvScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onExitApp = { exitApplication() }
                        )
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val state = viewModel.uiState.value
        if (state.currentScreen == AppScreen.LIVE_TV && !state.showSourcesModal && !state.showExitConfirmDialog) {
            val isFullscreenVideo = !state.showEpgOverlay && !state.showQuickGuide

            if (isFullscreenVideo) {
                when (keyCode) {
                    KeyEvent.KEYCODE_CHANNEL_UP -> {
                        viewModel.stepChannel(1)
                        return true
                    }
                    KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                        viewModel.stepChannel(-1)
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (!state.arePlayerControlsVisible) {
                            viewModel.stepChannel(1)
                            return true
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (!state.arePlayerControlsVisible) {
                            viewModel.stepChannel(-1)
                            return true
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (!state.arePlayerControlsVisible) {
                            viewModel.setShowQuickGuide(true)
                            return true
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        if (!state.arePlayerControlsVisible) {
                            viewModel.setPlayerControlsVisible(true)
                            return true
                        }
                    }
                }
            }

            if (keyCode == KeyEvent.KEYCODE_GUIDE || keyCode == KeyEvent.KEYCODE_MENU) {
                viewModel.setShowEpgOverlay(!state.showEpgOverlay)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun exitApplication() {
        viewModel.stopPlayback()
        finishAndRemoveTask()
    }

    private fun enableImmersiveFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}
