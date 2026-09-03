package com.clawstack.shellguard.totp.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Task 19 — Expandable FAB Interaction Controller.
 *
 * Coordinates the speed dial's animated expand/collapse transitions. UI-layer
 * concerns are wired by [ExpandableSpeedDialFab]: back-handler interception,
 * outside-touch scrim dismissals, and camera permission requests (the latter
 * are delegated to the CameraX scanner screen, which owns its own permission
 * flow on navigation).
 */
@Stable
class SpeedDialState {

    /** Whether the dial is logically expanded (pills visible / scrim up). */
    var isExpanded: Boolean by mutableStateOf(false)
        private set

    /** 0f (collapsed) → 1f (expanded); drives scrim alpha, icon rotation, pill entrances. */
    val progress = Animatable(0f)

    /** Instantaneous 0..1 fraction usable from composable read contexts. */
    val fraction: Float
        get() = progress.value

    /** Expand with a spring-driven transition (no UI jank — single Animatable). */
    suspend fun expand() {
        isExpanded = true
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    /** Collapse with a spring-driven transition. */
    suspend fun collapse() {
        isExpanded = false
        progress.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    /** Convenience toggle launched in the provided UI scope. */
    fun toggle(scope: CoroutineScope) {
        scope.launch { if (isExpanded) collapse() else expand() }
    }

    /** Scrim / outside-touch dismissal — no-op when already collapsed. */
    fun dismissOnOutsideTouch(scope: CoroutineScope) {
        if (isExpanded) toggle(scope)
    }
}

/** Remembers a [SpeedDialState] scoped to this composition. */
@Composable
fun rememberSpeedDialState(): SpeedDialState = remember { SpeedDialState() }
