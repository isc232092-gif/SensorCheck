package com.example.sensorcheck.ui.modules

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.sensorcheck.ui.components.ContenidoPantalla
import com.example.sensorcheck.ui.components.Espaciado
import com.example.sensorcheck.ui.components.EtiquetaEstado
import com.example.sensorcheck.ui.components.TarjetaGrupo
import com.example.sensorcheck.ui.components.TituloSeccion

/** Elemento planificado de un módulo aún no implementado. */
data class ElementoPlanificado(
    val icono: ImageVector,
    val nombre: String
)

@Composable
fun DiagnosticoScreen() = ModuloPendienteScreen(
    icono = Icons.Outlined.MonitorHeart,
    descripcion = "Las pruebas físicas aún no están implementadas. " +
        "Ninguna prueba se ha ejecutado.",
    tituloLista = "Pruebas previstas",
    elementos = listOf(
        ElementoPlanificado(Icons.Outlined.TouchApp, "Pantalla táctil"),
        ElementoPlanificado(Icons.Outlined.PanTool, "Multitáctil"),
        ElementoPlanificado(Icons.Outlined.PhotoCamera, "Cámara"),
        ElementoPlanificado(Icons.Outlined.FlashlightOn, "Flash"),
        ElementoPlanificado(Icons.AutoMirrored.Outlined.VolumeUp, "Audio"),
        ElementoPlanificado(Icons.Outlined.Mic, "Micrófono"),
        ElementoPlanificado(Icons.Outlined.Vibration, "Vibración"),
        ElementoPlanificado(Icons.Outlined.GpsFixed, "GPS"),
        ElementoPlanificado(Icons.Outlined.Wifi, "Wi-Fi"),
        ElementoPlanificado(Icons.Outlined.Bluetooth, "Bluetooth")
    )
)

/**
 * Estructura común para módulos pendientes: deja claro su estado
 * y no muestra disponibilidad ni resultados que no se han medido.
 */
@Composable
private fun ModuloPendienteScreen(
    icono: ImageVector,
    descripcion: String,
    tituloLista: String,
    elementos: List<ElementoPlanificado>
) {
    val colores = MaterialTheme.colorScheme

    ContenidoPantalla {

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colores.surfaceContainerHigh,
                shape = Espaciado.Esquinas
            ) {
                Row(
                    modifier = Modifier.padding(Espaciado.InternoTarjeta),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icono, contentDescription = null, tint = colores.tertiary)
                    Spacer(Modifier.width(Espaciado.InternoTarjeta))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        EtiquetaEstado("En desarrollo")
                        Text(
                            text = descripcion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colores.onSurface
                        )
                    }
                }
            }
        }

        item { TituloSeccion(tituloLista) }

        item {
            TarjetaGrupo {
                elementos.forEachIndexed { indice, elemento ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Espaciado.InternoTarjeta, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            elemento.icono,
                            contentDescription = null,
                            tint = colores.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(Espaciado.InternoTarjeta))
                        Text(
                            text = elemento.nombre,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colores.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Pendiente",
                            style = MaterialTheme.typography.labelMedium,
                            color = colores.onSurfaceVariant
                        )
                    }
                    if (indice < elementos.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = colores.outlineVariant
                        )
                    }
                }
            }
        }
    }
}
