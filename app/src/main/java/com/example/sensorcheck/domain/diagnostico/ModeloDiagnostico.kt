package com.example.sensorcheck.domain.diagnostico

/** Resultado de una prueba física. Solo CORRECTO implica que se verificó el componente. */
enum class ResultadoPrueba(val etiqueta: String) {
    CORRECTO("Correcto"),
    FALLO("Falló"),
    INDETERMINADO("Indeterminado"),
    NO_COMPATIBLE("No compatible"),
    NO_EJECUTADO("No ejecutado")
}

enum class CategoriaPrueba(val titulo: String) {
    PANTALLA("Pantalla"),
    MOVIMIENTO("Movimiento y sensores"),
    CAMARA("Cámara"),
    AUDIO("Audio"),
    VIBRACION("Vibración"),
    CONECTIVIDAD("Conectividad"),
    OTROS("Otros componentes")
}

enum class IdPrueba {
    TACTIL, MULTITACTIL, COLORES, PIXELES,
    ACELEROMETRO, GIROSCOPIO, PROXIMIDAD, LUZ, MAGNETOMETRO,
    CAMARA_TRASERA, CAMARA_FRONTAL, FLASH,
    ALTAVOZ, MICROFONO,
    VIBRACION,
    WIFI, BLUETOOTH, GPS,
    BATERIA, ALMACENAMIENTO, RAM, BOTONES
}

/**
 * Descripción de una prueba (qué se comprueba y con qué criterio).
 * @param implementada false = aún no hay ejecutor; nunca se presenta como ejecutable.
 * @param permisos permisos de tiempo de ejecución que se piden justo antes de iniciar.
 */
data class DefinicionPrueba(
    val id: IdPrueba,
    val nombre: String,
    val componente: String,
    val categoria: CategoriaPrueba,
    val instrucciones: List<String>,
    val criterio: String,
    val recomendacionFallo: String,
    val implementada: Boolean,
    val permisos: List<PermisoPrueba> = emptyList()
)

/** Permiso de Android con el rango de versiones en que debe solicitarse. */
data class PermisoPrueba(
    val permiso: String,
    val apiMinima: Int = 1,
    val apiMaxima: Int = Int.MAX_VALUE,
    val descripcion: String
) {
    fun aplica(api: Int): Boolean = api in apiMinima..apiMaxima
}

/** Si Android ofrece acceso suficiente para ejecutar la prueba en este dispositivo. */
sealed interface Compatibilidad {
    data object Compatible : Compatibilidad
    data class NoCompatible(val motivo: String) : Compatibilidad
}

/** Resultado producido por un evaluador, con la evidencia que lo justifica. */
data class ResultadoEvaluado(
    val resultado: ResultadoPrueba,
    val evidencia: List<String>,
    val recomendacion: String
)

/** Entrada del historial. Cada ejecución crea un registro nuevo; nunca se sobrescribe. */
data class RegistroDiagnostico(
    val id: String,
    val sesionId: String?,
    val fechaEpochMs: Long,
    val dispositivo: String,
    val prueba: IdPrueba,
    val resultado: ResultadoPrueba,
    val evidencia: List<String>,
    val recomendacion: String
)
