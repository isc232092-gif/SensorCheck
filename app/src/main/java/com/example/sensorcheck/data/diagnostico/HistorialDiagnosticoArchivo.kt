package com.example.sensorcheck.data.diagnostico

import android.content.Context
import android.os.Build
import com.example.sensorcheck.domain.diagnostico.IdPrueba
import com.example.sensorcheck.domain.diagnostico.RegistroDiagnostico
import com.example.sensorcheck.domain.diagnostico.ResultadoPrueba
import com.example.sensorcheck.domain.repository.HistorialDiagnosticoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Historial en un archivo privado JSON Lines: una línea por ejecución,
 * siempre en modo «añadir». Las líneas dañadas se ignoran al leer.
 */
class HistorialDiagnosticoArchivo(context: Context) : HistorialDiagnosticoRepository {

    private val archivo = File(context.applicationContext.filesDir, ARCHIVO)
    private val candado = Mutex()

    override fun dispositivo(): String =
        listOf(Build.MANUFACTURER, Build.MODEL).filter { !it.isNullOrBlank() }.joinToString(" ")

    override suspend fun guardar(registro: RegistroDiagnostico) = withContext(Dispatchers.IO) {
        candado.withLock {
            archivo.appendText(aJson(registro).toString() + "\n")
        }
    }

    override suspend fun leerTodos(): List<RegistroDiagnostico> = withContext(Dispatchers.IO) {
        candado.withLock {
            if (!archivo.exists()) return@withLock emptyList()
            archivo.readLines().mapNotNull { linea ->
                runCatching { desdeJson(JSONObject(linea)) }.getOrNull()
            }
        }
    }

    private fun aJson(r: RegistroDiagnostico) = JSONObject().apply {
        put("version", 1)
        put("id", r.id)
        put("sesion", r.sesionId ?: JSONObject.NULL)
        put("fecha", r.fechaEpochMs)
        put("dispositivo", r.dispositivo)
        put("prueba", r.prueba.name)
        put("resultado", r.resultado.name)
        put("evidencia", JSONArray(r.evidencia))
        put("recomendacion", r.recomendacion)
    }

    private fun desdeJson(o: JSONObject): RegistroDiagnostico {
        val evidencia = o.getJSONArray("evidencia")
        return RegistroDiagnostico(
            id = o.getString("id"),
            sesionId = if (o.isNull("sesion")) null else o.getString("sesion"),
            fechaEpochMs = o.getLong("fecha"),
            dispositivo = o.getString("dispositivo"),
            prueba = IdPrueba.valueOf(o.getString("prueba")),
            resultado = ResultadoPrueba.valueOf(o.getString("resultado")),
            evidencia = List(evidencia.length()) { evidencia.getString(it) },
            recomendacion = o.getString("recomendacion")
        )
    }

    private companion object {
        const val ARCHIVO = "diagnosticos.jsonl"
    }
}
