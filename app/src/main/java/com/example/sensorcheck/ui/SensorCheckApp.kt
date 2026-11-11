package com.example.sensorcheck.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sensorcheck.ui.device.DeviceInfoScreen
import com.example.sensorcheck.ui.device.HardwareSectionScreen
import com.example.sensorcheck.ui.diagnostico.DiagnosticoSeccion
import com.example.sensorcheck.ui.system.SystemInfoScreen
import com.example.sensorcheck.ui.navigation.Destino
import com.example.sensorcheck.ui.sensors.SensoresSeccion

/**
 * Estructura principal: barra superior, navegación inferior y
 * contenido. Scaffold, TopAppBar y NavigationBar gestionan sus
 * propios insets; el contenido solo recibe innerPadding una vez.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorCheckApp(
    estado: EstadoCarga,
    modoOscuro: Boolean,
    onCambiarTema: () -> Unit
) {

    var destino by rememberSaveable { mutableStateOf(Destino.entries.first()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = destino.titulo,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onCambiarTema) {
                        Icon(
                            imageVector = if (modoOscuro) {
                                Icons.Outlined.LightMode
                            } else {
                                Icons.Outlined.DarkMode
                            },
                            contentDescription = if (modoOscuro) {
                                "Cambiar a modo día"
                            } else {
                                "Cambiar a modo noche"
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Barra compacta (64 dp) que gestiona el inset de navegación
            ShortNavigationBar {
                Destino.entries.forEach { item ->
                    ShortNavigationBarItem(
                        selected = destino == item,
                        onClick = { destino = item },
                        icon = { Icon(item.icono, contentDescription = null) },
                        label = { Text(item.etiqueta, maxLines = 1) }
                    )
                }
            }
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            when (estado) {
                EstadoCarga.Cargando -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )

                is EstadoCarga.Error -> Text(
                    text = "No se pudo leer la información del dispositivo:\n${estado.mensaje}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )

                is EstadoCarga.Listo -> {
                    val datos = estado.datos
                    when (destino) {
                        Destino.RESUMEN -> DeviceInfoScreen(info = datos.dispositivo)
                        Destino.HARDWARE -> HardwareSectionScreen(
                            hardware = datos.hardware,
                            cpuPantalla = datos.cpuPantalla,
                            camaras = datos.camaras
                        )
                        Destino.SISTEMA -> SystemInfoScreen()
                        Destino.SENSORES -> SensoresSeccion()
                        Destino.DIAGNOSTICO -> DiagnosticoSeccion()
                    }
                }
            }
        }
    }
}
