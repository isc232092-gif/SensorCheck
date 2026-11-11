package com.example.sensorcheck.domain.format

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

/**
 * Transformaciones de datos del sistema a texto legible.
 * Kotlin puro (sin Android) para poder probarse con JUnit.
 */
object FormatoSistema {

    private const val SEG = 1_000L
    private const val MIN = 60 * SEG
    private const val HORA = 60 * MIN
    private const val DIA = 24 * HORA

    /**
     * Tiempo desde el arranque. Los segundos solo se muestran
     * cuando el tiempo es menor de un día.
     */
    fun tiempoActividad(ms: Long): String {
        if (ms < 0) return "No disponible"
        val dias = ms / DIA
        val horas = (ms % DIA) / HORA
        val minutos = (ms % HORA) / MIN
        val segundos = (ms % MIN) / SEG
        return buildList {
            if (dias > 0) add("$dias d")
            if (dias > 0 || horas > 0) add("$horas h")
            if (dias > 0 || horas > 0 || minutos > 0) add("$minutos min")
            if (dias == 0L) add("$segundos s")
        }.joinToString(" ")
    }

    /** Desfase horario, p. ej. "UTC-06:00" o "UTC+05:30". */
    fun desfaseUtc(desfaseMs: Int): String {
        val signo = if (desfaseMs < 0) "-" else "+"
        val totalMin = abs(desfaseMs) / 60_000
        return String.format(Locale.ROOT, "UTC%s%02d:%02d", signo, totalMin / 60, totalMin % 60)
    }

    /** Primera letra en mayúscula según el idioma ("español" -> "Español"). */
    fun capitalizar(texto: String?, locale: Locale): String? {
        if (texto.isNullOrBlank()) return null
        return texto.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    /** Convierte "AAAA-MM-DD" en una fecha legible; null si no es válida. */
    fun fechaParche(parche: String?, locale: Locale): String? =
        parsearParche(parche)?.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)
        )

    fun parsearParche(parche: String?): LocalDate? {
        if (parche.isNullOrBlank()) return null
        return try {
            LocalDate.parse(parche.trim())
        } catch (e: DateTimeParseException) {
            null
        }
    }

    /** Meses completos desde el parche; null si la fecha no es válida o es futura. */
    fun mesesDesdeParche(parche: String?, hoy: LocalDate): Long? {
        val fecha = parsearParche(parche) ?: return null
        if (fecha.isAfter(hoy)) return null
        return ChronoUnit.MONTHS.between(fecha, hoy)
    }

    fun antiguedad(meses: Long?): String? = when {
        meses == null -> null
        meses < 1 -> "Menos de 1 mes"
        meses < 12 -> if (meses == 1L) "1 mes" else "$meses meses"
        else -> {
            val anios = meses / 12
            val resto = meses % 12
            val txtAnios = if (anios == 1L) "1 año" else "$anios años"
            when (resto) {
                0L -> txtAnios
                1L -> "$txtAnios y 1 mes"
                else -> "$txtAnios y $resto meses"
            }
        }
    }

    fun tipoCompilacion(tipo: String?): String? = when (tipo?.trim()) {
        null, "" -> null
        "user" -> "user (producción)"
        "userdebug" -> "userdebug (depuración)"
        "eng" -> "eng (ingeniería)"
        else -> tipo
    }

    /**
     * Si la compilación del sistema es depurable según Build.TYPE.
     * null cuando el tipo no es uno de los estándar de AOSP.
     */
    fun compilacionDepurable(tipo: String?): Boolean? = when (tipo?.trim()) {
        "user" -> false
        "userdebug", "eng" -> true
        else -> null
    }

    /** Interpreta Build.TAGS (claves con las que se firmó el sistema). */
    fun clavesFirma(etiquetas: String?): String? {
        if (etiquetas.isNullOrBlank()) return null
        val lista = etiquetas.split(',').map { it.trim() }
        return when {
            "release-keys" in lista -> "Claves de lanzamiento (release-keys)"
            "test-keys" in lista -> "Claves de prueba (test-keys)"
            "dev-keys" in lista -> "Claves de desarrollo (dev-keys)"
            else -> etiquetas
        }
    }

    /** Versión desde /proc/version ("Linux version X ..."), como alternativa. */
    fun versionKernelDesdeProc(procVersion: String?): String? =
        procVersion?.let { Regex("""Linux version (\S+)""").find(it)?.groupValues?.get(1) }

    /** Identificador de compilación del kernel ("#1 SMP PREEMPT ..."). */
    fun identificadorKernel(procVersion: String?): String? =
        procVersion?.let { Regex("""#\d+.*$""").find(it.trim())?.value?.trim() }

    fun fechaHora(epochMs: Long, locale: Locale, zona: ZoneId): String =
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale)
            .format(Instant.ofEpochMilli(epochMs).atZone(zona))

    fun siNo(valor: Boolean?): String? = valor?.let { if (it) "Sí" else "No" }

    fun lista(valores: List<String>): String? =
        valores.filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.joinToString(", ")
}
