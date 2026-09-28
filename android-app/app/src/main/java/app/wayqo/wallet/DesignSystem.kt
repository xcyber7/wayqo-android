// SPDX-FileCopyrightText: 2026 Black Flag International Limited
// SPDX-License-Identifier: GPL-3.0-only
package app.wayqo.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Render-inspired visual tokens. The dashboard uses proprietary Neue Montreal/Roobert
// fonts, so Android's bundled modern sans family is used without a runtime font download.
internal val BrandGreen = Color(0xFF43B871)
internal val BrandGreenSoft = Color(0xFFE9F7ED)
internal val BrandGreenInk = Color(0xFF266B3D)
internal val BrandPurple = Color(0xFF7952D4)
internal val BrandLavender = Color(0xFFF2EEFC)
internal val BrandInk = Color(0xFF18151F)
internal val BrandGreyMuted = Color(0xFF696670)
internal val BrandBorder = Color(0xFFDEDCE5)
internal val BrandCanvas = Color(0xFFF7F7F8)

// Dark "focus" palette for the payment screens (Moreta-style): a pure-black ground
// with a hero amount that reads white when the wallet can cover it and red when short.
internal val PayBg = Color(0xFF000000)
internal val PaySurface = Color(0xFF1C1C1E)
internal val PayTextMuted = Color(0xFF9A9AA0)
internal val PayRed = Color(0xFFFF5A5F)
internal val PayBlue = Color(0xFF2F6BFF)
internal val PayDivider = Color(0xFF2A2A2E)

internal fun displayBalance(balance: String, hidden: Boolean): String = if (hidden) "••••••" else balance

private val UnboundColors = lightColorScheme(
    primary = BrandInk,
    onPrimary = Color.White,
    secondary = BrandPurple,
    onSecondary = Color.White,
    background = BrandCanvas,
    surface = Color.White,
    onSurface = BrandInk,
    surfaceVariant = BrandLavender,
    outline = BrandBorder,
    error = Color(0xFFB42318),
)

private val Sans = FontFamily.SansSerif

private val UnboundTypography = Typography(
    displaySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = (-1.8).sp),
    headlineLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-1.0).sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.7).sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 17.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.1.sp),
)

private val UnboundShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(10.dp),
)

@Composable
internal fun UnboundTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = UnboundColors,
        typography = UnboundTypography,
        shapes = UnboundShapes,
        content = content,
    )
}

@Composable
internal fun UnboundCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        content = content,
    )
}
