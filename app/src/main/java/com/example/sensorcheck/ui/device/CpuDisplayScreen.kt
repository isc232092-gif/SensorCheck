
package com.example.sensorcheck.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sensorcheck.domain.model.CpuDisplayInfo
import java.util.Locale

@Composable
fun CpuDisplayScreen(info: CpuDisplayInfo) {

    val colores = MaterialTheme.colorScheme

    val arquitecturaPrincipal =
        info.arquitecturas.firstOrNull() ?: "No disponible"

    val frecuenciaActual = info.frecuenciaActualHz?.let {
        String.format(Locale.getDefault(), "%.1f Hz", it)
    } ?: "No disponible"

    val frecuencias = if (info.frecuenciasSoportadasHz.isEmpty()) {
        "No disponible"
    } else {
        info.frecuenciasSoportadasHz.joinToString(", ") {
            String.format(Locale.getDefault(), "%.1f Hz", it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colores.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Procesador y pantalla",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = colores.onBackground
        )

        Text(
            text = "Características técnicas del dispositivo",
            style = MaterialTheme.typography.bodyMedium,
            color = colores.onSurfaceVariant
        )

        // Tarjeta principal con degradado adaptable
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            colores.primaryContainer,
                            colores.secondaryContainer
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(22.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Text(
                    text = "PROCESADOR",
                    color = colores.onPrimaryContainer,
                    style = MaterialTheme.typography.labelLarge
                )

                Text(
                    text = info.procesadoresDisponibles.toString(),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = colores.onPrimaryContainer
                )

                Text(
                    text = "Procesadores lógicos disponibles",
                    color = colores.onPrimaryContainer
                )

                HorizontalDivider(
                    color = colores.onPrimaryContainer.copy(
                        alpha = 0.20f
                    )
                )

                Text(
                    text = "Hardware: ${info.hardware}",
                    color = colores.onPrimaryContainer
                )

                Text(
                    text = "Arquitectura: $arquitecturaPrincipal",
                    color = colores.onPrimaryContainer
                )
            }
        }

        TituloCpu("Información del procesador")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Box(modifier = Modifier.weight(1f)) {
                TarjetaCpu(
                    titulo = "Procesadores",
                    valor = info.procesadoresDisponibles.toString(),
                    icono = "▣"
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                TarjetaCpu(
                    titulo = "Arquitectura",
                    valor = arquitecturaPrincipal,
                    icono = "◈"
                )
            }
        }

        TarjetaCpu(
            titulo = "Arquitecturas compatibles",
            valor = info.arquitecturas.joinToString(", "),
            icono = "◎"
        )

        TituloCpu("Información de pantalla")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Box(modifier = Modifier.weight(1f)) {
                TarjetaCpu(
                    titulo = "Resolución",
                    valor = info.resolucion,
                    icono = "▤"
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                TarjetaCpu(
                    titulo = "Densidad",
                    valor = "${info.densidadDpi} dpi",
                    icono = "▦"
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Box(modifier = Modifier.weight(1f)) {
                TarjetaCpu(
                    titulo = "Frecuencia actual",
                    valor = frecuenciaActual,
                    icono = "◉"
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                TarjetaCpu(
                    titulo = "Frecuencias admitidas",
                    valor = frecuencias,
                    icono = "↻"
                )
            }
        }

        Surface(
            color = colores.surfaceContainer,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Datos reportados por Android. " +
                        "La información disponible puede variar " +
                        "según el fabricante y el dispositivo.",
                color = colores.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun TituloCpu(titulo: String) {
    Text(
        text = titulo,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun TarjetaCpu(
    titulo: String,
    valor: String,
    icono: String
) {

    val colores = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colores.surfaceContainer,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.Start
        ) {

            Text(
                text = icono,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                color = colores.primary
            )

            Text(
                text = valor.ifBlank { "No disponible" },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = colores.onSurface
            )

            Text(
                text = titulo,
                style = MaterialTheme.typography.bodySmall,
                color = colores.onSurfaceVariant
            )
        }
    }
}
