package com.clawstack.shellguard.totp.engine

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration.Companion.seconds

/**
 * Snapshot state of the global TOTP ticker.
 */
data class TotpTickerState(
    val timestampMillis: Long,
    val remainingSeconds: Int,
    val progress: Float
)

/**
 * Global reactive ticker emitting once every second, synchronized with system epoch time via Kotlin Time.
 */
object TotpTicker {
    /**
     * Emits continuous 1-second ticks synchronized to timeStepSeconds boundaries.
     */
    fun observeTicker(periodSeconds: Long = TotpEngine.DEFAULT_TIME_STEP_SECONDS): Flow<TotpTickerState> = flow {
        while (true) {
            val now = System.currentTimeMillis()
            val remaining = TotpEngine.getRemainingSeconds(now, periodSeconds)
            val progress = TotpEngine.getProgressRatio(now, periodSeconds)

            emit(TotpTickerState(now, remaining, progress))
            delay(1.seconds)
        }
    }
}
