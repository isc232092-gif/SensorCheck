
package com.example.sensorcheck.ui.device

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sensorcheck.domain.model.DeviceInfo

@Composable
fun DeviceInfoScreen(info: DeviceInfo) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "SensorCheck",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Información del dispositivo",
            style = MaterialTheme.typography.titleLarge
        )

        DeviceItem(
            titulo = "Fabricante",
            valor = info.fabricante
        )

        DeviceItem(
            titulo = "Marca",
            valor = info.marca
        )

        DeviceItem(
            titulo = "Modelo",
            valor = info.modelo
        )

        DeviceItem(
            titulo = "Nombre del dispositivo",
            valor = info.dispositivo
        )

        DeviceItem(
            titulo = "Versión de Android",
            valor = info.versionAndroid
        )

        DeviceItem(
            titulo = "Nivel de API",
            valor = info.nivelApi.toString()
        )

        DeviceItem(
            titulo = "Placa",
            valor = info.placa
        )

        DeviceItem(
            titulo = "Hardware",
            valor = info.hardware
        )

        DeviceItem(
            titulo = "Producto",
            valor = info.producto
        )

        DeviceItem(
            titulo = "Arquitecturas de CPU",
            valor = info.arquitecturasCpu.joinToString(", ")
        )
    }
}

@Composable
private fun DeviceItem(
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
                text = valor.ifBlank { "No disponible" },
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
