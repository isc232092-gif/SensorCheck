package com.example.sensorcheck.ui.diagnostico.ejecutores

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.media.AudioDeviceInfo
import android.os.Handler
import android.os.Looper
import android.view.TextureView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.sensorcheck.data.diagnostico.CamaraPrueba
import com.example.sensorcheck.data.diagnostico.EventoCamara
import com.example.sensorcheck.data.diagnostico.LinternaPrueba
import com.example.sensorcheck.data.diagnostico.VibradorPrueba
import com.example.sensorcheck.data.diagnostico.generarPitidos
import com.example.sensorcheck.data.diagnostico.grabarMicrofono
import com.example.sensorcheck.data.diagnostico.nombreSalidaAudio
import com.example.sensorcheck.data.diagnostico.reproducirEnAltavoz
import com.example.sensorcheck.domain.diagnostico.CapturaCamara
import com.example.sensorcheck.domain.diagnostico.CatalogoPruebas
import com.example.sensorcheck.domain.diagnostico.EvaluadorCamara
import com.example.sensorcheck.domain.diagnostico.EvaluadorConteo
import com.example.sensorcheck.domain.diagnostico.EvaluadorMicrofono
import com.example.sensorcheck.domain.diagnostico.IdPrueba
import com.example.sensorcheck.domain.diagnostico.NivelAudio
import com.example.sensorcheck.domain.diagnostico.ResultadoEvaluado
import com.example.sensorcheck.domain.diagnostico.ResultadoPrueba
import com.example.sensorcheck.domain.diagnostico.RespuestaVisual
import com.example.sensorcheck.domain.model.OrientacionCamara
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

private fun indeterminado(ctx: ContextoEjecucion, motivo: String) = ResultadoEvaluado(
    ResultadoPrueba.INDETERMINADO, listOf(motivo),
    CatalogoPruebas.recomendacion(ctx.definicion.id, ResultadoPrueba.INDETERMINADO)
)

// ---------------- Cámara ----------------

@Composable
fun PruebaCamara(ctx: ContextoEjecucion, orientacion: OrientacionCamara) {
    val contexto = LocalContext.current
    var idCamara by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(orientacion) {
        val id = withContext(Dispatchers.Default) {
            runCatching { ctx.camaras.obtenerCamaras().camaras.firstOrNull { it.orientacion == orientacion }?.id }.getOrNull()
        }
        if (id == null) {
            ctx.terminar(
                ResultadoEvaluado(
                    ResultadoPrueba.NO_COMPATIBLE, listOf("Camera2 no informa una cámara ${orientacion.etiqueta.lowercase()}"),
                    CatalogoPruebas.recomendacion(ctx.definicion.id, ResultadoPrueba.NO_COMPATIBLE)
                )
            )
        } else idCamara = id
    }

    val id = idCamara
    MarcoPrueba(onCancelar = ctx::cancelar) {
        if (id == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
            return@MarcoPrueba
        }
        val principal = remember { Handler(Looper.getMainLooper()) }
        val cuadros = remember { AtomicInteger(0) }
        var cuadrosVisibles by remember { mutableIntStateOf(0) }
        var foto by remember { mutableStateOf<Bitmap?>(null) }
        var captura by remember { mutableStateOf<CapturaCamara?>(null) }
        var capturando by remember { mutableStateOf(false) }

        fun resultado(error: String?, enUso: Boolean, respuesta: RespuestaVisual?) = EvaluadorCamara.evaluar(
            ctx.definicion.id, id, cuadros.get(), error, enUso, captura, respuesta
        )

        val camara = remember(id) {
            CamaraPrueba(contexto, id) { evento ->
                when (evento) {
                    EventoCamara.Cuadro -> cuadros.incrementAndGet()
                    else -> principal.post {
                        when (evento) {
                            is EventoCamara.Capturada -> {
                                foto = evento.foto
                                captura = evento.datos
                                capturando = false
                            }
                            is EventoCamara.Error -> ctx.terminar(resultado(evento.mensaje, evento.enUso, null))
                            else -> Unit
                        }
                    }
                }
            }
        }
        DisposableEffect(camara) { onDispose { camara.cerrar() } }
        LaunchedEffect(camara) {
            while (true) {
                cuadrosVisibles = cuadros.get()
                delay(500)
            }
        }
        val tamano = remember(camara) { camara.tamanoVistaPrevia() }

        ColumnaPrueba(ctx) {
            val f = foto
            if (f == null) {
                Text("Apunta a un objeto bien iluminado y pulsa «Tomar foto».", style = MaterialTheme.typography.bodyLarge)
                AndroidView(
                    factory = { c ->
                        TextureView(c).apply {
                            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) = camara.abrir(st)
                                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) = Unit
                                override fun onSurfaceTextureDestroyed(st: SurfaceTexture) = true
                                override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        // La vista vertical intercambia ancho y alto del sensor
                        .aspectRatio(tamano?.let { it.height.toFloat() / it.width } ?: 0.75f)
                )
                Text(
                    "Cuadros de vista previa recibidos: $cuadrosVisibles",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { capturando = true; camara.capturar() },
                    enabled = cuadrosVisibles > 0 && !capturando,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                ) { Text(if (capturando) "Capturando…" else "Tomar foto") }
                OutlinedButton(
                    onClick = { ctx.terminar(resultado("El usuario indicó que la vista previa no funciona", false, null)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) { Text("La vista previa no se ve") }
            } else {
                Image(f.asImageBitmap(), contentDescription = "Foto de prueba", modifier = Modifier.fillMaxWidth())
                Text("¿La foto se ve nítida, con colores correctos y sin manchas?", style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = { ctx.terminar(resultado(null, false, RespuestaVisual.SIN_DEFECTOS)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) { Text("Sí, se ve bien") }
                Button(
                    onClick = { ctx.terminar(resultado(null, false, RespuestaVisual.CON_DEFECTOS)) },
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) { Text("No, tiene defectos") }
                OutlinedButton(
                    onClick = { ctx.terminar(resultado(null, false, RespuestaVisual.NO_SEGURO)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) { Text("No estoy seguro") }
                TextButton(onClick = { foto = null; captura = null }, modifier = Modifier.fillMaxWidth()) { Text("Tomar otra foto") }
            }
        }
    }
}

// ---------------- Flash ----------------

@Composable
fun PruebaFlash(ctx: ContextoEjecucion) {
    val contexto = LocalContext.current
    val emitidos = remember { Random.nextInt(2, 6) }
    var linterna by remember { mutableStateOf<LinternaPrueba?>(null) }
    var parpadeando by remember { mutableStateOf(true) }
    var repeticion by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val camaras = withContext(Dispatchers.Default) {
            runCatching { ctx.camaras.obtenerCamaras().camaras }.getOrDefault(emptyList())
        }
        val id = camaras.filter { it.tieneFlash == true }
            .minByOrNull { if (it.orientacion == OrientacionCamara.TRASERA) 0 else 1 }?.id
        if (id == null) {
            ctx.terminar(
                ResultadoEvaluado(
                    ResultadoPrueba.NO_COMPATIBLE, listOf("Ninguna cámara informa flash"),
                    CatalogoPruebas.recomendacion(IdPrueba.FLASH, ResultadoPrueba.NO_COMPATIBLE)
                )
            )
        } else linterna = LinternaPrueba(contexto, id)
    }
    val l = linterna
    if (l != null) {
        DisposableEffect(l) { onDispose { l.cerrar() } }
        LaunchedEffect(l, repeticion) {
            parpadeando = true
            try {
                delay(600)
                repeat(emitidos) {
                    l.encender(true)
                    delay(450)
                    l.encender(false)
                    delay(650)
                }
                parpadeando = false
            } catch (e: CameraAccessException) {
                ctx.terminar(indeterminado(ctx, "No se pudo controlar el flash: la cámara está en uso (${e.message})"))
            } catch (e: IllegalArgumentException) {
                ctx.terminar(indeterminado(ctx, "Android rechazó el control del flash: ${e.message}"))
            }
        }
    }

    MarcoPrueba(onCancelar = ctx::cancelar) {
        ColumnaPrueba(ctx) {
            if (l == null || parpadeando) {
                Text("Mira el flash junto a la cámara trasera…", style = MaterialTheme.typography.titleLarge)
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else {
                SelectorConteo(
                    pregunta = "¿Cuántos destellos viste?",
                    onRespuesta = { n ->
                        ctx.terminar(
                            EvaluadorConteo.evaluar(
                                IdPrueba.FLASH, "Destellos", emitidos, n,
                                listOf("Encendidos confirmados por Android: ${l.confirmaciones}")
                            )
                        )
                    },
                    onRepetirEstimulo = { repeticion++ }
                )
            }
        }
    }
}

// ---------------- Altavoz ----------------

@Composable
fun PruebaAltavoz(ctx: ContextoEjecucion) {
    val contexto = LocalContext.current
    val emitidos = remember { Random.nextInt(2, 6) }
    var reproduciendo by remember { mutableStateOf(true) }
    var repeticion by remember { mutableIntStateOf(0) }
    var salida by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(repeticion) {
        reproduciendo = true
        try {
            delay(400)
            salida = reproducirEnAltavoz(contexto, generarPitidos(emitidos))
            reproduciendo = false
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            ctx.terminar(indeterminado(ctx, "Android no permitió reproducir audio: ${e.message}"))
        }
    }

    MarcoPrueba(onCancelar = ctx::cancelar) {
        ColumnaPrueba(ctx) {
            if (reproduciendo) {
                Text("Escucha el altavoz…", style = MaterialTheme.typography.titleLarge)
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else {
                SelectorConteo(
                    pregunta = "¿Cuántos pitidos escuchaste?",
                    onRespuesta = { n ->
                        val s = salida
                        ctx.terminar(
                            EvaluadorConteo.evaluar(
                                IdPrueba.ALTAVOZ, "Pitidos", emitidos, n,
                                listOf("Salida de audio usada: ${nombreSalidaAudio(s)}"),
                                componenteIncorrecto = if (s != null && s != AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
                                    "El audio salió por ${nombreSalidaAudio(s)}, no por el altavoz integrado" else null
                            )
                        )
                    },
                    onRepetirEstimulo = { repeticion++ }
                )
            }
        }
    }
}

// ---------------- Vibración ----------------

@Composable
fun PruebaVibracion(ctx: ContextoEjecucion) {
    val contexto = LocalContext.current
    val vibrador = remember { VibradorPrueba(contexto) }
    val emitidos = remember { Random.nextInt(2, 5) }
    var vibrando by remember { mutableStateOf(true) }
    var repeticion by remember { mutableIntStateOf(0) }

    DisposableEffect(vibrador) { onDispose { vibrador.cancelar() } }
    LaunchedEffect(repeticion) {
        if (!vibrador.disponible) {
            ctx.terminar(
                ResultadoEvaluado(
                    ResultadoPrueba.NO_COMPATIBLE, listOf("Android no informa motor de vibración"),
                    CatalogoPruebas.recomendacion(IdPrueba.VIBRACION, ResultadoPrueba.NO_COMPATIBLE)
                )
            )
            return@LaunchedEffect
        }
        vibrando = true
        delay(500)
        delay(vibrador.pulsos(emitidos))
        vibrando = false
    }

    MarcoPrueba(onCancelar = ctx::cancelar) {
        ColumnaPrueba(ctx) {
            if (vibrando) {
                Text("Sostén el teléfono y cuenta las vibraciones…", style = MaterialTheme.typography.titleLarge)
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else {
                SelectorConteo(
                    pregunta = "¿Cuántas vibraciones sentiste?",
                    onRespuesta = { n ->
                        ctx.terminar(
                            EvaluadorConteo.evaluar(
                                IdPrueba.VIBRACION, "Vibraciones", emitidos, n,
                                listOf("Control de intensidad: " + if (vibrador.controlAmplitud) "sí" else "no")
                            )
                        )
                    },
                    onRepetirEstimulo = { repeticion++ }
                )
            }
        }
    }
}

// ---------------- Micrófono ----------------

private const val SEGUNDOS_GRABACION = 5
private const val BLOQUES_SILENCIO = 15 // 1,5 s en bloques de 100 ms
private const val MUESTRAS_BLOQUE = 4_410

@Composable
fun PruebaMicrofono(ctx: ContextoEjecucion) {
    val contexto = LocalContext.current
    var intento by remember { mutableIntStateOf(0) }
    var grabacion by remember { mutableStateOf<ShortArray?>(null) }
    var nivelActual by remember { mutableDoubleStateOf(-120.0) }
    var transcurrido by remember { mutableIntStateOf(0) }
    var escucho by remember { mutableStateOf<Boolean?>(null) }
    var reproduciendo by remember { mutableStateOf(false) }
    var pedirReproduccion by remember { mutableIntStateOf(0) }

    LaunchedEffect(intento) {
        grabacion = null
        escucho = null
        try {
            grabacion = grabarMicrofono(SEGUNDOS_GRABACION) { pcm, leidas ->
                val desde = (leidas - MUESTRAS_BLOQUE).coerceAtLeast(0)
                nivelActual = NivelAudio.dbfs(pcm, desde, leidas)
                transcurrido = leidas * 10 / com.example.sensorcheck.data.diagnostico.FRECUENCIA_MUESTREO
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            ctx.terminar(indeterminado(ctx, "No se pudo usar el micrófono: ${e.message}"))
        }
    }
    LaunchedEffect(pedirReproduccion) {
        val pcm = grabacion ?: return@LaunchedEffect
        if (pedirReproduccion == 0) return@LaunchedEffect
        reproduciendo = true
        runCatching { reproducirEnAltavoz(contexto, pcm) }
        reproduciendo = false
    }

    fun evaluar(reportaFallo: Boolean) {
        val pcm = grabacion ?: return
        ctx.terminar(
            EvaluadorMicrofono.evaluar(
                niveles = NivelAudio.niveles(pcm, MUESTRAS_BLOQUE),
                bloquesSilencio = BLOQUES_SILENCIO,
                senalNula = pcm.all { it.toInt() == 0 },
                usuarioReportaFallo = reportaFallo,
                escuchoReproduccion = escucho
            )
        )
    }

    MarcoPrueba(onCancelar = ctx::cancelar) {
        ColumnaPrueba(ctx) {
            val pcm = grabacion
            if (pcm == null) {
                Text(
                    if (transcurrido < BLOQUES_SILENCIO) "Silencio…" else "¡Habla o aplaude ahora!",
                    style = MaterialTheme.typography.headlineMedium
                )
                LinearProgressIndicator(
                    progress = { ((nivelActual + 80) / 80).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(12.dp)
                )
                Text(
                    "Nivel: ${"%.0f".format(nivelActual)} dBFS · ${transcurrido / 10},${transcurrido % 10} de $SEGUNDOS_GRABACION s",
                    style = MaterialTheme.typography.labelMedium
                )
            } else {
                val niveles = remember(pcm) { NivelAudio.niveles(pcm, MUESTRAS_BLOQUE) }
                val silencio = niveles.take(BLOQUES_SILENCIO).sorted().getOrElse(BLOQUES_SILENCIO / 2) { -120.0 }
                val pico = niveles.drop(BLOQUES_SILENCIO).maxOrNull() ?: -120.0
                Text("Grabación terminada (solo en memoria).", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Silencio: ${"%.0f".format(silencio)} dBFS · pico: ${"%.0f".format(pico)} dBFS · subida ${"%.0f".format(pico - silencio)} dB",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(
                    onClick = { pedirReproduccion++ },
                    enabled = !reproduciendo,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) { Text(if (reproduciendo) "Reproduciendo…" else "Reproducir grabación") }
                if (pedirReproduccion > 0 && !reproduciendo) {
                    Text("¿Escuchaste tu voz en la reproducción?", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = escucho == true, onClick = { escucho = true }, label = { Text("Sí") })
                        FilterChip(selected = escucho == false, onClick = { escucho = false }, label = { Text("No") })
                    }
                }
                Button(onClick = { evaluar(false) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text("Evaluar")
                }
                OutlinedButton(onClick = { evaluar(true) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text("Hablé y el nivel no subió")
                }
                TextButton(onClick = { intento++ }, modifier = Modifier.fillMaxWidth()) { Text("Grabar de nuevo") }
            }
        }
    }
}
