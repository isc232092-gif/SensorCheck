package com.example.sensorcheck.ui.diagnostico

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sensorcheck.data.device.AndroidCameraRepository
import com.example.sensorcheck.data.diagnostico.AndroidCompatibilidadPruebas
import com.example.sensorcheck.data.diagnostico.HistorialDiagnosticoArchivo
import com.example.sensorcheck.data.sensors.AndroidSensorRepository
import com.example.sensorcheck.domain.diagnostico.CatalogoPruebas
import com.example.sensorcheck.domain.diagnostico.Compatibilidad
import com.example.sensorcheck.domain.diagnostico.DefinicionPrueba
import com.example.sensorcheck.domain.diagnostico.IdPrueba
import com.example.sensorcheck.domain.diagnostico.RegistroDiagnostico
import com.example.sensorcheck.domain.diagnostico.ResultadoEvaluado
import com.example.sensorcheck.domain.diagnostico.SesionDiagnostico
import com.example.sensorcheck.domain.diagnostico.ultimoResultado
import com.example.sensorcheck.domain.repository.CameraRepository
import com.example.sensorcheck.domain.repository.CompatibilidadPruebasRepository
import com.example.sensorcheck.domain.repository.HistorialDiagnosticoRepository
import com.example.sensorcheck.domain.repository.SensorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** Prueba del catálogo con su compatibilidad real y su último resultado. */
data class ElementoPrueba(
    val definicion: DefinicionPrueba,
    val compatibilidad: Compatibilidad,
    val ultimo: RegistroDiagnostico?
) {
    val ejecutable: Boolean get() = definicion.implementada && compatibilidad is Compatibilidad.Compatible
}

sealed interface PantallaDiagnostico {
    data object Inicio : PantallaDiagnostico
    data class Individual(val id: IdPrueba) : PantallaDiagnostico
    data object Asistente : PantallaDiagnostico
}

/**
 * Controlador del módulo Diagnóstico (GRASP: Controlador y Creador).
 * Las pruebas se ejecutan de una en una; cada resultado se añade al historial.
 */
class DiagnosticoViewModel(application: Application) : AndroidViewModel(application) {

    /** Repositorio de sensores compartido con los ejecutores (sin duplicar SensorManager). */
    val sensores: SensorRepository = AndroidSensorRepository(application)
    /** Repositorio de cámaras compartido con las pruebas de cámara y flash. */
    val camaras: CameraRepository = AndroidCameraRepository(application)
    private val compatibilidad: CompatibilidadPruebasRepository =
        AndroidCompatibilidadPruebas(application, sensores, camaras)
    private val historial: HistorialDiagnosticoRepository = HistorialDiagnosticoArchivo(application)

    private val _elementos = MutableStateFlow<List<ElementoPrueba>?>(null)
    val elementos: StateFlow<List<ElementoPrueba>?> = _elementos.asStateFlow()

    private val _pantalla = MutableStateFlow<PantallaDiagnostico>(PantallaDiagnostico.Inicio)
    val pantalla: StateFlow<PantallaDiagnostico> = _pantalla.asStateFlow()

    private val _sesion = MutableStateFlow<SesionDiagnostico?>(null)
    val sesion: StateFlow<SesionDiagnostico?> = _sesion.asStateFlow()

    /** Resultado de la ejecución en curso (se muestra hasta Repetir o Continuar). */
    private val _resultado = MutableStateFlow<ResultadoEvaluado?>(null)
    val resultado: StateFlow<ResultadoEvaluado?> = _resultado.asStateFlow()

    private val _seleccion = MutableStateFlow<Set<IdPrueba>?>(null)
    /** null = modo selección desactivado. */
    val seleccion: StateFlow<Set<IdPrueba>?> = _seleccion.asStateFlow()

    var contactosDeclarados = 1
        private set

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            val (compat, registros) = withContext(Dispatchers.Default) {
                contactosDeclarados = compatibilidad.contactosDeclarados()
                CatalogoPruebas.pruebas.associate { it.id to compatibilidad.compatibilidad(it.id) } to
                    runCatching { historial.leerTodos() }.getOrDefault(emptyList())
            }
            _elementos.value = CatalogoPruebas.pruebas.map {
                ElementoPrueba(it, compat.getValue(it.id), ultimoResultado(registros, it.id))
            }
        }
    }

    fun elemento(id: IdPrueba): ElementoPrueba? = _elementos.value?.firstOrNull { it.definicion.id == id }

    // ---- Navegación ----

    fun abrirPrueba(id: IdPrueba) {
        _resultado.value = null
        _pantalla.value = PantallaDiagnostico.Individual(id)
    }

    fun volverAlInicio() {
        _resultado.value = null
        _sesion.value = null
        _pantalla.value = PantallaDiagnostico.Inicio
    }

    // ---- Selección individual ----

    fun alternarModoSeleccion() {
        _seleccion.value = if (_seleccion.value == null) emptySet() else null
    }

    fun alternarSeleccion(id: IdPrueba) {
        val actual = _seleccion.value ?: return
        _seleccion.value = if (id in actual) actual - id else actual + id
    }

    // ---- Diagnóstico guiado ----

    fun iniciarCompleto() = iniciarSesion(CatalogoPruebas.pruebas.map { it.id })

    fun iniciarSeleccionadas() {
        val ids = _seleccion.value.orEmpty()
        _seleccion.value = null
        iniciarSesion(ids)
    }

    fun repetirPendientes() {
        val anterior = _sesion.value ?: return
        iniciarSesion(anterior.paraRepetir())
    }

    private fun iniciarSesion(ids: Collection<IdPrueba>) {
        val sesion = SesionDiagnostico.crear(UUID.randomUUID().toString(), ids) { id ->
            elemento(id)?.ejecutable == true
        }
        if (sesion.cola.isEmpty()) return
        _resultado.value = null
        _sesion.value = sesion
        _pantalla.value = PantallaDiagnostico.Asistente
    }

    /** Guarda cada ejecución (incluidas canceladas) como un registro nuevo. */
    fun guardar(id: IdPrueba, evaluado: ResultadoEvaluado) {
        _resultado.value = evaluado
        val registro = RegistroDiagnostico(
            id = UUID.randomUUID().toString(),
            sesionId = _sesion.value?.id,
            fechaEpochMs = System.currentTimeMillis(),
            dispositivo = historial.dispositivo(),
            prueba = id,
            resultado = evaluado.resultado,
            evidencia = evaluado.evidencia,
            recomendacion = evaluado.recomendacion
        )
        viewModelScope.launch {
            runCatching { historial.guardar(registro) }
            actualizarUltimo(registro)
        }
    }

    fun repetir() {
        _resultado.value = null
    }

    /** Continúa con la siguiente prueba usando el último resultado mostrado. */
    fun continuarAsistente() {
        val sesion = _sesion.value ?: return
        val evaluado = _resultado.value ?: return
        _resultado.value = null
        _sesion.value = sesion.registrar(evaluado)
    }

    fun omitirActual() {
        val sesion = _sesion.value ?: return
        val id = sesion.actual ?: return
        guardar(id, CatalogoPruebas.noEjecutada(id, "Omitida por el usuario"))
        continuarAsistente()
    }

    private fun actualizarUltimo(registro: RegistroDiagnostico) {
        val lista = _elementos.value ?: return
        _elementos.value = lista.map {
            if (it.definicion.id == registro.prueba) {
                it.copy(ultimo = ultimoResultado(listOfNotNull(it.ultimo, registro), registro.prueba))
            } else it
        }
    }
}
