
package com.example.sensorcheck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.example.sensorcheck.data.device.AndroidDeviceRepository
import com.example.sensorcheck.data.device.AndroidHardwareRepository
import com.example.sensorcheck.data.device.AndroidCpuDisplayRepository

import com.example.sensorcheck.ui.device.DeviceInfoScreen
import com.example.sensorcheck.ui.device.HardwareInfoScreen
import com.example.sensorcheck.ui.device.CpuDisplayScreen
import com.example.sensorcheck.ui.theme.SensorCheckTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Información real del dispositivo Android
        val dispositivo = AndroidDeviceRepository()
            .obtenerInformacion()

        // RAM, almacenamiento y batería
        val hardware = AndroidHardwareRepository(
            applicationContext
        ).obtenerHardware()

        // Procesador y pantalla
        val cpuPantalla = AndroidCpuDisplayRepository(
            applicationContext
        ).obtenerInformacion()

        // Preferencias persistentes del usuario
        val preferencias = getSharedPreferences(
            "sensorcheck_preferencias",
            MODE_PRIVATE
        )

        setContent {

            // Detectar el tema actual del sistema Android
            val temaSistema = isSystemInDarkTheme()

            // Estado del modo día / noche
            var modoOscuro by remember {
                mutableStateOf(
                    preferencias.getBoolean(
                        "modo_oscuro",
                        temaSistema
                    )
                )
            }

            // Mantener la pestaña al recrearse la actividad
            var pantalla by rememberSaveable {
                mutableIntStateOf(0)
            }

            SensorCheckTheme(
                darkTheme = modoOscuro,
                dynamicColor = false
            ) {

                Scaffold(

                    // Barra superior
                    topBar = {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 16.dp,
                                        end = 8.dp
                                    ),
                                verticalAlignment =
                                    Alignment.CenterVertically,
                                horizontalArrangement =
                                    Arrangement.SpaceBetween
                            ) {

                                Text(
                                    text = "SensorCheck",
                                    style = MaterialTheme
                                        .typography.titleLarge,
                                    color = MaterialTheme
                                        .colorScheme.primary
                                )

                                IconButton(
                                    onClick = {

                                        // Alternar entre día y noche
                                        modoOscuro = !modoOscuro

                                        // Guardar el tema elegido
                                        preferencias.edit()
                                            .putBoolean(
                                                "modo_oscuro",
                                                modoOscuro
                                            )
                                            .apply()
                                    }
                                ) {
                                    Text(
                                        text = if (modoOscuro) {
                                            "☀️"
                                        } else {
                                            "🌙"
                                        },
                                        style = MaterialTheme
                                            .typography.titleLarge
                                    )
                                }
                            }
                        }
                    },

                    // Barra inferior de navegación
                    bottomBar = {
                        NavigationBar {

                            NavigationBarItem(
                                selected = pantalla == 0,
                                onClick = {
                                    pantalla = 0
                                },
                                icon = {
                                    Text("📱")
                                },
                                label = {
                                    Text("Dispositivo")
                                }
                            )

                            NavigationBarItem(
                                selected = pantalla == 1,
                                onClick = {
                                    pantalla = 1
                                },
                                icon = {
                                    Text("⚙")
                                },
                                label = {
                                    Text("Hardware")
                                }
                            )

                            NavigationBarItem(
                                selected = pantalla == 2,
                                onClick = {
                                    pantalla = 2
                                },
                                icon = {
                                    Text("▣")
                                },
                                label = {
                                    Text("CPU/Pantalla")
                                }
                            )
                        }
                    }

                ) { paddingValues ->

                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        color = MaterialTheme
                            .colorScheme.background
                    ) {

                        when (pantalla) {

                            // Información del dispositivo
                            0 -> DeviceInfoScreen(
                                info = dispositivo
                            )

                            // RAM, almacenamiento y batería
                            1 -> HardwareInfoScreen(
                                info = hardware
                            )

                            // Procesador y pantalla
                            2 -> CpuDisplayScreen(
                                info = cpuPantalla
                            )
                        }
                    }
                }
            }
        }
    }
}
