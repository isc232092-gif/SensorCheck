package com.example.sensorcheck.domain.sensors

import kotlin.math.abs
import kotlin.math.max

/**
 * Copia inmutable de las muestras recientes. Los valores ausentes o no
 * finitos se guardan como NaN y la gráfica corta la línea en ese punto.
 */
class SerieTemporal(
    val tiemposNs: LongArray,
    val series: List<FloatArray>
) {
    val tamano: Int get() = tiemposNs.size

    /** Duración cubierta por la ventana en segundos. */
    val duracionS: Float
        get() = if (tamano < 2) 0f else (tiemposNs.last() - tiemposNs.first()) / 1e9f

    /** Frecuencia media de eventos dentro de la ventana. */
    val frecuenciaHz: Float?
        get() = duracionS.takeIf { it > 0f }?.let { (tamano - 1) / it }

    /** Mínimo y máximo de los valores finitos; null si no hay ninguno. */
    fun extremos(): Pair<Float, Float>? {
        var min = Float.POSITIVE_INFINITY
        var max = Float.NEGATIVE_INFINITY
        series.forEach { serie ->
            serie.forEach { v ->
                if (v.isFinite()) {
                    if (v < min) min = v
                    if (v > max) max = v
                }
            }
        }
        return if (min <= max) min to max else null
    }

    /** Rango vertical con margen; evita un rango nulo cuando el valor es constante. */
    fun rangoVisible(): Pair<Float, Float>? {
        val (min, max) = extremos() ?: return null
        val amplitud = max - min
        val margen = if (amplitud > 0f) amplitud * 0.1f else max(abs(max) * 0.1f, 1f)
        return (min - margen) to (max + margen)
    }

    /** Índice de la muestra más cercana al instante indicado. */
    fun indiceCercano(tNs: Long): Int? {
        if (tamano == 0) return null
        var mejor = 0
        var distancia = Long.MAX_VALUE
        for (i in 0 until tamano) {
            val d = abs(tiemposNs[i] - tNs)
            if (d < distancia) {
                distancia = d
                mejor = i
            }
        }
        return mejor
    }

    companion object {
        val VACIA = SerieTemporal(LongArray(0), emptyList())
    }
}

/**
 * Búfer circular de tamaño fijo: guarda solo las últimas [capacidad]
 * muestras para no acumular memoria durante el monitoreo.
 */
class VentanaMuestras(
    val capacidad: Int,
    val series: Int
) {
    init {
        require(capacidad > 0) { "capacidad debe ser positiva" }
        require(series >= 0) { "series no puede ser negativo" }
    }

    private val tiempos = LongArray(capacidad)
    private val valores = Array(series) { FloatArray(capacidad) }
    private var inicio = 0
    var tamano = 0
        private set

    fun agregar(tiempoNs: Long, lectura: List<Float>) {
        val pos = (inicio + tamano) % capacidad
        tiempos[pos] = tiempoNs
        for (s in 0 until series) {
            valores[s][pos] = lectura.getOrNull(s)?.takeIf { it.isFinite() } ?: Float.NaN
        }
        if (tamano < capacidad) tamano++ else inicio = (inicio + 1) % capacidad
    }

    fun limpiar() {
        inicio = 0
        tamano = 0
    }

    fun instantanea(): SerieTemporal {
        if (tamano == 0) return SerieTemporal.VACIA
        val t = LongArray(tamano) { tiempos[(inicio + it) % capacidad] }
        val v = List(series) { s -> FloatArray(tamano) { valores[s][(inicio + it) % capacidad] } }
        return SerieTemporal(t, v)
    }
}
