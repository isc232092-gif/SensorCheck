
package com.example.sensorcheck.data.device

import android.os.Build
import com.example.sensorcheck.domain.model.DeviceInfo
import com.example.sensorcheck.domain.repository.DeviceRepository

class AndroidDeviceRepository : DeviceRepository {

    override fun obtenerInformacion(): DeviceInfo {

        return DeviceInfo(
            fabricante = Build.MANUFACTURER,
            marca = Build.BRAND,
            modelo = Build.MODEL,
            dispositivo = Build.DEVICE,
            versionAndroid = Build.VERSION.RELEASE,
            nivelApi = Build.VERSION.SDK_INT,
            placa = Build.BOARD,
            hardware = Build.HARDWARE,
            producto = Build.PRODUCT,
            arquitecturasCpu = Build.SUPPORTED_ABIS.toList()
        )
    }
}
