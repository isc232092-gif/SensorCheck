
package com.example.sensorcheck

import android.os.Bundle
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

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

        // Obtener información real del dispositivo
        val dispositivo = AndroidDeviceRepository()
            .obtenerInformacion()

        // Obtener información de RAM, almacenamiento y batería
        val hardware = AndroidHardwareRepository(
            applicationContext
        ).obtenerHardware()

        // Obtener información de CPU y pantalla
        val cpuPantalla = AndroidCpuDisplayRepository(
            applicationContext
        ).obtenerInformacion()

        // Iniciar la interfaz de SensorCheck
        setContent {

            SensorCheckTheme {

                // Control de la pantalla seleccionada
                var pantalla by remember {
                    mutableIntStateOf(0)
                }

                Scaffold(

                    bottomBar = {

                        NavigationBar {

                            // Pantalla de dispositivo
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

                            // Pantalla de hardware
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

                            // Pantalla de CPU y pantalla
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

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {

                        when (pantalla) {

                            // Información general
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
