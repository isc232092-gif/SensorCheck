package com.example.sensorcheck.data.device

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import com.example.sensorcheck.domain.model.CameraInfo
import com.example.sensorcheck.domain.model.CamerasResultado
import com.example.sensorcheck.domain.model.OrientacionCamara
import com.example.sensorcheck.domain.repository.CameraRepository
import java.util.Locale

/**
 * Adapta la API Camera2 al modelo de dominio (GoF: Adapter).
 * Solo lee CameraCharacteristics: no abre ninguna cámara,
 * por lo que no requiere el permiso CAMERA.
 */
class AndroidCameraRepository(
    private val context: Context
) : CameraRepository {

    override fun obtenerCamaras(): CamerasResultado {

        val cameraManager = context.getSystemService(
            Context.CAMERA_SERVICE
        ) as? CameraManager
            ?: return CamerasResultado(
                camaras = emptyList(),
                error = "Servicio de cámara no disponible"
            )

        return try {
            val camaras = cameraManager.cameraIdList.mapNotNull { id ->
                leerCamara(cameraManager, id)
            }
            CamerasResultado(camaras = camaras)
        } catch (e: CameraAccessException) {
            CamerasResultado(
                camaras = emptyList(),
                error = "No se pudo acceder a las cámaras: ${e.message}"
            )
        } catch (e: SecurityException) {
            CamerasResultado(
                camaras = emptyList(),
                error = "Acceso a cámaras denegado: ${e.message}"
            )
        }
    }

    private fun leerCamara(
        cameraManager: CameraManager,
        id: String
    ): CameraInfo? {

        val c = try {
            cameraManager.getCameraCharacteristics(id)
        } catch (e: CameraAccessException) {
            return null
        } catch (e: IllegalArgumentException) {
            return null
        }

        // Resolución máxima de captura JPEG
        val tamanoMaximo = c.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
        )?.getOutputSizes(ImageFormat.JPEG)
            ?.maxByOrNull { it.width.toLong() * it.height }

        val megapixeles = tamanoMaximo?.let {
            (it.width.toLong() * it.height) / 1_000_000f
        }

        val tamanoSensor = c.get(
            CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
        )?.let {
            String.format(
                Locale.getDefault(),
                "%.2f × %.2f mm",
                it.width,
                it.height
            )
        }

        val modosAf = c.get(
            CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES
        )

        val modosOis = c.get(
            CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION
        )

        val capacidades = c.get(
            CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
        ) ?: IntArray(0)

        val esLogica = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            capacidades.contains(
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA
            )

        val fisicas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            c.physicalCameraIds.sorted()
        } else {
            emptyList()
        }

        return CameraInfo(
            id = id,
            orientacion = orientacion(
                c.get(CameraCharacteristics.LENS_FACING)
            ),
            resolucionMaxima = tamanoMaximo?.let {
                "${it.width} × ${it.height} px"
            },
            megapixeles = megapixeles,
            distanciasFocalesMm = c.get(
                CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
            )?.toList()?.distinct()?.sorted() ?: emptyList(),
            aperturas = c.get(
                CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES
            )?.toList()?.distinct()?.sorted() ?: emptyList(),
            tamanoSensorMm = tamanoSensor,
            orientacionSensorGrados = c.get(
                CameraCharacteristics.SENSOR_ORIENTATION
            ),
            tieneFlash = c.get(
                CameraCharacteristics.FLASH_INFO_AVAILABLE
            ),
            tieneAutoenfoque = modosAf?.any {
                it != CameraMetadata.CONTROL_AF_MODE_OFF
            },
            tieneEstabilizacionOptica = modosOis?.contains(
                CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON
            ),
            zoomDigitalMaximo = zoomMaximo(c),
            nivelHardware = nivelHardware(
                c.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            ),
            esMultiCamaraLogica = esLogica,
            camarasFisicas = fisicas
        )
    }

    private fun orientacion(valor: Int?): OrientacionCamara =
        when (valor) {
            CameraMetadata.LENS_FACING_BACK -> OrientacionCamara.TRASERA
            CameraMetadata.LENS_FACING_FRONT -> OrientacionCamara.FRONTAL
            CameraMetadata.LENS_FACING_EXTERNAL -> OrientacionCamara.EXTERNA
            else -> OrientacionCamara.DESCONOCIDA
        }

    private fun zoomMaximo(c: CameraCharacteristics): Float? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            c.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
                ?.upper
                ?.let { return it }
        }
        return c.get(
            CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM
        )
    }

    private fun nivelHardware(valor: Int?): String =
        when (valor) {
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "Legacy"
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "Limited"
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "Full"
            CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "Level 3"
            NIVEL_EXTERNAL -> "External"
            null -> "No disponible"
            else -> "Desconocido ($valor)"
        }

    private companion object {
        // INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL (API 28)
        const val NIVEL_EXTERNAL = 4
    }
}
