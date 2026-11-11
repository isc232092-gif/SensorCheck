package com.example.sensorcheck.domain.model

/** Una lectura real recibida de un sensor. */
data class SensorReading(
    val valores: List<Float>,
    /** SensorEvent.timestamp (ns, base de tiempo del sensor). */
    val marcaTiempoNs: Long,
    /** Hora de reloj estimada a partir de la marca del evento. */
    val horaEpochMs: Long,
    val precision: Int?
)

/** Eventos emitidos durante el monitoreo de un sensor. */
sealed interface EventoSensor {
    data class Lectura(val lectura: SensorReading) : EventoSensor
    data class CambioPrecision(val precision: Int) : EventoSensor
    /** Sensor de activación única (TriggerEventListener). */
    data class Disparo(val lectura: SensorReading) : EventoSensor
    data class Error(val mensaje: String) : EventoSensor
}

/** Frecuencias de muestreo moderadas (sin SENSOR_DELAY_FASTEST). */
enum class RitmoMuestreo(val periodoUs: Int, val etiqueta: String) {
    NORMAL(200_000, "~5 Hz"),
    INTERFAZ(60_000, "~16 Hz"),
    JUEGO(20_000, "~50 Hz")
}
