package com.example.sensorcheck.domain.sensors

import com.example.sensorcheck.domain.model.CategoriaSensor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.example.sensorcheck.domain.sensors.CatalogoSensores as C

class FiltroSensoresTest {

    private val inventario = listOf(
        sensorPrueba(C.TYPE_ACCELEROMETER, "LSM6DSO Accelerometer", "STMicro"),
        sensorPrueba(C.TYPE_PRESSURE, "BMP380 Pressure", "Bosch"),
        sensorPrueba(C.TYPE_MAGNETIC_FIELD, "AK09918 Magnetometer", "AKM"),
        sensorPrueba(65_600, "Vendor Hall", "Samsung", stringType = "com.samsung.sensor.hall")
    ).map { ElementoSensor.de(it) }

    @Test fun busqueda_ignoraTildesYMayusculas() {
        val r = FiltroSensores.filtrar(inventario, "PRESION", null)
        assertEquals(listOf("BMP380 Pressure"), r.map { it.info.nombre })
    }

    @Test fun busqueda_porFabricante() =
        assertEquals(1, FiltroSensores.filtrar(inventario, "bosch", null).size)

    @Test fun busqueda_porStringType() =
        assertEquals(1, FiltroSensores.filtrar(inventario, "samsung.sensor", null).size)

    @Test fun filtro_porCategoria() {
        val r = FiltroSensores.filtrar(inventario, "", CategoriaSensor.POSICION)
        assertEquals(listOf(C.TYPE_MAGNETIC_FIELD), r.map { it.info.tipo })
    }

    @Test fun agrupar_omiteCategoriasVaciasYRespetaOrden() {
        val grupos = FiltroSensores.agrupar(inventario)
        assertEquals(
            listOf(CategoriaSensor.MOVIMIENTO, CategoriaSensor.AMBIENTAL, CategoriaSensor.POSICION, CategoriaSensor.OTROS),
            grupos.keys.toList()
        )
        assertTrue(FiltroSensores.agrupar(inventario.take(1)).keys == setOf(CategoriaSensor.MOVIMIENTO))
    }

    @Test fun tipoNoCatalogado_usaStringTypeOTipoNumerico() {
        assertEquals("com.samsung.sensor.hall", inventario.last().nombreTipo)
        assertEquals("Tipo 70000", ElementoSensor.de(sensorPrueba(70_000)).nombreTipo)
    }
}
