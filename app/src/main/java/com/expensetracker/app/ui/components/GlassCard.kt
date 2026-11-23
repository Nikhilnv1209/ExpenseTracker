package com.expensetracker.app.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Legacy entry point kept so existing screens keep compiling.
 * All visual weight now lives in [GlassSurface].
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    tint: Color = Color.White,
    tintAlpha: Float = 0.08f,
    borderAlpha: Float = 0.12f,
    elevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    GlassSurface(
        modifier = modifier,
        shape = shape,
        tint = tint,
        tintAlpha = tintAlpha,
        borderAlpha = borderAlpha,
        elevation = elevation,
        content = content,
    )
}
