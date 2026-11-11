package com.example.sensorcheck.domain.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogoPruebasTest {

    @Test fun todasLasPruebasTienenDefinicionUnica() {
        assertEquals(IdPrueba.entries.toSet(), CatalogoPruebas.pruebas.map { it.id }.toSet())
        assertEquals(IdPrueba.entries.size, CatalogoPruebas.pruebas.size)
    }

    @Test fun implementadasTienenInstruccionesYCriterio() {
        CatalogoPruebas.pruebas.filter { it.implementada }.forEach {
            assertTrue(it.nombre, it.instrucciones.isNotEmpty())
            assertTrue(it.nombre, it.recomendacionFallo.isNotBlank())
        }
    }

    @Test fun cancelacionNuncaEsAprobada() {
        val r = CatalogoPruebas.noEjecutada(IdPrueba.TACTIL, "Cancelada")
        assertEquals(ResultadoPrueba.NO_EJECUTADO, r.resultado)
        assertEquals(listOf("Cancelada"), r.evidencia)
    }

    @Test fun recomendacionDeFalloEsLaEspecificaDeLaPrueba() =
        assertEquals(
            CatalogoPruebas.definicion(IdPrueba.LUZ).recomendacionFallo,
            CatalogoPruebas.recomendacion(IdPrueba.LUZ, ResultadoPrueba.FALLO)
        )
}

class SesionDiagnosticoTest {

    private fun sesion(ids: List<IdPrueba>, ejecutable: (IdPrueba) -> Boolean = { true }) =
        SesionDiagnostico.crear("s", ids, ejecutable)

    private fun r(res: ResultadoPrueba) = ResultadoEvaluado(res, emptyList(), "")

    @Test fun crear_respetaOrdenDelCatalogoYExcluyeNoEjecutables() {
        val s = sesion(listOf(IdPrueba.LUZ, IdPrueba.TACTIL, IdPrueba.WIFI)) { it != IdPrueba.WIFI }
        assertEquals(listOf(IdPrueba.TACTIL, IdPrueba.LUZ), s.cola)
    }

    @Test fun unaPruebaPorVezConProgreso() {
        var s = sesion(listOf(IdPrueba.TACTIL, IdPrueba.COLORES))
        assertEquals(IdPrueba.TACTIL, s.actual)
        assertEquals(0f, s.progreso)
        s = s.registrar(r(ResultadoPrueba.CORRECTO))
        assertEquals(IdPrueba.COLORES, s.actual)
        assertEquals(0.5f, s.progreso)
        s = s.registrar(r(ResultadoPrueba.FALLO))
        assertTrue(s.terminada)
        assertNull(s.actual)
        assertEquals(1f, s.progreso)
    }

    @Test fun registrarConSesionTerminadaNoCambiaNada() {
        val s = sesion(listOf(IdPrueba.TACTIL)).registrar(r(ResultadoPrueba.CORRECTO))
        assertEquals(s, s.registrar(r(ResultadoPrueba.FALLO)))
    }

    @Test fun repetir_soloFallidasIndeterminadasYOmitidas() {
        var s = sesion(listOf(IdPrueba.TACTIL, IdPrueba.COLORES, IdPrueba.PIXELES, IdPrueba.LUZ))
        s = s.registrar(r(ResultadoPrueba.CORRECTO))
            .registrar(r(ResultadoPrueba.FALLO))
            .registrar(r(ResultadoPrueba.INDETERMINADO))
            .registrar(r(ResultadoPrueba.NO_EJECUTADO))
        assertEquals(listOf(IdPrueba.COLORES, IdPrueba.PIXELES, IdPrueba.LUZ), s.paraRepetir())
        assertEquals(1, s.conteo()[ResultadoPrueba.CORRECTO])
    }

    @Test fun ultimoResultado_ignoraNoEjecutadas() {
        fun reg(t: Long, res: ResultadoPrueba) =
            RegistroDiagnostico("$t", null, t, "D", IdPrueba.LUZ, res, emptyList(), "")
        val registros = listOf(reg(1, ResultadoPrueba.CORRECTO), reg(2, ResultadoPrueba.NO_EJECUTADO))
        assertEquals(ResultadoPrueba.CORRECTO, ultimoResultado(registros, IdPrueba.LUZ)?.resultado)
        assertNull(ultimoResultado(registros, IdPrueba.TACTIL))
    }
}

class EvaluadoresPantallaTest {

    @Test fun tactil_completoEsCorrecto() {
        val ev = EvaluadorTactil(2, 2)
        listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1).forEach { (c, f) -> ev.marcar(c, f) }
        assertTrue(ev.completo)
        assertEquals(ResultadoPrueba.CORRECTO, ev.evaluar(false).resultado)
    }

    @Test fun tactil_zonasFaltantesReportadasEsFalloConEvidencia() {
        val ev = EvaluadorTactil(3, 2)
        ev.marcar(0, 0)
        val r = ev.evaluar(usuarioReportaFallo = true)
        assertEquals(ResultadoPrueba.FALLO, r.resultado)
        assertTrue(r.evidencia.any { it.contains("1 de 6") })
        assertTrue(r.evidencia.any { it.startsWith("Zonas sin contacto") && it.contains("f1·c2") })
    }

    @Test fun tactil_ignoraFueraDeRangoYRepetidas() {
        val ev = EvaluadorTactil(2, 2)
        assertTrue(ev.marcar(0, 0))
        assertFalse(ev.marcar(0, 0))
        assertFalse(ev.marcar(5, 0))
        assertFalse(ev.marcar(-1, 0))
        assertEquals(1, ev.conteo)
    }

    @Test fun multitactil_alcanzaRequeridos() {
        val ev = EvaluadorMultitactil(5)
        listOf(1, 3, 5, 2).forEach(ev::registrar)
        assertEquals(5, ev.maximo)
        assertEquals(ResultadoPrueba.CORRECTO, ev.evaluar(false).resultado)
    }

    @Test fun multitactil_insuficienteReportadoEsFallo() {
        val ev = EvaluadorMultitactil(5)
        ev.registrar(3)
        assertEquals(ResultadoPrueba.FALLO, ev.evaluar(true).resultado)
        assertEquals(ResultadoPrueba.INDETERMINADO, ev.evaluar(false).resultado)
    }

    @Test fun visual_respuestasDelUsuario() {
        val vistas = listOf("Rojo", "Verde")
        assertEquals(ResultadoPrueba.CORRECTO, EvaluadorVisual.evaluar(IdPrueba.COLORES, vistas, RespuestaVisual.SIN_DEFECTOS).resultado)
        assertEquals(ResultadoPrueba.FALLO, EvaluadorVisual.evaluar(IdPrueba.COLORES, vistas, RespuestaVisual.CON_DEFECTOS).resultado)
        val dudoso = EvaluadorVisual.evaluar(IdPrueba.PIXELES, vistas, RespuestaVisual.NO_SEGURO)
        assertEquals(ResultadoPrueba.INDETERMINADO, dudoso.resultado)
        assertTrue(dudoso.evidencia.any { it.contains("inspección visual") })
    }
}

class EvaluadoresSensorTest {

    private val s = 1_000_000_000L

    /** Alimenta lecturas constantes durante [segundos] a 20 Hz. */
    private fun EvaluadorSensor.constante(desdeNs: Long, segundos: Int, vararg v: Float): Long {
        var t = desdeNs
        repeat(segundos * 20) {
            registrar(v.toList(), t)
            t += s / 20
        }
        return t
    }

    @Test fun reposo_detectaIntervaloEstable() {
        val d = DetectorReposo(s, 0.1f)
        var t = 0L
        repeat(30) { d.agregar(t, 9.8f + (it % 2) * 0.05f); t += s / 20 }
        assertEquals(9.825f, d.media!!, 0.01f)
    }

    @Test fun reposo_noSeDetectaConMovimiento() {
        val d = DetectorReposo(s, 0.1f)
        var t = 0L
        repeat(60) { d.agregar(t, if (it % 2 == 0) 8f else 11f); t += s / 20 }
        assertNull(d.media)
    }

    @Test fun acelerometro_reposoYEjesCompletosEsCorrecto() {
        val ev = EvaluadorAcelerometro()
        var t = ev.constante(0, 2, 0f, 0f, 9.81f)
        listOf(
            floatArrayOf(9.8f, 0f, 0f), floatArrayOf(-9.8f, 0f, 0f),
            floatArrayOf(0f, 9.8f, 0f), floatArrayOf(0f, -9.8f, 0f),
            floatArrayOf(0f, 0f, -9.8f)
        ).forEach { t = ev.constante(t, 1, *it) }
        assertTrue(ev.completo)
        assertEquals(ResultadoPrueba.CORRECTO, ev.evaluar(FinPrueba.COMPLETADA).resultado)
    }

    @Test fun acelerometro_gravedadFueraDeRangoEsFallo() {
        val ev = EvaluadorAcelerometro()
        ev.constante(0, 2, 0f, 0f, 6f)
        val r = ev.evaluar(FinPrueba.TIEMPO_AGOTADO)
        assertEquals(ResultadoPrueba.FALLO, r.resultado)
        assertTrue(r.evidencia.any { it.contains("fuera del rango") })
    }

    @Test fun acelerometro_sinEventosEsFallo() =
        assertEquals(ResultadoPrueba.FALLO, EvaluadorAcelerometro().evaluar(FinPrueba.TIEMPO_AGOTADO).resultado)

    @Test fun acelerometro_reposoSinGirarEsIndeterminado() {
        val ev = EvaluadorAcelerometro()
        ev.constante(0, 3, 0f, 0f, 9.81f)
        assertEquals(ResultadoPrueba.INDETERMINADO, ev.evaluar(FinPrueba.TIEMPO_AGOTADO).resultado)
    }

    @Test fun giroscopio_derivaAltaEsFallo() {
        val ev = EvaluadorGiroscopio()
        ev.constante(0, 2, 0.3f, 0f, 0f)
        assertEquals(ResultadoPrueba.FALLO, ev.evaluar(FinPrueba.TIEMPO_AGOTADO).resultado)
    }

    @Test fun giroscopio_reposoYGirosEsCorrecto() {
        val ev = EvaluadorGiroscopio()
        var t = ev.constante(0, 2, 0.01f, 0f, 0f)
        t = ev.constante(t, 1, 2f, 0f, 0f)
        t = ev.constante(t, 1, 0f, -2f, 0f)
        ev.constante(t, 1, 0f, 0f, 2f)
        assertEquals(ResultadoPrueba.CORRECTO, ev.evaluar(FinPrueba.COMPLETADA).resultado)
    }

    @Test fun magnetometro_variacionConCampoTerrestreEsCorrecto() {
        val ev = EvaluadorMagnetometro()
        var t = 0L
        listOf(
            floatArrayOf(30f, 0f, -30f), floatArrayOf(-15f, 30f, 15f), floatArrayOf(10f, -15f, -30f)
        ).forEach { t = ev.constante(t, 1, *it) }
        assertTrue(ev.completo)
        assertEquals(ResultadoPrueba.CORRECTO, ev.evaluar(FinPrueba.COMPLETADA).resultado)
    }

    @Test fun magnetometro_campoAnomaloEsIndeterminado() {
        val ev = EvaluadorMagnetometro()
        var t = 0L
        listOf(floatArrayOf(200f, 0f, 0f), floatArrayOf(150f, 30f, 30f)).forEach { t = ev.constante(t, 1, *it) }
        assertTrue(ev.completo)
        assertEquals(ResultadoPrueba.INDETERMINADO, ev.evaluar(FinPrueba.COMPLETADA).resultado)
    }

    @Test fun proximidad_cercaYLuegoLejos() {
        val ev = EvaluadorProximidad(5f)
        ev.registrar(listOf(5f), 0)
        assertFalse(ev.completo)
        ev.registrar(listOf(0f), 1)
        assertFalse(ev.completo)
        ev.registrar(listOf(5f), 2)
        assertTrue(ev.completo)
        assertEquals(ResultadoPrueba.CORRECTO, ev.evaluar(FinPrueba.COMPLETADA).resultado)
    }

    @Test fun proximidad_sinCambioReportadoEsFallo_tiempoAgotadoIndeterminado() {
        val ev = EvaluadorProximidad(5f)
        ev.registrar(listOf(5f), 0)
        assertEquals(ResultadoPrueba.INDETERMINADO, ev.evaluar(FinPrueba.TIEMPO_AGOTADO).resultado)
        assertEquals(ResultadoPrueba.FALLO, ev.evaluar(FinPrueba.USUARIO_REPORTA_FALLO).resultado)
    }

    @Test fun proximidad_sinEventosNoEsFallo() =
        assertEquals(ResultadoPrueba.INDETERMINADO, EvaluadorProximidad(5f).evaluar(FinPrueba.TIEMPO_AGOTADO).resultado)

    @Test fun luz_altaYCubiertaEsCorrecto() {
        val ev = EvaluadorLuz()
        ev.registrar(listOf(300f), 0)
        ev.registrar(listOf(40f), 1)
        assertTrue(ev.completo)
        assertEquals(ResultadoPrueba.CORRECTO, ev.evaluar(FinPrueba.COMPLETADA).resultado)
    }

    @Test fun luz_oscuridadTotalNoBasta() {
        val ev = EvaluadorLuz()
        ev.registrar(listOf(2f), 0)
        ev.registrar(listOf(0f), 1)
        assertFalse(ev.completo)
    }

    @Test fun usuarioReportaFalloTrasCumplirCriteriosNoLoAnula() {
        val ev = EvaluadorLuz()
        ev.registrar(listOf(300f), 0)
        ev.registrar(listOf(10f), 1)
        assertEquals(ResultadoPrueba.CORRECTO, ev.evaluar(FinPrueba.USUARIO_REPORTA_FALLO).resultado)
    }
}
