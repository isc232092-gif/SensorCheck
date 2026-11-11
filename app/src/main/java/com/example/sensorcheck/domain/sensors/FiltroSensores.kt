package com.example.sensorcheck.domain.sensors

import com.example.sensorcheck.domain.model.CategoriaSensor
import com.example.sensorcheck.domain.model.SensorInfo
import java.text.Normalizer
import java.util.Locale

/** Sensor detectado junto con su significado según el catálogo. */
data class ElementoSensor(
    val info: SensorInfo,
    val descriptor: DescriptorSensor?,
    val categoria: CategoriaSensor
) {
    /** Nombre legible del tipo; para tipos no catalogados, su stringType o número. */
    val nombreTipo: String
        get() = descriptor?.nombre ?: info.stringType ?: "Tipo ${info.tipo}"

    companion object {
        fun de(info: SensorInfo) = ElementoSensor(
            info = info,
            descriptor = CatalogoSensores.describir(info.tipo),
            categoria = CatalogoSensores.categoria(info.tipo)
        )
    }
}

/** Búsqueda y agrupación del inventario (lógica pura, sin Android). */
object FiltroSensores {

    fun filtrar(
        sensores: List<ElementoSensor>,
        texto: String,
        categoria: CategoriaSensor?
    ): List<ElementoSensor> {
        val consulta = normalizar(texto)
        return sensores.filter { elemento ->
            (categoria == null || elemento.categoria == categoria) &&
                (consulta.isEmpty() || coincide(elemento, consulta))
        }
    }

    fun agrupar(sensores: List<ElementoSensor>): Map<CategoriaSensor, List<ElementoSensor>> =
        CategoriaSensor.entries
            .associateWith { cat -> sensores.filter { it.categoria == cat } }
            .filterValues { it.isNotEmpty() }

    private fun coincide(e: ElementoSensor, consulta: String): Boolean =
        listOfNotNull(e.info.nombre, e.nombreTipo, e.info.fabricante, e.info.stringType)
            .any { normalizar(it).contains(consulta) }

    /** Minúsculas y sin tildes: "Presión" coincide con "presion". */
    fun normalizar(texto: String): String =
        Normalizer.normalize(texto.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
}
