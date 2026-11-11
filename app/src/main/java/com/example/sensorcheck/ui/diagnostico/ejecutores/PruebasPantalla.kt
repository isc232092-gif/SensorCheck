package com.example.sensorcheck.ui.diagnostico.ejecutores

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sensorcheck.domain.diagnostico.EvaluadorMultitactil
import com.example.sensorcheck.domain.diagnostico.EvaluadorTactil
import com.example.sensorcheck.domain.diagnostico.EvaluadorVisual
import com.example.sensorcheck.domain.diagnostico.IdPrueba
import com.example.sensorcheck.domain.diagnostico.RespuestaVisual
import kotlinx.coroutines.delay
import kotlin.math.ceil
import kotlin.math.max

/** Lado aproximado de cada zona táctil. */
private val LadoZona = 64.dp

@Composable
fun PruebaTactil(ctx: ContextoEjecucion) {
    var evaluador by remember { mutableStateOf<EvaluadorTactil?>(null) }

    MarcoPrueba(
        onCancelar = ctx::cancelar,
        inmersiva = true,
        opcionFallo = "Algunas zonas no responden",
        onFallo = { evaluador?.let { ctx.terminar(it.evaluar(usuarioReportaFallo = true)) } }
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val columnas = (maxWidth / LadoZona).toInt().coerceIn(4, 12)
            val filas = (maxHeight / LadoZona).toInt().coerceIn(6, 24)
            val ev = remember(columnas, filas) { EvaluadorTactil(columnas, filas).also { evaluador = it } }
            var version by remember(ev) { mutableIntStateOf(0) }
            val marcado = MaterialTheme.colorScheme.primary
            val rejilla = MaterialTheme.colorScheme.outlineVariant

            LaunchedEffect(ev, version) {
                if (ev.completo) {
                    delay(300)
                    ctx.terminar(ev.evaluar(usuarioReportaFallo = false))
                }
            }

            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(ev) {
                        val anchoZona = size.width.toFloat() / columnas
                        val altoZona = size.height.toFloat() / filas
                        fun marcar(p: Offset) {
                            if (ev.marcar((p.x / anchoZona).toInt(), (p.y / altoZona).toInt())) version++
                        }
                        awaitPointerEventScope {
                            while (true) {
                                val evento = awaitPointerEvent()
                                evento.changes.forEach { cambio ->
                                    if (cambio.pressed) {
                                        // Interpola el trazo para no saltar zonas en deslizamientos rápidos
                                        val desde = if (cambio.previousPressed) cambio.previousPosition else cambio.position
                                        val delta = cambio.position - desde
                                        val pasos = max(1, ceil(delta.getDistance() / (minOf(anchoZona, altoZona) / 2)).toInt())
                                        for (i in 0..pasos) marcar(desde + delta * (i.toFloat() / pasos))
                                    }
                                    cambio.consume()
                                }
                            }
                        }
                    }
            ) {
                version // lectura para redibujar al marcar zonas
                val w = size.width / columnas
                val h = size.height / filas
                for (f in 0 until filas) for (c in 0 until columnas) {
                    val origen = Offset(c * w, f * h)
                    if (ev.tocada(c, f)) drawRect(marcado, origen, Size(w, h))
                    drawRect(rejilla, origen, Size(w, h), style = Stroke(1f))
                }
            }

            Column(
                Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("${ev.conteo} / ${ev.total}", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Desliza por todas las zonas.\nAtrás: opciones",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PruebaMultitactil(ctx: ContextoEjecucion) {
    val ev = remember { EvaluadorMultitactil(ctx.contactosDeclarados.coerceAtLeast(2)) }
    var puntos by remember { mutableStateOf<Map<PointerId, Offset>>(emptyMap()) }
    val colores = MaterialTheme.colorScheme

    MarcoPrueba(
        onCancelar = ctx::cancelar,
        inmersiva = true,
        opcionFallo = "No detecta todos los dedos",
        onFallo = { ctx.terminar(ev.evaluar(usuarioReportaFallo = true)) }
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val evento = awaitPointerEvent()
                            puntos = evento.changes.filter { it.pressed }.associate { it.id to it.position }
                            ev.registrar(puntos.size)
                            evento.changes.forEach { it.consume() }
                            // Termina cuando se alcanzó el objetivo y se levantan los dedos
                            if (ev.alcanzado && puntos.isEmpty()) ctx.terminar(ev.evaluar(usuarioReportaFallo = false))
                        }
                    }
                }
        ) {
            puntos.values.forEach { p ->
                drawCircle(colores.primary.copy(alpha = 0.25f), radius = 56.dp.toPx(), center = p)
                drawCircle(colores.primary, radius = 56.dp.toPx(), center = p, style = Stroke(3.dp.toPx()))
            }
        }
        Column(
            Modifier.align(Alignment.Center).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("${puntos.size}", style = MaterialTheme.typography.displayMedium)
            Text(
                "Contactos ahora · máximo ${ev.maximo} de ${ev.requeridos}" +
                    if (ev.alcanzado) "\nObjetivo alcanzado: levanta los dedos" else "",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = colores.onSurfaceVariant
            )
        }
    }
}

private val ColoresPrueba = listOf(
    "Rojo" to Color(0xFFFF0000),
    "Verde" to Color(0xFF00FF00),
    "Azul" to Color(0xFF0000FF),
    "Blanco" to Color.White,
    "Negro" to Color.Black
)

private val FondosPixeles = listOf(
    "Negro" to Color.Black,
    "Blanco" to Color.White,
    "Gris" to Color(0xFF808080)
)

@Composable
fun PruebaColores(ctx: ContextoEjecucion) = PruebaVisual(
    ctx, IdPrueba.COLORES, ColoresPrueba,
    "¿Los colores se vieron uniformes, sin manchas, líneas ni tonos incorrectos?",
    sinDefectos = "Sí, se vieron uniformes", conDefectos = "No, vi defectos"
)

@Composable
fun PruebaPixeles(ctx: ContextoEjecucion) = PruebaVisual(
    ctx, IdPrueba.PIXELES, FondosPixeles,
    "¿Viste puntos que no cambiaban de color (píxeles muertos o atascados)?",
    sinDefectos = "No vi ninguno", conDefectos = "Sí, vi puntos"
)

/** Muestra fondos sólidos a brillo máximo y después pregunta al usuario. */
@Composable
private fun PruebaVisual(
    ctx: ContextoEjecucion,
    id: IdPrueba,
    pantallas: List<Pair<String, Color>>,
    pregunta: String,
    sinDefectos: String,
    conDefectos: String
) {
    var indice by remember { mutableIntStateOf(0) }

    if (indice < pantallas.size) {
        val (nombre, color) = pantallas[indice]
        var pista by remember(indice) { mutableStateOf(true) }
        LaunchedEffect(indice) {
            delay(1_500)
            pista = false
        }
        MarcoPrueba(onCancelar = ctx::cancelar, inmersiva = true, brilloMaximo = true, fondo = color) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { indice++ }
            )
            if (pista) {
                Text(
                    "$nombre · ${indice + 1} de ${pantallas.size}\nToca para continuar",
                    color = if (color.luminance() > 0.5f) Color.Black else Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    } else {
        MarcoPrueba(onCancelar = ctx::cancelar) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .safeDrawingPadding()
                    .padding(24.dp)
                    .widthIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(pregunta, style = MaterialTheme.typography.titleLarge)
                val vistas = pantallas.map { it.first }
                fun responder(r: RespuestaVisual) = ctx.terminar(EvaluadorVisual.evaluar(id, vistas, r))
                Button(onClick = { responder(RespuestaVisual.SIN_DEFECTOS) }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text(sinDefectos)
                }
                Button(
                    onClick = { responder(RespuestaVisual.CON_DEFECTOS) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text(conDefectos) }
                OutlinedButton(onClick = { responder(RespuestaVisual.NO_SEGURO) }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text("No estoy seguro")
                }
                TextButton(onClick = { indice = 0 }, Modifier.fillMaxWidth()) { Text("Ver las pantallas de nuevo") }
            }
        }
    }
}
