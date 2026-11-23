package com.expensetracker.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/**
 * 0 → 1 open progress for a bottom sheet: slides up and fades in on open,
 * slides back down on close. Callers keep the sheet composed while the
 * progress animates back to zero.
 */
@Composable
fun rememberSheetProgress(visible: Boolean): Float {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            progress.animateTo(1f, tween(durationMillis = 340, easing = FastOutSlowInEasing))
        } else {
            progress.animateTo(0f, tween(durationMillis = 240, easing = FastOutLinearInEasing))
        }
    }
    return progress.value
}
