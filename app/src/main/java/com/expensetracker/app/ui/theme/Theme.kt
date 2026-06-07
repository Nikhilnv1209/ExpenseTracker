package com.expensetracker.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColorScheme = darkColorScheme(
    primary = Violet500,
    onPrimary = Color.White,
    secondary = Violet400,
    background = DarkBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
)

@Composable
fun ExpenseTrackerTheme(content: @Composable () -> Unit) {
    androidx.compose.material3.MaterialTheme(
        colorScheme = ColorScheme,
        content = content
    )
}
