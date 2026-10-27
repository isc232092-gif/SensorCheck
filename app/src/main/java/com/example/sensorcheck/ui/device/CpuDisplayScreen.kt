
package com.example.sensorcheck.ui.device

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sensorcheck.domain.model.CpuDisplayInfo
import java.util.Locale

@Composable
fun CpuDisplayScreen(info: CpuDisplayInfo) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Procesador y pantalla",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Información del procesador",
            style = MaterialTheme.typography.titleLarge
        )

        CpuDisplayCard(
            "Procesadores lógicos disponibles",
            info.procesadoresDisponibles.toString()
        )

        CpuDisplayCard(
            "Arquitecturas compatibles",
            info.arquitecturas.joinToString(", ")
        )

        CpuDisplayCard("Hardware", info.hardware)

        HorizontalDivider()

        Text(
            text = "Información de pantalla",
            style = MaterialTheme.typography.titleLarge
        )

        CpuDisplayCard(
            "Resolución del modo activo",
            info.resolucion
        )

        CpuDisplayCard(
            "Densidad lógica",
            "${info.densidadDpi} dpi"
        )

        CpuDisplayCard(
            "Frecuencia del modo activo",
            info.frecuenciaActualHz?.let {
                String.format(Locale.getDefault(), "%.1f Hz", it)
            } ?: "No disponible"
        )

        CpuDisplayCard(
            "Frecuencias admitidas",
            if (info.frecuenciasSoportadasHz.isEmpty()) {
                "No disponible"
            } else {
                info.frecuenciasSoportadasHz.joinToString(", ") {
                    String.format(
                        Locale.getDefault(),
                        "%.1f Hz",
                        it
                    )
                }
            }
        )
    }
}

@Composable
private fun CpuDisplayCard(
    titulo: String,
    valor: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = valor,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
