package com.example.sensorcheck.ui.sensors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.sensorcheck.domain.sensors.FormatoSensor
import com.example.sensorcheck.domain.sensors.SerieTemporal

/** Tonos categóricos en orden fijo (claro / oscuro), validados para daltonismo. */
private val SeriesClaro = listOf(Color(0xFF2A78D6), Color(0xFFEB6834), Color(0xFF1BAF7A), Color(0xFFEDA100))
private val SeriesOscuro = listOf(Color(0xFF3987E5), Color(0xFFD95926), Color(0xFF199E70), Color(0xFFC98500))

@Composable
fun coloresSeries(): List<Color> =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) SeriesOscuro else SeriesClaro

/**
 * Gráfica de líneas de las muestras recientes. Solo dibuja datos reales:
 * con menos de dos muestras no se muestra ninguna curva.
 * Mantener pulsado y arrastrar muestra los valores en ese instante.
 */
@Composable
fun SensorChart(
    muestras: SerieTemporal,
    nombres: List<String>,
    unidad: String?,
    modifier: Modifier = Modifier
) {
    val colores = MaterialTheme.colorScheme
    val tonos = coloresSeries()
    val series = muestras.series.size.coerceAtMost(tonos.size)
    var toqueX by remember { mutableStateOf<Float?>(null) }

    if (muestras.tamano < 2 || series == 0) {
        Text(
            text = "La gráfica aparecerá cuando el sensor entregue lecturas.",
            style = MaterialTheme.typography.bodySmall,
            color = colores.onSurfaceVariant,
            modifier = modifier.padding(vertical = 8.dp)
        )
        return
    }

    val rango = muestras.rangoVisible()
    val t0 = muestras.tiemposNs.first()
    val t1 = muestras.tiemposNs.last()
    val duracion = muestras.duracionS
    val textoUnidad = unidad ?: "sin unidad"

    // Índice inspeccionado a partir de la posición del toque
    val indice = toqueX?.let { f -> muestras.indiceCercano(t0 + ((t1 - t0) * f).toLong()) }

    Column(
        modifier = modifier.semantics {
            contentDescription = "Gráfica de ${nombres.take(series).joinToString()} en $textoUnidad, " +
                "últimos ${FormatoSensor.numero(duracion)} segundos"
        },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (series >= 2) {
            Leyenda(nombres.take(series), tonos)
        }

        Text(
            text = if (indice != null) {
                val atras = (t1 - muestras.tiemposNs[indice]) / 1e9f
                "−${FormatoSensor.numero(atras)} s: " + (0 until series).joinToString(" · ") { s ->
                    val v = muestras.series[s][indice]
                    if (series >= 2) "${nombres.getOrElse(s) { "" }} ${FormatoSensor.numero(v)}"
                    else FormatoSensor.numero(v)
                }
            } else {
                "Eje vertical: $textoUnidad · mantén pulsado para inspeccionar"
            },
            style = MaterialTheme.typography.labelSmall,
            color = colores.onSurfaceVariant,
            maxLines = 2
        )

        Row(Modifier.fillMaxWidth().height(160.dp)) {
            // Etiquetas del eje vertical
            Column(
                modifier = Modifier.width(52.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                val (min, max) = rango ?: (0f to 0f)
                listOf(max, (min + max) / 2f, min).forEach { v ->
                    Text(
                        text = if (rango == null) "" else FormatoSensor.numero(v),
                        style = MaterialTheme.typography.labelSmall,
                        color = colores.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.width(6.dp))

            val rejilla = colores.outlineVariant
            val cero = colores.outline
            val cursor = colores.onSurfaceVariant
            val fondo = colores.surfaceContainerHighest.copy(alpha = 0.35f)

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val inicio = awaitFirstDown()
                            toqueX = (inicio.position.x / size.width).coerceIn(0f, 1f)
                            do {
                                val evento = awaitPointerEvent()
                                val cambio = evento.changes.firstOrNull() ?: break
                                toqueX = (cambio.position.x / size.width).coerceIn(0f, 1f)
                                cambio.consume()
                            } while (cambio.pressed)
                            toqueX = null
                        }
                    }
            ) {
                drawRect(fondo)
                val (min, max) = rango ?: return@Canvas
                val alto = size.height
                val ancho = size.width
                fun y(v: Float) = alto - (v - min) / (max - min) * alto
                fun x(t: Long) = if (t1 == t0) 0f else (t - t0).toFloat() / (t1 - t0) * ancho

                // Rejilla discreta
                listOf(0f, 0.5f, 1f).forEach { f ->
                    val yy = alto * f
                    drawLine(rejilla, Offset(0f, yy), Offset(ancho, yy), strokeWidth = 1f)
                }
                if (min < 0f && max > 0f) {
                    drawLine(
                        cero, Offset(0f, y(0f)), Offset(ancho, y(0f)), strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )
                }

                val trazo = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                for (s in 0 until series) {
                    val valores = muestras.series[s]
                    val ruta = Path()
                    var abierta = false
                    for (i in 0 until muestras.tamano) {
                        val v = valores[i]
                        if (!v.isFinite()) {
                            abierta = false // valor ausente: se corta la línea
                            continue
                        }
                        val px = x(muestras.tiemposNs[i])
                        val py = y(v)
                        if (abierta) ruta.lineTo(px, py) else ruta.moveTo(px, py)
                        abierta = true
                    }
                    drawPath(ruta, tonos[s], style = trazo)
                }

                if (indice != null) {
                    val cx = x(muestras.tiemposNs[indice])
                    drawLine(cursor, Offset(cx, 0f), Offset(cx, alto), strokeWidth = 1.5f)
                    for (s in 0 until series) {
                        val v = muestras.series[s][indice]
                        if (v.isFinite()) {
                            drawCircle(colores.surface, radius = 6.dp.toPx(), center = Offset(cx, y(v)))
                            drawCircle(tonos[s], radius = 4.dp.toPx(), center = Offset(cx, y(v)))
                        }
                    }
                }
            }
        }

        // Eje horizontal: tiempo relativo a la última muestra
        Row(Modifier.fillMaxWidth().padding(start = 58.dp)) {
            Text(
                text = "−${FormatoSensor.numero(duracion)} s",
                style = MaterialTheme.typography.labelSmall,
                color = colores.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Tiempo · ahora",
                style = MaterialTheme.typography.labelSmall,
                color = colores.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Leyenda(nombres: List<String>, tonos: List<Color>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        nombres.forEachIndexed { i, nombre ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(width = 14.dp, height = 3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(tonos[i])
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = nombre,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
