package com.expensetracker.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class FabAction(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit,
)

@Composable
fun ExpandableFab(
    actions: List<FabAction>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "fabRotation",
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (expanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { expanded = false },
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.End,
        ) {
            actions.forEachIndexed { index, action ->
                AnimatedVisibility(
                    visible = expanded,
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    ) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { it / 2 },
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    ) + fadeOut(),
                ) {
                    Row(
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                action.onClick()
                                expanded = false
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShapeOnly(20, 12, 12, 20))
                                .background(Color(0xFF1A1825))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(
                                text = action.label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            action.color.copy(alpha = 0.25f),
                                            action.color.copy(alpha = 0.12f),
                                        )
                                    )
                                )
                                .border(1.dp, action.color.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.label,
                                tint = action.color,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF7C3AED).copy(alpha = 0.3f), Color(0xFF5B21B6).copy(alpha = 0.15f))
                        )
                    )
                    .border(1.dp, Color(0xFF7C3AED).copy(alpha = 0.4f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { expanded = !expanded },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add",
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(rotation),
                )
            }
        }
    }
}

private fun RoundedCornerShapeOnly(
    topLeft: Int,
    topRight: Int,
    bottomRight: Int,
    bottomLeft: Int,
) = androidx.compose.foundation.shape.RoundedCornerShape(
    topStart = topLeft.dp,
    topEnd = topRight.dp,
    bottomEnd = bottomRight.dp,
    bottomStart = bottomLeft.dp,
)
