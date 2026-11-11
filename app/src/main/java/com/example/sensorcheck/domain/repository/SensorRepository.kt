package com.example.sensorcheck.domain.repository

import com.example.sensorcheck.domain.model.EventoSensor
import com.example.sensorcheck.domain.model.SensorInfo
import kotlinx.coroutines.flow.Flow

/** Acceso a los sensores del dispositivo (GRASP: Indirección). */
interface SensorRepository {

    /** Sensores expuestos por Android (getSensorList(TYPE_ALL)). */
    fun obtenerSensores(): List<SensorInfo>

    /**
     * Lecturas de un único sensor. El listener se registra al empezar a
     * recolectar y se desregistra al cancelar la recolección.
     */
    fun monitorear(clave: String, periodoUs: Int): Flow<EventoSensor>
}
