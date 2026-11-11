package com.example.sensorcheck.domain.sensors

import com.example.sensorcheck.domain.model.ModoReporte
import com.example.sensorcheck.domain.model.SensorInfo
import com.example.sensorcheck.domain.model.SensorReading

fun sensorPrueba(
    tipo: Int,
    nombre: String = "Sensor $tipo",
    fabricante: String? = "Fabricante",
    stringType: String? = null,
    modo: ModoReporte = ModoReporte.CONTINUO,
    rangoMaximo: Float = 10f
) = SensorInfo(
    clave = "$tipo|$nombre",
    nombre = nombre,
    tipo = tipo,
    stringType = stringType,
    fabricante = fabricante,
    version = 1,
    resolucion = 0.01f,
    rangoMaximo = rangoMaximo,
    consumoMa = 0.2f,
    retardoMinimoUs = 10_000,
    retardoMaximoUs = 200_000,
    modoReporte = modo,
    despiertaDispositivo = false,
    fifoMaximo = 0,
    fifoReservado = 0,
    dinamico = false,
    idSensor = null,
    permisoRequerido = null
)

fun lecturaPrueba(tNs: Long, vararg valores: Float, precision: Int? = 3) =
    SensorReading(valores.toList(), tNs, tNs / 1_000_000, precision)
