package com.example.sensorcheck.domain.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class FormatoSistemaTest {

    private val es = Locale.forLanguageTag("es-MX")

    private fun ms(d: Long = 0, h: Long = 0, m: Long = 0, s: Long = 0) =
        ((d * 24 + h) * 60 + m) * 60_000 + s * 1_000

    // ---- Tiempo de actividad ----

    @Test fun tiempoActividad_soloSegundos() =
        assertEquals("42 s", FormatoSistema.tiempoActividad(ms(s = 42)))

    @Test fun tiempoActividad_cero() =
        assertEquals("0 s", FormatoSistema.tiempoActividad(0))

    @Test fun tiempoActividad_minutosYSegundos() =
        assertEquals("5 min 7 s", FormatoSistema.tiempoActividad(ms(m = 5, s = 7)))

    @Test fun tiempoActividad_horasMuestraMinutosEnCero() =
        assertEquals("3 h 0 min 9 s", FormatoSistema.tiempoActividad(ms(h = 3, s = 9)))

    @Test fun tiempoActividad_conDiasOcultaSegundos() =
        assertEquals("2 d 4 h 15 min", FormatoSistema.tiempoActividad(ms(d = 2, h = 4, m = 15, s = 59)))

    @Test fun tiempoActividad_ignoraMilisegundos() =
        assertEquals("1 s", FormatoSistema.tiempoActividad(1_999))

    @Test fun tiempoActividad_negativoNoDisponible() =
        assertEquals("No disponible", FormatoSistema.tiempoActividad(-1))

    // ---- Desfase UTC ----

    @Test fun desfase_negativo() =
        assertEquals("UTC-06:00", FormatoSistema.desfaseUtc(-6 * 3_600_000))

    @Test fun desfase_mediaHora() =
        assertEquals("UTC+05:30", FormatoSistema.desfaseUtc(5 * 3_600_000 + 30 * 60_000))

    @Test fun desfase_cero() =
        assertEquals("UTC+00:00", FormatoSistema.desfaseUtc(0))

    // ---- Texto ----

    @Test fun capitalizar_idioma() =
        assertEquals("Español", FormatoSistema.capitalizar("español", es))

    @Test fun capitalizar_vacioEsNull() {
        assertNull(FormatoSistema.capitalizar("", es))
        assertNull(FormatoSistema.capitalizar(null, es))
    }

    @Test fun lista_filtraVacios() {
        assertEquals("arm64-v8a, armeabi-v7a", FormatoSistema.lista(listOf("arm64-v8a", "", "armeabi-v7a")))
        assertNull(FormatoSistema.lista(emptyList()))
    }

    @Test fun siNo() {
        assertEquals("Sí", FormatoSistema.siNo(true))
        assertEquals("No", FormatoSistema.siNo(false))
        assertNull(FormatoSistema.siNo(null))
    }

    // ---- Parche de seguridad ----

    @Test fun fechaParche_valida() {
        val texto = FormatoSistema.fechaParche("2024-05-01", es)!!
        assertTrue(texto, texto.contains("mayo") && texto.contains("2024"))
    }

    @Test fun fechaParche_invalidaEsNull() {
        assertNull(FormatoSistema.fechaParche("2024-13-40", es))
        assertNull(FormatoSistema.fechaParche("", es))
        assertNull(FormatoSistema.fechaParche(null, es))
    }

    @Test fun mesesDesdeParche() {
        val hoy = LocalDate.of(2026, 11, 11)
        assertEquals(6L, FormatoSistema.mesesDesdeParche("2026-05-01", hoy))
        assertEquals(0L, FormatoSistema.mesesDesdeParche("2026-11-01", hoy))
        assertNull(FormatoSistema.mesesDesdeParche("2027-01-01", hoy))
        assertNull(FormatoSistema.mesesDesdeParche("no-fecha", hoy))
    }

    @Test fun antiguedad() {
        assertEquals("Menos de 1 mes", FormatoSistema.antiguedad(0))
        assertEquals("1 mes", FormatoSistema.antiguedad(1))
        assertEquals("7 meses", FormatoSistema.antiguedad(7))
        assertEquals("1 año", FormatoSistema.antiguedad(12))
        assertEquals("1 año y 1 mes", FormatoSistema.antiguedad(13))
        assertEquals("2 años y 5 meses", FormatoSistema.antiguedad(29))
        assertNull(FormatoSistema.antiguedad(null))
    }

    // ---- Compilación ----

    @Test fun tipoCompilacion() {
        assertEquals("user (producción)", FormatoSistema.tipoCompilacion("user"))
        assertEquals("userdebug (depuración)", FormatoSistema.tipoCompilacion("userdebug"))
        assertEquals("eng (ingeniería)", FormatoSistema.tipoCompilacion("eng"))
        assertEquals("personalizado", FormatoSistema.tipoCompilacion("personalizado"))
        assertNull(FormatoSistema.tipoCompilacion(""))
    }

    @Test fun compilacionDepurable() {
        assertFalse(FormatoSistema.compilacionDepurable("user")!!)
        assertTrue(FormatoSistema.compilacionDepurable("userdebug")!!)
        assertTrue(FormatoSistema.compilacionDepurable("eng")!!)
        assertNull(FormatoSistema.compilacionDepurable("otro"))
        assertNull(FormatoSistema.compilacionDepurable(null))
    }

    @Test fun clavesFirma() {
        assertEquals("Claves de lanzamiento (release-keys)", FormatoSistema.clavesFirma("release-keys"))
        assertEquals("Claves de prueba (test-keys)", FormatoSistema.clavesFirma("test-keys"))
        assertEquals("Claves de lanzamiento (release-keys)", FormatoSistema.clavesFirma("dev, release-keys"))
        assertNull(FormatoSistema.clavesFirma(null))
    }

    // ---- Kernel ----

    private val procVersion =
        "Linux version 4.14.190-perf-gabc1234 (builder@abfarm-01) " +
            "(Android (6443078 based on r383902) clang version 11.0.1) " +
            "#1 SMP PREEMPT Tue Oct 3 10:00:00 CST 2023"

    @Test fun versionKernelDesdeProc() =
        assertEquals("4.14.190-perf-gabc1234", FormatoSistema.versionKernelDesdeProc(procVersion))

    @Test fun identificadorKernel() =
        assertEquals("#1 SMP PREEMPT Tue Oct 3 10:00:00 CST 2023", FormatoSistema.identificadorKernel(procVersion))

    @Test fun kernel_sinDatosEsNull() {
        assertNull(FormatoSistema.versionKernelDesdeProc(null))
        assertNull(FormatoSistema.identificadorKernel("cadena sin formato"))
    }

    // ---- Fecha y hora ----

    @Test fun fechaHora_incluyeAnio() {
        // 2026-01-15 12:00 UTC
        val texto = FormatoSistema.fechaHora(1_768_478_400_000, es, ZoneId.of("UTC"))
        assertTrue(texto, texto.contains("2026"))
    }
}
