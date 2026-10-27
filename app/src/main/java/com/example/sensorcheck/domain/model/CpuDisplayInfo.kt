
package com.example.sensorcheck.domain.model

data class CpuDisplayInfo(
    val procesadoresDisponibles: Int,
    val arquitecturas: List<String>,
    val hardware: String,
    val resolucion: String,
    val densidadDpi: Int,
    val frecuenciaActualHz: Float?,
    val frecuenciasSoportadasHz: List<Float>
)
