package com.example.sensorcheck.data.diagnostico

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import com.example.sensorcheck.domain.diagnostico.Compatibilidad
import com.example.sensorcheck.domain.diagnostico.IdPrueba
import com.example.sensorcheck.domain.model.OrientacionCamara
import com.example.sensorcheck.domain.repository.CameraRepository
import com.example.sensorcheck.domain.repository.CompatibilidadPruebasRepository
import com.example.sensorcheck.domain.repository.SensorRepository
import com.example.sensorcheck.domain.sensors.CatalogoSensores

/**
 * Compatibilidad real de cada prueba. Reutiliza los repositorios de
 * sensores y cámaras en lugar de consultar de nuevo SensorManager o Camera2.
 */
class AndroidCompatibilidadPruebas(
    context: Context,
    private val sensores: SensorRepository,
    private val camaras: CameraRepository
) : CompatibilidadPruebasRepository {

    private val app = context.applicationContext
    private val pm = app.packageManager

    private val tiposSensor: Set<Int> by lazy {
        runCatching { sensores.obtenerSensores().map { it.tipo }.toSet() }.getOrDefault(emptySet())
    }
    private val listaCamaras by lazy { runCatching { camaras.obtenerCamaras().camaras }.getOrDefault(emptyList()) }

    override fun compatibilidad(id: IdPrueba): Compatibilidad = when (id) {
        IdPrueba.TACTIL -> caracteristica(PackageManager.FEATURE_TOUCHSCREEN, "El dispositivo no declara pantalla táctil.")
        IdPrueba.MULTITACTIL -> caracteristica(
            PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH, "El panel no declara soporte multitáctil."
        )
        IdPrueba.COLORES, IdPrueba.PIXELES -> Compatibilidad.Compatible

        IdPrueba.ACELEROMETRO -> sensor(CatalogoSensores.TYPE_ACCELEROMETER, "acelerómetro")
        IdPrueba.GIROSCOPIO -> sensor(CatalogoSensores.TYPE_GYROSCOPE, "giroscopio")
        IdPrueba.PROXIMIDAD -> sensor(CatalogoSensores.TYPE_PROXIMITY, "sensor de proximidad")
        IdPrueba.LUZ -> sensor(CatalogoSensores.TYPE_LIGHT, "sensor de luz ambiental")
        IdPrueba.MAGNETOMETRO -> sensor(CatalogoSensores.TYPE_MAGNETIC_FIELD, "magnetómetro")

        IdPrueba.CAMARA_TRASERA -> camara(OrientacionCamara.TRASERA, "cámara trasera")
        IdPrueba.CAMARA_FRONTAL -> camara(OrientacionCamara.FRONTAL, "cámara frontal")
        IdPrueba.FLASH ->
            if (listaCamaras.any { it.tieneFlash == true }) Compatibilidad.Compatible
            else Compatibilidad.NoCompatible("Ninguna cámara informa flash.")

        IdPrueba.ALTAVOZ -> caracteristica(PackageManager.FEATURE_AUDIO_OUTPUT, "El dispositivo no declara salida de audio.")
        IdPrueba.MICROFONO -> caracteristica(PackageManager.FEATURE_MICROPHONE, "El dispositivo no declara micrófono.")
        IdPrueba.VIBRACION ->
            if (tieneVibrador()) Compatibilidad.Compatible
            else Compatibilidad.NoCompatible("Android no informa motor de vibración.")

        IdPrueba.WIFI -> caracteristica(PackageManager.FEATURE_WIFI, "El dispositivo no declara Wi-Fi.")
        IdPrueba.BLUETOOTH -> caracteristica(PackageManager.FEATURE_BLUETOOTH, "El dispositivo no declara Bluetooth.")
        IdPrueba.GPS -> caracteristica(PackageManager.FEATURE_LOCATION_GPS, "El dispositivo no declara GPS.")

        IdPrueba.BATERIA, IdPrueba.ALMACENAMIENTO, IdPrueba.RAM, IdPrueba.BOTONES -> Compatibilidad.Compatible
    }

    override fun contactosDeclarados(): Int = when {
        pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_JAZZHAND) -> 5
        pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH) -> 2
        else -> 1
    }

    private fun caracteristica(nombre: String, motivo: String) =
        if (pm.hasSystemFeature(nombre)) Compatibilidad.Compatible else Compatibilidad.NoCompatible(motivo)

    private fun sensor(tipo: Int, nombre: String) =
        if (tipo in tiposSensor) Compatibilidad.Compatible
        else Compatibilidad.NoCompatible("Android no expone un $nombre en este dispositivo.")

    private fun camara(orientacion: OrientacionCamara, nombre: String) =
        if (listaCamaras.any { it.orientacion == orientacion }) Compatibilidad.Compatible
        else Compatibilidad.NoCompatible("Camera2 no informa una $nombre.")

    private fun tieneVibrador(): Boolean = runCatching {
        val vibrador = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            app.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrador?.hasVibrator() == true
    }.getOrDefault(false)
}
