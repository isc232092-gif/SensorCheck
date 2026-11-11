package com.example.sensorcheck.data.diagnostico

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

const val FRECUENCIA_MUESTREO = 44_100

/** Genera [n] pitidos de 1 kHz con rampas para evitar chasquidos. */
fun generarPitidos(n: Int, msPitido: Int = 250, msPausa: Int = 450): ShortArray {
    val porPitido = FRECUENCIA_MUESTREO * msPitido / 1000
    val porPausa = FRECUENCIA_MUESTREO * msPausa / 1000
    val rampa = FRECUENCIA_MUESTREO / 200 // 5 ms
    val pcm = ShortArray(porPausa + n * (porPitido + porPausa))
    var pos = porPausa
    repeat(n) {
        for (i in 0 until porPitido) {
            val envolvente = min(1.0, min(i, porPitido - 1 - i).toDouble() / rampa)
            pcm[pos + i] = (sin(2 * PI * 1000 * i / FRECUENCIA_MUESTREO) * envolvente * 0.8 * Short.MAX_VALUE).toInt().toShort()
        }
        pos += porPitido + porPausa
    }
    return pcm
}

/**
 * Reproduce PCM por el altavoz integrado. Sube temporalmente el volumen
 * multimedia si está bajo y lo restaura al terminar.
 * @return tipo de dispositivo por el que salió el audio (AudioDeviceInfo.TYPE_*), si Android lo informa.
 */
suspend fun reproducirEnAltavoz(context: Context, pcm: ShortArray, subirVolumen: Boolean = true): Int? {
    val audio = context.getSystemService(AudioManager::class.java)
    val anterior = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
    val maximo = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val objetivo = (maximo * 0.7f).toInt()
    val cambiado = subirVolumen && anterior < objetivo && runCatching {
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, objetivo, 0)
    }.isSuccess

    val pista = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(FRECUENCIA_MUESTREO)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
        )
        .setTransferMode(AudioTrack.MODE_STATIC)
        .setBufferSizeInBytes(pcm.size * 2)
        .build()
    try {
        audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            ?.let { pista.setPreferredDevice(it) }
        pista.write(pcm, 0, pcm.size)
        pista.play()
        delay(150)
        val ruta = pista.routedDevice?.type
        delay(pcm.size * 1000L / FRECUENCIA_MUESTREO)
        return ruta
    } finally {
        runCatching { pista.stop() }
        pista.release()
        if (cambiado) runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, anterior, 0) }
    }
}

fun nombreSalidaAudio(tipo: Int?): String = when (tipo) {
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "altavoz integrado"
    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "auricular de llamadas"
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "auriculares con cable"
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "auriculares Bluetooth"
    AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> "dispositivo USB"
    null -> "no informado"
    else -> "dispositivo tipo $tipo"
}

/**
 * Graba [segundos] del micrófono principal en memoria. El AudioRecord se
 * libera siempre, también si se cancela la corrutina.
 */
@SuppressLint("MissingPermission") // el permiso se solicita antes de iniciar la prueba
suspend fun grabarMicrofono(segundos: Int, onNivel: (ShortArray, Int) -> Unit): ShortArray = withContext(Dispatchers.IO) {
    val minimo = AudioRecord.getMinBufferSize(FRECUENCIA_MUESTREO, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    require(minimo > 0) { "Formato de grabación no admitido" }
    val grabador = AudioRecord(
        MediaRecorder.AudioSource.MIC, FRECUENCIA_MUESTREO,
        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minimo * 4
    )
    try {
        check(grabador.state == AudioRecord.STATE_INITIALIZED) { "No se pudo inicializar el micrófono" }
        val total = ShortArray(FRECUENCIA_MUESTREO * segundos)
        val bloque = FRECUENCIA_MUESTREO / 10
        grabador.startRecording()
        var leidas = 0
        while (leidas < total.size && coroutineContext.isActive) {
            val n = grabador.read(total, leidas, min(bloque, total.size - leidas))
            if (n < 0) error("Error de lectura del micrófono ($n)")
            leidas += n
            onNivel(total, leidas)
        }
        total
    } finally {
        runCatching { grabador.stop() }
        grabador.release()
    }
}

/** Motor de vibración por la API recomendada para cada versión. */
class VibradorPrueba(context: Context) {
    private val vibrador: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    val disponible: Boolean get() = vibrador?.hasVibrator() == true
    val controlAmplitud: Boolean get() = vibrador?.hasAmplitudeControl() == true

    /** @return duración del patrón en ms. */
    fun pulsos(n: Int, msPulso: Long = 350, msPausa: Long = 500): Long {
        val tiempos = LongArray(n * 2) { if (it % 2 == 0) msPausa else msPulso }
        vibrador?.vibrate(VibrationEffect.createWaveform(tiempos, -1))
        return tiempos.sum()
    }

    fun cancelar() = vibrador?.cancel()
}
