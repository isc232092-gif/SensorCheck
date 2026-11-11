package com.example.sensorcheck.ui.sensors

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sensorcheck.data.sensors.AndroidSensorRepository
import com.example.sensorcheck.domain.model.EventoSensor
import com.example.sensorcheck.domain.model.RitmoMuestreo
import com.example.sensorcheck.domain.repository.SensorRepository
import com.example.sensorcheck.domain.sensors.AcumuladorLecturas
import com.example.sensorcheck.domain.sensors.ElementoSensor
import com.example.sensorcheck.domain.sensors.EstadoMonitoreo
import com.example.sensorcheck.domain.sensors.FaseMonitoreo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface EstadoFicha {
    data object Cargando : EstadoFicha
    data class Lista(val elemento: ElementoSensor) : EstadoFicha
    data object NoEncontrado : EstadoFicha
}

/**
 * Controlador de la ficha de un sensor (GRASP: Controlador).
 * Registra como máximo un listener, solo mientras el usuario lo pidió
 * y la pantalla está visible; el Flow del repositorio lo libera al cancelarse.
 */
class SensorDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio: SensorRepository = AndroidSensorRepository(application)

    private val _ficha = MutableStateFlow<EstadoFicha>(EstadoFicha.Cargando)
    val ficha: StateFlow<EstadoFicha> = _ficha.asStateFlow()

    private val _monitoreo = MutableStateFlow(EstadoMonitoreo())
    val monitoreo: StateFlow<EstadoMonitoreo> = _monitoreo.asStateFlow()

    private var clave: String? = null
    private var acumulador = AcumuladorLecturas(series = 0)
    private var ritmo = RitmoMuestreo.INTERFAZ
    private var trabajo: Job? = null
    private var carga: Job? = null

    /** El usuario pulsó «Iniciar monitoreo» y no lo ha detenido. */
    private var solicitado = false
    private var visible = false

    fun abrir(nuevaClave: String) {
        if (nuevaClave == clave) return
        detener()
        clave = nuevaClave
        acumulador = AcumuladorLecturas(series = 0)
        _ficha.value = EstadoFicha.Cargando
        publicar()
        carga?.cancel()
        carga = viewModelScope.launch {
            val info = try {
                withContext(Dispatchers.Default) {
                    repositorio.obtenerSensores().firstOrNull { it.clave == nuevaClave }
                }
            } catch (e: Exception) {
                null
            }
            if (info == null) {
                _ficha.value = EstadoFicha.NoEncontrado
            } else {
                val elemento = ElementoSensor.de(info)
                acumulador = AcumuladorLecturas(series = elemento.descriptor?.seriesGrafica ?: 0)
                _ficha.value = EstadoFicha.Lista(elemento)
            }
            publicar()
        }
    }

    fun iniciar() {
        if (_ficha.value !is EstadoFicha.Lista) return
        solicitado = true
        acumulador.iniciar()
        if (visible) conectar() else acumulador.pausar()
        publicar()
    }

    fun detener() {
        solicitado = false
        desconectar()
        acumulador.detener()
        publicar()
    }

    fun cambiarRitmo(nuevo: RitmoMuestreo) {
        if (nuevo == ritmo) return
        ritmo = nuevo
        if (trabajo?.isActive == true) {
            acumulador.iniciar()
            conectar()
        }
        publicar()
    }

    /** Llamado por el ciclo de vida de la pantalla (onStart / onStop). */
    fun pantallaVisible(esVisible: Boolean) {
        visible = esVisible
        if (!esVisible) {
            desconectar()
            if (solicitado) acumulador.pausar()
        } else if (solicitado && trabajo == null) {
            acumulador.reanudar()
            conectar()
        }
        publicar()
    }

    /** Permiso necesario no concedido: no se registra ningún listener. */
    fun sinPermiso(mensaje: String) {
        solicitado = false
        desconectar()
        acumulador.iniciar()
        acumulador.fallar(mensaje)
        publicar()
    }

    private fun conectar() {
        desconectar()
        val c = clave ?: return
        val acc = acumulador
        trabajo = viewModelScope.launch {
            var pendiente = false
            // Limita la recomposición a ~10 Hz aunque el sensor entregue más eventos
            val publicador = launch {
                while (isActive) {
                    delay(INTERVALO_PUBLICACION_MS)
                    if (pendiente) {
                        pendiente = false
                        publicar()
                    }
                }
            }
            try {
                repositorio.monitorear(c, ritmo.periodoUs)
                    .buffer(64, BufferOverflow.DROP_OLDEST)
                    .collect { evento ->
                        acc.aplicar(evento)
                        if (evento is EventoSensor.Lectura || evento is EventoSensor.CambioPrecision) {
                            pendiente = true
                        } else {
                            publicar()
                        }
                    }
            } catch (e: SecurityException) {
                acc.fallar("Android denegó el acceso al sensor: ${e.message}")
            } finally {
                publicador.cancel()
            }
            // El Flow terminó solo: disparo único o error
            if (acc.fase == FaseMonitoreo.DISPARADO || acc.fase == FaseMonitoreo.ERROR) {
                solicitado = false
            }
            trabajo = null
            publicar()
        }
    }

    private fun desconectar() {
        trabajo?.cancel()
        trabajo = null
    }

    private fun publicar() {
        _monitoreo.value = acumulador.estado(ritmo)
    }

    private companion object {
        const val INTERVALO_PUBLICACION_MS = 100L
    }
}
