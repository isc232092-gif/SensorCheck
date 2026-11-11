package com.example.sensorcheck.ui.device

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import com.example.sensorcheck.domain.model.CpuDisplayInfo
import com.example.sensorcheck.ui.components.ContenidoPantalla
import com.example.sensorcheck.ui.components.DatoInfo
import com.example.sensorcheck.ui.components.GrupoInfo
import com.example.sensorcheck.ui.components.TituloSeccion
import java.util.Locale

@Composable
fun CpuDisplayScreen(info: CpuDisplayInfo) {

    val frecuenciaActual = info.frecuenciaActualHz?.let { formatoHz(it) }

    val frecuenciasAdmitidas = info.frecuenciasSoportadasHz
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ") { formatoHz(it) }

    ContenidoPantalla {

        item { TituloSeccion("Procesador") }
        item {
            GrupoInfo(
                listOf(
                    DatoInfo(
                        Icons.Outlined.Memory,
                        "Procesadores lógicos disponibles",
                        info.procesadoresDisponibles.toString()
                    ),
                    DatoInfo(
                        Icons.Outlined.Layers,
                        "Arquitecturas compatibles",
                        info.arquitecturas.joinToString(", ")
                    ),
                    DatoInfo(
                        Icons.Outlined.Tag,
                        "Identificador de hardware",
                        info.hardware
                    )
                )
            )
        }

        item { TituloSeccion("Pantalla") }
        item {
            GrupoInfo(
                listOf(
                    DatoInfo(Icons.Outlined.AspectRatio, "Resolución", info.resolucion),
                    DatoInfo(Icons.Outlined.Grain, "Densidad", "${info.densidadDpi} dpi"),
                    DatoInfo(Icons.Outlined.Speed, "Frecuencia del modo activo", frecuenciaActual),
                    DatoInfo(Icons.Outlined.Tune, "Frecuencias admitidas", frecuenciasAdmitidas)
                )
            )
        }
    }
}

private fun formatoHz(valor: Float): String =
    String.format(Locale.getDefault(), "%.1f Hz", valor)
