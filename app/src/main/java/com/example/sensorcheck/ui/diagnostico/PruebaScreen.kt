package com.example.sensorcheck.ui.diagnostico

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.sensorcheck.domain.diagnostico.CatalogoPruebas
import com.example.sensorcheck.domain.diagnostico.Compatibilidad
import com.example.sensorcheck.domain.diagnostico.IdPrueba
import com.example.sensorcheck.domain.diagnostico.ResultadoEvaluado
import com.example.sensorcheck.domain.diagnostico.ResultadoPrueba
import com.example.sensorcheck.domain.repository.CameraRepository
import com.example.sensorcheck.domain.repository.SensorRepository
import com.example.sensorcheck.ui.components.ContenidoPantalla
import com.example.sensorcheck.ui.components.Espaciado
import com.example.sensorcheck.ui.components.EtiquetaEstado
import com.example.sensorcheck.ui.components.TarjetaGrupo
import com.example.sensorcheck.ui.components.TarjetaPrincipal
import com.example.sensorcheck.ui.components.TituloSeccion
import com.example.sensorcheck.ui.diagnostico.ejecutores.ContextoEjecucion
import com.example.sensorcheck.ui.diagnostico.ejecutores.Ejecutores

/**
 * Flujo común de cada prueba: instrucciones y compatibilidad → permisos
 * → ejecución → evidencia y resultado → repetir o continuar.
 */
@Composable
fun PruebaScreen(
    elemento: ElementoPrueba,
    sensores: SensorRepository,
    camaras: CameraRepository,
    contactosDeclarados: Int,
    resultado: ResultadoEvaluado?,
    onGuardar: (ResultadoEvaluado) -> Unit,
    onRepetir: () -> Unit,
    onContinuar: () -> Unit,
    textoContinuar: String,
    onVolver: (() -> Unit)? = null,
    onOmitir: (() -> Unit)? = null,
    cabecera: @Composable () -> Unit = {}
) {
    val def = elemento.definicion
    val ejecutor = Ejecutores.para(def.id)
    var ejecutando by rememberSaveable(def.id) { mutableStateOf(false) }
    /** Cada ejecución usa un contexto nuevo (Repetir no reutiliza el anterior). */
    var corrida by rememberSaveable(def.id) { mutableIntStateOf(0) }
    val contexto = LocalContext.current

    val permisos = def.permisos.filter { it.aplica(Build.VERSION.SDK_INT) }
    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        val denegados = permisos.filter { r[it.permiso] == false }
        if (denegados.isEmpty()) {
            corrida++
            ejecutando = true
        } else onGuardar(
            CatalogoPruebas.noEjecutada(def.id, "Permiso denegado: ${denegados.joinToString { it.descripcion }}")
        )
    }

    fun iniciar() {
        val faltan = permisos.filter {
            ContextCompat.checkSelfPermission(contexto, it.permiso) != PackageManager.PERMISSION_GRANTED
        }
        if (faltan.isEmpty()) {
            corrida++
            ejecutando = true
        } else lanzador.launch(faltan.map { it.permiso }.toTypedArray())
    }

    if (ejecutando && resultado == null && ejecutor != null) {
        val ctx = remember(def.id, corrida) {
            ContextoEjecucion(def, sensores, camaras, contactosDeclarados) { r ->
                ejecutando = false
                onGuardar(r)
            }
        }
        ejecutor(ctx)
    }

    ContenidoPantalla {
        item(key = "cabecera") { cabecera() }
        if (onVolver != null) {
            item(key = "volver") {
                TextButton(onClick = onVolver, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Todas las pruebas")
                }
            }
        }
        item(key = "titulo") {
            TarjetaPrincipal(icono = iconoPrueba(def.id), etiqueta = def.componente, titulo = def.nombre, subtitulo = def.categoria.titulo)
        }

        if (resultado != null) {
            item(key = "resultado") { TarjetaResultado(resultado) }
            item(key = "acciones") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onContinuar, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(textoContinuar) }
                    if (elemento.ejecutable && ejecutor != null) {
                        OutlinedButton(onClick = onRepetir, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                            Icon(Icons.Outlined.Replay, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Repetir prueba")
                        }
                    }
                }
            }
        } else {
            item(key = "compatibilidad") { FilaCompatibilidad(elemento) }
            if (def.instrucciones.isNotEmpty()) {
                item(key = "t_instrucciones") { TituloSeccion("Instrucciones") }
                item(key = "instrucciones") {
                    TarjetaGrupo {
                        Column(Modifier.padding(Espaciado.InternoTarjeta), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            def.instrucciones.forEachIndexed { i, paso ->
                                Text("${i + 1}. $paso", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
            item(key = "t_criterio") { TituloSeccion("Criterio de evaluación") }
            item(key = "criterio") {
                Text(def.criterio, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 4.dp))
            }
            if (permisos.isNotEmpty()) {
                item(key = "permisos") {
                    Text(
                        "Se pedirá permiso para: ${permisos.joinToString { it.descripcion }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
            item(key = "iniciar") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = ::iniciar,
                        enabled = elemento.ejecutable && ejecutor != null,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Iniciar prueba")
                    }
                    if (onOmitir != null) {
                        TextButton(onClick = onOmitir, modifier = Modifier.fillMaxWidth()) { Text("Omitir esta prueba") }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaCompatibilidad(elemento: ElementoPrueba) {
    val c = MaterialTheme.colorScheme
    val (texto, detalle, fondo, tinta) = when {
        !elemento.definicion.implementada ->
            Cuatro("Pendiente", "Esta prueba aún no está implementada.", c.surfaceContainerHighest, c.onSurfaceVariant)
        elemento.compatibilidad is Compatibilidad.NoCompatible ->
            Cuatro("No compatible", elemento.compatibilidad.motivo, c.surfaceContainerHighest, c.onSurfaceVariant)
        else -> Cuatro("Compatible", "Android ofrece acceso a este componente.", c.secondaryContainer, c.onSecondaryContainer)
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        EtiquetaEstado(texto, fondo, tinta)
        Spacer(Modifier.width(8.dp))
        Text(detalle, style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
    }
}

private data class Cuatro(val a: String, val b: String, val c: Color, val d: Color)

@Composable
fun TarjetaResultado(r: ResultadoEvaluado) {
    val (fondo, tinta) = coloresResultado(r.resultado)
    Column(verticalArrangement = Arrangement.spacedBy(Espaciado.Elementos)) {
        Surface(color = fondo, contentColor = tinta, shape = Espaciado.Esquinas, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(Espaciado.InternoTarjeta), verticalAlignment = Alignment.CenterVertically) {
                Icon(iconoResultado(r.resultado), contentDescription = null, modifier = Modifier.size(36.dp))
                Spacer(Modifier.width(Espaciado.InternoTarjeta))
                Column {
                    Text("RESULTADO", style = MaterialTheme.typography.labelMedium)
                    Text(r.resultado.etiqueta, style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
        TituloSeccion("Evidencia")
        TarjetaGrupo {
            Column(Modifier.padding(Espaciado.InternoTarjeta), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                r.evidencia.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            }
        }
        TituloSeccion("Recomendación")
        Text(r.recomendacion, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
fun coloresResultado(r: ResultadoPrueba): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (r) {
        ResultadoPrueba.CORRECTO -> c.primaryContainer to c.onPrimaryContainer
        ResultadoPrueba.FALLO -> c.errorContainer to c.onErrorContainer
        ResultadoPrueba.INDETERMINADO -> c.tertiaryContainer to c.onTertiaryContainer
        ResultadoPrueba.NO_COMPATIBLE, ResultadoPrueba.NO_EJECUTADO -> c.surfaceContainerHighest to c.onSurfaceVariant
    }
}

fun iconoResultado(r: ResultadoPrueba): ImageVector = when (r) {
    ResultadoPrueba.CORRECTO -> Icons.Outlined.CheckCircle
    ResultadoPrueba.FALLO -> Icons.Outlined.Cancel
    ResultadoPrueba.INDETERMINADO -> Icons.AutoMirrored.Outlined.HelpOutline
    ResultadoPrueba.NO_COMPATIBLE -> Icons.Outlined.Block
    ResultadoPrueba.NO_EJECUTADO -> Icons.Outlined.RemoveCircleOutline
}

fun iconoPrueba(id: IdPrueba): ImageVector = when (id) {
    IdPrueba.TACTIL -> Icons.Outlined.TouchApp
    IdPrueba.MULTITACTIL -> Icons.Outlined.PanTool
    IdPrueba.COLORES -> Icons.Outlined.Palette
    IdPrueba.PIXELES -> Icons.Outlined.GridOn
    IdPrueba.ACELEROMETRO -> Icons.Outlined.Speed
    IdPrueba.GIROSCOPIO -> Icons.Outlined._3dRotation
    IdPrueba.PROXIMIDAD -> Icons.Outlined.Sensors
    IdPrueba.LUZ -> Icons.Outlined.LightMode
    IdPrueba.MAGNETOMETRO -> Icons.Outlined.Explore
    IdPrueba.CAMARA_TRASERA, IdPrueba.CAMARA_FRONTAL -> Icons.Outlined.PhotoCamera
    IdPrueba.FLASH -> Icons.Outlined.FlashlightOn
    IdPrueba.ALTAVOZ -> Icons.AutoMirrored.Outlined.VolumeUp
    IdPrueba.MICROFONO -> Icons.Outlined.Mic
    IdPrueba.VIBRACION -> Icons.Outlined.Vibration
    IdPrueba.WIFI -> Icons.Outlined.Wifi
    IdPrueba.BLUETOOTH -> Icons.Outlined.Bluetooth
    IdPrueba.GPS -> Icons.Outlined.GpsFixed
    IdPrueba.BATERIA -> Icons.Outlined.BatteryFull
    IdPrueba.ALMACENAMIENTO -> Icons.Outlined.Storage
    IdPrueba.RAM -> Icons.Outlined.Memory
    IdPrueba.BOTONES -> Icons.Outlined.SmartButton
}
