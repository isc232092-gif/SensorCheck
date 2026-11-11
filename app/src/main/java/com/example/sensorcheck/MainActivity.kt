package com.example.sensorcheck

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.sensorcheck.ui.EstadoCarga
import com.example.sensorcheck.ui.SensorCheckApp
import com.example.sensorcheck.ui.SensorCheckViewModel
import com.example.sensorcheck.ui.theme.SensorCheckTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SensorCheckViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {

        // Debe llamarse antes de super.onCreate()
        val splash = installSplashScreen()

        super.onCreate(savedInstanceState)

        // El splash se mantiene solo mientras se leen los datos reales
        splash.setKeepOnScreenCondition {
            viewModel.estado.value is EstadoCarga.Cargando
        }

        enableEdgeToEdge()

        setContent {

            val preferencia by viewModel.modoOscuro.collectAsState()
            val estado by viewModel.estado.collectAsState()
            val modoOscuro = preferencia ?: isSystemInDarkTheme()

            // Iconos de las barras del sistema acordes al tema elegido
            DisposableEffect(modoOscuro) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT
                    ) { modoOscuro },
                    navigationBarStyle = SystemBarStyle.auto(
                        ScrimClaro,
                        ScrimOscuro
                    ) { modoOscuro }
                )
                onDispose { }
            }

            SensorCheckTheme(
                darkTheme = modoOscuro,
                dynamicColor = false
            ) {
                SensorCheckApp(
                    estado = estado,
                    modoOscuro = modoOscuro,
                    onCambiarTema = { viewModel.cambiarModoOscuro(!modoOscuro) }
                )
            }
        }
    }

    private companion object {
        // Scrims usados solo en navegación por botones (Android < 10)
        val ScrimClaro = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val ScrimOscuro = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
