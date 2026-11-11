package com.slmlab.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Type scale tuned for reading on a phone: a clear title hierarchy, comfortable line length
 * for lesson text, and small uppercase labels for section overlines.
 */
val SlmTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.9.sp,
    ),
)
