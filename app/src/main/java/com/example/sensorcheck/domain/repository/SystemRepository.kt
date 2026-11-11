package com.example.sensorcheck.domain.repository

import com.example.sensorcheck.domain.model.SystemInfo
import com.example.sensorcheck.domain.model.TiempoActividad

/** Contrato de acceso a la información del sistema (Protected Variations). */
interface SystemRepository {

    fun obtenerInformacion(): SystemInfo

    fun obtenerTiempoActividad(): TiempoActividad
}
