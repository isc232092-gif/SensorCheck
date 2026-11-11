package com.example.sensorcheck.domain.model

/**
 * Información de una cámara reportada por Camera2.
 * Los valores nulos o listas vacías indican que el
 * dispositivo no reporta ese dato.
 */
data class CameraInfo(
    val id: String,
    val orientacion: OrientacionCamara,
    val resolucionMaxima: String?,
    val megapixeles: Float?,
    val distanciasFocalesMm: List<Float>,
    val aperturas: List<Float>,
    val tamanoSensorMm: String?,
    val orientacionSensorGrados: Int?,
    val tieneFlash: Boolean?,
    val tieneAutoenfoque: Boolean?,
    val tieneEstabilizacionOptica: Boolean?,
    val zoomDigitalMaximo: Float?,
    val nivelHardware: String,
    val esMultiCamaraLogica: Boolean,
    val camarasFisicas: List<String>
)

enum class OrientacionCamara(val etiqueta: String) {
    TRASERA("Trasera"),
    FRONTAL("Frontal"),
    EXTERNA("Externa"),
    DESCONOCIDA("Desconocida")
}

/**
 * Resultado de la consulta de cámaras: incluye un posible
 * error para que la interfaz lo muestre sin inventar datos.
 */
data class CamerasResultado(
    val camaras: List<CameraInfo>,
    val error: String? = null
)
