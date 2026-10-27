
package com.example.sensorcheck.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Colores del modo noche
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF91BEFF),
    onPrimary = Color(0xFF002D60),
    primaryContainer = Color(0xFF204779),
    onPrimaryContainer = Color(0xFFD9E7FF),

    secondary = Color(0xFFB4C9E7),
    onSecondary = Color(0xFF203248),
    secondaryContainer = Color(0xFF34465F),
    onSecondaryContainer = Color(0xFFD5E4FF),

    tertiary = Color(0xFF9DD9D4),

    background = Color(0xFF0B1220),
    onBackground = Color(0xFFF1F5FD),

    surface = Color(0xFF111B2B),
    onSurface = Color(0xFFF1F5FD),

    surfaceContainer = Color(0xFF1C2940),
    onSurfaceVariant = Color(0xFFB5C4DA),

    outline = Color(0xFF536781)
)

// Colores del modo día
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF245FA9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEAFF),
    onPrimaryContainer = Color(0xFF102E56),

    secondary = Color(0xFF536B8C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8E5F7),
    onSecondaryContainer = Color(0xFF233750),

    tertiary = Color(0xFF287E79),

    background = Color(0xFFF3F7FD),
    onBackground = Color(0xFF142642),

    surface = Color.White,
    onSurface = Color(0xFF142642),

    surfaceContainer = Color(0xFFE6EEF9),
    onSurfaceVariant = Color(0xFF526781),

    outline = Color(0xFF8497B1)
)

@Composable
fun SensorCheckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {

    val colorScheme = when {

        dynamicColor &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {

            val context = LocalContext.current

            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        darkTheme -> DarkColorScheme

        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
