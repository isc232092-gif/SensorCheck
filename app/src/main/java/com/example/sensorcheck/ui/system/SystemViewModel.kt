package com.example.sensorcheck.ui.system

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sensorcheck.data.system.AndroidSystemRepository
import com.example.sensorcheck.domain.model.SystemInfo
import com.example.sensorcheck.domain.model.TiempoActividad
import com.example.sensorcheck.domain.repository.SystemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Controlador del módulo Sistema (GRASP: Controlador).
 * La pantalla solo presenta; los datos vienen del repositorio.
 */
class SystemViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repositorio: SystemRepository = AndroidSystemRepository(application)

    private val _informacion = MutableStateFlow<SystemInfo?>(null)
    val informacion: StateFlow<SystemInfo?> = _informacion.asStateFlow()

    /**
     * Tiempo de actividad que se actualiza cada segundo solo mientras
     * hay un observador activo; se detiene 5 s después de que la
     * pantalla deja de estar visible.
     */
    val tiempoActividad: StateFlow<TiempoActividad?> = flow {
        while (true) {
            val actual = repositorio.obtenerTiempoActividad()
            emit(actual)
            // Alinear con el cambio de segundo para que el contador sea estable
            delay(1_000L - actual.desdeArranqueMs % 1_000L)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    init {
        actualizar()
    }

    /**
     * Relee la información. Se llama al volver a la pantalla para reflejar
     * cambios de idioma, zona horaria o ajustes de depuración.
     */
    fun actualizar() {
        viewModelScope.launch {
            _informacion.value = withContext(Dispatchers.IO) {
                repositorio.obtenerInformacion()
            }
        }
    }
}
