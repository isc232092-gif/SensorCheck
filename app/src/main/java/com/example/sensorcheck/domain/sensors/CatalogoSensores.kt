package com.example.sensorcheck.domain.sensors

import com.example.sensorcheck.domain.model.CategoriaSensor

/** Un valor de SensorEvent.values con su significado y unidad (null = adimensional). */
data class Eje(val nombre: String, val unidad: String?)

enum class PermisoSensor(val permiso: String, val apiMinima: Int, val descripcion: String) {
    ACTIVIDAD_FISICA("android.permission.ACTIVITY_RECOGNITION", 29, "Actividad física"),
    DATOS_CORPORALES("android.permission.BODY_SENSORS", 20, "Sensores corporales")
}

/**
 * Significado de un tipo de sensor según la documentación de Android.
 * @param derivado Android define el tipo como combinación de otros sensores
 *                 (no indica cómo lo implementa el fabricante).
 * @param seriesGrafica cuántos valores iniciales se grafican (0 = no graficable).
 */
data class DescriptorSensor(
    val nombre: String,
    val categoria: CategoriaSensor,
    val ejes: List<Eje>,
    val derivado: Boolean = false,
    val seriesGrafica: Int = 0,
    val permiso: PermisoSensor? = null
) {
    /** Unidad común de las series graficadas, si todas comparten la misma. */
    val unidadGrafica: String?
        get() = ejes.take(seriesGrafica).map { it.unidad }.distinct().singleOrNull()
}

/**
 * Diccionario de tipos Android -> significado. NO es la lista de sensores
 * del dispositivo: esa se obtiene siempre con SensorManager.getSensorList().
 * Las constantes coinciden con android.hardware.Sensor.TYPE_* (verificado en pruebas).
 */
object CatalogoSensores {

    const val TYPE_ACCELEROMETER = 1
    const val TYPE_MAGNETIC_FIELD = 2
    const val TYPE_ORIENTATION = 3
    const val TYPE_GYROSCOPE = 4
    const val TYPE_LIGHT = 5
    const val TYPE_PRESSURE = 6
    const val TYPE_TEMPERATURE = 7
    const val TYPE_PROXIMITY = 8
    const val TYPE_GRAVITY = 9
    const val TYPE_LINEAR_ACCELERATION = 10
    const val TYPE_ROTATION_VECTOR = 11
    const val TYPE_RELATIVE_HUMIDITY = 12
    const val TYPE_AMBIENT_TEMPERATURE = 13
    const val TYPE_MAGNETIC_FIELD_UNCALIBRATED = 14
    const val TYPE_GAME_ROTATION_VECTOR = 15
    const val TYPE_GYROSCOPE_UNCALIBRATED = 16
    const val TYPE_SIGNIFICANT_MOTION = 17
    const val TYPE_STEP_DETECTOR = 18
    const val TYPE_STEP_COUNTER = 19
    const val TYPE_GEOMAGNETIC_ROTATION_VECTOR = 20
    const val TYPE_HEART_RATE = 21
    const val TYPE_POSE_6DOF = 28
    const val TYPE_STATIONARY_DETECT = 29
    const val TYPE_MOTION_DETECT = 30
    const val TYPE_HEART_BEAT = 31
    const val TYPE_LOW_LATENCY_OFFBODY_DETECT = 34
    const val TYPE_ACCELEROMETER_UNCALIBRATED = 35
    const val TYPE_HINGE_ANGLE = 36
    const val TYPE_HEAD_TRACKER = 37
    const val TYPE_ACCELEROMETER_LIMITED_AXES = 38
    const val TYPE_GYROSCOPE_LIMITED_AXES = 39
    const val TYPE_ACCELEROMETER_LIMITED_AXES_UNCALIBRATED = 40
    const val TYPE_GYROSCOPE_LIMITED_AXES_UNCALIBRATED = 41
    const val TYPE_HEADING = 42

    /** Desde este valor los tipos son privados del fabricante. */
    const val TYPE_DEVICE_PRIVATE_BASE = 0x10000

    private const val MS2 = "m/s²"
    private const val UT = "μT"
    private const val RADS = "rad/s"

    private fun xyz(unidad: String?) = listOf(Eje("X", unidad), Eje("Y", unidad), Eje("Z", unidad))
    private fun prefijado(prefijo: String, unidad: String?) =
        listOf(Eje("$prefijo X", unidad), Eje("$prefijo Y", unidad), Eje("$prefijo Z", unidad))
    private val ejesAdmitidos = listOf(
        Eje("Eje X admitido (1/0)", null), Eje("Eje Y admitido (1/0)", null), Eje("Eje Z admitido (1/0)", null)
    )
    private val cuaternion = listOf(
        Eje("X·sen(θ/2)", null), Eje("Y·sen(θ/2)", null), Eje("Z·sen(θ/2)", null), Eje("cos(θ/2)", null)
    )

    private val descriptores: Map<Int, DescriptorSensor> = mapOf(
        TYPE_ACCELEROMETER to DescriptorSensor("Acelerómetro", CategoriaSensor.MOVIMIENTO, xyz(MS2), seriesGrafica = 3),
        TYPE_ACCELEROMETER_UNCALIBRATED to DescriptorSensor(
            "Acelerómetro no calibrado", CategoriaSensor.MOVIMIENTO,
            xyz(MS2) + prefijado("Sesgo", MS2), seriesGrafica = 3
        ),
        TYPE_ACCELEROMETER_LIMITED_AXES to DescriptorSensor(
            "Acelerómetro de ejes limitados", CategoriaSensor.MOVIMIENTO, xyz(MS2) + ejesAdmitidos, seriesGrafica = 3
        ),
        TYPE_ACCELEROMETER_LIMITED_AXES_UNCALIBRATED to DescriptorSensor(
            "Acelerómetro de ejes limitados no calibrado", CategoriaSensor.MOVIMIENTO,
            xyz(MS2) + prefijado("Sesgo", MS2) + ejesAdmitidos, seriesGrafica = 3
        ),
        TYPE_GYROSCOPE to DescriptorSensor("Giroscopio", CategoriaSensor.MOVIMIENTO, xyz(RADS), seriesGrafica = 3),
        TYPE_GYROSCOPE_UNCALIBRATED to DescriptorSensor(
            "Giroscopio no calibrado", CategoriaSensor.MOVIMIENTO,
            xyz(RADS) + prefijado("Deriva", RADS), seriesGrafica = 3
        ),
        TYPE_GYROSCOPE_LIMITED_AXES to DescriptorSensor(
            "Giroscopio de ejes limitados", CategoriaSensor.MOVIMIENTO, xyz(RADS) + ejesAdmitidos, seriesGrafica = 3
        ),
        TYPE_GYROSCOPE_LIMITED_AXES_UNCALIBRATED to DescriptorSensor(
            "Giroscopio de ejes limitados no calibrado", CategoriaSensor.MOVIMIENTO,
            xyz(RADS) + prefijado("Deriva", RADS) + ejesAdmitidos, seriesGrafica = 3
        ),
        TYPE_GRAVITY to DescriptorSensor("Gravedad", CategoriaSensor.MOVIMIENTO, xyz(MS2), derivado = true, seriesGrafica = 3),
        TYPE_LINEAR_ACCELERATION to DescriptorSensor(
            "Aceleración lineal", CategoriaSensor.MOVIMIENTO, xyz(MS2), derivado = true, seriesGrafica = 3
        ),
        TYPE_ROTATION_VECTOR to DescriptorSensor(
            "Vector de rotación", CategoriaSensor.MOVIMIENTO,
            cuaternion + Eje("Precisión estimada", "rad"), derivado = true, seriesGrafica = 4
        ),
        TYPE_GAME_ROTATION_VECTOR to DescriptorSensor(
            "Vector de rotación de juego", CategoriaSensor.MOVIMIENTO, cuaternion, derivado = true, seriesGrafica = 4
        ),
        TYPE_SIGNIFICANT_MOTION to DescriptorSensor(
            "Movimiento significativo", CategoriaSensor.MOVIMIENTO, listOf(Eje("Evento", null))
        ),
        TYPE_STEP_DETECTOR to DescriptorSensor(
            "Detector de pasos", CategoriaSensor.MOVIMIENTO, listOf(Eje("Paso detectado", null)),
            permiso = PermisoSensor.ACTIVIDAD_FISICA
        ),
        TYPE_STEP_COUNTER to DescriptorSensor(
            "Contador de pasos", CategoriaSensor.MOVIMIENTO, listOf(Eje("Pasos desde el último reinicio", "pasos")),
            permiso = PermisoSensor.ACTIVIDAD_FISICA
        ),
        TYPE_STATIONARY_DETECT to DescriptorSensor(
            "Detector de reposo", CategoriaSensor.MOVIMIENTO, listOf(Eje("Evento", null))
        ),
        TYPE_MOTION_DETECT to DescriptorSensor(
            "Detector de movimiento", CategoriaSensor.MOVIMIENTO, listOf(Eje("Evento", null))
        ),

        TYPE_LIGHT to DescriptorSensor("Luz ambiental", CategoriaSensor.AMBIENTAL, listOf(Eje("Iluminancia", "lx")), seriesGrafica = 1),
        TYPE_PROXIMITY to DescriptorSensor("Proximidad", CategoriaSensor.AMBIENTAL, listOf(Eje("Distancia", "cm")), seriesGrafica = 1),
        TYPE_PRESSURE to DescriptorSensor("Presión atmosférica", CategoriaSensor.AMBIENTAL, listOf(Eje("Presión", "hPa")), seriesGrafica = 1),
        TYPE_AMBIENT_TEMPERATURE to DescriptorSensor(
            "Temperatura ambiente", CategoriaSensor.AMBIENTAL, listOf(Eje("Temperatura", "°C")), seriesGrafica = 1
        ),
        TYPE_TEMPERATURE to DescriptorSensor(
            "Temperatura (tipo obsoleto)", CategoriaSensor.AMBIENTAL, listOf(Eje("Temperatura", "°C")), seriesGrafica = 1
        ),
        TYPE_RELATIVE_HUMIDITY to DescriptorSensor(
            "Humedad relativa", CategoriaSensor.AMBIENTAL, listOf(Eje("Humedad", "%")), seriesGrafica = 1
        ),

        TYPE_MAGNETIC_FIELD to DescriptorSensor("Magnetómetro", CategoriaSensor.POSICION, xyz(UT), seriesGrafica = 3),
        TYPE_MAGNETIC_FIELD_UNCALIBRATED to DescriptorSensor(
            "Campo magnético no calibrado", CategoriaSensor.POSICION, xyz(UT) + prefijado("Sesgo", UT), seriesGrafica = 3
        ),
        TYPE_GEOMAGNETIC_ROTATION_VECTOR to DescriptorSensor(
            "Vector de rotación geomagnética", CategoriaSensor.POSICION,
            cuaternion + Eje("Precisión estimada", "rad"), derivado = true, seriesGrafica = 4
        ),
        TYPE_ORIENTATION to DescriptorSensor(
            "Orientación (tipo obsoleto)", CategoriaSensor.POSICION,
            listOf(Eje("Azimut", "°"), Eje("Inclinación", "°"), Eje("Alabeo", "°")), derivado = true, seriesGrafica = 3
        ),
        TYPE_HINGE_ANGLE to DescriptorSensor("Ángulo de bisagra", CategoriaSensor.POSICION, listOf(Eje("Ángulo", "°")), seriesGrafica = 1),
        TYPE_HEADING to DescriptorSensor(
            "Rumbo", CategoriaSensor.POSICION, listOf(Eje("Rumbo", "°"), Eje("Precisión", "°")), seriesGrafica = 1
        ),

        TYPE_HEART_RATE to DescriptorSensor(
            "Frecuencia cardíaca", CategoriaSensor.OTROS, listOf(Eje("Frecuencia", "lpm")),
            permiso = PermisoSensor.DATOS_CORPORALES
        ),
        TYPE_HEART_BEAT to DescriptorSensor(
            "Latido cardíaco", CategoriaSensor.OTROS, listOf(Eje("Confianza (0–1)", null)),
            permiso = PermisoSensor.DATOS_CORPORALES
        ),
        TYPE_LOW_LATENCY_OFFBODY_DETECT to DescriptorSensor(
            "Detección fuera del cuerpo", CategoriaSensor.OTROS, listOf(Eje("Estado (1 = en el cuerpo)", null))
        ),
        TYPE_POSE_6DOF to DescriptorSensor("Pose de 6 grados de libertad", CategoriaSensor.OTROS, emptyList()),
        TYPE_HEAD_TRACKER to DescriptorSensor(
            "Seguimiento de cabeza", CategoriaSensor.OTROS,
            prefijado("Rotación", "rad") + prefijado("Velocidad", RADS) + Eje("Contador de discontinuidad", null)
        )
    )

    /** Tipos habituales que se indican como «No detectado» si faltan. */
    val tiposHabituales: List<Int> = listOf(
        TYPE_ACCELEROMETER, TYPE_GYROSCOPE, TYPE_GRAVITY, TYPE_LINEAR_ACCELERATION,
        TYPE_ROTATION_VECTOR, TYPE_GAME_ROTATION_VECTOR, TYPE_SIGNIFICANT_MOTION,
        TYPE_STEP_DETECTOR, TYPE_STEP_COUNTER,
        TYPE_LIGHT, TYPE_PROXIMITY, TYPE_PRESSURE, TYPE_AMBIENT_TEMPERATURE, TYPE_RELATIVE_HUMIDITY,
        TYPE_MAGNETIC_FIELD, TYPE_MAGNETIC_FIELD_UNCALIBRATED, TYPE_GEOMAGNETIC_ROTATION_VECTOR
    )

    /** null = tipo no catalogado (p. ej. propio del fabricante). */
    fun describir(tipo: Int): DescriptorSensor? = descriptores[tipo]

    fun categoria(tipo: Int): CategoriaSensor = describir(tipo)?.categoria ?: CategoriaSensor.OTROS

    fun esPrivadoFabricante(tipo: Int): Boolean = tipo >= TYPE_DEVICE_PRIVATE_BASE

    /** Tipos habituales que no aparecen entre los detectados. */
    fun noDetectados(tiposDetectados: Set<Int>): List<Int> =
        tiposHabituales.filterNot { it in tiposDetectados }
}
