package com.flux.recorder

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flux.recorder.data.AudioSource
import com.flux.recorder.data.RecordingState
import com.flux.recorder.service.RecorderService
import com.flux.recorder.ui.theme.FluxRecorderTheme
import com.flux.recorder.utils.PreferencesManager
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.extra.SuperSwitch

class RecordingShortcutActivity : ComponentActivity() {

    companion object {
        private const val TAG = "RecordingShortcut"
        private const val PREFS_NAME = "recording_shortcut_prefs"
        private const val KEY_SYSTEM_AUDIO = "system_audio"
        private const val KEY_MICROPHONE = "microphone"
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val prefsManager = PreferencesManager(this)
            val settings = prefsManager.getRecordingSettings().copy(
                audioSource = getAudioSource()
            )
            val intent = Intent(this, RecorderService::class.java).apply {
                action = RecorderService.ACTION_START_RECORDING
                putExtra(RecorderService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(RecorderService.EXTRA_RESULT_DATA, result.data)
                putExtra(RecorderService.EXTRA_SETTINGS, settings)
            }
            startService(intent)
        } else {
            Log.w(TAG, "MediaProjection permission denied")
        }
        finish()
    }

    private fun getAudioSource(): AudioSource {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val systemAudio = prefs.getBoolean(KEY_SYSTEM_AUDIO, true)
        val microphone = prefs.getBoolean(KEY_MICROPHONE, false)
        return when {
            systemAudio && microphone -> AudioSource.BOTH
            systemAudio -> AudioSource.INTERNAL
            microphone -> AudioSource.MICROPHONE
            else -> AudioSource.NONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (RecorderService.recordingState.value !is RecordingState.Idle) {
            finish()
            return
        }

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        setContent {
            FluxRecorderTheme {
                val showDialog = remember { mutableStateOf(true) }
                var systemAudio by remember { mutableStateOf(prefs.getBoolean(KEY_SYSTEM_AUDIO, true)) }
                var microphone by remember { mutableStateOf(prefs.getBoolean(KEY_MICROPHONE, false)) }

                SuperDialog(
                    title = getString(R.string.dialog_record_title),
                    summary = getString(R.string.dialog_record_summary),
                    show = showDialog,
                    onDismissRequest = {
                        showDialog.value = false
                    },
                    onDismissFinished = {
                        finish()
                    }
                ) {
                    Card {
                        SuperSwitch(
                            title = getString(R.string.dialog_record_system_audio),
                            checked = systemAudio,
                            onCheckedChange = {
                                systemAudio = it
                                prefs.edit().putBoolean(KEY_SYSTEM_AUDIO, it).apply()
                            }
                        )
                        SuperSwitch(
                            title = getString(R.string.dialog_record_microphone),
                            checked = microphone,
                            onCheckedChange = {
                                microphone = it
                                prefs.edit().putBoolean(KEY_MICROPHONE, it).apply()
                            }
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            text = getString(R.string.cancel),
                            onClick = {
                                showDialog.value = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        TextButton(
                            text = getString(R.string.dialog_record_start),
                            onClick = {
                                showDialog.value = false
                                val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                mediaProjectionLauncher.launch(manager.createScreenCaptureIntent())
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColorsPrimary()
                        )
                    }
                }
            }
        }
    }
}
