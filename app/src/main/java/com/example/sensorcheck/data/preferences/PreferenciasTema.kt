package com.example.sensorcheck.data.preferences

import android.content.Context
import androidx.core.content.edit

/**
 * Persistencia de la preferencia de tema (GRASP: Indirección).
 * Mantiene las mismas claves usadas anteriormente para no perder
 * la elección ya guardada por el usuario.
 */
class PreferenciasTema(context: Context) {

    private val preferencias = context.getSharedPreferences(
        ARCHIVO,
        Context.MODE_PRIVATE
    )

    /** null = el usuario aún no eligió; se sigue el tema del sistema. */
    fun leerModoOscuro(): Boolean? =
        if (preferencias.contains(CLAVE_MODO_OSCURO)) {
            preferencias.getBoolean(CLAVE_MODO_OSCURO, false)
        } else {
            null
        }

    fun guardarModoOscuro(oscuro: Boolean) {
        preferencias.edit { putBoolean(CLAVE_MODO_OSCURO, oscuro) }
    }

    private companion object {
        const val ARCHIVO = "sensorcheck_preferencias"
        const val CLAVE_MODO_OSCURO = "modo_oscuro"
    }
}
