package com.example.sensorcheck.data.diagnostico

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class EstadoWifi(val activado: Boolean, val conectado: Boolean, val validado: Boolean, val rssiDbm: Int?)

/** Estado del Wi-Fi sin permisos de ubicación (no se lee el nombre de la red). */
fun leerWifi(context: Context): EstadoWifi {
    val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
    val red = context.getSystemService(ConnectivityManager::class.java)
    val capacidades = red?.activeNetwork?.let { red.getNetworkCapabilities(it) }
    val conectado = capacidades?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    val rssi = when {
        !conectado -> null
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
            (capacidades?.transportInfo as? WifiInfo)?.rssi ?: capacidades?.signalStrength
        else -> @Suppress("DEPRECATION") wifi?.connectionInfo?.rssi
    }?.takeIf { it in -127..0 }
    return EstadoWifi(
        activado = wifi?.isWifiEnabled == true,
        conectado = conectado,
        validado = capacidades?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
        rssiDbm = rssi
    )
}

fun bluetoothActivado(context: Context): Boolean? =
    context.getSystemService(BluetoothManager::class.java)?.adapter?.let { runCatching { it.isEnabled }.getOrNull() }

sealed interface EventoBle {
    data class Encontrados(val total: Int) : EventoBle
    data class Fallo(val mensaje: String) : EventoBle
}

/** Escaneo BLE de baja potencia; se detiene al cancelar la recolección. */
@SuppressLint("MissingPermission") // permisos solicitados antes de iniciar la prueba
fun escanearBle(context: Context): Flow<EventoBle> = callbackFlow {
    val escaner = context.getSystemService(BluetoothManager::class.java)?.adapter?.bluetoothLeScanner
    if (escaner == null) {
        trySend(EventoBle.Fallo("El escáner Bluetooth no está disponible (¿Bluetooth apagado?)"))
        close()
        awaitClose { }
        return@callbackFlow
    }
    val vistos = HashSet<String>()
    val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (vistos.add(result.device.address)) trySend(EventoBle.Encontrados(vistos.size))
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) = results.forEach { onScanResult(0, it) }

        override fun onScanFailed(errorCode: Int) {
            trySend(EventoBle.Fallo("Android rechazó el escaneo (código $errorCode)"))
        }
    }
    try {
        escaner.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build(), callback)
    } catch (e: SecurityException) {
        trySend(EventoBle.Fallo("Permiso de Bluetooth denegado"))
    }
    awaitClose { runCatching { escaner.stopScan(callback) } }
}

fun ubicacionActivada(context: Context): Boolean =
    context.getSystemService(LocationManager::class.java)?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true

sealed interface EventoGps {
    data class Satelites(val vistos: Int, val enUso: Int) : EventoGps
    data class Posicion(val precisionM: Float) : EventoGps
    data class Fallo(val mensaje: String) : EventoGps
}

/** Estado GNSS y posición del proveedor GPS; todo se libera al cancelar. */
@SuppressLint("MissingPermission") // ubicación solicitada antes de iniciar la prueba
fun escucharGps(context: Context): Flow<EventoGps> = callbackFlow {
    val lm = context.getSystemService(LocationManager::class.java)
    val handler = Handler(Looper.getMainLooper())
    val estado = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val enUso = (0 until status.satelliteCount).count { status.usedInFix(it) }
            trySend(EventoGps.Satelites(status.satelliteCount, enUso))
        }
    }
    val oyente = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            trySend(EventoGps.Posicion(if (location.hasAccuracy()) location.accuracy else Float.NaN))
        }

        @Deprecated("Requerido en API < 29")
        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
        override fun onProviderEnabled(provider: String) = Unit
        override fun onProviderDisabled(provider: String) = Unit
    }
    try {
        @Suppress("DEPRECATION")
        lm.registerGnssStatusCallback(estado, handler)
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1_000L, 0f, oyente, Looper.getMainLooper())
    } catch (e: SecurityException) {
        trySend(EventoGps.Fallo("Permiso de ubicación denegado"))
    } catch (e: IllegalArgumentException) {
        trySend(EventoGps.Fallo("El proveedor GPS no está disponible"))
    }
    awaitClose {
        runCatching { lm.unregisterGnssStatusCallback(estado) }
        runCatching { lm.removeUpdates(oyente) }
    }
}
