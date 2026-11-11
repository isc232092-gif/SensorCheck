package com.example.sensorcheck.domain.diagnostico

import java.util.Locale
import kotlin.math.log10
import kotlin.math.sqrt

private fun res(id: IdPrueba, r: ResultadoPrueba, evidencia: List<String>) =
    ResultadoEvaluado(r, evidencia, CatalogoPruebas.recomendacion(id, r))

private fun dec(v: Double, d: Int = 1) = String.format(Locale.getDefault(), "%.${d}f", v)

/**
 * Pruebas por conteo (altavoz, vibración, flash): se emite un número
 * aleatorio de estímulos y el usuario indica cuántos percibió, de modo
 * que no basta con pulsar «sí».
 */
object EvaluadorConteo {
    fun evaluar(
        id: IdPrueba,
        estimulo: String,
        emitidos: Int,
        respuesta: Int,
        evidenciaExtra: List<String> = emptyList(),
        /** Motivo por el que el estímulo no salió por el componente evaluado. */
        componenteIncorrecto: String? = null
    ): ResultadoEvaluado {
        val evidencia = listOf("$estimulo emitidos: $emitidos", "$estimulo percibidos según el usuario: $respuesta") +
            evidenciaExtra
        return when {
            componenteIncorrecto != null -> res(id, ResultadoPrueba.INDETERMINADO, evidencia + componenteIncorrecto)
            respuesta == emitidos -> res(id, ResultadoPrueba.CORRECTO, evidencia)
            respuesta == 0 -> res(id, ResultadoPrueba.FALLO, evidencia + "El usuario no percibió ningún estímulo")
            else -> res(id, ResultadoPrueba.INDETERMINADO, evidencia + "El número no coincide: puede ser un error de conteo")
        }
    }
}

/** Utilidades de nivel de audio. */
object NivelAudio {
    /** Nivel RMS en dBFS de muestras PCM de 16 bits; −120 para silencio digital. */
    fun dbfs(muestras: ShortArray, desde: Int = 0, hasta: Int = muestras.size): Double {
        if (hasta <= desde) return -120.0
        var suma = 0.0
        for (i in desde until hasta) {
            val v = muestras[i] / 32768.0
            suma += v * v
        }
        val rms = sqrt(suma / (hasta - desde))
        return if (rms <= 1e-6) -120.0 else 20 * log10(rms)
    }

    /** Niveles por bloques consecutivos de [tamanoBloque] muestras. */
    fun niveles(muestras: ShortArray, tamanoBloque: Int): List<Double> =
        (0 until muestras.size / tamanoBloque).map { dbfs(muestras, it * tamanoBloque, (it + 1) * tamanoBloque) }
}

object EvaluadorMicrofono {
    const val SUBIDA_MIN_DB = 15.0
    const val PICO_MIN_DBFS = -45.0

    /**
     * @param niveles niveles por bloque de la grabación completa.
     * @param bloquesSilencio cuántos bloques iniciales corresponden al silencio pedido.
     */
    fun evaluar(
        niveles: List<Double>,
        bloquesSilencio: Int,
        senalNula: Boolean,
        usuarioReportaFallo: Boolean,
        escuchoReproduccion: Boolean?
    ): ResultadoEvaluado {
        val id = IdPrueba.MICROFONO
        val silencio = niveles.take(bloquesSilencio).sorted().let { if (it.isEmpty()) -120.0 else it[it.size / 2] }
        val pico = niveles.drop(bloquesSilencio).maxOrNull() ?: -120.0
        val evidencia = mutableListOf(
            "Nivel en silencio (mediana): ${dec(silencio)} dBFS",
            "Pico al hablar: ${dec(pico)} dBFS",
            "Subida: ${dec(pico - silencio)} dB (mínimo ${dec(SUBIDA_MIN_DB, 0)} dB)"
        )
        when (escuchoReproduccion) {
            true -> evidencia += "El usuario escuchó la reproducción de la grabación"
            false -> evidencia += "El usuario no escuchó la reproducción (revisa también el altavoz)"
            null -> Unit
        }
        val cumple = pico - silencio >= SUBIDA_MIN_DB && pico >= PICO_MIN_DBFS
        return when {
            senalNula -> res(
                id, ResultadoPrueba.INDETERMINADO,
                evidencia + "Señal digital nula: Android puede bloquear el micrófono si otra app lo usa o por privacidad"
            )
            cumple -> res(id, ResultadoPrueba.CORRECTO, evidencia)
            usuarioReportaFallo -> res(id, ResultadoPrueba.FALLO, evidencia + "El usuario habló y el nivel no subió lo suficiente")
            else -> res(id, ResultadoPrueba.INDETERMINADO, evidencia + "El nivel no subió lo suficiente")
        }
    }
}

/** Datos de la foto de prueba (solo en memoria). */
data class CapturaCamara(val ancho: Int, val alto: Int, val bytes: Int, val luminanciaMedia: Double, val luminanciaDesviacion: Double)

object EvaluadorCamara {
    /** Desviación de luminancia (0–255) por debajo de la cual la imagen es casi uniforme. */
    const val DESVIACION_MIN = 4.0

    fun evaluar(
        id: IdPrueba,
        idCamara: String,
        cuadrosVistaPrevia: Int,
        error: String?,
        enUso: Boolean,
        captura: CapturaCamara?,
        respuesta: RespuestaVisual?
    ): ResultadoEvaluado {
        val evidencia = mutableListOf("Cámara Camera2 id $idCamara", "Cuadros de vista previa recibidos: $cuadrosVistaPrevia")
        captura?.let {
            evidencia += "Foto capturada: ${it.ancho} × ${it.alto} px, ${it.bytes / 1024} KB (no guardada)"
            evidencia += "Luminancia media ${dec(it.luminanciaMedia)} · desviación ${dec(it.luminanciaDesviacion)}"
        }
        error?.let { evidencia += "Error: $it" }
        respuesta?.let {
            evidencia += "Respuesta del usuario: " + when (it) {
                RespuestaVisual.SIN_DEFECTOS -> "la foto se ve correcta"
                RespuestaVisual.CON_DEFECTOS -> "la foto tiene defectos"
                RespuestaVisual.NO_SEGURO -> "no está seguro"
            }
        }
        val uniforme = captura != null && captura.luminanciaDesviacion < DESVIACION_MIN
        return when {
            enUso -> res(id, ResultadoPrueba.INDETERMINADO, evidencia + "La cámara está en uso por otra app o por el sistema")
            error != null -> res(id, ResultadoPrueba.FALLO, evidencia)
            cuadrosVistaPrevia == 0 -> res(id, ResultadoPrueba.FALLO, evidencia + "La cámara no entregó imagen de vista previa")
            captura == null || respuesta == null -> res(id, ResultadoPrueba.INDETERMINADO, evidencia + "No se completó la captura")
            respuesta == RespuestaVisual.CON_DEFECTOS -> res(id, ResultadoPrueba.FALLO, evidencia)
            respuesta == RespuestaVisual.NO_SEGURO -> res(id, ResultadoPrueba.INDETERMINADO, evidencia)
            uniforme -> res(id, ResultadoPrueba.INDETERMINADO, evidencia + "La imagen es casi uniforme: ¿lente tapada o escena sin detalle?")
            else -> res(id, ResultadoPrueba.CORRECTO, evidencia)
        }
    }
}

/** Lectura del controlador de batería (Intent ACTION_BATTERY_CHANGED). */
data class LecturaBateria(
    val nivelPorcentaje: Int?,
    /** BatteryManager.BATTERY_HEALTH_*. */
    val salud: Int,
    val temperaturaC: Float?,
    val voltajeMv: Int?,
    val conectada: Boolean
)

object EvaluadorBateria {
    const val SALUD_DESCONOCIDA = 1
    const val SALUD_BUENA = 2
    private val SALUD_MALA = setOf(3, 4, 5, 6, 7) // sobrecalentada, muerta, sobretensión, fallo, fría

    fun nombreSalud(s: Int) = when (s) {
        2 -> "Buena"; 3 -> "Sobrecalentada"; 4 -> "Agotada"; 5 -> "Sobretensión"
        6 -> "Fallo no especificado"; 7 -> "Fría"; else -> "Desconocida"
    }

    /** Algunos fabricantes informan voltios en lugar de milivoltios. */
    fun normalizarMv(v: Int?): Int? = when {
        v == null || v <= 0 -> null
        v < 100 -> v * 1000
        else -> v
    }

    /** @param cargaDetectada null = el usuario no probó el cargador. */
    fun evaluar(l: LecturaBateria, cargaDetectada: Boolean?): ResultadoEvaluado {
        val id = IdPrueba.BATERIA
        val mv = normalizarMv(l.voltajeMv)
        val evidencia = mutableListOf("Salud informada por el controlador: ${nombreSalud(l.salud)}")
        l.nivelPorcentaje?.let { evidencia += "Nivel: $it %" }
        l.temperaturaC?.let { evidencia += "Temperatura: ${dec(it.toDouble())} °C" }
        mv?.let { evidencia += "Voltaje: ${dec(it / 1000.0, 2)} V" }
        evidencia += when (cargaDetectada) {
            true -> "Carga detectada al conectar el cargador"
            false -> "No se detectó carga tras conectar el cargador"
            null -> "No se comprobó la carga"
        }
        val temperaturaOk = l.temperaturaC?.let { it in 0f..45f }
        val voltajeOk = mv?.let { it in 3000..4600 }
        return when {
            l.salud in SALUD_MALA -> res(id, ResultadoPrueba.FALLO, evidencia)
            cargaDetectada == false -> res(id, ResultadoPrueba.FALLO, evidencia)
            temperaturaOk == false || voltajeOk == false ->
                res(id, ResultadoPrueba.INDETERMINADO, evidencia + "Temperatura o voltaje fuera del rango habitual")
            l.salud != SALUD_BUENA || temperaturaOk == null || voltajeOk == null || cargaDetectada == null ->
                res(id, ResultadoPrueba.INDETERMINADO, evidencia + "Faltan datos para completar la comprobación")
            else -> res(id, ResultadoPrueba.CORRECTO, evidencia)
        }
    }
}

/** Resultado de escribir y releer un archivo temporal. */
data class MedicionAlmacenamiento(
    val bytes: Long,
    val msEscritura: Long,
    val msLectura: Long,
    val integridadCorrecta: Boolean?,
    val espacioInsuficiente: Boolean = false,
    val error: String? = null
)

object EvaluadorAlmacenamiento {
    fun mbPorSegundo(bytes: Long, ms: Long): Double = if (ms <= 0) 0.0 else bytes / 1_048_576.0 / (ms / 1000.0)

    fun evaluar(m: MedicionAlmacenamiento): ResultadoEvaluado {
        val id = IdPrueba.ALMACENAMIENTO
        val evidencia = mutableListOf("Archivo temporal: ${m.bytes / 1_048_576} MB")
        if (m.integridadCorrecta != null) {
            evidencia += "Escritura (con sincronización): ${dec(mbPorSegundo(m.bytes, m.msEscritura))} MB/s"
            evidencia += "Lectura: ${dec(mbPorSegundo(m.bytes, m.msLectura))} MB/s (puede incluir caché del sistema)"
            evidencia += "Integridad SHA-256: " + if (m.integridadCorrecta) "coincide" else "NO coincide"
        }
        m.error?.let { evidencia += "Error: $it" }
        return when {
            m.espacioInsuficiente -> res(id, ResultadoPrueba.INDETERMINADO, evidencia + "Espacio libre insuficiente para la prueba")
            m.integridadCorrecta == true -> res(id, ResultadoPrueba.CORRECTO, evidencia)
            else -> res(id, ResultadoPrueba.FALLO, evidencia)
        }
    }
}

object EvaluadorRam {
    fun evaluar(
        mbVerificados: Int,
        errores: Long,
        sinMemoria: Boolean,
        totalMb: Long?,
        disponibleMb: Long?
    ): ResultadoEvaluado {
        val id = IdPrueba.RAM
        val evidencia = mutableListOf<String>()
        totalMb?.let { evidencia += "RAM total informada: $it MB" }
        disponibleMb?.let { evidencia += "RAM disponible al iniciar: $it MB" }
        evidencia += "Bloque verificado: $mbVerificados MB (solo la memoria asignada a la app)"
        evidencia += "Palabras con error: $errores"
        return when {
            errores > 0 -> res(id, ResultadoPrueba.FALLO, evidencia)
            sinMemoria && mbVerificados == 0 -> res(id, ResultadoPrueba.INDETERMINADO, evidencia + "Android no concedió memoria para la prueba")
            mbVerificados > 0 -> res(id, ResultadoPrueba.CORRECTO, evidencia)
            else -> res(id, ResultadoPrueba.INDETERMINADO, evidencia)
        }
    }
}

object EvaluadorBotones {
    fun evaluar(subir: Int, bajar: Int, usuarioReportaFallo: Boolean): ResultadoEvaluado {
        val id = IdPrueba.BOTONES
        val evidencia = listOf(
            "Pulsaciones de subir volumen: $subir",
            "Pulsaciones de bajar volumen: $bajar",
            "Encendido e inicio no son accesibles para las apps"
        )
        return when {
            subir > 0 && bajar > 0 -> res(id, ResultadoPrueba.CORRECTO, evidencia)
            usuarioReportaFallo -> res(id, ResultadoPrueba.FALLO, evidencia + "El usuario pulsó y no se recibió la pulsación")
            else -> res(id, ResultadoPrueba.INDETERMINADO, evidencia)
        }
    }
}

/** Estado de una radio de conectividad observado durante la prueba. */
object EvaluadorConectividad {

    fun wifi(activado: Boolean, conectado: Boolean, validado: Boolean, rssiDbm: Int?, usuarioReportaFallo: Boolean): ResultadoEvaluado {
        val id = IdPrueba.WIFI
        val evidencia = mutableListOf(
            "Wi-Fi activado: ${siNo(activado)}",
            "Conectado a una red Wi-Fi: ${siNo(conectado)}"
        )
        if (conectado) evidencia += "Acceso a internet validado por Android: ${siNo(validado)}"
        rssiDbm?.let { evidencia += "Intensidad de señal: $it dBm" }
        return when {
            conectado -> res(id, ResultadoPrueba.CORRECTO, evidencia)
            usuarioReportaFallo -> res(id, ResultadoPrueba.FALLO, evidencia + "El usuario no logró conectarse a una red conocida")
            else -> res(id, ResultadoPrueba.INDETERMINADO, evidencia)
        }
    }

    fun bluetooth(activado: Boolean, dispositivos: Int, errorEscaneo: String?, usuarioReportaFallo: Boolean): ResultadoEvaluado {
        val id = IdPrueba.BLUETOOTH
        val evidencia = mutableListOf("Bluetooth activado: ${siNo(activado)}", "Dispositivos encontrados: $dispositivos")
        errorEscaneo?.let { evidencia += "Error de escaneo: $it" }
        return when {
            dispositivos > 0 -> res(id, ResultadoPrueba.CORRECTO, evidencia)
            usuarioReportaFallo -> res(id, ResultadoPrueba.FALLO, evidencia + "El usuario confirma que había dispositivos Bluetooth cerca")
            else -> res(id, ResultadoPrueba.INDETERMINADO, evidencia)
        }
    }

    fun gps(
        activado: Boolean,
        satelitesVistos: Int,
        satelitesEnUso: Int,
        posicionObtenida: Boolean,
        precisionM: Float?,
        usuarioReportaFallo: Boolean
    ): ResultadoEvaluado {
        val id = IdPrueba.GPS
        val evidencia = mutableListOf(
            "Ubicación activada: ${siNo(activado)}",
            "Satélites visibles (máximo): $satelitesVistos",
            "Satélites usados en la posición (máximo): $satelitesEnUso"
        )
        if (posicionObtenida) {
            evidencia += "Posición GPS obtenida (no se guarda)" +
                (precisionM?.let { ", precisión ${dec(it.toDouble())} m" } ?: ", precisión no informada")
        }
        return when {
            posicionObtenida -> res(id, ResultadoPrueba.CORRECTO, evidencia)
            usuarioReportaFallo -> res(id, ResultadoPrueba.FALLO, evidencia + "El usuario estaba al aire libre y no se obtuvo posición")
            satelitesVistos > 0 -> res(id, ResultadoPrueba.INDETERMINADO, evidencia + "Ve satélites pero no calculó posición: prueba al aire libre")
            else -> res(id, ResultadoPrueba.INDETERMINADO, evidencia)
        }
    }

    private fun siNo(b: Boolean) = if (b) "sí" else "no"
}
