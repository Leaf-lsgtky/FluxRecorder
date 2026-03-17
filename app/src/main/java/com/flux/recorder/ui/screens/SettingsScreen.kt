package com.flux.recorder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flux.recorder.R
import com.flux.recorder.data.AudioSource
import com.flux.recorder.data.FrameRate
import com.flux.recorder.data.RecordingSettings
import com.flux.recorder.data.VideoQuality
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.extra.SuperDropdown
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun SettingsScreen(
    settings: RecordingSettings,
    onSettingsChanged: (RecordingSettings) -> Unit,
    onNavigateBack: () -> Unit
) {
    var currentSettings by remember { mutableStateOf(settings) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.settings),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(MiuixIcons.Back, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .overScrollVertical()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp)
        ) {
            // Video Quality
            SmallTitle(text = stringResource(R.string.video_quality))
            Card(
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                val qualityItems = VideoQuality.entries.map { stringResource(it.labelResId) }
                SuperDropdown(
                    title = stringResource(R.string.video_quality),
                    items = qualityItems,
                    selectedIndex = VideoQuality.entries.indexOf(currentSettings.videoQuality),
                    onSelectedIndexChange = {
                        currentSettings = currentSettings.copy(videoQuality = VideoQuality.entries[it])
                        onSettingsChanged(currentSettings)
                    }
                )
            }

            // Frame Rate
            SmallTitle(text = stringResource(R.string.frame_rate))
            Card(
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                val fpsItems = FrameRate.entries.map { stringResource(it.labelResId) }
                SuperDropdown(
                    title = stringResource(R.string.frame_rate),
                    items = fpsItems,
                    selectedIndex = FrameRate.entries.indexOf(currentSettings.frameRate),
                    onSelectedIndexChange = {
                        currentSettings = currentSettings.copy(frameRate = FrameRate.entries[it])
                        onSettingsChanged(currentSettings)
                    }
                )
            }

            // Audio Source
            SmallTitle(text = stringResource(R.string.audio_source))
            Card(
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                val audioItems = AudioSource.entries.map { stringResource(it.labelResId) }
                SuperDropdown(
                    title = stringResource(R.string.audio_source),
                    items = audioItems,
                    selectedIndex = AudioSource.entries.indexOf(currentSettings.audioSource),
                    onSelectedIndexChange = {
                        currentSettings = currentSettings.copy(audioSource = AudioSource.entries[it])
                        onSettingsChanged(currentSettings)
                    }
                )
            }

            // Toggles
            SmallTitle(text = stringResource(R.string.section_camera))
            Card(
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                SuperSwitch(
                    title = stringResource(R.string.enable_facecam),
                    checked = currentSettings.enableFacecam,
                    onCheckedChange = {
                        currentSettings = currentSettings.copy(enableFacecam = it)
                        onSettingsChanged(currentSettings)
                    }
                )
            }

            SmallTitle(text = stringResource(R.string.section_gestures))
            Card(
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                SuperSwitch(
                    title = stringResource(R.string.shake_to_stop),
                    checked = currentSettings.enableShakeToStop,
                    onCheckedChange = {
                        currentSettings = currentSettings.copy(enableShakeToStop = it)
                        onSettingsChanged(currentSettings)
                    }
                )
            }
        }
    }
}
