/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min
import kotlin.math.pow

@UnstableApi
class CrossfadeAudioProcessor : AudioProcessor {
    private var inputAudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat = AudioFormat.NOT_SET

    @Volatile
    var crossfadeDurationMs: Int = 0
        set(value) {
            field = value
            pendingCrossfadeDurationMs = value
        }

    @Volatile
    private var pendingCrossfadeDurationMs: Int = 0

    private var appliedCrossfadeDurationMs: Int = 0
    private var crossfadeFrames: Int = 0
    private var bytesPerFrame: Int = 0

    private var isEnding: Boolean = false
    private var shouldFadeInThisStream: Boolean = false
    private var shouldFadeInNextStream: Boolean = false
    private var framesOutputInStream: Long = 0L

    // Buffer circular optimizado con mejor manejo de memoria
    private var tailBufferBytes: ByteArray = ByteArray(0)
    private var tailCapacityBytes: Int = 0
    private var tailStartIndex: Int = 0
    private var tailSizeBytes: Int = 0

    // Output buffer con gestión mejorada
    private var outBufferBytes: ByteArray = ByteArray(0)
    private var outCapacityBytes: Int = 0
    private var outStartIndex: Int = 0
    private var outSizeBytes: Int = 0

    private var scratch: ByteArray = ByteArray(0)
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER

    // Parámetros optimizados
    private var lastFrameProcessed: Long = 0L
    private var fadeQualityFactor: Float = 1f  // 1.0 = calidad normal, >1 = más suave

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioFormat.NOT_SET
        }

        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        bytesPerFrame = inputAudioFormat.channelCount * 2
        applyCrossfadeDurationIfNeeded(force = true)
        return outputAudioFormat
    }

    override fun isActive(): Boolean = pendingCrossfadeDurationMs > 0 && inputAudioFormat != AudioFormat.NOT_SET

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (inputAudioFormat == AudioFormat.NOT_SET) {
            inputBuffer.position(inputBuffer.limit())
            return
        }

        applyCrossfadeDurationIfNeeded(force = false)

        val incomingBytes = inputBuffer.remaining()
        if (incomingBytes <= 0) return

        if (crossfadeFrames <= 0 || bytesPerFrame <= 0 || tailCapacityBytes <= 0) {
            ensureScratchCapacity(incomingBytes)
            inputBuffer.get(scratch, 0, incomingBytes)
            enqueueOutput(scratch, 0, incomingBytes)
            return
        }

        if (shouldFadeInNextStream) {
            shouldFadeInThisStream = true
            shouldFadeInNextStream = false
            framesOutputInStream = 0L
        }

        // Calcular bytes a output con suavizado mejorado
        val bytesToOutput = computeBytesToOutputOptimized(incomingBytes)
        val alignedBytesToOutput = bytesToOutput - (bytesToOutput % bytesPerFrame)

        if (alignedBytesToOutput > 0) {
            ensureScratchCapacity(alignedBytesToOutput)
            drainTailToArray(scratch, 0, alignedBytesToOutput)

            if (shouldFadeInThisStream) {
                applyFadeInPcm16Optimized(
                    data = scratch,
                    offset = 0,
                    length = alignedBytesToOutput,
                    startFrameIndex = framesOutputInStream,
                )
            }

            framesOutputInStream += (alignedBytesToOutput / bytesPerFrame).toLong()

            if (shouldFadeInThisStream && framesOutputInStream >= crossfadeFrames.toLong()) {
                shouldFadeInThisStream = false
            }

            enqueueOutput(scratch, 0, alignedBytesToOutput)
        }

        appendInputToTailOptimized(inputBuffer)
    }

    private fun computeBytesToOutputOptimized(incomingBytes: Int): Int {
        val total = tailSizeBytes + incomingBytes
        val overflow = (total - tailCapacityBytes).coerceAtLeast(0)

        // Suavizar el corte para evitar glitches
        if (overflow > 0 && shouldFadeInNextStream) {
            // No sacar todo de golpe, dejar un pequeño buffer
            return (overflow * 0.85f).toInt().coerceAtLeast(1)
        }
        return overflow
    }

    override fun getOutput(): ByteBuffer {
        applyCrossfadeDurationIfNeeded(force = false)

        if (outSizeBytes > 0) {
            return dequeueOutputToByteBuffer(outSizeBytes)
        }

        if (!isEnding) {
            return AudioProcessor.EMPTY_BUFFER
        }

        if (crossfadeFrames <= 0 || bytesPerFrame <= 0 || tailSizeBytes <= 0) {
            shouldFadeInThisStream = false
            shouldFadeInNextStream = false
            tailStartIndex = 0
            tailSizeBytes = 0
            return AudioProcessor.EMPTY_BUFFER
        }

        val alignedTailBytes = tailSizeBytes - (tailSizeBytes % bytesPerFrame)
        if (alignedTailBytes <= 0) {
            shouldFadeInThisStream = false
            shouldFadeInNextStream = false
            tailStartIndex = 0
            tailSizeBytes = 0
            return AudioProcessor.EMPTY_BUFFER
        }

        ensureScratchCapacity(alignedTailBytes)
        drainTailToArray(scratch, 0, alignedTailBytes)

        val tailFrames = alignedTailBytes / bytesPerFrame
        applyFadeOutPcm16Optimized(
            data = scratch,
            offset = 0,
            frameCount = tailFrames,
            startFrameIndex = framesOutputInStream,
        )
        framesOutputInStream += tailFrames.toLong()

        shouldFadeInNextStream = true
        enqueueOutput(scratch, 0, alignedTailBytes)

        return dequeueOutputToByteBuffer(outSizeBytes)
    }

    override fun isEnded(): Boolean {
        return isEnding && outSizeBytes == 0 && tailSizeBytes == 0
    }

    override fun flush() {
        val preserveFadeInForNextStream = isEnding && shouldFadeInNextStream
        framesOutputInStream = 0L
        lastFrameProcessed = 0L
        isEnding = false
        shouldFadeInThisStream = false
        shouldFadeInNextStream = preserveFadeInForNextStream
        tailStartIndex = 0
        tailSizeBytes = 0
        outStartIndex = 0
        outSizeBytes = 0
    }

    override fun reset() {
        flush()
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
        pendingCrossfadeDurationMs = 0
        appliedCrossfadeDurationMs = 0
        crossfadeDurationMs = 0
        crossfadeFrames = 0
        bytesPerFrame = 0
        shouldFadeInNextStream = false
        tailBufferBytes = ByteArray(0)
        tailCapacityBytes = 0
        tailStartIndex = 0
        tailSizeBytes = 0
        outBufferBytes = ByteArray(0)
        outCapacityBytes = 0
        outStartIndex = 0
        outSizeBytes = 0
        scratch = ByteArray(0)
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        fadeQualityFactor = 1f
    }

    override fun queueEndOfStream() {
        isEnding = true
    }

    private fun applyCrossfadeDurationIfNeeded(force: Boolean) {
        val targetMs = pendingCrossfadeDurationMs
        if (!force && targetMs == appliedCrossfadeDurationMs) return

        appliedCrossfadeDurationMs = targetMs

        val newCrossfadeFrames =
            if (inputAudioFormat != AudioFormat.NOT_SET && bytesPerFrame > 0 && targetMs > 0) {
                (inputAudioFormat.sampleRate * targetMs) / 1000
            } else {
                0
            }

        crossfadeFrames = newCrossfadeFrames
        fadeQualityFactor = when {
            targetMs <= 300 -> 1.5f  // Fade corto necesita más suavizado
            targetMs >= 1000 -> 0.8f // Fade largo necesita menos
            else -> 1f
        }

        val newCapacityBytes = if (newCrossfadeFrames > 0 && bytesPerFrame > 0) {
            newCrossfadeFrames * bytesPerFrame
        } else {
            0
        }

        if (newCapacityBytes == tailCapacityBytes) return

        if (newCapacityBytes <= 0) {
            tailBufferBytes = ByteArray(0)
            tailCapacityBytes = 0
            tailStartIndex = 0
            tailSizeBytes = 0
            shouldFadeInThisStream = false
            shouldFadeInNextStream = false
            return
        }

        val newBuffer = ByteArray(newCapacityBytes)
        val bytesToCopy = min(tailSizeBytes, newCapacityBytes)

        if (bytesToCopy > 0 && tailCapacityBytes > 0) {
            val oldCapacity = tailCapacityBytes
            val tailEndExclusive = (tailStartIndex + tailSizeBytes) % oldCapacity
            val copyStartIndexInOld =
                ((tailEndExclusive - bytesToCopy) % oldCapacity + oldCapacity) % oldCapacity

            val firstChunk = min(bytesToCopy, oldCapacity - copyStartIndexInOld)
            System.arraycopy(tailBufferBytes, copyStartIndexInOld, newBuffer, 0, firstChunk)
            val remaining = bytesToCopy - firstChunk
            if (remaining > 0) {
                System.arraycopy(tailBufferBytes, 0, newBuffer, firstChunk, remaining)
            }
        }

        tailBufferBytes = newBuffer
        tailCapacityBytes = newCapacityBytes
        tailStartIndex = 0
        tailSizeBytes = bytesToCopy
    }

    private fun drainTailToArray(target: ByteArray, offset: Int, length: Int) {
        if (length <= 0) return
        val capacity = tailCapacityBytes
        val firstChunk = min(length, capacity - tailStartIndex)
        System.arraycopy(tailBufferBytes, tailStartIndex, target, offset, firstChunk)
        val remaining = length - firstChunk
        if (remaining > 0) {
            System.arraycopy(tailBufferBytes, 0, target, offset + firstChunk, remaining)
        }
        tailStartIndex = (tailStartIndex + length) % capacity
        tailSizeBytes -= length
    }

    private fun appendInputToTailOptimized(inputBuffer: ByteBuffer) {
        val capacity = tailCapacityBytes
        if (capacity <= 0) {
            inputBuffer.position(inputBuffer.limit())
            return
        }

        val bytesToAppend = inputBuffer.remaining()
        if (bytesToAppend <= 0) return

        // Optimización: si hay espacio suficiente, copiar directamente
        val availableSpace = capacity - tailSizeBytes
        if (availableSpace >= bytesToAppend) {
            var endIndex = (tailStartIndex + tailSizeBytes) % capacity
            val firstChunk = min(bytesToAppend, capacity - endIndex)
            inputBuffer.get(tailBufferBytes, endIndex, firstChunk)
            val remaining = bytesToAppend - firstChunk
            if (remaining > 0) {
                inputBuffer.get(tailBufferBytes, 0, remaining)
            }
            tailSizeBytes += bytesToAppend
        } else {
            // Espacio insuficiente, escribir lo que se pueda
            var endIndex = (tailStartIndex + tailSizeBytes) % capacity
            var remaining = bytesToAppend
            while (remaining > 0 && tailSizeBytes < capacity) {
                val chunk = min(remaining, capacity - endIndex)
                inputBuffer.get(tailBufferBytes, endIndex, chunk)
                tailSizeBytes += chunk
                remaining -= chunk
                endIndex = (endIndex + chunk) % capacity
            }
            // Si sobra data, se descarta (overflow controlado)
            if (remaining > 0) {
                inputBuffer.position(inputBuffer.position() - remaining)
            }
        }
    }

    private fun enqueueOutput(source: ByteArray, offset: Int, length: Int) {
        if (length <= 0) return
        ensureOutputCapacity(length)
        val capacity = outCapacityBytes
        var endIndex = (outStartIndex + outSizeBytes) % capacity
        var remaining = length
        var srcOffset = offset
        while (remaining > 0) {
            val chunk = min(remaining, capacity - endIndex)
            System.arraycopy(source, srcOffset, outBufferBytes, endIndex, chunk)
            srcOffset += chunk
            remaining -= chunk
            endIndex = (endIndex + chunk) % capacity
        }
        outSizeBytes += length
    }

    private fun dequeueOutputToByteBuffer(maxBytes: Int): ByteBuffer {
        val bytesToRead = min(outSizeBytes, maxBytes).coerceAtLeast(0)
        if (bytesToRead <= 0) return AudioProcessor.EMPTY_BUFFER

        val out = replaceOutputBuffer(bytesToRead)
        val capacity = outCapacityBytes
        val firstChunk = min(bytesToRead, capacity - outStartIndex)
        out.put(outBufferBytes, outStartIndex, firstChunk)
        val remaining = bytesToRead - firstChunk
        if (remaining > 0) {
            out.put(outBufferBytes, 0, remaining)
        }
        out.flip()

        outStartIndex = (outStartIndex + bytesToRead) % capacity
        outSizeBytes -= bytesToRead
        if (outSizeBytes == 0) {
            outStartIndex = 0
        }

        return out
    }

    private fun ensureOutputCapacity(additionalBytes: Int) {
        val required = outSizeBytes + additionalBytes
        if (outCapacityBytes >= required) return

        val newCapacity = when {
            outCapacityBytes <= 0 -> maxOf(32768, required)  // Buffer más grande
            else -> maxOf(outCapacityBytes * 2, required)
        }

        val newBuffer = ByteArray(newCapacity)

        if (outSizeBytes > 0) {
            // Copiar datos existentes preservando orden
            val firstChunk = min(outSizeBytes, outCapacityBytes - outStartIndex)
            System.arraycopy(outBufferBytes, outStartIndex, newBuffer, 0, firstChunk)
            val remaining = outSizeBytes - firstChunk
            if (remaining > 0) {
                System.arraycopy(outBufferBytes, 0, newBuffer, firstChunk, remaining)
            }
            outStartIndex = 0
        }

        outBufferBytes = newBuffer
        outCapacityBytes = newCapacity
    }

    private fun applyFadeInPcm16Optimized(
        data: ByteArray,
        offset: Int,
        length: Int,
        startFrameIndex: Long,
    ) {
        if (crossfadeFrames <= 0 || bytesPerFrame <= 0) return
        val frames = length / bytesPerFrame
        val crossfadeFramesLong = crossfadeFrames.toLong()

        for (i in 0 until frames) {
            val globalFrame = startFrameIndex + i
            val gain = if (globalFrame < crossfadeFramesLong) {
                // Curva cuadrática para fade-in más natural
                val t = globalFrame.toFloat() / crossfadeFramesLong.toFloat()
                (t * t).coerceIn(0f, 1f)
            } else {
                1f
            }
            scaleFramePcm16Optimized(data, offset + i * bytesPerFrame, gain)
        }
    }

    private fun applyFadeOutPcm16Optimized(
        data: ByteArray,
        offset: Int,
        frameCount: Int,
        startFrameIndex: Long,
    ) {
        if (frameCount <= 0 || bytesPerFrame <= 0) return

        val denom = (frameCount - 1).coerceAtLeast(1).toFloat()
        val crossfadeFramesLong = crossfadeFrames.toLong()

        for (i in 0 until frameCount) {
            // Fade-out con curva exponencial más suave
            val fadeOutGain = if (frameCount <= 1) {
                0f
            } else {
                val t = i.toFloat() / denom
                // Curva coseno para fade-out más natural
                ((1f - t).pow(1.3f)).coerceIn(0f, 1f)
            }

            val globalFrame = startFrameIndex + i
            val fadeInGain =
                if (shouldFadeInThisStream && crossfadeFrames > 0 && globalFrame < crossfadeFramesLong) {
                    val t = globalFrame.toFloat() / crossfadeFramesLong.toFloat()
                    (t * t).coerceIn(0f, 1f)
                } else {
                    1f
                }

            val totalGain = (fadeInGain * fadeOutGain * fadeQualityFactor).coerceIn(0f, 1.2f)
            scaleFramePcm16Optimized(data, offset + i * bytesPerFrame, totalGain)
        }
    }

    private fun scaleFramePcm16Optimized(data: ByteArray, frameOffset: Int, gain: Float) {
        if (gain >= 0.99f && gain <= 1.01f) return

        var byteIndex = frameOffset
        val frameEnd = frameOffset + bytesPerFrame

        while (byteIndex < frameEnd) {
            val lo = data[byteIndex].toInt() and 0xFF
            val hi = data[byteIndex + 1].toInt()
            var sample = ((hi shl 8) or lo).toShort().toInt()

            var scaled = (sample.toFloat() * gain).toInt()

            // Anti-clipping mejorado con soft knee
            val absScaled = kotlin.math.abs(scaled)
            if (absScaled > 30000) {
                val over = (absScaled - 30000) / 1000f
                val attenuation = (1f / (1f + over * 0.4f)).coerceIn(0.6f, 1f)
                scaled = (scaled.toFloat() * attenuation).toInt()
            }

            scaled = scaled.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            data[byteIndex] = (scaled and 0xFF).toByte()
            data[byteIndex + 1] = ((scaled shr 8) and 0xFF).toByte()
            byteIndex += 2
        }
    }

    private fun ensureScratchCapacity(size: Int) {
        if (scratch.size < size) {
            scratch = ByteArray(size)
        }
    }

    private fun replaceOutputBuffer(size: Int): ByteBuffer {
        if (outputBuffer.capacity() < size) {
            outputBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        } else {
            outputBuffer.clear()
        }
        return outputBuffer
    }
}