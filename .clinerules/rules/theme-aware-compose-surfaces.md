# Rule: Theme-Aware Compose Surfaces

Derived from the Phase 11.5 light-mode polish (v0.0.2.1 Build 13→14): three shipped
defects, one root cause — hardcoded appearance that ignored the app's theme state.

## Hard Requirements

1. **No hardcoded `Color.White` / `Color.Black`** for text, icons, scrims, or
   overlays. Always resolve via `MaterialTheme.colorScheme` or
   `LocalShellGuardColors`. Hardcoded color = broken in the opposite theme mode.

2. **System-bar icon appearance follows the APP's theme, not the system's.**
   `isAppearanceLightStatusBars` must be derived from the app's *effective*
   dark/light state and re-asserted when it changes:
   ```kotlin
   DisposableEffect(effectiveDark) {
       WindowCompat.getInsetsController(window, window.decorView)
           .isAppearanceLightStatusBars = !effectiveDark
       onDispose { }
   }
   ```
   (See `MainActivity.kt` — the fix for unreadable white-on-white status bar in light mode.)

3. **Decorative containers behind dynamic content (scrims, sheets, speed dials)
   must be full-screen** — fill the entire layout, not the slot of the composable
   that spawned them. (Fix: speed-dial scrim produced white edge gaps in light mode.)

4. **Modifier order matters for drawn borders:**
   ```kotlin
   // CORRECT: border drawn outside the clip
   .background(color, shape).border(1.dp, borderColor, shape).clip(shape)
   // WRONG: clip first shaves the border stroke in half
   .clip(shape).background(color, shape).border(1.dp, borderColor, shape)
   ```
   (Fix: Gateway back-button's flat-topped circle.)

## Verification
Every theme-facing change requires a **live sweep of BOTH modes on device**:
dashboard → settings hub → each sub-screen → FAB/speed-dial → lock screen,
plus the dark→light→dark revert of the status bar icons.
