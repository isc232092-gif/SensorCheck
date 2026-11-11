package com.example.sensorcheck.domain.sensors

import android.hardware.SensorManager
import com.example.sensorcheck.domain.model.ModoReporte
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale
import com.example.sensorcheck.domain.sensors.CatalogoSensores as C

class FormatoSensorTest {

    private val raiz = Locale.ROOT
    private val es = Locale.forLanguageTag("es-ES")

    @Test fun precision_constantesCoincidenConAndroid() {
        assertEquals(SensorManager.SENSOR_STATUS_NO_CONTACT, FormatoSensor.PRECISION_SIN_CONTACTO)
        assertEquals(SensorManager.SENSOR_STATUS_UNRELIABLE, FormatoSensor.PRECISION_NO_FIABLE)
        assertEquals(SensorManager.SENSOR_STATUS_ACCURACY_LOW, FormatoSensor.PRECISION_BAJA)
        assertEquals(SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM, FormatoSensor.PRECISION_MEDIA)
        assertEquals(SensorManager.SENSOR_STATUS_ACCURACY_HIGH, FormatoSensor.PRECISION_ALTA)
    }

    @Test fun numero_cuatroCifrasSignificativas() {
        assertEquals("9.807", FormatoSensor.numero(9.80665f, raiz))
        assertEquals("0.002396", FormatoSensor.numero(0.0023956299f, raiz))
        assertEquals("-0.1", FormatoSensor.numero(-0.1f, raiz))
        assertEquals("0", FormatoSensor.numero(0f, raiz))
    }

    @Test fun numero_grandeSinNotacionCientifica() =
        assertEquals("1013.3", FormatoSensor.numero(1013.25f, raiz))

    @Test fun numero_usaSeparadorDelIdioma() =
        assertEquals("9,807", FormatoSensor.numero(9.80665f, es))

    @Test fun numero_noFinito() = assertEquals("—", FormatoSensor.numero(Float.NaN, raiz))

    @Test fun conUnidad_ySinUnidad() {
        assertEquals("1013.3 hPa", FormatoSensor.conUnidad(1013.25f, "hPa", raiz))
        assertEquals("0.5", FormatoSensor.conUnidad(0.5f, null, raiz))
    }

    @Test fun retardoMinimo_segunModo() {
        assertEquals("10 ms · máx. ≈ 100 Hz", FormatoSensor.retardoMinimo(10_000, ModoReporte.CONTINUO, raiz))
        assertEquals("500 µs · máx. ≈ 2000 Hz", FormatoSensor.retardoMinimo(500, ModoReporte.CONTINUO, raiz))
        assertEquals("0 (informa solo al cambiar)", FormatoSensor.retardoMinimo(0, ModoReporte.AL_CAMBIO, raiz))
        assertEquals("No aplica (activación única)", FormatoSensor.retardoMinimo(-1, ModoReporte.UNICO, raiz))
    }

    @Test fun retardoMaximo_noReportado() {
        assertEquals("No reportado", FormatoSensor.retardoMaximo(0, raiz))
        assertEquals("200 ms · mín. ≈ 5 Hz", FormatoSensor.retardoMaximo(200_000, raiz))
    }

    @Test fun precision_textos() {
        assertEquals("Alta", FormatoSensor.precision(3))
        assertEquals("No fiable", FormatoSensor.precision(0))
        assertEquals("Código 7", FormatoSensor.precision(7))
        assertNull(FormatoSensor.precision(null))
    }

    @Test fun valores_acelerometroXYZConUnidad() {
        val v = FormatoSensor.valores(C.describir(C.TYPE_ACCELEROMETER), listOf(0.1f, 0.2f, 9.8f))
        assertEquals(listOf("X", "Y", "Z"), v.map { it.nombre })
        assertEquals(setOf("m/s²"), v.map { it.unidad }.toSet())
    }

    @Test fun valores_luzUnSoloValor() {
        val v = FormatoSensor.valores(C.describir(C.TYPE_LIGHT), listOf(250f))
        assertEquals(1, v.size)
        assertEquals("lx", v.single().unidad)
    }

    @Test fun valores_tipoDesconocidoSinUnidad() {
        val v = FormatoSensor.valores(null, listOf(1f, 2f))
        assertEquals(listOf("Valor 1", "Valor 2"), v.map { it.nombre })
        assertEquals(listOf(null, null), v.map { it.unidad })
    }

    @Test fun valores_extrasNoDocumentadosSinUnidad() {
        val v = FormatoSensor.valores(C.describir(C.TYPE_LIGHT), listOf(250f, 3f))
        assertEquals("Valor 2", v[1].nombre)
        assertNull(v[1].unidad)
    }

    @Test fun unidades_agrupaEjesConsecutivos() {
        assertEquals("X, Y, Z: m/s²", FormatoSensor.unidades(C.describir(C.TYPE_ACCELEROMETER)))
        assertEquals(
            "X, Y, Z, Sesgo X, Sesgo Y, Sesgo Z: μT",
            FormatoSensor.unidades(C.describir(C.TYPE_MAGNETIC_FIELD_UNCALIBRATED))
        )
        assertEquals("Iluminancia: lx", FormatoSensor.unidades(C.describir(C.TYPE_LIGHT)))
        assertNull(FormatoSensor.unidades(null))
    }

    @Test fun unidades_vectorRotacionAdimensionalYPrecisionEnRad() =
        assertEquals(
            "X·sen(θ/2), Y·sen(θ/2), Z·sen(θ/2), cos(θ/2): adimensional · Precisión estimada: rad",
            FormatoSensor.unidades(C.describir(C.TYPE_ROTATION_VECTOR))
        )

    @Test fun proximidad_binaria() {
        assertEquals("Lejos", FormatoSensor.proximidad(5f, 5f))
        assertEquals("Cerca", FormatoSensor.proximidad(0f, 5f))
        assertNull(FormatoSensor.proximidad(0f, 0f))
    }

    @Test fun fifo_yConsumo() {
        assertEquals("Sin cola por lotes", FormatoSensor.fifo(0, 0))
        assertEquals("Máximo 3000 eventos · reservado 300", FormatoSensor.fifo(3000, 300))
        assertEquals("No reportado", FormatoSensor.consumo(0f, raiz))
        assertEquals("0.15 mA", FormatoSensor.consumo(0.15f, raiz))
    }

    @Test fun antiguedad() {
        assertEquals("ahora", FormatoSensor.antiguedad(200))
        assertEquals("hace 4 s", FormatoSensor.antiguedad(4_500))
        assertEquals("hace 2 min", FormatoSensor.antiguedad(125_000))
    }
}
