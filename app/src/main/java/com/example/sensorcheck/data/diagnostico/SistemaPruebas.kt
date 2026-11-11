package com.example.sensorcheck.data.diagnostico

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.example.sensorcheck.domain.diagnostico.LecturaBateria
import com.example.sensorcheck.domain.diagnostico.MedicionAlmacenamiento
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import kotlin.random.Random

/** Convierte el Intent ACTION_BATTERY_CHANGED en una lectura de dominio. */
fun leerBateria(intent: Intent?): LecturaBateria? {
    intent ?: return null
    val nivel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val escala = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
    val voltaje = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
    val estado = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val enchufe = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
    return LecturaBateria(
        nivelPorcentaje = if (nivel >= 0 && escala > 0) nivel * 100 / escala else null,
        salud = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN),
        temperaturaC = if (temp == Int.MIN_VALUE) null else temp / 10f,
        voltajeMv = voltaje.takeIf { it > 0 },
        conectada = enchufe != 0 &&
            (estado == BatteryManager.BATTERY_STATUS_CHARGING || estado == BatteryManager.BATTERY_STATUS_FULL)
    )
}

/**
 * Escribe un archivo temporal con datos aleatorios, lo sincroniza con el
 * disco, lo relee y compara los SHA-256. El archivo se borra siempre.
 */
suspend fun probarAlmacenamiento(dir: File, bytes: Long = 32L * 1024 * 1024): MedicionAlmacenamiento =
    withContext(Dispatchers.IO) {
        if (dir.usableSpace < bytes * 3) {
            return@withContext MedicionAlmacenamiento(bytes, 0, 0, null, espacioInsuficiente = true)
        }
        val archivo = File(dir, "sensorcheck_prueba_almacenamiento.tmp")
        val bloque = ByteArray(1024 * 1024)
        val aleatorio = Random(System.nanoTime())
        try {
            val escrito = MessageDigest.getInstance("SHA-256")
            var nsEscritura = 0L
            FileOutputStream(archivo).use { salida ->
                repeat((bytes / bloque.size).toInt()) {
                    aleatorio.nextBytes(bloque)
                    escrito.update(bloque)
                    val t = System.nanoTime()
                    salida.write(bloque)
                    nsEscritura += System.nanoTime() - t
                }
                val t = System.nanoTime()
                salida.fd.sync()
                nsEscritura += System.nanoTime() - t
            }
            val leido = MessageDigest.getInstance("SHA-256")
            val t0 = System.nanoTime()
            FileInputStream(archivo).use { entrada ->
                while (true) {
                    val n = entrada.read(bloque)
                    if (n < 0) break
                    leido.update(bloque, 0, n)
                }
            }
            val nsLectura = System.nanoTime() - t0
            MedicionAlmacenamiento(
                bytes = bytes,
                msEscritura = nsEscritura / 1_000_000,
                msLectura = nsLectura / 1_000_000,
                integridadCorrecta = escrito.digest().contentEquals(leido.digest())
            )
        } catch (e: IOException) {
            MedicionAlmacenamiento(bytes, 0, 0, null, error = e.message ?: e.javaClass.simpleName)
        } finally {
            archivo.delete()
        }
    }

data class MedicionRam(val mbVerificados: Int, val errores: Long, val sinMemoria: Boolean, val totalMb: Long?, val disponibleMb: Long?)

/**
 * Reserva bloques de 4 MB (hasta 64 MB o un cuarto del montón de la app),
 * escribe un patrón y su inverso y verifica ambos.
 */
suspend fun probarRam(context: Context): MedicionRam = withContext(Dispatchers.Default) {
    val am = context.getSystemService(ActivityManager::class.java)
    val info = ActivityManager.MemoryInfo().also { am?.getMemoryInfo(it) }
    val enterosPorBloque = 1 shl 20 // 4 MB
    val objetivo = minOf(16, (Runtime.getRuntime().maxMemory() / 4 / (4L shl 20)).toInt()).coerceAtLeast(1)
    val bloques = ArrayList<IntArray>()
    var sinMemoria = false
    try {
        repeat(objetivo) { bloques += IntArray(enterosPorBloque) }
    } catch (e: OutOfMemoryError) {
        sinMemoria = true
    }
    var errores = 0L
    val semilla = Random.nextInt()
    for (pasada in 0..1) {
        bloques.forEachIndexed { b, bloque ->
            val base = semilla xor (b * -0x61c88647)
            for (i in bloque.indices) bloque[i] = (base xor i).let { if (pasada == 1) it.inv() else it }
        }
        bloques.forEachIndexed { b, bloque ->
            val base = semilla xor (b * -0x61c88647)
            for (i in bloque.indices) {
                val esperado = (base xor i).let { if (pasada == 1) it.inv() else it }
                if (bloque[i] != esperado) errores++
            }
        }
    }
    val mb = bloques.size * 4
    bloques.clear()
    MedicionRam(
        mbVerificados = mb,
        errores = errores,
        sinMemoria = sinMemoria,
        totalMb = info.totalMem.takeIf { it > 0 }?.div(1_048_576),
        disponibleMb = info.availMem.takeIf { it > 0 }?.div(1_048_576)
    )
}
