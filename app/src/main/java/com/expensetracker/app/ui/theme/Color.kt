package com.expensetracker.app.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * "Smoked Olive & Champagne"
 * Warm obsidian canvas, champagne accent, olive / terracotta semantics.
 * Token names follow the design system exported from Stitch so the values
 * stay traceable back to the reference.
 */

// ── Obsidian base ────────────────────────────────────────────────────────
val Obsidian = Color(0xFF0D0B0A)          // canvas
val ObsidianSurface = Color(0xFF120F0D)   // surface
val SmokedGlass = Color(0xFF1A1613)       // raised card
val Espresso = Color(0xFF24201D)          // higher elevation
val EspressoDeep = Color(0xFF2E2824)      // pressed / inset
val EspressoTaupe = Color(0xFF787066)     // outline

// ── Champagne / gold ─────────────────────────────────────────────────────
val GoldChampagne = Color(0xFFE2C799)
val GoldLuxe = Color(0xFFD4AF37)
val GoldSoft = Color(0xFFF3E5AB)
val BronzeGlow = Color(0xFF9A7B4F)

// ── Text ─────────────────────────────────────────────────────────────────
val IvoryWhite = Color(0xFFFAF7F2)
val SandGray = Color(0xFFC5BCB1)
val SandMuted = Color(0xFFA89F91)

// ── Semantic ─────────────────────────────────────────────────────────────
val SageSoft = Color(0xFF7CA682)
val SageBright = Color(0xFF6EE7B7)
val TerracottaSoft = Color(0xFFD08075)
val TerracottaDeep = Color(0xFFB4655B)

// ── Hairlines & washes ───────────────────────────────────────────────────
val HairlineWarm = Color(0x1FE2C799)      // rgba(226,199,153,0.12)
val HairlineWarmStrong = Color(0x33E2C799) // 20%
val WashGold = Color(0x14E2C799)          // 8% champagne fill
val WashSage = Color(0x147CA682)
val WashTerracotta = Color(0x14D08075)

// ── Category palette (muted, same warm family) ───────────────────────────
val CategoryGold = Color(0xFFE2C799)
val CategorySage = Color(0xFF8FA882)
val CategoryTerracotta = Color(0xFFD08075)
val CategoryBronze = Color(0xFFC08A5A)
val CategorySand = Color(0xFFC5BCB1)
val CategoryMoss = Color(0xFF7CA682)
val CategoryClay = Color(0xFFC98E6B)
val CategoryWheat = Color(0xFFD9C9A8)

val CategoryColors = listOf(
    CategoryGold,
    CategorySage,
    CategoryTerracotta,
    CategoryBronze,
    CategorySand,
    CategoryMoss,
    CategoryClay,
    CategoryWheat,
)
