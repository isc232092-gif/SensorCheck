package com.example.sensorcheck.domain.repository

import com.example.sensorcheck.domain.diagnostico.Compatibilidad
import com.example.sensorcheck.domain.diagnostico.IdPrueba
import com.example.sensorcheck.domain.diagnostico.RegistroDiagnostico

/** Consulta qué pruebas permite ejecutar este dispositivo (GRASP: Variaciones protegidas). */
interface CompatibilidadPruebasRepository {
    fun compatibilidad(id: IdPrueba): Compatibilidad

    /** Contactos simultáneos que Android declara para el panel táctil. */
    fun contactosDeclarados(): Int
}

/**
 * Historial de diagnósticos. Solo añade registros: nunca modifica ni
 * borra ejecuciones anteriores. Punto de integración del módulo Resultados.
 */
interface HistorialDiagnosticoRepository {
    suspend fun guardar(registro: RegistroDiagnostico)
    suspend fun leerTodos(): List<RegistroDiagnostico>
    fun dispositivo(): String
}
