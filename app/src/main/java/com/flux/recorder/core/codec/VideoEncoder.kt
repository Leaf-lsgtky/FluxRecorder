package com.flux.recorder.core.codec

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer

/**
 * Hardware-accelerated video encoder using MediaCodec
 * Supports H.264/AVC and H.265/HEVC with optional HDR
 */
class VideoEncoder(
    private val width: Int,
    private val height: Int,
    private val bitrate: Int,
    private val frameRate: Int,
    private val mimeType: String = MediaFormat.MIMETYPE_VIDEO_AVC,
    private val hdr: Boolean = false
) {
    private var mediaCodec: MediaCodec? = null
    var inputSurface: Surface? = null
        private set

    companion object {
        private const val TAG = "VideoEncoder"
        private const val I_FRAME_INTERVAL = 2
        private const val TIMEOUT_US = 10000L
    }

    fun prepare(): Surface? {
        try {
            val format = MediaFormat.createVideoFormat(mimeType, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
                setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
                setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000L / frameRate)

                // HDR support for H.265
                if (hdr && mimeType == MediaFormat.MIMETYPE_VIDEO_HEVC) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        setInteger(MediaFormat.KEY_COLOR_STANDARD, MediaFormat.COLOR_STANDARD_BT2020)
                        setInteger(MediaFormat.KEY_COLOR_TRANSFER, MediaFormat.COLOR_TRANSFER_HLG)
                        setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_FULL)
                        setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10)
                        setFeatureEnabled(MediaCodecInfo.CodecCapabilities.FEATURE_HdrEditing, true)
                        Log.d(TAG, "HDR (HLG) enabled for HEVC encoder")
                    } else {
                        setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10)
                        Log.d(TAG, "HDR requested but API < 33, using Main10 profile only")
                    }
                }
            }

            mediaCodec = MediaCodec.createEncoderByType(mimeType).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                inputSurface = createInputSurface()
                start()
            }

            Log.d(TAG, "Video encoder initialized: ${width}x${height} @ ${frameRate}fps, ${bitrate}bps, codec=$mimeType, hdr=$hdr")
            return inputSurface

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize video encoder", e)
            release()
            return null
        }
    }

    sealed interface EncoderOutput {
        data class Data(val buffer: ByteBuffer, val info: MediaCodec.BufferInfo, val index: Int) : EncoderOutput
        object FormatChanged : EncoderOutput
        object TryAgain : EncoderOutput
    }

    fun getEncodedData(): EncoderOutput {
        val codec = mediaCodec ?: return EncoderOutput.TryAgain

        val bufferInfo = MediaCodec.BufferInfo()
        val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)

        return when {
            outputBufferIndex >= 0 -> {
                val outputBuffer = codec.getOutputBuffer(outputBufferIndex)
                if (outputBuffer != null) {
                    EncoderOutput.Data(outputBuffer, bufferInfo, outputBufferIndex)
                } else {
                    EncoderOutput.TryAgain
                }
            }
            outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                Log.d(TAG, "Output format changed: ${codec.outputFormat}")
                EncoderOutput.FormatChanged
            }
            outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                EncoderOutput.TryAgain
            }
            else -> EncoderOutput.TryAgain
        }
    }

    fun releaseOutputBuffer(index: Int) {
        mediaCodec?.releaseOutputBuffer(index, false)
    }

    fun signalEndOfStream() {
        try {
            mediaCodec?.signalEndOfInputStream()
        } catch (e: Exception) {
            Log.e(TAG, "Error signaling end of stream", e)
        }
    }

    fun getOutputFormat(): MediaFormat? {
        return mediaCodec?.outputFormat
    }

    fun release() {
        try {
            inputSurface?.release()
            inputSurface = null

            mediaCodec?.stop()
            mediaCodec?.release()
            mediaCodec = null

            Log.d(TAG, "Video encoder released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing video encoder", e)
        }
    }

    fun isActive(): Boolean {
        return mediaCodec != null
    }
}
