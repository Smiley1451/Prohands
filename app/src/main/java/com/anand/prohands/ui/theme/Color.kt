package com.anand.prohands.ui.theme

import androidx.compose.ui.graphics.Color

// --- Brand Colors (Yellow/Gold Primary) ---
val PrimaryYellow = Color(0xFFFFD700)      // Main Brand Color
val PrimaryOrangeGold = Color(0xFFFFB300)  // Accent / Darker Yellow
val DarkGold = Color(0xFFF57F17)           // Contrast
val LightYellowBg = Color(0xFFFFFDE7)      // Light Backgrounds

// --- Secondary / Neutral Colors ---
val BrandBlack = Color(0xFF1A1A1A)         // Primary Text
val BrandDarkGray = Color(0xFF424242)      // Secondary Text / Icons
val BrandGray = Color(0xFF9E9E9E)          // Disabled / Hints
val BrandLightGray = Color(0xFFEEEEEE)     // Borders / Dividers
val BrandWhite = Color(0xFFFFFFFF)         // Surfaces

// --- Semantic Colors ---
val SuccessGreen = Color(0xFF4CAF50)
val ErrorRed = Color(0xFFD32F2F)
val InfoBlue = Color(0xFF2196F3)

// --- Legacy / Other Colors (keeping for compatibility during refactor) ---
val RoyalBlue = Color(0xFF9A86EF)
val LightBlueAccent = Color(0xFFE8EAF6)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
val LightBlueBg = Color(0xFFE0F7FA)

// --- Professional Palette Object ---
object ProColors {
    val Primary = PrimaryYellow
    val PrimaryVariant = PrimaryOrangeGold
    val Secondary = BrandBlack
    val Background = BrandWhite
    val Surface = BrandWhite
    val SurfaceVariant = LightYellowBg
    val Error = ErrorRed
    val OnPrimary = BrandBlack // Black text on Yellow
    val OnSecondary = BrandWhite
    val OnBackground = BrandBlack
    val OnSurface = BrandBlack
    
    // Helper Text Colors
    val TextPrimary = BrandBlack
    val TextSecondary = BrandDarkGray
    val TextTertiary = BrandGray
    
    // Specific UI Colors
    val Divider = BrandLightGray
    val Success = SuccessGreen
    val Info = InfoBlue
}
