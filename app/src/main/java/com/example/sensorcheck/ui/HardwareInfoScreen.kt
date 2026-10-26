
package com.example.sensorcheck.ui.device

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sensorcheck.domain.model.HardwareInfo
import java.util.Locale

@Composable
fun HardwareInfoScreen(info: HardwareInfo) {

    val gb = 1024.0 * 1024.0 * 1024.0

    fun formatoGB(bytes: Long): String =
        String.format(
            Locale.getDefault(),
            "%.2f GB",
            bytes / gb
        )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            "Hardware del dispositivo",
            style = MaterialTheme.typography.headlineSmall
        )

        HardwareCard(
            "Memoria RAM total",
            formatoGB(info.ramTotalBytes)
        )

        HardwareCard(
            "RAM disponible",
            formatoGB(info.ramDisponibleBytes)
        )

        HardwareCard(
            "Almacenamiento total",
            formatoGB(info.almacenamientoTotalBytes)
        )

        HardwareCard(
            "Almacenamiento disponible",
            formatoGB(info.almacenamientoDisponibleBytes)
        )

        HardwareCard(
            "Nivel de batería",
            info.bateriaPorcentaje?.let { "$it%" }
                ?: "No disponible"
        )

        HardwareCard(
            "Temperatura de batería",
            info.bateriaTemperaturaC?.let { "$it °C" }
                ?: "No disponible"
        )

        HardwareCard(
            "Voltaje de batería",
            info.bateriaVoltajeMv?.let { "$it mV" }
                ?: "No disponible"
        )
    }
}

@Composable
private fun HardwareCard(
    titulo: String,
    valor: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                titulo,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.height(6.dp))
            Text(
                valor,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
