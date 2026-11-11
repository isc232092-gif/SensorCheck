package com.example.sensorcheck.data.system

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import com.example.sensorcheck.domain.model.CompilacionInfo
import com.example.sensorcheck.domain.model.EstadoCifrado
import com.example.sensorcheck.domain.model.KernelInfo
import com.example.sensorcheck.domain.model.RegionalInfo
import com.example.sensorcheck.domain.model.SeguridadInfo
import com.example.sensorcheck.domain.model.SistemaOperativoInfo
import com.example.sensorcheck.domain.model.SystemInfo
import com.example.sensorcheck.domain.model.TiempoActividad
import com.example.sensorcheck.domain.repository.SystemRepository
import java.io.File
import java.util.Locale
import java.util.TimeZone

/**
 * Obtiene la información del sistema con APIs públicas de Android
 * (Build, Settings.Global, Configuration, TimeZone, SystemClock).
 * No requiere permisos, root ni comandos privilegiados.
 */
class AndroidSystemRepository(
    private val context: Context
) : SystemRepository {

    override fun obtenerInformacion(): SystemInfo = SystemInfo(
        sistemaOperativo = SistemaOperativoInfo(
            nombre = "Android",
            version = Build.VERSION.RELEASE,
            nivelApi = Build.VERSION.SDK_INT,
            idCompilacion = Build.ID.limpio(),
            numeroCompilacion = Build.DISPLAY.limpio()
        ),
        seguridad = leerSeguridad(),
        kernel = leerKernel(),
        regional = leerRegional(),
        compilacion = leerCompilacion()
    )

    override fun obtenerTiempoActividad(): TiempoActividad {
        // elapsedRealtime incluye el tiempo en reposo profundo desde el arranque
        val desdeArranque = SystemClock.elapsedRealtime()
        return TiempoActividad(
            desdeArranqueMs = desdeArranque,
            arranqueEpochMs = System.currentTimeMillis() - desdeArranque
        )
    }

    private fun leerSeguridad(): SeguridadInfo {
        val resolver = context.contentResolver

        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val politicas = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager

        return SeguridadInfo(
            parcheSeguridad = Build.VERSION.SECURITY_PATCH.limpio(),
            tipoCompilacion = Build.TYPE.limpio(),
            etiquetas = Build.TAGS.limpio(),
            depuracionUsbActiva = leerAjusteGlobal(resolver, Settings.Global.ADB_ENABLED),
            opcionesDesarrolladorActivas = leerAjusteGlobal(
                resolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED
            ),
            bloqueoPantallaSeguro = runCatching { keyguard?.isDeviceSecure }.getOrNull(),
            cifrado = when (runCatching { politicas?.storageEncryptionStatus }.getOrNull()) {
                DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE,
                DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY -> EstadoCifrado.ACTIVO
                DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER -> EstadoCifrado.ACTIVO_POR_USUARIO
                DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE -> EstadoCifrado.INACTIVO
                DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED -> EstadoCifrado.NO_COMPATIBLE
                else -> EstadoCifrado.DESCONOCIDO
            }
        )
    }

    private fun leerKernel(): KernelInfo = KernelInfo(
        version = System.getProperty("os.version").limpio(),
        cadenaCompleta = leerProcVersion(),
        arquitectura = System.getProperty("os.arch").limpio(),
        abis = Build.SUPPORTED_ABIS.toList(),
        abis64 = Build.SUPPORTED_64_BIT_ABIS.toList(),
        abis32 = Build.SUPPORTED_32_BIT_ABIS.toList()
    )

    private fun leerRegional(): RegionalInfo {
        val locales = context.resources.configuration.locales
        val locale: Locale? = if (locales.isEmpty) null else locales[0]
        val zona = TimeZone.getDefault()
        val ahora = System.currentTimeMillis()
        val textoLocale = locale ?: Locale.getDefault()

        return RegionalInfo(
            idioma = locale?.getDisplayLanguage(locale).limpio(),
            codigoIdioma = locale?.language.limpio(),
            region = locale?.getDisplayCountry(locale).limpio(),
            codigoRegion = locale?.country.limpio(),
            etiquetaLocale = locale?.toLanguageTag().limpio(),
            idiomasPreferidos = (0 until locales.size()).map {
                val l = locales[it]
                l.getDisplayName(l)
            },
            zonaHorariaId = zona.id.limpio(),
            zonaHorariaNombre = zona.getDisplayName(
                zona.inDaylightTime(java.util.Date(ahora)),
                TimeZone.LONG,
                textoLocale
            ).limpio(),
            desfaseUtcMs = zona.getOffset(ahora)
        )
    }

    private fun leerCompilacion(): CompilacionInfo = CompilacionInfo(
        huella = Build.FINGERPRINT.limpio(),
        incremental = Build.VERSION.INCREMENTAL.limpio(),
        etiquetas = Build.TAGS.limpio(),
        codename = Build.VERSION.CODENAME.limpio(),
        sistemaBase = Build.VERSION.BASE_OS.limpio(),
        fechaCompilacionMs = Build.TIME.takeIf { it > 0 },
        host = Build.HOST.limpio(),
        usuario = Build.USER.limpio(),
        bootloader = Build.BOOTLOADER.limpio(),
        bandaBase = runCatching { Build.getRadioVersion() }.getOrNull().limpio(),
        versionArt = System.getProperty("java.vm.version").limpio(),
        claseRendimientoMultimedia =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Build.VERSION.MEDIA_PERFORMANCE_CLASS
            } else {
                null
            }
    )

    /** Lee un ajuste global público (0/1). null si no existe o no es accesible. */
    private fun leerAjusteGlobal(
        resolver: android.content.ContentResolver,
        nombre: String
    ): Boolean? = runCatching {
        when (Settings.Global.getInt(resolver, nombre, -1)) {
            1 -> true
            0 -> false
            else -> null
        }
    }.getOrNull()

    /** /proc/version puede no ser legible según la política SELinux del fabricante. */
    private fun leerProcVersion(): String? = runCatching {
        File("/proc/version").readText().trim()
    }.getOrNull().limpio()

    /** Normaliza valores vacíos o "unknown" a null (= No disponible). */
    private fun String?.limpio(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() && !it.equals(Build.UNKNOWN, ignoreCase = true) }
}
