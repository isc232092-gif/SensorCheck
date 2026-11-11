package com.example.sensorcheck.domain.diagnostico

/**
 * Estado inmutable del diagnóstico guiado: una prueba por vez, en orden.
 * Lógica pura para poder probarla sin Android.
 */
data class SesionDiagnostico(
    val id: String,
    val cola: List<IdPrueba>,
    val indice: Int = 0,
    val resultados: Map<IdPrueba, ResultadoEvaluado> = emptyMap()
) {
    val actual: IdPrueba? get() = cola.getOrNull(indice)
    val terminada: Boolean get() = indice >= cola.size
    val total: Int get() = cola.size

    /** Fracción completada entre 0 y 1. */
    val progreso: Float get() = if (cola.isEmpty()) 1f else indice.coerceAtMost(cola.size).toFloat() / cola.size

    fun registrar(resultado: ResultadoEvaluado): SesionDiagnostico {
        val id = actual ?: return this
        return copy(indice = indice + 1, resultados = resultados + (id to resultado))
    }

    fun conteo(): Map<ResultadoPrueba, Int> =
        resultados.values.groupingBy { it.resultado }.eachCount()

    /** Pruebas que conviene repetir: fallidas, indeterminadas u omitidas. */
    fun paraRepetir(): List<IdPrueba> = cola.filter {
        resultados[it]?.resultado in REPETIBLES
    }

    companion object {
        val REPETIBLES = setOf(ResultadoPrueba.FALLO, ResultadoPrueba.INDETERMINADO, ResultadoPrueba.NO_EJECUTADO)

        /**
         * Crea la cola con las pruebas pedidas que pueden ejecutarse (implementadas
         * y compatibles), respetando el orden del catálogo.
         */
        fun crear(
            id: String,
            solicitadas: Collection<IdPrueba>,
            ejecutable: (IdPrueba) -> Boolean
        ): SesionDiagnostico {
            val cola = CatalogoPruebas.pruebas.map { it.id }.filter { it in solicitadas && ejecutable(it) }
            return SesionDiagnostico(id, cola)
        }
    }
}

/** Último resultado significativo: se ignoran las ejecuciones canceladas u omitidas. */
fun ultimoResultado(registros: List<RegistroDiagnostico>, prueba: IdPrueba): RegistroDiagnostico? =
    registros.filter { it.prueba == prueba && it.resultado != ResultadoPrueba.NO_EJECUTADO }
        .maxByOrNull { it.fechaEpochMs }
