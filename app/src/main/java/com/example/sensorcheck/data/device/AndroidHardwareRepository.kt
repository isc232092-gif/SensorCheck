
package com.example.sensorcheck.data.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import com.example.sensorcheck.domain.model.HardwareInfo

class AndroidHardwareRepository(
    private val context: Context
) {

    fun obtenerHardware(): HardwareInfo {

        val activityManager = context.getSystemService(
            Context.ACTIVITY_SERVICE
        ) as ActivityManager

        val memoria = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoria)

        val almacenamiento = StatFs(
            Environment.getDataDirectory().absolutePath
        )

        val bateria = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val nivel = bateria?.getIntExtra(
            BatteryManager.EXTRA_LEVEL, -1
        ) ?: -1

        val escala = bateria?.getIntExtra(
            BatteryManager.EXTRA_SCALE, -1
        ) ?: -1

        val temperatura = bateria?.getIntExtra(
            BatteryManager.EXTRA_TEMPERATURE, -1
        ) ?: -1

        val voltaje = bateria?.getIntExtra(
            BatteryManager.EXTRA_VOLTAGE, -1
        ) ?: -1

        return HardwareInfo(
            ramTotalBytes = memoria.totalMem,
            ramDisponibleBytes = memoria.availMem,
            almacenamientoTotalBytes = almacenamiento.totalBytes,
            almacenamientoDisponibleBytes =
                almacenamiento.availableBytes,
            bateriaPorcentaje =
                if (nivel >= 0 && escala > 0)
                    nivel * 100 / escala
                else null,
            bateriaTemperaturaC =
                if (temperatura >= 0)
                    temperatura / 10f
                else null,
            bateriaVoltajeMv =
                voltaje.takeIf { it >= 0 }
        )
    }
}
