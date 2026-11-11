package com.example.sensorcheck.ui.device

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.sensorcheck.domain.model.CameraInfo
import com.example.sensorcheck.domain.model.CamerasResultado
import com.example.sensorcheck.domain.model.OrientacionCamara
import com.example.sensorcheck.ui.components.ContenidoPantalla
import com.example.sensorcheck.ui.components.DatoInfo
import com.example.sensorcheck.ui.components.Espaciado
import com.example.sensorcheck.ui.components.GrupoInfo
import com.example.sensorcheck.ui.components.TarjetaPrincipal
import com.example.sensorcheck.ui.components.TituloSeccion
import java.util.Locale

@Composable
fun CameraInfoScreen(resultado: CamerasResultado) {

    val camaras = resultado.camaras

    ContenidoPantalla {

        item {
            TarjetaPrincipal(
                icono = Icons.Outlined.PhotoCamera,
                etiqueta = "Cámaras detectadas",
                titulo = camaras.size.toString(),
                subtitulo = "Traseras: ${camaras.cuenta(OrientacionCamara.TRASERA)}" +
                    " · Frontales: ${camaras.cuenta(OrientacionCamara.FRONTAL)}" +
                    " · Externas: ${camaras.cuenta(OrientacionCamara.EXTERNA)}"
            )
        }

        resultado.error?.let { mensaje ->
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = Espaciado.Esquinas
                ) {
                    Text(
                        text = mensaje,
                        modifier = Modifier.padding(Espaciado.InternoTarjeta)
                    )
                }
            }
        }

        if (camaras.isEmpty() && resultado.error == null) {
            item {
                Text(
                    text = "Este dispositivo no reporta cámaras.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        camaras.forEach { camara ->
            item(key = camara.id) {
                TituloSeccion("Cámara ${camara.orientacion.etiqueta.lowercase(Locale.getDefault())} · ID ${camara.id}")
            }
            item(key = "datos_${camara.id}") {
                GrupoInfo(datosCamara(camara))
            }
        }
    }
}

private fun List<CameraInfo>.cuenta(orientacion: OrientacionCamara): Int =
    count { it.orientacion == orientacion }

private fun datosCamara(camara: CameraInfo): List<DatoInfo> = buildList {
    add(DatoInfo(Icons.Outlined.AspectRatio, "Resolución máxima", camara.resolucionMaxima))
    add(DatoInfo(Icons.Outlined.PhotoCamera, "Megapíxeles", camara.megapixeles?.let { formato("%.1f MP", it) }))
    add(
        DatoInfo(
            Icons.Outlined.CenterFocusStrong,
            "Distancia focal",
            camara.distanciasFocalesMm.takeIf { it.isNotEmpty() }
                ?.joinToString(", ") { formato("%.2f mm", it) }
        )
    )
    add(
        DatoInfo(
            Icons.Outlined.Camera,
            "Apertura",
            camara.aperturas.takeIf { it.isNotEmpty() }
                ?.joinToString(", ") { formato("f/%.1f", it) }
        )
    )
    add(DatoInfo(Icons.Outlined.CropFree, "Tamaño del sensor", camara.tamanoSensorMm))
    add(DatoInfo(Icons.Outlined.ScreenRotation, "Orientación del sensor", camara.orientacionSensorGrados?.let { "$it°" }))
    add(DatoInfo(Icons.Outlined.FlashOn, "Flash", siNo(camara.tieneFlash)))
    add(DatoInfo(Icons.Outlined.CenterFocusWeak, "Autoenfoque", siNo(camara.tieneAutoenfoque)))
    add(DatoInfo(Icons.Outlined.Vibration, "Estabilización óptica", siNo(camara.tieneEstabilizacionOptica)))
    add(DatoInfo(Icons.Outlined.ZoomIn, "Zoom máximo", camara.zoomDigitalMaximo?.let { formato("%.1f×", it) }))
    add(DatoInfo(Icons.Outlined.Tune, "Nivel de hardware Camera2", camara.nivelHardware))
    if (camara.esMultiCamaraLogica) {
        add(
            DatoInfo(
                Icons.Outlined.Layers,
                "Cámaras físicas agrupadas",
                camara.camarasFisicas.takeIf { it.isNotEmpty() }
                    ?.joinToString(", ") { "ID $it" }
            )
        )
    }
}

private fun siNo(valor: Boolean?): String? =
    valor?.let { if (it) "Sí" else "No" }

private fun formato(patron: String, valor: Float): String =
    String.format(Locale.getDefault(), patron, valor)
