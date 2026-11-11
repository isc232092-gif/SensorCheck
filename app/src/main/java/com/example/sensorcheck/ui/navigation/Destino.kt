package com.example.sensorcheck.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.ui.graphics.vector.ImageVector

/** Secciones de la navegación principal. */
enum class Destino(
    val etiqueta: String,
    val titulo: String,
    val icono: ImageVector
) {
    RESUMEN("Resumen", "SensorCheck", Icons.Outlined.Dashboard),
    HARDWARE("Hardware", "Hardware", Icons.Outlined.Memory),
    SISTEMA("Sistema", "Sistema", Icons.Outlined.Android),
    SENSORES("Sensores", "Sensores", Icons.Outlined.Sensors),
    DIAGNOSTICO("Diagnóstico", "Diagnóstico", Icons.Outlined.MonitorHeart)
}
