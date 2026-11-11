package com.example.sensorcheck.domain.sensors

import com.example.sensorcheck.domain.model.EventoSensor
import com.example.sensorcheck.domain.model.RitmoMuestreo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VentanaMuestrasTest {

    @Test fun limitaLaCapacidadYConservaLasMasRecientes() {
        val v = VentanaMuestras(capacidad = 3, series = 1)
        (1..5).forEach { v.agregar(it * 1_000_000_000L, listOf(it.toFloat())) }
        val s = v.instantanea()
        assertEquals(3, s.tamano)
        assertEquals(listOf(3f, 4f, 5f), s.series[0].toList())
        assertEquals(3_000_000_000L, s.tiemposNs.first())
    }

    @Test fun valoresFaltantesONoFinitos_seGuardanComoNaN() {
        val v = VentanaMuestras(capacidad = 4, series = 3)
        v.agregar(1, listOf(1f))
        v.agregar(2, listOf(1f, Float.POSITIVE_INFINITY, 2f))
        val s = v.instantanea()
        assertTrue(s.series[1][0].isNaN())
        assertTrue(s.series[2][0].isNaN())
        assertTrue(s.series[1][1].isNaN())
        assertEquals(2f, s.series[2][1])
    }

    @Test fun instantanea_esUnaCopiaIndependiente() {
        val v = VentanaMuestras(capacidad = 2, series = 1)
        v.agregar(1, listOf(1f))
        val s = v.instantanea()
        v.agregar(2, listOf(2f))
        assertEquals(1, s.tamano)
    }

    @Test fun vacia_sinRangoNiFrecuencia() {
        val s = VentanaMuestras(4, 1).instantanea()
        assertNull(s.rangoVisible())
        assertNull(s.frecuenciaHz)
    }

    @Test fun rango_valorConstanteNoEsNulo() {
        val v = VentanaMuestras(4, 1)
        v.agregar(0, listOf(5f))
        v.agregar(1, listOf(5f))
        val (min, max) = v.instantanea().rangoVisible()!!
        assertTrue(min < 5f && max > 5f)
    }

    @Test fun rango_ignoraNaN() {
        val v = VentanaMuestras(4, 2)
        v.agregar(0, listOf(-2f))
        v.agregar(1, listOf(4f, 1f))
        assertEquals(-2f to 4f, v.instantanea().extremos())
    }

    @Test fun frecuencia_aPartirDeMarcasDeTiempo() {
        val v = VentanaMuestras(20, 1)
        (0..10).forEach { v.agregar(it * 100_000_000L, listOf(0f)) } // 10 Hz durante 1 s
        assertEquals(10f, v.instantanea().frecuenciaHz!!, 0.001f)
    }

    @Test fun indiceCercano() {
        val v = VentanaMuestras(4, 1)
        listOf(0L, 100L, 200L).forEach { v.agregar(it, listOf(0f)) }
        assertEquals(1, v.instantanea().indiceCercano(130))
    }
}

class AcumuladorLecturasTest {

    private val ritmo = RitmoMuestreo.INTERFAZ

    @Test fun inicio_esperaHastaElPrimerEvento() {
        val a = AcumuladorLecturas(series = 3)
        a.iniciar()
        val e = a.estado(ritmo)
        assertEquals(FaseMonitoreo.ESPERANDO, e.fase)
        assertTrue(e.activo)
        assertNull(e.ultima)
    }

    @Test fun lecturas_actualizanUltimaContadorYVentana() {
        val a = AcumuladorLecturas(series = 3)
        a.iniciar()
        a.aplicar(EventoSensor.Lectura(lecturaPrueba(1, 0.1f, 0.2f, 9.8f)))
        a.aplicar(EventoSensor.Lectura(lecturaPrueba(2, 0.0f, 0.3f, 9.7f)))
        val e = a.estado(ritmo)
        assertEquals(FaseMonitoreo.RECIBIENDO, e.fase)
        assertEquals(2, e.eventos)
        assertEquals(listOf(0.0f, 0.3f, 9.7f), e.ultima!!.valores)
        assertEquals(2, e.muestras.tamano)
    }

    @Test fun cambioDePrecision_seConservaSiLaLecturaNoLaTrae() {
        val a = AcumuladorLecturas(series = 1)
        a.iniciar()
        a.aplicar(EventoSensor.CambioPrecision(1))
        a.aplicar(EventoSensor.Lectura(lecturaPrueba(1, 5f, precision = null)))
        assertEquals(1, a.estado(ritmo).precision)
    }

    @Test fun capacidad_noCreceIndefinidamente() {
        val a = AcumuladorLecturas(series = 1, capacidad = 10)
        a.iniciar()
        repeat(1_000) { a.aplicar(EventoSensor.Lectura(lecturaPrueba(it.toLong(), it.toFloat()))) }
        val e = a.estado(ritmo)
        assertEquals(10, e.muestras.tamano)
        assertEquals(1_000, e.eventos)
    }

    @Test fun pausaYReanudacion_reiniciaLaGrafica() {
        val a = AcumuladorLecturas(series = 1)
        a.iniciar()
        a.aplicar(EventoSensor.Lectura(lecturaPrueba(1, 1f)))
        a.pausar()
        assertEquals(FaseMonitoreo.PAUSADO, a.fase)
        assertFalse(a.estado(ritmo).activo)
        a.reanudar()
        val e = a.estado(ritmo)
        assertEquals(FaseMonitoreo.ESPERANDO, e.fase)
        assertEquals(0, e.muestras.tamano)
        assertEquals(1f, e.ultima!!.valores.single())
    }

    @Test fun pausar_sinMonitoreoNoCambiaNada() {
        val a = AcumuladorLecturas(series = 1)
        a.pausar()
        assertEquals(FaseMonitoreo.INACTIVO, a.fase)
    }

    @Test fun detener_conservaLaUltimaLectura() {
        val a = AcumuladorLecturas(series = 1)
        a.iniciar()
        a.aplicar(EventoSensor.Lectura(lecturaPrueba(1, 3f)))
        a.detener()
        val e = a.estado(ritmo)
        assertEquals(FaseMonitoreo.INACTIVO, e.fase)
        assertEquals(3f, e.ultima!!.valores.single())
    }

    @Test fun disparoUnico_terminaEnDisparado() {
        val a = AcumuladorLecturas(series = 0)
        a.iniciar()
        a.aplicar(EventoSensor.Disparo(lecturaPrueba(5, 1f, precision = null)))
        a.detener()
        val e = a.estado(ritmo)
        assertEquals(FaseMonitoreo.DISPARADO, e.fase)
        assertEquals(1, e.eventos)
        assertEquals(0, e.muestras.tamano)
    }

    @Test fun error_conservaElPrimerMensaje() {
        val a = AcumuladorLecturas(series = 1)
        a.iniciar()
        a.aplicar(EventoSensor.Error("Permiso denegado"))
        a.aplicar(EventoSensor.Error("No se pudo registrar"))
        a.detener()
        val e = a.estado(ritmo)
        assertEquals(FaseMonitoreo.ERROR, e.fase)
        assertEquals("Permiso denegado", e.error)
    }

    @Test fun reinicio_limpiaElError() {
        val a = AcumuladorLecturas(series = 1)
        a.iniciar()
        a.fallar("x")
        a.iniciar()
        assertNull(a.estado(ritmo).error)
        assertEquals(FaseMonitoreo.ESPERANDO, a.fase)
    }
}
