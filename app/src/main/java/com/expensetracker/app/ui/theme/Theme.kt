package com.expensetracker.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AppDarkColorScheme = darkColorScheme(
    primary = GoldChampagne,
    onPrimary = Color(0xFF2A1E10),
    primaryContainer = EspressoDeep,
    onPrimaryContainer = GoldSoft,

    secondary = SageSoft,
    onSecondary = Color(0xFF10190F),
    secondaryContainer = Color(0xFF262D24),
    onSecondaryContainer = SageBright,

    tertiary = TerracottaSoft,
    onTertiary = Color(0xFF2A1310),
    tertiaryContainer = Color(0xFF2E1A16),
    onTertiaryContainer = Color(0xFFF0C3BB),

    background = Obsidian,
    onBackground = IvoryWhite,

    surface = Obsidian,
    onSurface = IvoryWhite,
    surfaceVariant = Espresso,
    onSurfaceVariant = SandMuted,

    surfaceContainerLowest = Obsidian,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = SmokedGlass,
    surfaceContainerHigh = Espresso,
    surfaceContainerHighest = EspressoDeep,

    outline = EspressoTaupe,
    outlineVariant = HairlineWarmStrong,

    error = TerracottaSoft,
    onError = Color(0xFF2A1310),
    errorContainer = Color(0xFF3A1F1A),
    onErrorContainer = Color(0xFFF5CFC8),

    inverseSurface = IvoryWhite,
    inverseOnSurface = Obsidian,
    inversePrimary = BronzeGlow,

    scrim = Color(0xFF000000),
)

/** Tokens that Material 3 has no slot for. */
data class AppColors(
    val primary: Color = GoldChampagne,
    val secondary: Color = SageSoft,
    val tertiary: Color = TerracottaSoft,
    val income: Color = SageSoft,
    val incomeBright: Color = SageBright,
    val expense: Color = TerracottaSoft,
    val expenseDeep: Color = TerracottaDeep,
    val gold: Color = GoldChampagne,
    val goldDeep: Color = GoldLuxe,
    val goldSoft: Color = GoldSoft,
    val bronze: Color = BronzeGlow,
    val canvas: Color = Obsidian,
    val surface: Color = ObsidianSurface,
    val card: Color = SmokedGlass,
    val cardRaised: Color = Espresso,
    val textPrimary: Color = IvoryWhite,
    val textSecondary: Color = SandGray,
    val textMuted: Color = SandMuted,
    val textFaint: Color = EspressoTaupe,
    val hairline: Color = HairlineWarm,
    val hairlineStrong: Color = HairlineWarmStrong,
    val washGold: Color = WashGold,
    val washSage: Color = WashSage,
    val washTerracotta: Color = WashTerracotta,
    val category: List<Color> = CategoryColors,
)

val LocalAppColors = staticCompositionLocalOf { AppColors() }

/** Shortcut: `AppTheme.colors.income`. */
object AppTheme {
    val colors: AppColors
        @Composable get() = LocalAppColors.current
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun ExpenseTrackerTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // The app is dark-only by design: the obsidian canvas is the product's identity.
    CompositionLocalProvider(LocalAppColors provides AppColors()) {
        MaterialTheme(
            colorScheme = AppDarkColorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content,
        )
    }
}
