
package com.example.sensorcheck.domain.model

data class HardwareInfo(
    val ramTotalBytes: Long,
    val ramDisponibleBytes: Long,
    val almacenamientoTotalBytes: Long,
    val almacenamientoDisponibleBytes: Long,
    val bateriaPorcentaje: Int?,
    val bateriaTemperaturaC: Float?,
    val bateriaVoltajeMv: Int?
)
