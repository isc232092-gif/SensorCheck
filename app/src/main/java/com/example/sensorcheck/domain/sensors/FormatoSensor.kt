package com.example.sensorcheck.domain.sensors

import com.example.sensorcheck.domain.model.ModoReporte
import java.math.BigDecimal
import java.math.MathContext
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/** Valor de una lectura listo para mostrar. [unidad] null = sin unidad conocida. */
data class ValorMostrado(val nombre: String, val valor: Float, val unidad: String?)

/** Formato de características y lecturas de sensores (lógica pura, sin Android). */
object FormatoSensor {

    /** Constantes SensorManager.SENSOR_STATUS_* (verificadas en pruebas). */
    const val PRECISION_SIN_CONTACTO = -1
    const val PRECISION_NO_FIABLE = 0
    const val PRECISION_BAJA = 1
    const val PRECISION_MEDIA = 2
    const val PRECISION_ALTA = 3

    /** Cuatro cifras significativas, sin notación científica. */
    fun numero(valor: Float, locale: Locale = Locale.getDefault()): String {
        if (valor.isNaN() || valor.isInfinite()) return "—"
        if (valor == 0f) return "0"
        val texto = if (abs(valor) >= 1000f) {
            BigDecimal(valor.toString()).setScale(1, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString()
        } else {
            BigDecimal(valor.toString()).round(MathContext(4)).stripTrailingZeros().toPlainString()
        }
        val separador = DecimalFormatSymbols.getInstance(locale).decimalSeparator
        return if (separador == '.') texto else texto.replace('.', separador)
    }

    fun conUnidad(valor: Float, unidad: String?, locale: Locale = Locale.getDefault()): String =
        if (unidad == null) numero(valor, locale) else "${numero(valor, locale)} $unidad"

    fun modoReporte(modo: ModoReporte): String = when (modo) {
        ModoReporte.CONTINUO -> "Continuo"
        ModoReporte.AL_CAMBIO -> "Al cambiar el valor"
        ModoReporte.UNICO -> "Activación única"
        ModoReporte.ESPECIAL -> "Disparo especial"
        ModoReporte.DESCONOCIDO -> "No reportado"
    }

    fun activacion(modo: ModoReporte): String = when (modo) {
        ModoReporte.CONTINUO -> "Requiere un listener activo; entrega eventos a ritmo constante"
        ModoReporte.AL_CAMBIO -> "Requiere un listener activo; solo emite cuando el valor cambia"
        ModoReporte.UNICO -> "Se desactiva automáticamente tras el primer evento"
        ModoReporte.ESPECIAL -> "Requiere un listener activo; emite ante eventos concretos"
        ModoReporte.DESCONOCIDO -> "No reportado"
    }

    fun retardoMinimo(us: Int, modo: ModoReporte, locale: Locale = Locale.getDefault()): String = when {
        modo == ModoReporte.UNICO -> "No aplica (activación única)"
        us < 0 -> "No reportado"
        us == 0 -> "0 (informa solo al cambiar)"
        else -> "${duracion(us, locale)} · máx. ≈ ${numero(1_000_000f / us, locale)} Hz"
    }

    fun retardoMaximo(us: Int, locale: Locale = Locale.getDefault()): String =
        if (us <= 0) "No reportado"
        else "${duracion(us, locale)} · mín. ≈ ${numero(1_000_000f / us, locale)} Hz"

    private fun duracion(us: Int, locale: Locale): String =
        if (us < 1000) "$us µs" else "${numero(us / 1000f, locale)} ms"

    fun precision(valor: Int?): String? = when (valor) {
        null -> null
        PRECISION_SIN_CONTACTO -> "Sin contacto"
        PRECISION_NO_FIABLE -> "No fiable"
        PRECISION_BAJA -> "Baja"
        PRECISION_MEDIA -> "Media"
        PRECISION_ALTA -> "Alta"
        else -> "Código $valor"
    }

    fun fifo(maximo: Int, reservado: Int): String =
        if (maximo <= 0) "Sin cola por lotes" else "Máximo $maximo eventos · reservado $reservado"

    fun consumo(mA: Float, locale: Locale = Locale.getDefault()): String =
        if (mA <= 0f) "No reportado" else "${numero(mA, locale)} mA"

    /** Unidad de la característica (resolución, rango): la del primer valor. */
    fun unidadPrincipal(descriptor: DescriptorSensor?): String? = descriptor?.ejes?.firstOrNull()?.unidad

    /**
     * Unidades de los valores del evento agrupando ejes consecutivos con la
     * misma unidad, p. ej. «X, Y, Z: m/s²». null = tipo sin documentación.
     */
    fun unidades(descriptor: DescriptorSensor?): String? {
        val ejes = descriptor?.ejes?.takeIf { it.isNotEmpty() } ?: return null
        val grupos = mutableListOf<Pair<MutableList<String>, String?>>()
        ejes.forEach { eje ->
            val ultimo = grupos.lastOrNull()
            if (ultimo != null && ultimo.second == eje.unidad) ultimo.first += eje.nombre
            else grupos += mutableListOf(eje.nombre) to eje.unidad
        }
        return grupos.joinToString(" · ") { (nombres, unidad) ->
            "${nombres.joinToString()}: ${unidad ?: "adimensional"}"
        }
    }

    /**
     * Asocia cada valor recibido con su significado. Si el tipo no está
     * catalogado o envía más valores de los documentados, se muestran como
     * «Valor N» sin unidad: no se inventan magnitudes.
     */
    fun valores(descriptor: DescriptorSensor?, valores: List<Float>): List<ValorMostrado> =
        valores.mapIndexed { i, v ->
            val eje = descriptor?.ejes?.getOrNull(i)
            ValorMostrado(eje?.nombre ?: "Valor ${i + 1}", v, eje?.unidad)
        }

    /**
     * Interpretación binaria de proximidad: Android indica que los sensores
     * cerca/lejos reportan el rango máximo en «lejos» y un valor menor en «cerca».
     */
    fun proximidad(distancia: Float, rangoMaximo: Float): String? = when {
        rangoMaximo <= 0f || distancia.isNaN() -> null
        distancia >= rangoMaximo -> "Lejos"
        else -> "Cerca"
    }

    fun frecuencia(hz: Float?, locale: Locale = Locale.getDefault()): String? =
        hz?.takeIf { it.isFinite() && it > 0f }?.let { "≈ ${numero(it, locale)} Hz" }

    /** Segundos transcurridos en texto corto. */
    fun antiguedad(ms: Long): String = when {
        ms < 1_000 -> "ahora"
        ms < 60_000 -> "hace ${ms / 1_000} s"
        else -> "hace ${ms / 60_000} min"
    }
}
