package com.example.sensorcheck.ui.device

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.example.sensorcheck.domain.model.CamerasResultado
import com.example.sensorcheck.domain.model.CpuDisplayInfo
import com.example.sensorcheck.domain.model.HardwareInfo

private enum class SubseccionHardware(val titulo: String) {
    MEMORIA("Memoria"),
    CPU_PANTALLA("CPU y pantalla"),
    CAMARAS("Cámaras")
}

/**
 * Sección Hardware: agrupa memoria/batería, CPU y pantalla,
 * y cámaras, reutilizando sus pantallas sin modificarlas.
 */
@Composable
fun HardwareSectionScreen(
    hardware: HardwareInfo,
    cpuPantalla: CpuDisplayInfo,
    camaras: CamerasResultado
) {

    var subseccion by rememberSaveable { mutableStateOf(SubseccionHardware.MEMORIA) }

    Column(modifier = Modifier.fillMaxSize()) {

        PrimaryTabRow(selectedTabIndex = subseccion.ordinal) {
            SubseccionHardware.entries.forEach { item ->
                Tab(
                    selected = subseccion == item,
                    onClick = { subseccion = item },
                    text = {
                        Text(
                            text = item.titulo,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (subseccion) {
                SubseccionHardware.MEMORIA -> HardwareInfoScreen(info = hardware)
                SubseccionHardware.CPU_PANTALLA -> CpuDisplayScreen(info = cpuPantalla)
                SubseccionHardware.CAMARAS -> CameraInfoScreen(resultado = camaras)
            }
        }
    }
}
