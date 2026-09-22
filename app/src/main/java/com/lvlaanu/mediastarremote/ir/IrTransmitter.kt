package com.lvlaanu.mediastarremote.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.abs

/**
 * Describes what the device's IR hardware can actually do.
 */
data class IrCapability(
    val hasEmitter: Boolean,
    /** Carrier ranges reported by the HAL, as (minHz, maxHz) pairs. */
    val carrierRanges: List<IntRange>,
    /** Set when the HAL refused to report its ranges; transmission may still work. */
    val rangesUnknown: Boolean,
) {
    fun supports(carrierHz: Int): Boolean =
        rangesUnknown || carrierRanges.any { carrierHz in it }

    companion object {
        val NONE = IrCapability(hasEmitter = false, carrierRanges = emptyList(), rangesUnknown = false)
    }
}

/**
 * Serialised access to [ConsumerIrManager].
 *
 * The framework's `transmit` call is blocking and is documented as unsafe to
 * call from several threads at once, so every burst goes through a single
 * dedicated thread fed by a conflated channel. A conflated channel is the right
 * shape here: if the user mashes buttons faster than the hardware can send,
 * dropping the intermediate presses is far better than queueing a backlog that
 * plays out seconds later.
 */
class IrTransmitter(context: Context) {

    private val appContext = context.applicationContext

    private val irManager: ConsumerIrManager? =
        appContext.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "ir-transmit").apply { isDaemon = true }
    }
    private val scope = CoroutineScope(SupervisorJob() + executor.asCoroutineDispatcher())

    private val queue = Channel<IrBurst>(
        capacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    val capability: IrCapability = probeCapability()

    init {
        scope.launch {
            for (burst in queue) {
                transmitBlocking(burst)
            }
        }
    }

    /**
     * Queues a burst. Returns false when there is no usable emitter, so the UI
     * can tell the user instead of silently doing nothing.
     */
    fun send(burst: IrBurst): Boolean {
        if (!capability.hasEmitter) {
            _lastError.value = "This device has no IR emitter"
            return false
        }
        queue.trySend(burst)
        return true
    }

    fun send(signal: IrSignal): Boolean = send(signal.toBurst())

    fun clearError() {
        _lastError.value = null
    }

    fun shutdown() {
        queue.close()
        executor.shutdown()
    }

    /**
     * Runs on the dedicated IR thread. Any HAL exception is captured rather
     * than thrown, because a vendor IR service that dies should not take the
     * app with it.
     */
    private fun transmitBlocking(burst: IrBurst) {
        val manager = irManager ?: return
        val carrier = adjustCarrier(burst.carrierHz)
        try {
            manager.transmit(carrier, burst.patternUs)
        } catch (e: RuntimeException) {
            // Seen on some MIUI builds when the IR service is restarting.
            Log.e(TAG, "IR transmit failed", e)
            _lastError.value = e.message ?: "IR transmission failed"
        }
    }

    /**
     * Clamps the requested carrier into a range the hardware admits to
     * supporting.
     *
     * Xiaomi and POCO devices typically report a single wide range such as
     * 30000-60000 Hz, which covers 38 kHz comfortably. Some report nothing at
     * all, in which case the request is passed through untouched: the HAL
     * usually still works, and refusing to transmit would be worse than trying.
     */
    private fun adjustCarrier(requestedHz: Int): Int {
        val cap = capability
        if (cap.rangesUnknown || cap.carrierRanges.isEmpty()) return requestedHz
        if (cap.supports(requestedHz)) return requestedHz

        val nearest = cap.carrierRanges
            .map { range -> requestedHz.coerceIn(range.first, range.last) }
            .minByOrNull { abs(it - requestedHz) }
            ?: requestedHz
        Log.w(TAG, "Carrier $requestedHz Hz unsupported, falling back to $nearest Hz")
        return nearest
    }

    private fun probeCapability(): IrCapability {
        val manager = irManager ?: return IrCapability.NONE
        if (!manager.hasIrEmitter()) return IrCapability.NONE

        return try {
            val ranges = manager.carrierFrequencies
                ?.map { it.minFrequency..it.maxFrequency }
                .orEmpty()
            IrCapability(
                hasEmitter = true,
                carrierRanges = ranges,
                rangesUnknown = ranges.isEmpty(),
            )
        } catch (e: RuntimeException) {
            Log.w(TAG, "Could not read carrier frequencies", e)
            IrCapability(hasEmitter = true, carrierRanges = emptyList(), rangesUnknown = true)
        }
    }

    private companion object {
        const val TAG = "IrTransmitter"
    }
}
