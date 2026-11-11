package com.example.sensorcheck.ui.components

import android.content.ClipData
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Locale

const val TEXTO_NO_DISPONIBLE = "No disponible"

/** Espaciados compartidos por todas las pantallas. */
object Espaciado {
    val Elementos = 12.dp
    val InternoTarjeta = 16.dp
    val AnchoMaximoContenido = 720.dp
    val Esquinas = RoundedCornerShape(16.dp)

    /** Margen lateral según el ancho disponible. */
    fun margenLateral(ancho: Dp): Dp = when {
        ancho < 360.dp -> 12.dp
        ancho < 600.dp -> 16.dp
        else -> 24.dp
    }
}

/** Dato con icono, título y valor para las listas de información. */
data class DatoInfo(
    val icono: ImageVector,
    val titulo: String,
    val valor: String?,
    /** Muestra un botón para copiar el valor (valores técnicos largos). */
    val copiable: Boolean = false
)

/**
 * Contenedor desplazable común. Adapta márgenes al ancho de la
 * pantalla y limita el ancho del contenido en tablets/horizontal.
 * No aplica insets: los gestiona Scaffold.
 */
@Composable
fun ContenidoPantalla(
    separacion: Dp = Espaciado.Elementos,
    contenido: LazyListScope.() -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val margen = Espaciado.margenLateral(maxWidth)
        LazyColumn(
            modifier = Modifier
                .widthIn(max = Espaciado.AnchoMaximoContenido)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = margen,
                vertical = Espaciado.Elementos
            ),
            verticalArrangement = Arrangement.spacedBy(separacion),
            content = contenido
        )
    }
}

/** Tarjeta principal compacta de cada pantalla. */
@Composable
fun TarjetaPrincipal(
    icono: ImageVector,
    etiqueta: String,
    titulo: String,
    subtitulo: String? = null
) {
    val colores = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colores.primaryContainer,
        contentColor = colores.onPrimaryContainer,
        shape = Espaciado.Esquinas
    ) {
        Row(
            modifier = Modifier.padding(Espaciado.InternoTarjeta),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = colores.primary,
                contentColor = colores.onPrimary,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icono, contentDescription = null, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(Modifier.width(Espaciado.InternoTarjeta))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = etiqueta.uppercase(Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = titulo.ifBlank { TEXTO_NO_DISPONIBLE },
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitulo != null) {
                    Text(
                        text = subtitulo,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/** Título de sección dentro de una pantalla. */
@Composable
fun TituloSeccion(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

/** Tarjeta con superficie común para agrupar contenido. */
@Composable
fun TarjetaGrupo(
    contenido: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = Espaciado.Esquinas,
        tonalElevation = 1.dp
    ) {
        Column(content = contenido)
    }
}

/** Grupo de filas de información separadas por divisores. */
@Composable
fun GrupoInfo(datos: List<DatoInfo>) {
    TarjetaGrupo {
        datos.forEachIndexed { indice, dato ->
            FilaInfo(dato)
            if (indice < datos.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 52.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

/** Fila compacta: icono, título y valor (los valores largos se ajustan). */
@Composable
fun FilaInfo(dato: DatoInfo) {
    val colores = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Espaciado.InternoTarjeta, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = dato.icono,
            contentDescription = null,
            tint = colores.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(Espaciado.InternoTarjeta))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = dato.titulo,
                style = MaterialTheme.typography.labelMedium,
                color = colores.onSurfaceVariant
            )
            Text(
                text = dato.valor?.ifBlank { null } ?: TEXTO_NO_DISPONIBLE,
                style = MaterialTheme.typography.bodyLarge,
                color = colores.onSurface
            )
        }
        val valor = dato.valor?.ifBlank { null }
        if (dato.copiable && valor != null) {
            BotonCopiar(etiqueta = dato.titulo, valor = valor)
        }
    }
}

/** Copia un valor al portapapeles. Android 13+ ya muestra su propio aviso. */
@Composable
private fun BotonCopiar(etiqueta: String, valor: String) {
    val portapapeles = LocalClipboard.current
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    IconButton(
        onClick = {
            alcance.launch {
                portapapeles.setClipEntry(ClipEntry(ClipData.newPlainText(etiqueta, valor)))
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(contexto, "$etiqueta copiado", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.ContentCopy,
            contentDescription = "Copiar $etiqueta",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Sección plegable con encabezado pulsable. */
@Composable
fun SeccionExpandible(
    titulo: String,
    expandida: Boolean,
    onCambiar: () -> Unit,
    contenido: @Composable () -> Unit
) {
    val colores = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(Espaciado.Elementos)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = if (expandida) "Contraer" else "Expandir") { onCambiar() }
                .padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                color = colores.primary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expandida) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = colores.primary
            )
        }
        AnimatedVisibility(visible = expandida) {
            contenido()
        }
    }
}

/**
 * Tarjeta con barra de progreso. [fraccion] debe calcularse a partir
 * de mediciones reales; si es null se muestra "No disponible".
 */
@Composable
fun TarjetaProgreso(
    icono: ImageVector,
    titulo: String,
    fraccion: Float?,
    detalle: String?,
    color: Color = MaterialTheme.colorScheme.primary,
    datosAdicionales: List<DatoInfo> = emptyList()
) {
    val colores = MaterialTheme.colorScheme
    TarjetaGrupo {
        Column(
            modifier = Modifier.padding(Espaciado.InternoTarjeta),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Espaciado.InternoTarjeta))
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = colores.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = fraccion?.let { formatoPorcentaje(it) } ?: TEXTO_NO_DISPONIBLE,
                    style = MaterialTheme.typography.titleMedium,
                    color = color
                )
            }
            if (fraccion != null) {
                LinearProgressIndicator(
                    progress = { fraccion.coerceIn(0f, 1f) },
                    color = color,
                    trackColor = colores.surfaceContainerHighest,
                    drawStopIndicator = {},
                    gapSize = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )
            }
            if (detalle != null) {
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colores.onSurfaceVariant
                )
            }
        }
        if (datosAdicionales.isNotEmpty()) {
            HorizontalDivider(color = colores.outlineVariant)
            datosAdicionales.forEach { FilaInfo(it) }
        }
    }
}

/** Etiqueta pequeña de estado (p. ej. "En desarrollo"). */
@Composable
fun EtiquetaEstado(
    texto: String,
    contenedor: Color = MaterialTheme.colorScheme.tertiaryContainer,
    contenido: Color = MaterialTheme.colorScheme.onTertiaryContainer
) {
    Surface(
        color = contenedor,
        contentColor = contenido,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

fun formatoPorcentaje(fraccion: Float): String =
    String.format(Locale.getDefault(), "%.0f %%", fraccion * 100f)
