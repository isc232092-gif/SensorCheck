package com.example.sensorcheck.domain.diagnostico

import com.example.sensorcheck.domain.sensors.FormatoSensor
import java.util.ArrayDeque
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

enum class FinPrueba {
    /** Se cumplieron todos los criterios. */
    COMPLETADA,
    TIEMPO_AGOTADO,
    /** El usuario afirma haber seguido las instrucciones sin respuesta del sensor. */
    USUARIO_REPORTA_FALLO
}

data class Criterio(val texto: String, val cumplido: Boolean)

/**
 * Estrategia de evaluación de una prueba guiada de sensor (GoF: Strategy).
 * Recibe lecturas reales y decide según criterios explícitos.
 */
interface EvaluadorSensor {
    val id: IdPrueba
    val eventos: Int
    val completo: Boolean
    fun registrar(valores: List<Float>, tiempoNs: Long)
    fun criterios(): List<Criterio>
    fun evaluar(fin: FinPrueba): ResultadoEvaluado
}

private fun n(v: Float) = FormatoSensor.numero(v, Locale.getDefault())

/** Detecta un intervalo de reposo: la magnitud casi no varía durante [ventanaNs]. */
class DetectorReposo(private val ventanaNs: Long, private val tolerancia: Float) {
    private val muestras = ArrayDeque<Pair<Long, Float>>()
    var media: Float? = null
        private set

    fun agregar(tiempoNs: Long, valor: Float) {
        if (media != null) return
        muestras.addLast(tiempoNs to valor)
        while (muestras.size > 1 && tiempoNs - muestras.first.first > ventanaNs) muestras.removeFirst()
        val cubre = tiempoNs - muestras.first.first >= ventanaNs * 9 / 10
        if (cubre && muestras.size >= 5) {
            val valores = muestras.map { it.second }
            if ((valores.max() - valores.min()) <= tolerancia) media = valores.average().toFloat()
        }
    }
}

/** Mínimos y máximos por eje de las lecturas recibidas. */
private class Recorrido(ejes: Int) {
    val min = FloatArray(ejes) { Float.POSITIVE_INFINITY }
    val max = FloatArray(ejes) { Float.NEGATIVE_INFINITY }
    fun agregar(v: List<Float>) = v.take(min.size).forEachIndexed { i, x ->
        if (x.isFinite()) {
            if (x < min[i]) min[i] = x
            if (x > max[i]) max[i] = x
        }
    }
    fun amplitud(i: Int) = if (max[i] >= min[i]) max[i] - min[i] else 0f
    fun picoAbs(i: Int) = if (max[i] >= min[i]) maxOf(abs(min[i]), abs(max[i])) else 0f
}

private fun magnitud(v: List<Float>) = sqrt(v.take(3).sumOf { (it * it).toDouble() }).toFloat()
private val EJES = listOf("X", "Y", "Z")

/** Base común: evidencia de eventos y resultado según el motivo de finalización. */
abstract class EvaluadorBase(final override val id: IdPrueba, private val continuo: Boolean) : EvaluadorSensor {
    final override var eventos = 0
        private set

    final override fun registrar(valores: List<Float>, tiempoNs: Long) {
        eventos++
        procesar(valores, tiempoNs)
    }

    protected abstract fun procesar(valores: List<Float>, tiempoNs: Long)
    protected abstract fun medidas(): List<String>

    /** Fallo detectado por la propia medición (p. ej. gravedad fuera de rango). */
    protected open fun falloMedido(): String? = null

    /** Con todos los criterios cumplidos, si la medición no es concluyente. */
    protected open fun dudaMedida(): String? = null

    final override fun evaluar(fin: FinPrueba): ResultadoEvaluado {
        val evidencia = mutableListOf("Eventos recibidos: $eventos")
        evidencia += medidas()
        criterios().forEach { evidencia += (if (it.cumplido) "✓ " else "✗ ") + it.texto }
        val fallo = falloMedido()
        val resultado = when {
            fallo != null -> { evidencia += fallo; ResultadoPrueba.FALLO }
            fin == FinPrueba.USUARIO_REPORTA_FALLO && !completo -> {
                evidencia += "El usuario indicó que el sensor no responde a las instrucciones"
                ResultadoPrueba.FALLO
            }
            eventos == 0 && continuo -> {
                evidencia += "Un sensor continuo no entregó ningún evento"
                ResultadoPrueba.FALLO
            }
            completo -> dudaMedida()?.let { evidencia += it; ResultadoPrueba.INDETERMINADO } ?: ResultadoPrueba.CORRECTO
            else -> {
                evidencia += "La prueba terminó sin cumplir todos los criterios"
                ResultadoPrueba.INDETERMINADO
            }
        }
        return ResultadoEvaluado(resultado, evidencia, CatalogoPruebas.recomendacion(id, resultado))
    }
}

class EvaluadorAcelerometro : EvaluadorBase(IdPrueba.ACELEROMETRO, continuo = true) {
    private val reposo = DetectorReposo(1_500_000_000L, 0.3f)
    private val recorrido = Recorrido(3)

    override fun procesar(valores: List<Float>, tiempoNs: Long) {
        if (valores.size < 3) return
        reposo.agregar(tiempoNs, magnitud(valores))
        recorrido.agregar(valores)
    }

    private fun gravedadValida() = reposo.media?.let { it in GRAVEDAD_MIN..GRAVEDAD_MAX } == true

    override fun criterios() = listOf(
        Criterio("Reposo con gravedad entre ${n(GRAVEDAD_MIN)} y ${n(GRAVEDAD_MAX)} m/s²", gravedadValida())
    ) + EJES.mapIndexed { i, e -> Criterio("Eje $e recorre al menos ${n(AMPLITUD)} m/s²", recorrido.amplitud(i) >= AMPLITUD) }

    override val completo: Boolean
        get() = reposo.media != null && (0..2).all { recorrido.amplitud(it) >= AMPLITUD }

    override fun medidas() = listOfNotNull(
        reposo.media?.let { "Gravedad medida en reposo: ${n(it)} m/s²" }
    ) + EJES.mapIndexed { i, e -> "Recorrido eje $e: ${n(recorrido.amplitud(i))} m/s²" }

    override fun falloMedido() = reposo.media?.takeIf { !gravedadValida() }
        ?.let { "La gravedad medida en reposo (${n(it)} m/s²) está fuera del rango esperado" }

    companion object {
        const val GRAVEDAD_MIN = 8.81f
        const val GRAVEDAD_MAX = 10.81f
        const val AMPLITUD = 14f
    }
}

class EvaluadorGiroscopio : EvaluadorBase(IdPrueba.GIROSCOPIO, continuo = true) {
    private val reposo = DetectorReposo(1_500_000_000L, 0.05f)
    private val recorrido = Recorrido(3)

    override fun procesar(valores: List<Float>, tiempoNs: Long) {
        if (valores.size < 3) return
        reposo.agregar(tiempoNs, magnitud(valores))
        recorrido.agregar(valores)
    }

    private fun derivaValida() = reposo.media?.let { it < DERIVA_MAX } == true

    override fun criterios() = listOf(
        Criterio("En reposo marca menos de ${n(DERIVA_MAX)} rad/s", derivaValida())
    ) + EJES.mapIndexed { i, e -> Criterio("Giro en eje $e superior a ${n(GIRO_MIN)} rad/s", recorrido.picoAbs(i) >= GIRO_MIN) }

    override val completo: Boolean
        get() = reposo.media != null && (0..2).all { recorrido.picoAbs(it) >= GIRO_MIN }

    override fun medidas() = listOfNotNull(
        reposo.media?.let { "Velocidad angular en reposo: ${n(it)} rad/s" }
    ) + EJES.mapIndexed { i, e -> "Pico eje $e: ${n(recorrido.picoAbs(i))} rad/s" }

    override fun falloMedido() = reposo.media?.takeIf { !derivaValida() }
        ?.let { "En reposo el giroscopio marca ${n(it)} rad/s (deriva excesiva)" }

    companion object {
        const val DERIVA_MAX = 0.1f
        const val GIRO_MIN = 1.5f
    }
}

class EvaluadorMagnetometro : EvaluadorBase(IdPrueba.MAGNETOMETRO, continuo = true) {
    private val recorrido = Recorrido(3)
    private var sumaCampo = 0.0
    private var muestras = 0

    override fun procesar(valores: List<Float>, tiempoNs: Long) {
        if (valores.size < 3) return
        recorrido.agregar(valores)
        sumaCampo += magnitud(valores)
        muestras++
    }

    private fun campoMedio(): Float? = if (muestras == 0) null else (sumaCampo / muestras).toFloat()

    override fun criterios() =
        EJES.mapIndexed { i, e -> Criterio("Eje $e varía al menos ${n(VARIACION)} μT al girar", recorrido.amplitud(i) >= VARIACION) }

    override val completo: Boolean get() = (0..2).all { recorrido.amplitud(it) >= VARIACION }

    override fun medidas() = listOfNotNull(campoMedio()?.let { "Campo magnético medio: ${n(it)} μT" }) +
        EJES.mapIndexed { i, e -> "Variación eje $e: ${n(recorrido.amplitud(i))} μT" }

    override fun dudaMedida() = campoMedio()?.takeIf { it !in CAMPO_MIN..CAMPO_MAX }
        ?.let { "El campo medio (${n(it)} μT) está fuera del rango terrestre: posible interferencia" }

    companion object {
        const val VARIACION = 20f
        const val CAMPO_MIN = 20f
        const val CAMPO_MAX = 80f
    }
}

/** Proximidad: un sensor cerca/lejos reporta el rango máximo en «lejos» y un valor menor en «cerca». */
class EvaluadorProximidad(private val rangoMaximo: Float) : EvaluadorBase(IdPrueba.PROXIMIDAD, continuo = false) {
    private var cerca = false
    private var lejosTrasCerca = false
    private var minimo = Float.POSITIVE_INFINITY

    override fun procesar(valores: List<Float>, tiempoNs: Long) {
        val d = valores.firstOrNull()?.takeIf { it.isFinite() } ?: return
        if (d < minimo) minimo = d
        if (d < rangoMaximo) cerca = true
        else if (cerca) lejosTrasCerca = true
    }

    override fun criterios() = listOf(
        Criterio("Informa «cerca» al cubrirlo", cerca),
        Criterio("Vuelve a «lejos» al descubrirlo", lejosTrasCerca)
    )

    override val completo: Boolean get() = cerca && lejosTrasCerca

    override fun medidas() = listOfNotNull(
        "Rango máximo declarado: ${n(rangoMaximo)} cm",
        minimo.takeIf { it.isFinite() }?.let { "Distancia mínima recibida: ${n(it)} cm" }
    )
}

class EvaluadorLuz : EvaluadorBase(IdPrueba.LUZ, continuo = false) {
    private var maximo = Float.NEGATIVE_INFINITY
    private var minimo = Float.POSITIVE_INFINITY

    override fun procesar(valores: List<Float>, tiempoNs: Long) {
        val lx = valores.firstOrNull()?.takeIf { it.isFinite() } ?: return
        if (lx > maximo) maximo = lx
        if (lx < minimo) minimo = lx
    }

    private fun luzSuficiente() = maximo >= LUZ_MIN
    private fun oscurecido() = luzSuficiente() && minimo <= maximo * FRACCION_OSCURA

    override fun criterios() = listOf(
        Criterio("Mide al menos ${n(LUZ_MIN)} lx con luz", luzSuficiente()),
        Criterio("Al cubrirlo baja al ${(FRACCION_OSCURA * 100).toInt()} % o menos", oscurecido())
    )

    override val completo: Boolean get() = oscurecido()

    override fun medidas() = listOfNotNull(
        maximo.takeIf { it.isFinite() }?.let { "Iluminancia máxima: ${n(it)} lx" },
        minimo.takeIf { it.isFinite() }?.let { "Iluminancia mínima: ${n(it)} lx" }
    )

    companion object {
        const val LUZ_MIN = 10f
        const val FRACCION_OSCURA = 0.3f
    }
}
