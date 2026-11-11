package com.example.sensorcheck.ui.device

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.example.sensorcheck.domain.model.HardwareInfo
import com.example.sensorcheck.ui.components.ContenidoPantalla
import com.example.sensorcheck.ui.components.DatoInfo
import com.example.sensorcheck.ui.components.TarjetaProgreso
import com.example.sensorcheck.ui.components.TituloSeccion
import java.util.Locale

private const val BYTES_POR_GB = 1024.0 * 1024.0 * 1024.0

@Composable
fun HardwareInfoScreen(info: HardwareInfo) {

    val colores = MaterialTheme.colorScheme

    val ramUsada = info.ramTotalBytes - info.ramDisponibleBytes
    val almacenamientoUsado =
        info.almacenamientoTotalBytes - info.almacenamientoDisponibleBytes

    ContenidoPantalla {

        item { TituloSeccion("Memoria") }
        item {
            TarjetaProgreso(
                icono = Icons.Outlined.Memory,
                titulo = "RAM en uso",
                fraccion = fraccion(ramUsada, info.ramTotalBytes),
                detalle = "${formatoGB(ramUsada)} usados de ${formatoGB(info.ramTotalBytes)}",
                color = colores.primary,
                datosAdicionales = listOf(
                    DatoInfo(Icons.Outlined.Memory, "RAM total", formatoGB(info.ramTotalBytes)),
                    DatoInfo(Icons.Outlined.CheckCircleOutline, "RAM disponible", formatoGB(info.ramDisponibleBytes))
                )
            )
        }

        item { TituloSeccion("Almacenamiento interno") }
        item {
            TarjetaProgreso(
                icono = Icons.Outlined.Storage,
                titulo = "Almacenamiento en uso",
                fraccion = fraccion(almacenamientoUsado, info.almacenamientoTotalBytes),
                detalle = "${formatoGB(almacenamientoUsado)} usados de " +
                    formatoGB(info.almacenamientoTotalBytes),
                color = colores.secondary,
                datosAdicionales = listOf(
                    DatoInfo(Icons.Outlined.Storage, "Almacenamiento total", formatoGB(info.almacenamientoTotalBytes)),
                    DatoInfo(Icons.Outlined.CheckCircleOutline, "Almacenamiento disponible", formatoGB(info.almacenamientoDisponibleBytes))
                )
            )
        }

        item { TituloSeccion("Batería") }
        item {
            TarjetaProgreso(
                icono = Icons.Outlined.BatteryStd,
                titulo = "Nivel de batería",
                fraccion = info.bateriaPorcentaje?.let { it / 100f },
                detalle = null,
                color = colores.tertiary,
                datosAdicionales = listOf(
                    DatoInfo(
                        Icons.Outlined.Thermostat,
                        "Temperatura",
                        info.bateriaTemperaturaC?.let {
                            String.format(Locale.getDefault(), "%.1f °C", it)
                        }
                    ),
                    DatoInfo(
                        Icons.Outlined.Bolt,
                        "Voltaje",
                        info.bateriaVoltajeMv?.let { "$it mV" }
                    )
                )
            )
        }
    }
}

/** Fracción usada/total; null si el total no es válido. */
private fun fraccion(usado: Long, total: Long): Float? =
    if (total > 0 && usado >= 0) usado.toFloat() / total else null

private fun formatoGB(bytes: Long): String =
    String.format(Locale.getDefault(), "%.2f GB", bytes / BYTES_POR_GB)
