package com.example.sensorcheck.domain.model

/**
 * Información del sistema leída de APIs públicas de Android.
 * Se guardan valores en bruto; el formato se aplica en presentación.
 * null = Android no proporciona el dato en este dispositivo/versión.
 */
data class SystemInfo(
    val sistemaOperativo: SistemaOperativoInfo,
    val seguridad: SeguridadInfo,
    val kernel: KernelInfo,
    val regional: RegionalInfo,
    val compilacion: CompilacionInfo
)

data class SistemaOperativoInfo(
    val nombre: String,
    val version: String,
    val nivelApi: Int,
    val idCompilacion: String?,
    val numeroCompilacion: String?
)

data class SeguridadInfo(
    /** Formato AAAA-MM-DD tal como lo reporta Build.VERSION.SECURITY_PATCH. */
    val parcheSeguridad: String?,
    val tipoCompilacion: String?,
    val etiquetas: String?,
    val depuracionUsbActiva: Boolean?,
    val opcionesDesarrolladorActivas: Boolean?,
    val bloqueoPantallaSeguro: Boolean?,
    val cifrado: EstadoCifrado
)

enum class EstadoCifrado {
    ACTIVO,
    ACTIVO_POR_USUARIO,
    INACTIVO,
    NO_COMPATIBLE,
    DESCONOCIDO
}

data class KernelInfo(
    /** System.getProperty("os.version"). */
    val version: String?,
    /** Contenido de /proc/version, si Android permite leerlo. */
    val cadenaCompleta: String?,
    /** System.getProperty("os.arch"). */
    val arquitectura: String?,
    val abis: List<String>,
    val abis64: List<String>,
    val abis32: List<String>
)

data class RegionalInfo(
    val idioma: String?,
    val codigoIdioma: String?,
    val region: String?,
    val codigoRegion: String?,
    val etiquetaLocale: String?,
    val idiomasPreferidos: List<String>,
    val zonaHorariaId: String?,
    val zonaHorariaNombre: String?,
    val desfaseUtcMs: Int?
)

data class CompilacionInfo(
    val huella: String?,
    val incremental: String?,
    val etiquetas: String?,
    val codename: String?,
    val sistemaBase: String?,
    val fechaCompilacionMs: Long?,
    val host: String?,
    val usuario: String?,
    val bootloader: String?,
    val bandaBase: String?,
    val versionArt: String?,
    /** Build.VERSION.MEDIA_PERFORMANCE_CLASS (API 31+); 0 = no declarada. */
    val claseRendimientoMultimedia: Int?
)

/** Tiempo desde el último arranque (no es tiempo total de uso). */
data class TiempoActividad(
    val desdeArranqueMs: Long,
    val arranqueEpochMs: Long
)
