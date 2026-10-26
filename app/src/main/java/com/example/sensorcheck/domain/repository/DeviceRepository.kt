
package com.example.sensorcheck.domain.repository

import com.example.sensorcheck.domain.model.DeviceInfo

interface DeviceRepository {

    fun obtenerInformacion(): DeviceInfo

}

