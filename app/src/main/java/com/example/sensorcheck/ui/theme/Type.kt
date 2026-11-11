package com.example.sensorcheck.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Base = Typography()

// Tipografía compacta y legible basada en la escala de Material 3
val Typography = Typography(
    headlineSmall = Base.headlineSmall.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = Base.titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = Base.titleMedium.copy(
        fontWeight = FontWeight.SemiBold
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.2.sp
    ),
    labelLarge = Base.labelLarge.copy(
        fontWeight = FontWeight.SemiBold
    )
)
