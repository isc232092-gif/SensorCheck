package com.example.sensorcheck.domain.sensors

import com.example.sensorcheck.domain.model.EventoSensor
import com.example.sensorcheck.domain.model.RitmoMuestreo
import com.example.sensorcheck.domain.model.SensorReading

enum class FaseMonitoreo {
    /** Sin listener registrado. */
    INACTIVO,
    /** Listener registrado; aún no llega ningún evento. */
    ESPERANDO,
    /** Se han recibido eventos. */
    RECIBIENDO,
    /** El usuario lo inició pero la pantalla no está visible: listener liberado. */
    PAUSADO,
    /** Sensor de activación única que ya se disparó (Android lo desactivó). */
    DISPARADO,
    ERROR
}

data class EstadoMonitoreo(
    val fase: FaseMonitoreo = FaseMonitoreo.INACTIVO,
    val ultima: SensorReading? = null,
    val precision: Int? = null,
    val eventos: Long = 0,
    val muestras: SerieTemporal = SerieTemporal.VACIA,
    val error: String? = null,
    val ritmo: RitmoMuestreo = RitmoMuestreo.INTERFAZ
) {
    /** Hay un listener registrado en este momento. */
    val activo: Boolean
        get() = fase == FaseMonitoreo.ESPERANDO || fase == FaseMonitoreo.RECIBIENDO
}

/**
 * Acumula los eventos de un monitoreo (GRASP: Experto en información).
 * No conoce Android ni corrutinas; el ViewModel decide cuándo publicar.
 */
class AcumuladorLecturas(series: Int, capacidad: Int = CAPACIDAD_POR_DEFECTO) {

    private val ventana = VentanaMuestras(capacidad, series)

    var fase: FaseMonitoreo = FaseMonitoreo.INACTIVO
        private set
    private var ultima: SensorReading? = null
    private var precision: Int? = null
    private var eventos = 0L
    private var error: String? = null

    fun iniciar() {
        fase = FaseMonitoreo.ESPERANDO
        ultima = null
        precision = null
        eventos = 0
        error = null
        ventana.limpiar()
    }

    fun aplicar(evento: EventoSensor) {
        when (evento) {
            is EventoSensor.Lectura -> {
                val l = evento.lectura
                ultima = l
                precision = l.precision ?: precision
                eventos++
                ventana.agregar(l.marcaTiempoNs, l.valores)
                fase = FaseMonitoreo.RECIBIENDO
            }
            is EventoSensor.CambioPrecision -> precision = evento.precision
            is EventoSensor.Disparo -> {
                ultima = evento.lectura
                eventos++
                fase = FaseMonitoreo.DISPARADO
            }
            is EventoSensor.Error -> fallar(evento.mensaje)
        }
    }

    /** Conserva el primer mensaje: suele ser el más específico. */
    fun fallar(mensaje: String) {
        if (error == null) error = mensaje
        fase = FaseMonitoreo.ERROR
    }

    fun pausar() {
        if (fase == FaseMonitoreo.ESPERANDO || fase == FaseMonitoreo.RECIBIENDO) {
            fase = FaseMonitoreo.PAUSADO
        }
    }

    /** Tras una pausa la gráfica empieza de nuevo para no unir tramos separados en el tiempo. */
    fun reanudar() {
        if (fase == FaseMonitoreo.PAUSADO) {
            fase = FaseMonitoreo.ESPERANDO
            ventana.limpiar()
        }
    }

    fun detener() {
        if (fase != FaseMonitoreo.ERROR && fase != FaseMonitoreo.DISPARADO) {
            fase = FaseMonitoreo.INACTIVO
        }
    }

    fun estado(ritmo: RitmoMuestreo) = EstadoMonitoreo(
        fase = fase,
        ultima = ultima,
        precision = precision,
        eventos = eventos,
        muestras = ventana.instantanea(),
        error = error,
        ritmo = ritmo
    )

    companion object {
        const val CAPACIDAD_POR_DEFECTO = 240
    }
}
