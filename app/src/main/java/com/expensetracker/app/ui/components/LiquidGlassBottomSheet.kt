package com.expensetracker.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isSheetOpen) Modifier.blur(blurRadius) else Modifier),
        ) {
            mainContent()
        }

        if (isSheetOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.12f))
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
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            ) + fadeOut(),
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
