
package com.example.sensorcheck.data.device

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import com.example.sensorcheck.domain.model.CpuDisplayInfo

class AndroidCpuDisplayRepository(
    private val context: Context
) {

    fun obtenerInformacion(): CpuDisplayInfo {

        val displayManager =
            context.getSystemService(
                Context.DISPLAY_SERVICE
            ) as DisplayManager

        val pantalla = displayManager.getDisplay(
            Display.DEFAULT_DISPLAY
        )

        val modo = pantalla?.mode

        val frecuencias = pantalla
            ?.supportedModes
            ?.map { it.refreshRate }
            ?.distinct()
            ?.sorted()
            ?: emptyList()

        val densidad = context.resources
            .displayMetrics.densityDpi

        return CpuDisplayInfo(
            procesadoresDisponibles =
                Runtime.getRuntime().availableProcessors(),

            arquitecturas = Build.SUPPORTED_ABIS.toList(),

            hardware = Build.HARDWARE,

            resolucion = if (modo != null) {
                "${modo.physicalWidth} × ${modo.physicalHeight} px"
            } else {
                "No disponible"
            },

            densidadDpi = densidad,

            frecuenciaActualHz = modo?.refreshRate,

            frecuenciasSoportadasHz = frecuencias
        )
    }
}
