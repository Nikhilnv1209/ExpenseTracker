package com.expensetracker.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

private val GlassSurface = Color(0xFF1A1825)
private val GlassSurfaceLight = Color(0xFF252236)
private val GlassHighlight = Color.White
private val GlassBorder = Color.White

val LiquidGlassSheetColor = Color(0xFF1A1825)

@Composable
fun Modifier.liquidGlassSheetHighlight(): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "sheetShimmer")
    val shimmerProgress = infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sheetShimmerOffset",
    )
    val shimmerVal = shimmerProgress.value

    return this
        .background(
            Brush.verticalGradient(
                listOf(
                    GlassHighlight.copy(alpha = 0.08f),
                    Color.Transparent,
                ),
                startY = 0f,
                endY = 250f,
            )
        )
        .drawBehind {
            drawRect(
                brush = Brush.linearGradient(
                    listOf(
                        Color.Transparent,
                        GlassHighlight.copy(alpha = 0.03f),
                        Color.Transparent,
                    ),
                    start = Offset(shimmerVal * size.width * 0.5f, 0f),
                    end = Offset(shimmerVal * size.width * 0.5f + size.width * 0.3f, size.height),
                ),
            )
        }
}

@Composable
fun LiquidGlassTopHighlight() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        GlassHighlight.copy(alpha = 0.3f),
                        Color.Transparent,
                    )
                )
            )
    )
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    tint: Color = Color.Transparent,
    tintAlpha: Float = 0.0f,
    shimmer: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerProgress = infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(GlassSurface, GlassSurfaceLight, GlassSurface),
                    start = Offset(0f, 0f),
                    end = Offset(600f, 1200f),
                )
            )
            .then(
                if (tintAlpha > 0f && tint != Color.Transparent) {
                    Modifier.background(tint.copy(alpha = tintAlpha))
                } else Modifier
            )
            .then(
                if (shimmer) {
                    Modifier.background(
                        Brush.linearGradient(
                            listOf(
                                Color.Transparent,
                                GlassHighlight.copy(alpha = 0.06f),
                                Color.Transparent,
                            ),
                            start = Offset(shimmerProgress.value * 600f, 0f),
                            end = Offset(shimmerProgress.value * 600f + 300f, 600f),
                        )
                    )
                } else Modifier
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        GlassBorder.copy(alpha = 0.22f),
                        GlassBorder.copy(alpha = 0.04f),
                    )
                ),
                shape = shape,
            ),
        content = content,
    )
}
