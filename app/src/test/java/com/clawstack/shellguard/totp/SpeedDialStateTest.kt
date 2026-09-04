package com.clawstack.shellguard.totp

import androidx.compose.runtime.MonotonicFrameClock
import com.clawstack.shellguard.totp.ui.components.SpeedDialState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 10 / Task 19 — SpeedDialState expand/collapse controller tests.
 *
 * Supplies an immediate [MonotonicFrameClock] so the Animatable-driven spring
 * transitions complete deterministically on the plain JVM, allowing both the
 * logical expand/collapse flags and the 0..1 progress fraction to be asserted.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SpeedDialStateTest {

    /** Frame clock that advances 16ms per frame — springs converge deterministically. */
    private class ImmediateFrameClock : MonotonicFrameClock, kotlin.coroutines.CoroutineContext.Element {
        private val frameCounter = java.util.concurrent.atomic.AtomicLong(0L)
        override val key: kotlin.coroutines.CoroutineContext.Key<*> get() = MonotonicFrameClock
        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R =
            onFrame(frameCounter.addAndGet(16_000_000L))
    }

    private val immediateFrameClock: kotlin.coroutines.CoroutineContext.Element = ImmediateFrameClock()

    @Test
    fun initialStateIsCollapsed() {
        val state = SpeedDialState()
        assertFalse(state.isExpanded)
        assertEquals(0f, state.fraction, 0.001f)
    }

    @Test
    fun expandSetsExpandedAndProgressToOne() = runTest(UnconfinedTestDispatcher()) {
        val state = SpeedDialState()
        withContext(immediateFrameClock) { state.expand() }
        assertTrue(state.isExpanded)
        assertEquals(1f, state.fraction, 0.001f)
    }

    @Test
    fun collapseAfterExpandReturnsToCollapsed() = runTest(UnconfinedTestDispatcher()) {
        val state = SpeedDialState()
        withContext(immediateFrameClock) { state.expand() }
        withContext(immediateFrameClock) { state.collapse() }
        assertFalse(state.isExpanded)
        assertEquals(0f, state.fraction, 0.001f)
    }

    @Test
    fun dismissOnOutsideTouchCollapsesExpandedDial() = runTest(UnconfinedTestDispatcher()) {
        val state = SpeedDialState()
        withContext(immediateFrameClock) { state.expand() }
        assertTrue(state.isExpanded)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler) + immediateFrameClock)
        state.dismissOnOutsideTouch(scope)
        assertFalse(state.isExpanded)
    }

    @Test
    fun dismissOnOutsideTouchIsNoOpWhenCollapsed() {
        val state = SpeedDialState()
        // No-op when collapsed: must not throw nor change state.
        state.dismissOnOutsideTouch(CoroutineScope(UnconfinedTestDispatcher()))
        assertFalse(state.isExpanded)
        assertEquals(0f, state.fraction, 0.001f)
    }
}
