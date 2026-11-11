package com.example.sensorcheck.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sensorcheck.data.device.AndroidCameraRepository
import com.example.sensorcheck.data.device.AndroidCpuDisplayRepository
import com.example.sensorcheck.data.device.AndroidDeviceRepository
import com.example.sensorcheck.data.device.AndroidHardwareRepository
import com.example.sensorcheck.data.preferences.PreferenciasTema
import com.example.sensorcheck.domain.model.CamerasResultado
import com.example.sensorcheck.domain.model.CpuDisplayInfo
import com.example.sensorcheck.domain.model.DeviceInfo
import com.example.sensorcheck.domain.model.HardwareInfo
import com.example.sensorcheck.domain.repository.CameraRepository
import com.example.sensorcheck.domain.repository.DeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Datos reales leídos del dispositivo al iniciar. */
data class DatosSensorCheck(
    val dispositivo: DeviceInfo,
    val hardware: HardwareInfo,
    val cpuPantalla: CpuDisplayInfo,
    val camaras: CamerasResultado
)

sealed interface EstadoCarga {
    data object Cargando : EstadoCarga
    data class Listo(val datos: DatosSensorCheck) : EstadoCarga
    data class Error(val mensaje: String) : EstadoCarga
}

/**
 * Controlador de la interfaz (GRASP: Controlador y Creador).
 * Crea los repositorios, carga los datos fuera del hilo principal
 * y gestiona la preferencia de modo día/noche.
 */
class SensorCheckViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val preferenciasTema = PreferenciasTema(application)

    private val deviceRepository: DeviceRepository = AndroidDeviceRepository()
    private val hardwareRepository = AndroidHardwareRepository(application)
    private val cpuDisplayRepository = AndroidCpuDisplayRepository(application)
    private val cameraRepository: CameraRepository = AndroidCameraRepository(application)

    private val _estado = MutableStateFlow<EstadoCarga>(EstadoCarga.Cargando)
    val estado: StateFlow<EstadoCarga> = _estado.asStateFlow()

    private val _modoOscuro = MutableStateFlow(preferenciasTema.leerModoOscuro())
    /** null = seguir el tema del sistema. */
    val modoOscuro: StateFlow<Boolean?> = _modoOscuro.asStateFlow()

    init {
        cargarDatos()
    }

    fun cambiarModoOscuro(oscuro: Boolean) {
        _modoOscuro.value = oscuro
        preferenciasTema.guardarModoOscuro(oscuro)
    }

    private fun cargarDatos() {
        viewModelScope.launch {
            _estado.value = try {
                val datos = withContext(Dispatchers.Default) {
                    DatosSensorCheck(
                        dispositivo = deviceRepository.obtenerInformacion(),
                        hardware = hardwareRepository.obtenerHardware(),
                        cpuPantalla = cpuDisplayRepository.obtenerInformacion(),
                        camaras = cameraRepository.obtenerCamaras()
                    )
                }
                EstadoCarga.Listo(datos)
            } catch (e: Exception) {
                EstadoCarga.Error(e.message ?: e.javaClass.simpleName)
            }
        }
    }
}
