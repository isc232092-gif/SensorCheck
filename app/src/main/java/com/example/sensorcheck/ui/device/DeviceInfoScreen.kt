package com.example.sensorcheck.ui.device

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import com.example.sensorcheck.domain.model.DeviceInfo
import com.example.sensorcheck.ui.components.ContenidoPantalla
import com.example.sensorcheck.ui.components.DatoInfo
import com.example.sensorcheck.ui.components.GrupoInfo
import com.example.sensorcheck.ui.components.TarjetaPrincipal
import com.example.sensorcheck.ui.components.TituloSeccion

@Composable
fun DeviceInfoScreen(info: DeviceInfo) {

    ContenidoPantalla {

        item {
            TarjetaPrincipal(
                icono = Icons.Outlined.Smartphone,
                etiqueta = info.fabricante,
                titulo = info.modelo,
                subtitulo = "Android ${info.versionAndroid} · API ${info.nivelApi}"
            )
        }

        item { TituloSeccion("Identificación") }
        item {
            GrupoInfo(
                listOf(
                    DatoInfo(Icons.Outlined.Factory, "Fabricante", info.fabricante),
                    DatoInfo(Icons.Outlined.Sell, "Marca", info.marca),
                    DatoInfo(Icons.Outlined.Smartphone, "Modelo", info.modelo),
                    DatoInfo(Icons.Outlined.Badge, "Nombre del dispositivo", info.dispositivo),
                    DatoInfo(Icons.Outlined.Inventory2, "Producto", info.producto)
                )
            )
        }

        item { TituloSeccion("Software") }
        item {
            GrupoInfo(
                listOf(
                    DatoInfo(Icons.Outlined.Android, "Versión de Android", info.versionAndroid),
                    DatoInfo(Icons.Outlined.Code, "Nivel de API", info.nivelApi.toString())
                )
            )
        }

        item { TituloSeccion("Plataforma") }
        item {
            GrupoInfo(
                listOf(
                    DatoInfo(Icons.Outlined.DeveloperBoard, "Placa", info.placa),
                    DatoInfo(Icons.Outlined.Memory, "Hardware", info.hardware),
                    DatoInfo(
                        Icons.Outlined.Layers,
                        "Arquitecturas compatibles",
                        info.arquitecturasCpu.joinToString(", ")
                    )
                )
            )
        }
    }
}
