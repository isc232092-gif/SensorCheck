package com.example.sensorcheck.domain.sensors

import android.hardware.Sensor
import com.example.sensorcheck.domain.model.CategoriaSensor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.sensorcheck.domain.sensors.CatalogoSensores as C

class CatalogoSensoresTest {

    /** Las constantes propias deben coincidir con android.hardware.Sensor (se inlinean al compilar). */
    @Test fun constantes_coincidenConAndroid() {
        val pares = listOf(
            C.TYPE_ACCELEROMETER to Sensor.TYPE_ACCELEROMETER,
            C.TYPE_MAGNETIC_FIELD to Sensor.TYPE_MAGNETIC_FIELD,
            C.TYPE_GYROSCOPE to Sensor.TYPE_GYROSCOPE,
            C.TYPE_LIGHT to Sensor.TYPE_LIGHT,
            C.TYPE_PRESSURE to Sensor.TYPE_PRESSURE,
            C.TYPE_PROXIMITY to Sensor.TYPE_PROXIMITY,
            C.TYPE_GRAVITY to Sensor.TYPE_GRAVITY,
            C.TYPE_LINEAR_ACCELERATION to Sensor.TYPE_LINEAR_ACCELERATION,
            C.TYPE_ROTATION_VECTOR to Sensor.TYPE_ROTATION_VECTOR,
            C.TYPE_RELATIVE_HUMIDITY to Sensor.TYPE_RELATIVE_HUMIDITY,
            C.TYPE_AMBIENT_TEMPERATURE to Sensor.TYPE_AMBIENT_TEMPERATURE,
            C.TYPE_MAGNETIC_FIELD_UNCALIBRATED to Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,
            C.TYPE_GAME_ROTATION_VECTOR to Sensor.TYPE_GAME_ROTATION_VECTOR,
            C.TYPE_GYROSCOPE_UNCALIBRATED to Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
            C.TYPE_SIGNIFICANT_MOTION to Sensor.TYPE_SIGNIFICANT_MOTION,
            C.TYPE_STEP_DETECTOR to Sensor.TYPE_STEP_DETECTOR,
            C.TYPE_STEP_COUNTER to Sensor.TYPE_STEP_COUNTER,
            C.TYPE_GEOMAGNETIC_ROTATION_VECTOR to Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,
            C.TYPE_HEART_RATE to Sensor.TYPE_HEART_RATE,
            C.TYPE_POSE_6DOF to Sensor.TYPE_POSE_6DOF,
            C.TYPE_STATIONARY_DETECT to Sensor.TYPE_STATIONARY_DETECT,
            C.TYPE_MOTION_DETECT to Sensor.TYPE_MOTION_DETECT,
            C.TYPE_HEART_BEAT to Sensor.TYPE_HEART_BEAT,
            C.TYPE_LOW_LATENCY_OFFBODY_DETECT to Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT,
            C.TYPE_ACCELEROMETER_UNCALIBRATED to Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
            C.TYPE_HINGE_ANGLE to Sensor.TYPE_HINGE_ANGLE,
            C.TYPE_HEAD_TRACKER to Sensor.TYPE_HEAD_TRACKER,
            C.TYPE_ACCELEROMETER_LIMITED_AXES to Sensor.TYPE_ACCELEROMETER_LIMITED_AXES,
            C.TYPE_GYROSCOPE_LIMITED_AXES to Sensor.TYPE_GYROSCOPE_LIMITED_AXES,
            C.TYPE_ACCELEROMETER_LIMITED_AXES_UNCALIBRATED to Sensor.TYPE_ACCELEROMETER_LIMITED_AXES_UNCALIBRATED,
            C.TYPE_GYROSCOPE_LIMITED_AXES_UNCALIBRATED to Sensor.TYPE_GYROSCOPE_LIMITED_AXES_UNCALIBRATED,
            C.TYPE_HEADING to Sensor.TYPE_HEADING,
            C.TYPE_DEVICE_PRIVATE_BASE to Sensor.TYPE_DEVICE_PRIVATE_BASE
        )
        pares.forEach { (propia, android) -> assertEquals(android, propia) }
    }

    private fun unidad(tipo: Int) = C.describir(tipo)!!.ejes.first().unidad

    @Test fun unidades_segunTipo() {
        assertEquals("m/s²", unidad(C.TYPE_ACCELEROMETER))
        assertEquals("rad/s", unidad(C.TYPE_GYROSCOPE))
        assertEquals("μT", unidad(C.TYPE_MAGNETIC_FIELD))
        assertEquals("lx", unidad(C.TYPE_LIGHT))
        assertEquals("cm", unidad(C.TYPE_PROXIMITY))
        assertEquals("hPa", unidad(C.TYPE_PRESSURE))
        assertEquals("%", unidad(C.TYPE_RELATIVE_HUMIDITY))
        assertEquals("°C", unidad(C.TYPE_AMBIENT_TEMPERATURE))
    }

    @Test fun ejesXYZ_enSensoresDeTresEjes() {
        listOf(C.TYPE_ACCELEROMETER, C.TYPE_GYROSCOPE, C.TYPE_MAGNETIC_FIELD).forEach { tipo ->
            val d = C.describir(tipo)!!
            assertEquals(listOf("X", "Y", "Z"), d.ejes.map { it.nombre })
            assertEquals(3, d.seriesGrafica)
        }
    }

    @Test fun vectorRotacion_esAdimensionalYDerivado() {
        val d = C.describir(C.TYPE_ROTATION_VECTOR)!!
        assertNull(d.unidadGrafica)
        assertTrue(d.derivado)
        assertEquals(4, d.seriesGrafica)
    }

    @Test fun sensoresDeEvento_noSonGraficables() {
        listOf(C.TYPE_SIGNIFICANT_MOTION, C.TYPE_STEP_DETECTOR, C.TYPE_STEP_COUNTER).forEach {
            assertEquals(0, C.describir(it)!!.seriesGrafica)
        }
    }

    @Test fun sensoresDePasos_requierenActividadFisica() {
        assertEquals(PermisoSensor.ACTIVIDAD_FISICA, C.describir(C.TYPE_STEP_COUNTER)!!.permiso)
        assertEquals(PermisoSensor.ACTIVIDAD_FISICA, C.describir(C.TYPE_STEP_DETECTOR)!!.permiso)
        assertNull(C.describir(C.TYPE_ACCELEROMETER)!!.permiso)
    }

    @Test fun categorias_principales() {
        assertEquals(CategoriaSensor.MOVIMIENTO, C.categoria(C.TYPE_GYROSCOPE))
        assertEquals(CategoriaSensor.AMBIENTAL, C.categoria(C.TYPE_LIGHT))
        assertEquals(CategoriaSensor.POSICION, C.categoria(C.TYPE_MAGNETIC_FIELD))
    }

    @Test fun tipoDesconocido_noSeDescribeYVaAOtros() {
        assertNull(C.describir(65_537))
        assertEquals(CategoriaSensor.OTROS, C.categoria(65_537))
        assertTrue(C.esPrivadoFabricante(65_537))
        assertFalse(C.esPrivadoFabricante(C.TYPE_LIGHT))
    }

    @Test fun telefonoSinBarometroNiHumedad_losMarcaNoDetectados() {
        val detectados = C.tiposHabituales.toSet() - setOf(C.TYPE_PRESSURE, C.TYPE_RELATIVE_HUMIDITY)
        assertEquals(listOf(C.TYPE_PRESSURE, C.TYPE_RELATIVE_HUMIDITY).sorted(), C.noDetectados(detectados).sorted())
    }

    @Test fun inventarioVacio_todosLosHabitualesNoDetectados() =
        assertEquals(C.tiposHabituales, C.noDetectados(emptySet()))

    @Test fun inventarioCompleto_ningunoNoDetectado() =
        assertTrue(C.noDetectados(C.tiposHabituales.toSet()).isEmpty())
}
