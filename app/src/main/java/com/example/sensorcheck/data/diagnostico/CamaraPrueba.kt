package com.example.sensorcheck.data.diagnostico

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import com.example.sensorcheck.domain.diagnostico.CapturaCamara
import kotlin.math.max
import kotlin.math.sqrt

/** Eventos de la sesión de cámara (se entregan en el hilo de la cámara). */
sealed interface EventoCamara {
    data class VistaPrevia(val tamano: Size) : EventoCamara
    data object Cuadro : EventoCamara
    data class Capturada(val foto: Bitmap, val datos: CapturaCamara) : EventoCamara
    data class Error(val mensaje: String, val enUso: Boolean = false) : EventoCamara
}

/**
 * Sesión Camera2 mínima para la prueba: vista previa y una foto JPEG en
 * memoria. [cerrar] libera cámara, sesión, lector e hilo.
 */
class CamaraPrueba(context: Context, private val idCamara: String, private val alEvento: (EventoCamara) -> Unit) {

    private val manager = context.getSystemService(CameraManager::class.java)
    private val hilo = HandlerThread("SensorCheckCamara").apply { start() }
    private val handler = Handler(hilo.looper)
    private var camara: CameraDevice? = null
    private var sesion: CameraCaptureSession? = null
    private var lector: ImageReader? = null
    private var vistaPrevia: Surface? = null
    private val orientacionSensor: Int by lazy {
        runCatching { manager.getCameraCharacteristics(idCamara).get(CameraCharacteristics.SENSOR_ORIENTATION) }
            .getOrNull() ?: 0
    }

    /** Tamaño de vista previa ≤ 1280×720 con la proporción más cercana a 4:3 o 16:9 disponible. */
    fun tamanoVistaPrevia(): Size? = runCatching {
        val mapa = manager.getCameraCharacteristics(idCamara).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        mapa?.getOutputSizes(SurfaceTexture::class.java)
            ?.filter { it.width <= 1280 && it.height <= 1280 }
            ?.maxByOrNull { it.width.toLong() * it.height }
    }.getOrNull()

    @SuppressLint("MissingPermission") // CAMERA se solicita antes de iniciar la prueba
    fun abrir(textura: SurfaceTexture) {
        val tamano = tamanoVistaPrevia() ?: run {
            alEvento(EventoCamara.Error("La cámara no informa tamaños de vista previa"))
            return
        }
        textura.setDefaultBufferSize(tamano.width, tamano.height)
        vistaPrevia = Surface(textura)
        val jpeg = runCatching {
            manager.getCameraCharacteristics(idCamara).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?.getOutputSizes(ImageFormat.JPEG)
                ?.filter { it.width <= 1920 && it.height <= 1920 }
                ?.maxByOrNull { it.width.toLong() * it.height }
        }.getOrNull() ?: tamano
        lector = ImageReader.newInstance(jpeg.width, jpeg.height, ImageFormat.JPEG, 1).apply {
            setOnImageAvailableListener({ r -> procesarFoto(r) }, handler)
        }
        try {
            manager.openCamera(idCamara, object : CameraDevice.StateCallback() {
                override fun onOpened(c: CameraDevice) {
                    camara = c
                    alEvento(EventoCamara.VistaPrevia(tamano))
                    crearSesion(c)
                }

                override fun onDisconnected(c: CameraDevice) {
                    c.close()
                    alEvento(EventoCamara.Error("La cámara se desconectó (posiblemente la usa otra app)", enUso = true))
                }

                override fun onError(c: CameraDevice, error: Int) {
                    c.close()
                    val enUso = error == ERROR_CAMERA_IN_USE || error == ERROR_MAX_CAMERAS_IN_USE ||
                        error == ERROR_CAMERA_DISABLED
                    alEvento(EventoCamara.Error("Error al abrir la cámara (código $error)", enUso))
                }
            }, handler)
        } catch (e: CameraAccessException) {
            alEvento(EventoCamara.Error("Acceso a la cámara denegado: ${e.message}", enUso = true))
        } catch (e: SecurityException) {
            alEvento(EventoCamara.Error("Permiso de cámara no concedido", enUso = true))
        }
    }

    @Suppress("DEPRECATION") // createCaptureSession(List) sigue siendo válido desde API 21
    private fun crearSesion(c: CameraDevice) {
        val superficies = listOfNotNull(vistaPrevia, lector?.surface)
        try {
            c.createCaptureSession(superficies, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(s: CameraCaptureSession) {
                    sesion = s
                    val peticion = c.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                        vistaPrevia?.let { addTarget(it) }
                    }.build()
                    runCatching {
                        s.setRepeatingRequest(peticion, object : CameraCaptureSession.CaptureCallback() {
                            override fun onCaptureCompleted(s: CameraCaptureSession, r: CaptureRequest, t: TotalCaptureResult) {
                                alEvento(EventoCamara.Cuadro)
                            }
                        }, handler)
                    }.onFailure { alEvento(EventoCamara.Error("No se pudo iniciar la vista previa: ${it.message}")) }
                }

                override fun onConfigureFailed(s: CameraCaptureSession) {
                    alEvento(EventoCamara.Error("No se pudo configurar la sesión de cámara"))
                }
            }, handler)
        } catch (e: CameraAccessException) {
            alEvento(EventoCamara.Error("Error de cámara: ${e.message}", enUso = true))
        }
    }

    fun capturar() {
        val c = camara ?: return
        val s = sesion ?: return
        val superficie = lector?.surface ?: return
        handler.post {
            runCatching {
                val peticion = c.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                    addTarget(superficie)
                }.build()
                s.capture(peticion, null, handler)
            }.onFailure { alEvento(EventoCamara.Error("No se pudo capturar la foto: ${it.message}")) }
        }
    }

    private fun procesarFoto(lector: ImageReader) {
        val imagen = lector.acquireLatestImage() ?: return
        try {
            val buffer = imagen.planes[0].buffer
            val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
            val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones)
            // Miniatura de ~800 px: suficiente para revisar y sin consumir mucha memoria
            val muestreo = Integer.highestOneBit(max(1, max(opciones.outWidth, opciones.outHeight) / 800))
            val decodificada = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = muestreo })
                ?: run { alEvento(EventoCamara.Error("La foto capturada no se pudo decodificar")); return }
            val foto = rotar(decodificada)
            val (media, desviacion) = luminancia(foto)
            alEvento(
                EventoCamara.Capturada(
                    foto, CapturaCamara(opciones.outWidth, opciones.outHeight, bytes.size, media, desviacion)
                )
            )
        } finally {
            imagen.close()
        }
    }

    /** Endereza la foto para una interfaz en vertical. */
    private fun rotar(b: Bitmap): Bitmap {
        if (orientacionSensor == 0) return b
        val m = Matrix().apply { postRotate(orientacionSensor.toFloat()) }
        return Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
    }

    private fun luminancia(b: Bitmap): Pair<Double, Double> {
        val paso = max(1, max(b.width, b.height) / 64)
        var suma = 0.0
        var suma2 = 0.0
        var n = 0
        for (y in 0 until b.height step paso) for (x in 0 until b.width step paso) {
            val p = b.getPixel(x, y)
            val l = 0.299 * ((p shr 16) and 0xFF) + 0.587 * ((p shr 8) and 0xFF) + 0.114 * (p and 0xFF)
            suma += l
            suma2 += l * l
            n++
        }
        val media = suma / n
        return media to sqrt(max(0.0, suma2 / n - media * media))
    }

    fun cerrar() {
        runCatching { sesion?.close() }
        runCatching { camara?.close() }
        runCatching { lector?.close() }
        runCatching { vistaPrevia?.release() }
        sesion = null
        camara = null
        hilo.quitSafely()
    }
}

/** Linterna mediante CameraManager.setTorchMode (no requiere permiso de cámara). */
class LinternaPrueba(context: Context, private val idCamara: String) {
    private val manager = context.getSystemService(CameraManager::class.java)
    private val handler = Handler(context.mainLooper)

    /** Encendidos confirmados por Android a través de TorchCallback. */
    var confirmaciones = 0
        private set

    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == idCamara && enabled) confirmaciones++
        }
    }

    init {
        manager.registerTorchCallback(callback, handler)
    }

    /** @throws CameraAccessException si la cámara está en uso. */
    fun encender(encendida: Boolean) = manager.setTorchMode(idCamara, encendida)

    fun cerrar() {
        runCatching { manager.setTorchMode(idCamara, false) }
        manager.unregisterTorchCallback(callback)
    }
}
