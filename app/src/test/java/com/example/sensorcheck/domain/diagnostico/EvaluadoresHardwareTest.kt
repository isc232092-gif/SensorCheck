package com.example.sensorcheck.domain.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class EvaluadorConteoTest {
    private fun r(emitidos: Int, respuesta: Int, incorrecto: String? = null) =
        EvaluadorConteo.evaluar(IdPrueba.ALTAVOZ, "Pitidos", emitidos, respuesta, componenteIncorrecto = incorrecto).resultado

    @Test fun coincide() = assertEquals(ResultadoPrueba.CORRECTO, r(3, 3))
    @Test fun nadaPercibidoEsFallo() = assertEquals(ResultadoPrueba.FALLO, r(3, 0))
    @Test fun conteoDistintoEsIndeterminado() = assertEquals(ResultadoPrueba.INDETERMINADO, r(3, 2))
    @Test fun salidaPorOtroDispositivoNoAprueba() =
        assertEquals(ResultadoPrueba.INDETERMINADO, r(3, 3, "El audio salió por auriculares Bluetooth"))
}

class EvaluadorMicrofonoTest {

    private fun tono(muestras: Int, amplitud: Double) =
        ShortArray(muestras) { (sin(2 * PI * 440 * it / 44_100) * amplitud * Short.MAX_VALUE).toInt().toShort() }

    @Test fun dbfs_silencioDigitalYEscalaCompleta() {
        assertEquals(-120.0, NivelAudio.dbfs(ShortArray(100)), 0.0)
        assertEquals(-3.0, NivelAudio.dbfs(tono(44_100, 1.0)), 0.1) // seno a escala completa ≈ −3 dBFS
    }

    @Test fun niveles_porBloques() = assertEquals(10, NivelAudio.niveles(ShortArray(44_100), 4_410).size)

    private fun niveles(silencio: Double, voz: Double) = List(15) { silencio } + List(35) { voz }

    @Test fun subidaSuficienteEsCorrecto() =
        assertEquals(ResultadoPrueba.CORRECTO, EvaluadorMicrofono.evaluar(niveles(-60.0, -20.0), 15, false, false, null).resultado)

    @Test fun sinSubidaEsIndeterminadoOFalloSiSeReporta() {
        assertEquals(ResultadoPrueba.INDETERMINADO, EvaluadorMicrofono.evaluar(niveles(-60.0, -55.0), 15, false, false, null).resultado)
        assertEquals(ResultadoPrueba.FALLO, EvaluadorMicrofono.evaluar(niveles(-60.0, -55.0), 15, false, true, null).resultado)
    }

    @Test fun picoDemasiadoBajoNoAprueba() =
        assertEquals(ResultadoPrueba.INDETERMINADO, EvaluadorMicrofono.evaluar(niveles(-90.0, -60.0), 15, false, false, null).resultado)

    @Test fun senalNulaNoSeDeclaraFallo() {
        val r = EvaluadorMicrofono.evaluar(niveles(-120.0, -120.0), 15, true, true, null)
        assertEquals(ResultadoPrueba.INDETERMINADO, r.resultado)
        assertTrue(r.evidencia.any { it.contains("nula") })
    }
}

class EvaluadorCamaraTest {
    private val buena = CapturaCamara(1920, 1080, 400_000, 120.0, 45.0)
    private fun r(
        cuadros: Int = 30, error: String? = null, enUso: Boolean = false,
        captura: CapturaCamara? = buena, respuesta: RespuestaVisual? = RespuestaVisual.SIN_DEFECTOS
    ) = EvaluadorCamara.evaluar(IdPrueba.CAMARA_TRASERA, "0", cuadros, error, enUso, captura, respuesta).resultado

    @Test fun capturaConfirmadaEsCorrecto() = assertEquals(ResultadoPrueba.CORRECTO, r())
    @Test fun sinVistaPreviaEsFallo() = assertEquals(ResultadoPrueba.FALLO, r(cuadros = 0, captura = null, respuesta = null))
    @Test fun errorDeAperturaEsFallo() = assertEquals(ResultadoPrueba.FALLO, r(error = "código 4"))
    @Test fun camaraEnUsoEsIndeterminado() = assertEquals(ResultadoPrueba.INDETERMINADO, r(error = "en uso", enUso = true))
    @Test fun defectosConfirmadosEsFallo() = assertEquals(ResultadoPrueba.FALLO, r(respuesta = RespuestaVisual.CON_DEFECTOS))
    @Test fun imagenUniformeNoAprueba() =
        assertEquals(ResultadoPrueba.INDETERMINADO, r(captura = buena.copy(luminanciaMedia = 2.0, luminanciaDesviacion = 0.5)))
}

class EvaluadorBateriaTest {
    private val buena = LecturaBateria(80, EvaluadorBateria.SALUD_BUENA, 30f, 3900, false)

    @Test fun todoCorrectoConCarga() = assertEquals(ResultadoPrueba.CORRECTO, EvaluadorBateria.evaluar(buena, true).resultado)
    @Test fun sinProbarCargaEsIndeterminado() = assertEquals(ResultadoPrueba.INDETERMINADO, EvaluadorBateria.evaluar(buena, null).resultado)
    @Test fun cargaNoDetectadaEsFallo() = assertEquals(ResultadoPrueba.FALLO, EvaluadorBateria.evaluar(buena, false).resultado)
    @Test fun saludMalaEsFallo() = assertEquals(ResultadoPrueba.FALLO, EvaluadorBateria.evaluar(buena.copy(salud = 3), true).resultado)
    @Test fun temperaturaAltaEsIndeterminado() =
        assertEquals(ResultadoPrueba.INDETERMINADO, EvaluadorBateria.evaluar(buena.copy(temperaturaC = 50f), true).resultado)

    @Test fun voltajeEnVoltiosSeNormaliza() {
        assertEquals(4000, EvaluadorBateria.normalizarMv(4))
        assertEquals(ResultadoPrueba.CORRECTO, EvaluadorBateria.evaluar(buena.copy(voltajeMv = 4), true).resultado)
    }
}

class EvaluadoresSistemaTest {
    @Test fun almacenamiento() {
        val ok = MedicionAlmacenamiento(32L shl 20, 200, 50, true)
        assertEquals(ResultadoPrueba.CORRECTO, EvaluadorAlmacenamiento.evaluar(ok).resultado)
        assertEquals(ResultadoPrueba.FALLO, EvaluadorAlmacenamiento.evaluar(ok.copy(integridadCorrecta = false)).resultado)
        assertEquals(ResultadoPrueba.FALLO, EvaluadorAlmacenamiento.evaluar(MedicionAlmacenamiento(1, 0, 0, null, error = "EIO")).resultado)
        assertEquals(
            ResultadoPrueba.INDETERMINADO,
            EvaluadorAlmacenamiento.evaluar(MedicionAlmacenamiento(1, 0, 0, null, espacioInsuficiente = true)).resultado
        )
        assertEquals(160.0, EvaluadorAlmacenamiento.mbPorSegundo(32L shl 20, 200), 0.01)
    }

    @Test fun ram() {
        assertEquals(ResultadoPrueba.CORRECTO, EvaluadorRam.evaluar(64, 0, false, 4000, 1500).resultado)
        assertEquals(ResultadoPrueba.FALLO, EvaluadorRam.evaluar(64, 3, false, null, null).resultado)
        assertEquals(ResultadoPrueba.INDETERMINADO, EvaluadorRam.evaluar(0, 0, true, null, null).resultado)
    }

    @Test fun botones() {
        assertEquals(ResultadoPrueba.CORRECTO, EvaluadorBotones.evaluar(1, 2, false).resultado)
        assertEquals(ResultadoPrueba.FALLO, EvaluadorBotones.evaluar(1, 0, true).resultado)
        assertEquals(ResultadoPrueba.INDETERMINADO, EvaluadorBotones.evaluar(1, 0, false).resultado)
    }

    @Test fun conectividad() {
        assertEquals(ResultadoPrueba.CORRECTO, EvaluadorConectividad.wifi(true, true, false, -60, false).resultado)
        assertEquals(ResultadoPrueba.FALLO, EvaluadorConectividad.wifi(true, false, false, null, true).resultado)
        assertEquals(ResultadoPrueba.INDETERMINADO, EvaluadorConectividad.wifi(false, false, false, null, false).resultado)
        assertEquals(ResultadoPrueba.CORRECTO, EvaluadorConectividad.bluetooth(true, 2, null, false).resultado)
        assertEquals(ResultadoPrueba.INDETERMINADO, EvaluadorConectividad.bluetooth(true, 0, null, false).resultado)
        assertEquals(ResultadoPrueba.CORRECTO, EvaluadorConectividad.gps(true, 9, 6, true, null, false).resultado)
        assertEquals(ResultadoPrueba.INDETERMINADO, EvaluadorConectividad.gps(true, 4, 0, false, null, false).resultado)
        assertEquals(ResultadoPrueba.FALLO, EvaluadorConectividad.gps(true, 0, 0, false, null, true).resultado)
    }
}
