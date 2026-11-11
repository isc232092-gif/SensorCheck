package com.example.sensorcheck.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Esquema completo de modo noche (incluye contenedores de superficie
// para que Material 3 no use sus tonos predeterminados).
private val DarkColorScheme = darkColorScheme(
    primary = TurquesaNoche,
    onPrimary = SobreTurquesaNoche,
    primaryContainer = TurquesaContenedorNoche,
    onPrimaryContainer = SobreTurquesaContenedorNoche,

    secondary = MarinoNoche,
    onSecondary = SobreMarinoNoche,
    secondaryContainer = MarinoContenedorNoche,
    onSecondaryContainer = SobreMarinoContenedorNoche,

    tertiary = NaranjaNoche,
    onTertiary = SobreNaranjaNoche,
    tertiaryContainer = NaranjaContenedorNoche,
    onTertiaryContainer = SobreNaranjaContenedorNoche,

    background = FondoNoche,
    onBackground = TextoNoche,
    surface = FondoNoche,
    onSurface = TextoNoche,
    surfaceVariant = Color(0xFF2A3A50),
    onSurfaceVariant = TextoSecundarioNoche,
    surfaceTint = TurquesaNoche,

    surfaceContainerLowest = Color(0xFF07101C),
    surfaceContainerLow = Color(0xFF0F1C2E),
    surfaceContainer = Color(0xFF132235),
    surfaceContainerHigh = Color(0xFF192A40),
    surfaceContainerHighest = Color(0xFF20324A),
    surfaceBright = Color(0xFF263A55),
    surfaceDim = FondoNoche,

    outline = Color(0xFF7E8C9E),
    outlineVariant = Color(0xFF34455C),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    inverseSurface = TextoNoche,
    inverseOnSurface = FondoNoche,
    inversePrimary = TurquesaDia
)

// Esquema completo de modo día
private val LightColorScheme = lightColorScheme(
    primary = TurquesaDia,
    onPrimary = Color.White,
    primaryContainer = TurquesaContenedorDia,
    onPrimaryContainer = SobreTurquesaContenedorDia,

    secondary = MarinoDia,
    onSecondary = Color.White,
    secondaryContainer = MarinoContenedorDia,
    onSecondaryContainer = SobreMarinoContenedorDia,

    tertiary = NaranjaDia,
    onTertiary = Color.White,
    tertiaryContainer = NaranjaContenedorDia,
    onTertiaryContainer = SobreNaranjaContenedorDia,

    background = FondoDia,
    onBackground = TextoDia,
    surface = FondoDia,
    onSurface = TextoDia,
    surfaceVariant = Color(0xFFDCE3EC),
    onSurfaceVariant = TextoSecundarioDia,
    surfaceTint = TurquesaDia,

    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEBEFF4),
    surfaceContainerHigh = Color(0xFFE5EAF0),
    surfaceContainerHighest = Color(0xFFDFE5EC),
    surfaceBright = FondoDia,
    surfaceDim = Color(0xFFD5DCE5),

    outline = Color(0xFF74808F),
    outlineVariant = Color(0xFFC4CCD6),

    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    inverseSurface = Color(0xFF263345),
    inverseOnSurface = Color(0xFFEDF1F6),
    inversePrimary = TurquesaNoche
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
