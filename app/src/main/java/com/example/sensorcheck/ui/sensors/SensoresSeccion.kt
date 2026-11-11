package com.example.sensorcheck.ui.sensors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Punto de entrada de la sección Sensores: listado o ficha del sensor
 * seleccionado. La barra de navegación principal sigue visible.
 */
@Composable
fun SensoresSeccion() {
    var seleccionado by rememberSaveable { mutableStateOf<String?>(null) }

    when (val clave = seleccionado) {
        null -> SensorsScreen(onAbrir = { seleccionado = it })
        else -> SensorDetailScreen(clave = clave, onVolver = { seleccionado = null })
    }
}
