package com.example.sensorcheck.domain.repository

import com.example.sensorcheck.domain.model.CamerasResultado

/**
 * Contrato para obtener las cámaras del dispositivo.
 * La UI depende de esta abstracción y no de Camera2
 * (GRASP: Protected Variations / GoF: Strategy).
 */
interface CameraRepository {

    fun obtenerCamaras(): CamerasResultado

}
