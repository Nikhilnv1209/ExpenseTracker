package com.expensetracker.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

private val GlassTint = Color(0xFF1A1825).copy(alpha = 0.4f)
private val GlassHighlight = Color.White

@Composable
fun LiquidGlassLayout(
    isSheetOpen: Boolean,
    onDismiss: () -> Unit = {},
    blurRadius: Dp = 28.dp,
    mainContent: @Composable () -> Unit,
    sheetContent: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val animatedBlur by animateDpAsState(
            targetValue = if (isSheetOpen) blurRadius else 0.dp,
            animationSpec = tween(350),
            label = "blur",
        )
        val overlayAlpha by animateFloatAsState(
            targetValue = if (isSheetOpen) 0.12f else 0f,
            animationSpec = tween(350),
            label = "overlay",
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (animatedBlur > 0.dp) Modifier.blur(animatedBlur) else Modifier),
        ) {
            mainContent()
        }

        if (overlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = overlayAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    )
                    .zIndex(1f),
            )
        }

        AnimatedVisibility(
            visible = isSheetOpen,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(350),
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(350),
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(2f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(GlassTint)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                GlassHighlight.copy(alpha = 0.1f),
                                Color.Transparent,
                            ),
                            startY = 0f,
                            endY = 300f,
                        )
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    GlassHighlight.copy(alpha = 0.35f),
                                    Color.Transparent,
                                )
                            )
                        )
                )

                sheetContent()
            }
        }
    }
}
