package com.expensetracker.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expensetracker.app.ui.theme.AppTheme
import com.expensetracker.app.ui.theme.AppType
import com.expensetracker.app.ui.theme.Inter
import com.expensetracker.app.ui.theme.IvoryWhite
import com.expensetracker.app.ui.theme.SageSoft

/**
 * Warm ambient backdrop. Two very low-alpha blooms give the obsidian canvas
 * depth so the glass panels have something to sit on.
 */
@Composable
fun WarmBackdrop(modifier: Modifier = Modifier) {
    val c = AppTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.canvas)
            .drawWithCache {
                val topBloom = Brush.radialGradient(
                    colors = listOf(c.gold.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.18f, size.height * 0.02f),
                    radius = size.width * 0.95f,
                )
                val bottomBloom = Brush.radialGradient(
                    colors = listOf(SageSoft.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(size.width * 0.92f, size.height * 0.72f),
                    radius = size.width * 1.05f,
                )
                val vignette = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                    startY = size.height * 0.55f,
                    endY = size.height,
                )
                onDrawBehind {
                    drawRect(topBloom)
                    drawRect(bottomBloom)
                    drawRect(vignette)
                }
            },
    )
}

/**
 * The core glass panel: translucent warm fill, 1px champagne hairline that is
 * brighter top-left, an inner top highlight, and a soft drop shadow.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    tint: Color = Color.Transparent,
    tintAlpha: Float = 0f,
    fillAlpha: Float = 1f,
    borderAlpha: Float = 1f,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.985f else 1f,
        animationSpec = tween(120),
        label = "glassPress",
    )

    Box(
        modifier = modifier
            .scale(scale)
            .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape, clip = false) else Modifier)
            .clip(shape)
            .background(c.card.copy(alpha = 0.70f * fillAlpha))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        c.gold.copy(alpha = 0.05f * fillAlpha),
                        Color.Transparent,
                        c.secondary.copy(alpha = 0.03f * fillAlpha),
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                )
            )
            .then(
                if (tintAlpha > 0f && tint != Color.Transparent) {
                    Modifier.background(tint.copy(alpha = tintAlpha))
                } else Modifier
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                    )
                } else Modifier
            ),
        content = content,
    )
}

/** 40dp circular icon holder used in settings rows and detail sheets. */
@Composable
fun IconOrb(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = AppTheme.colors.gold,
    wash: Color = tint.copy(alpha = 0.10f),
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(wash)
            .border(1.dp, tint.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.46f),
        )
    }
}

/** Glass circle button — header actions. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color = AppTheme.colors.gold,
    contentDescription: String? = null,
) {
    val c = AppTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(c.card.copy(alpha = 0.9f))
            .background(c.washGold)
            .border(1.dp, c.hairline, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.45f),
        )
    }
}

/** Primary champagne CTA. */
@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 52.dp,
) {
    val c = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(120),
        label = "goldPress",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    listOf(c.goldSoft, c.gold, c.goldDeep),
                    start = Offset.Zero,
                    end = Offset(900f, 200f),
                )
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color(0xFF2A1E10), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = TextStyle(
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                ),
                color = Color(0xFF2A1E10),
            )
        }
    }
}

/** Uppercase group label above a card. */
@Composable
fun GroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = AppType.groupLabel,
        color = AppTheme.colors.textFaint,
        modifier = modifier,
    )
}

/** Filter pill — "All", "Expenses", "HDFC". */
@Composable
fun GlassPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AppTheme.colors
    val bg by animateColorAsState(
        targetValue = if (selected) c.gold.copy(alpha = 0.14f) else c.card.copy(alpha = 0.75f),
        animationSpec = tween(180),
        label = "pillBg",
    )
    val border by animateColorAsState(
        targetValue = if (selected) c.gold.copy(alpha = 0.35f) else c.hairline,
        animationSpec = tween(180),
        label = "pillBorder",
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) c.gold else c.textSecondary,
        animationSpec = tween(180),
        label = "pillText",
    )
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, border, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text = text, style = AppType.groupLabel.copy(letterSpacing = 0.4.sp), color = textColor)
    }
}

/** True circle with the soft glow the design uses for status dots. */
@Composable
fun Dot(color: Color, size: Dp = 6.dp, modifier: Modifier = Modifier, glow: Boolean = true) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(size)) {
        val r = this.size.minDimension / 2f
        if (glow) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.55f), Color.Transparent),
                    center = center,
                    radius = r * 2.4f,
                ),
                radius = r * 2.4f,
                center = center,
            )
        }
        drawCircle(color = color, radius = r, center = center)
    }
}

/** Segmented control: pill container, highlighted active segment with a dot. */
@Composable
fun SegmentedTextTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = AppTheme.colors
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(c.canvas.copy(alpha = 0.55f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { index, label ->
            val active = index == selectedIndex
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (active) c.cardRaised else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(index) }
                    .padding(horizontal = if (active) 14.dp else 12.dp, vertical = 6.dp),
            ) {
                if (active) {
                    Dot(color = c.secondary, size = 5.dp, glow = false)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) c.textPrimary else c.textFaint,
                )
            }
        }
    }
}

/** 1px hairline. */
@Composable
fun Hairline(modifier: Modifier = Modifier, alpha: Float = 1f) {
    val c = AppTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(c.hairline.copy(alpha = c.hairline.alpha * alpha)),
    )
}
