package com.example.sensorcheck.domain.model

/**
 * Características de un sensor tal como las reporta el fabricante
 * a través de android.hardware.Sensor. No son mediciones.
 */
data class SensorInfo(
    /** Clave estable dentro de la sesión (tipo, nombre, fabricante, versión). */
    val clave: String,
    val nombre: String,
    val tipo: Int,
    val stringType: String?,
    val fabricante: String?,
    val version: Int,
    val resolucion: Float,
    val rangoMaximo: Float,
    /** Consumo reportado en mA. */
    val consumoMa: Float,
    /** Retardo mínimo en µs; 0 = solo informa al cambiar el valor. */
    val retardoMinimoUs: Int,
    /** Retardo máximo en µs; 0 o negativo = no reportado. */
    val retardoMaximoUs: Int,
    val modoReporte: ModoReporte,
    val despiertaDispositivo: Boolean,
    val fifoMaximo: Int,
    val fifoReservado: Int,
    val dinamico: Boolean,
    /** Sensor.getId(): 0 o -1 si el fabricante no lo define. */
    val idSensor: Int?,
    val permisoRequerido: String?
)

enum class ModoReporte {
    CONTINUO,
    AL_CAMBIO,
    UNICO,
    ESPECIAL,
    DESCONOCIDO
}

enum class CategoriaSensor(val titulo: String) {
    MOVIMIENTO("Movimiento"),
    AMBIENTAL("Ambientales"),
    POSICION("Posición"),
    OTROS("Otros")
}
