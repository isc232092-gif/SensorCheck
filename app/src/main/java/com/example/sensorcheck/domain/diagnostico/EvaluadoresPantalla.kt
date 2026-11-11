package com.example.sensorcheck.domain.diagnostico

/** Prueba táctil por zonas: cada celda debe registrar al menos un contacto. */
class EvaluadorTactil(val columnas: Int, val filas: Int) {

    init {
        require(columnas > 0 && filas > 0)
    }

    private val tocadas = BooleanArray(columnas * filas)
    var conteo = 0
        private set
    val total: Int get() = columnas * filas
    val completo: Boolean get() = conteo == total

    fun tocada(columna: Int, fila: Int): Boolean =
        tocadas[fila * columnas + columna]

    /** @return true si la zona no estaba marcada. */
    fun marcar(columna: Int, fila: Int): Boolean {
        if (columna !in 0 until columnas || fila !in 0 until filas) return false
        val i = fila * columnas + columna
        if (tocadas[i]) return false
        tocadas[i] = true
        conteo++
        return true
    }

    fun evaluar(usuarioReportaFallo: Boolean): ResultadoEvaluado {
        val evidencia = mutableListOf(
            "Cuadrícula de $columnas × $filas zonas",
            "Zonas con contacto: $conteo de $total"
        )
        val faltantes = (0 until total).filterNot { tocadas[it] }
        if (faltantes.isNotEmpty()) {
            val lista = faltantes.take(MAX_LISTADAS).joinToString { "f${it / columnas + 1}·c${it % columnas + 1}" }
            val resto = if (faltantes.size > MAX_LISTADAS) " y ${faltantes.size - MAX_LISTADAS} más" else ""
            evidencia += "Zonas sin contacto: $lista$resto"
        }
        val resultado = when {
            completo -> ResultadoPrueba.CORRECTO
            usuarioReportaFallo -> {
                evidencia += "El usuario indicó que esas zonas no responden"
                ResultadoPrueba.FALLO
            }
            else -> ResultadoPrueba.INDETERMINADO
        }
        return ResultadoEvaluado(resultado, evidencia, CatalogoPruebas.recomendacion(IdPrueba.TACTIL, resultado))
    }

    private companion object {
        const val MAX_LISTADAS = 10
    }
}

/** Multitáctil: se exige el número de contactos simultáneos que declara Android. */
class EvaluadorMultitactil(val requeridos: Int) {

    var maximo = 0
        private set
    val alcanzado: Boolean get() = maximo >= requeridos

    fun registrar(simultaneos: Int) {
        if (simultaneos > maximo) maximo = simultaneos
    }

    fun evaluar(usuarioReportaFallo: Boolean): ResultadoEvaluado {
        val evidencia = mutableListOf(
            "Contactos simultáneos detectados: $maximo",
            "Contactos requeridos según Android: $requeridos"
        )
        val resultado = when {
            alcanzado -> ResultadoPrueba.CORRECTO
            usuarioReportaFallo -> {
                evidencia += "El usuario indicó que no se detectan todos los dedos"
                ResultadoPrueba.FALLO
            }
            else -> ResultadoPrueba.INDETERMINADO
        }
        return ResultadoEvaluado(resultado, evidencia, CatalogoPruebas.recomendacion(IdPrueba.MULTITACTIL, resultado))
    }
}

enum class RespuestaVisual { SIN_DEFECTOS, CON_DEFECTOS, NO_SEGURO }

/**
 * Pruebas de inspección visual (colores y píxeles). El teléfono no puede
 * observar su propio panel: la evidencia es la respuesta del usuario tras
 * haber visto todas las pantallas.
 */
object EvaluadorVisual {

    fun evaluar(id: IdPrueba, pantallasVistas: List<String>, respuesta: RespuestaVisual): ResultadoEvaluado {
        val evidencia = listOf(
            "Pantallas mostradas a brillo máximo: ${pantallasVistas.joinToString()}",
            "Respuesta del usuario: " + when (respuesta) {
                RespuestaVisual.SIN_DEFECTOS -> "no observó defectos"
                RespuestaVisual.CON_DEFECTOS -> "observó defectos"
                RespuestaVisual.NO_SEGURO -> "no está seguro"
            },
            "Resultado basado en inspección visual del usuario"
        )
        val resultado = when (respuesta) {
            RespuestaVisual.SIN_DEFECTOS -> ResultadoPrueba.CORRECTO
            RespuestaVisual.CON_DEFECTOS -> ResultadoPrueba.FALLO
            RespuestaVisual.NO_SEGURO -> ResultadoPrueba.INDETERMINADO
        }
        return ResultadoEvaluado(resultado, evidencia, CatalogoPruebas.recomendacion(id, resultado))
    }
}
