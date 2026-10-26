
package com.example.sensorcheck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import com.example.sensorcheck.data.device.AndroidDeviceRepository
import com.example.sensorcheck.data.device.AndroidHardwareRepository
import com.example.sensorcheck.ui.device.DeviceInfoScreen
import com.example.sensorcheck.ui.device.HardwareInfoScreen
import com.example.sensorcheck.ui.theme.SensorCheckTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Obtener información real del dispositivo
        val dispositivo =
            AndroidDeviceRepository().obtenerInformacion()

        // Obtener información real de RAM, batería y almacenamiento
        val hardware =
            AndroidHardwareRepository(
                applicationContext
            ).obtenerHardware()

        setContent {
            SensorCheckTheme {

                var pantalla by remember {
                    mutableStateOf(0)
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar {

                            NavigationBarItem(
                                selected = pantalla == 0,
                                onClick = { pantalla = 0 },
                                icon = { Text("📱") },
                                label = { Text("Dispositivo") }
                            )

                            NavigationBarItem(
                                selected = pantalla == 1,
                                onClick = { pantalla = 1 },
                                icon = { Text("⚙") },
                                label = { Text("Hardware") }
                            )
                        }
                    }
                ) { paddingValues ->

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {

                        when (pantalla) {

                            0 -> DeviceInfoScreen(
                                info = dispositivo
                            )

                            1 -> HardwareInfoScreen(
                                info = hardware
                            )
                        }
                    }
                }
            }
        }
    }
}
