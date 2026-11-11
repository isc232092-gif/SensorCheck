package com.example.sensorcheck.ui.sensors

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sensorcheck.data.sensors.AndroidSensorRepository
import com.example.sensorcheck.domain.model.CategoriaSensor
import com.example.sensorcheck.domain.repository.SensorRepository
import com.example.sensorcheck.domain.sensors.CatalogoSensores
import com.example.sensorcheck.domain.sensors.ElementoSensor
import com.example.sensorcheck.domain.sensors.FiltroSensores
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface EstadoInventario {
    data object Cargando : EstadoInventario
    data class Listo(
        val sensores: List<ElementoSensor>,
        val tiposNoDetectados: List<Int>
    ) : EstadoInventario
    data class Error(val mensaje: String) : EstadoInventario
}

/** Inventario ya filtrado y agrupado para la pantalla. */
data class VistaInventario(
    val total: Int,
    val conteoPorCategoria: Map<CategoriaSensor, Int>,
    val grupos: Map<CategoriaSensor, List<ElementoSensor>>,
    val tiposNoDetectados: List<Int>,
    val hayFiltro: Boolean
)

/** Controlador del listado de sensores. No registra ningún listener. */
class SensorsViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio: SensorRepository = AndroidSensorRepository(application)

    private val _estado = MutableStateFlow<EstadoInventario>(EstadoInventario.Cargando)
    val estado: StateFlow<EstadoInventario> = _estado.asStateFlow()

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    private val _categoria = MutableStateFlow<CategoriaSensor?>(null)
    val categoria: StateFlow<CategoriaSensor?> = _categoria.asStateFlow()

    val vista: StateFlow<VistaInventario?> =
        combine(_estado, _busqueda, _categoria) { estado, texto, cat ->
            (estado as? EstadoInventario.Listo)?.let { listo ->
                val filtrados = FiltroSensores.filtrar(listo.sensores, texto, cat)
                VistaInventario(
                    total = listo.sensores.size,
                    conteoPorCategoria = listo.sensores.groupingBy { it.categoria }.eachCount(),
                    grupos = FiltroSensores.agrupar(filtrados),
                    tiposNoDetectados = listo.tiposNoDetectados.filter {
                        cat == null || CatalogoSensores.categoria(it) == cat
                    },
                    hayFiltro = texto.isNotBlank() || cat != null
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        cargar()
    }

    fun buscar(texto: String) {
        _busqueda.value = texto
    }

    fun filtrar(categoria: CategoriaSensor?) {
        _categoria.value = categoria
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = try {
                val sensores = withContext(Dispatchers.Default) {
                    repositorio.obtenerSensores().map { ElementoSensor.de(it) }
                }
                EstadoInventario.Listo(
                    sensores = sensores,
                    tiposNoDetectados = CatalogoSensores.noDetectados(sensores.map { it.info.tipo }.toSet())
                )
            } catch (e: Exception) {
                EstadoInventario.Error(e.message ?: e.javaClass.simpleName)
            }
        }
    }
}
