package com.example.sensorcheck.ui.sensors

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sensorcheck.domain.model.ModoReporte
import com.example.sensorcheck.domain.model.RitmoMuestreo
import com.example.sensorcheck.domain.sensors.CatalogoSensores
import com.example.sensorcheck.domain.sensors.ElementoSensor
import com.example.sensorcheck.domain.sensors.EstadoMonitoreo
import com.example.sensorcheck.domain.sensors.FaseMonitoreo
import com.example.sensorcheck.domain.sensors.FormatoSensor
import com.example.sensorcheck.domain.sensors.PermisoSensor
import com.example.sensorcheck.ui.components.ContenidoPantalla
import com.example.sensorcheck.ui.components.DatoInfo
import com.example.sensorcheck.ui.components.Espaciado
import com.example.sensorcheck.ui.components.EtiquetaEstado
import com.example.sensorcheck.ui.components.GrupoInfo
import com.example.sensorcheck.ui.components.TarjetaGrupo
import com.example.sensorcheck.ui.components.TarjetaPrincipal
import com.example.sensorcheck.ui.components.TituloSeccion
import com.example.sensorcheck.domain.format.FormatoSistema
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FormatoHora = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

/** Segundos sin eventos a partir de los cuales se avisa al usuario. */
private const val SILENCIO_AVISO_MS = 3_000L

/**
 * Ficha de un sensor: características del fabricante y monitoreo en vivo.
 * El listener se libera al detener, al salir de la pantalla y al pasar a
 * segundo plano; se conserva solo durante un cambio de configuración.
 */
@Composable
fun SensorDetailScreen(
    clave: String,
    onVolver: () -> Unit,
    viewModel: SensorDetailViewModel = viewModel()
) {
    LaunchedEffect(clave) { viewModel.abrir(clave) }

    val actividad = LocalActivity.current
    LifecycleStartEffect(viewModel) {
        viewModel.pantallaVisible(true)
        onStopOrDispose { viewModel.pantallaVisible(false) }
    }
    DisposableEffect(viewModel) {
        onDispose {
            if (actividad?.isChangingConfigurations != true) viewModel.detener()
        }
    }
    BackHandler(onBack = onVolver)

    val ficha by viewModel.ficha.collectAsStateWithLifecycle()
    val monitoreo by viewModel.monitoreo.collectAsStateWithLifecycle()

    when (val f = ficha) {
        EstadoFicha.Cargando -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator()
        }

        EstadoFicha.NoEncontrado -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Android ya no expone este sensor (pudo desconectarse si era dinámico).",
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onVolver) { Text("Volver al listado") }
        }

        is EstadoFicha.Lista -> FichaSensor(
            elemento = f.elemento,
            monitoreo = monitoreo,
            onVolver = onVolver,
            onIniciar = viewModel::iniciar,
            onDetener = viewModel::detener,
            onRitmo = viewModel::cambiarRitmo,
            onSinPermiso = viewModel::sinPermiso
        )
    }
}

@Composable
private fun FichaSensor(
    elemento: ElementoSensor,
    monitoreo: EstadoMonitoreo,
    onVolver: () -> Unit,
    onIniciar: () -> Unit,
    onDetener: () -> Unit,
    onRitmo: (RitmoMuestreo) -> Unit,
    onSinPermiso: (String) -> Unit
) {
    val info = elemento.info
    val contexto = LocalContext.current
    val permiso = info.permisoRequerido
    val nombrePermiso = PermisoSensor.entries.firstOrNull { it.permiso == permiso }?.descripcion ?: permiso

    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) onIniciar()
        else onSinPermiso("Permiso «$nombrePermiso» denegado. Android no entrega lecturas de este sensor sin él.")
    }

    val iniciarConPermiso: () -> Unit = {
        when {
            permiso == null || tienePermiso(contexto, permiso) -> onIniciar()
            !permisoDeclarado(contexto, permiso) -> onSinPermiso(
                "Este sensor requiere el permiso «$nombrePermiso», que SensorCheck no solicita. " +
                    "No es posible leerlo desde la aplicación."
            )
            else -> lanzador.launch(permiso)
        }
    }

    ContenidoPantalla {
        item(key = "volver") {
            TextButton(onClick = onVolver, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Todos los sensores")
            }
        }

        item(key = "cabecera") {
            TarjetaPrincipal(
                icono = iconoSensor(info.tipo),
                etiqueta = elemento.nombreTipo,
                titulo = info.nombre,
                subtitulo = info.fabricante
            )
        }

        item(key = "titulo_lectura") { TituloSeccion("Lectura en tiempo real") }
        item(key = "lectura") {
            TarjetaMonitoreo(
                elemento = elemento,
                estado = monitoreo,
                onIniciar = iniciarConPermiso,
                onDetener = onDetener,
                onRitmo = onRitmo
            )
        }

        item(key = "titulo_fabricante") { TituloSeccion("Características reportadas por el fabricante") }
        item(key = "aviso_fabricante") {
            Text(
                text = "Valores declarados por el fabricante a través de Android; no son mediciones.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        item(key = "fabricante") { GrupoInfo(datosFabricante(elemento)) }

        item(key = "titulo_identificacion") { TituloSeccion("Identificación") }
        item(key = "identificacion") { GrupoInfo(datosIdentificacion(elemento)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarjetaMonitoreo(
    elemento: ElementoSensor,
    estado: EstadoMonitoreo,
    onIniciar: () -> Unit,
    onDetener: () -> Unit,
    onRitmo: (RitmoMuestreo) -> Unit
) {
    val colores = MaterialTheme.colorScheme
    val info = elemento.info
    val descriptor = elemento.descriptor
    val unico = info.modoReporte == ModoReporte.UNICO
    val graficable = descriptor != null && descriptor.seriesGrafica > 0 && !unico

    // Reloj para indicar cuánto hace de la última lectura
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(estado.activo) {
        while (estado.activo) {
            ahora = System.currentTimeMillis()
            delay(1_000)
        }
    }

    TarjetaGrupo {
        Column(
            modifier = Modifier.padding(Espaciado.InternoTarjeta),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (texto, fondo, tinta) = etiquetaFase(estado.fase)
                EtiquetaEstado(texto, fondo, tinta)
                Spacer(Modifier.weight(1f))
                if (estado.fase == FaseMonitoreo.RECIBIENDO) {
                    FormatoSensor.frecuencia(estado.muestras.frecuenciaHz)?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium, color = colores.onSurfaceVariant)
                    }
                }
            }

            textoFase(estado, info.modoReporte, ahora)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (estado.fase == FaseMonitoreo.ERROR) colores.error else colores.onSurfaceVariant
                )
            }

            if (info.modoReporte == ModoReporte.CONTINUO) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    RitmoMuestreo.entries.forEachIndexed { i, r ->
                        SegmentedButton(
                            selected = estado.ritmo == r,
                            onClick = { onRitmo(r) },
                            shape = SegmentedButtonDefaults.itemShape(i, RitmoMuestreo.entries.size),
                            label = { Text(r.etiqueta, maxLines = 1) }
                        )
                    }
                }
            }

            if (estado.activo || estado.fase == FaseMonitoreo.PAUSADO) {
                OutlinedButton(onClick = onDetener, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (unico) "Cancelar espera" else "Detener monitoreo")
                }
            } else {
                Button(onClick = onIniciar, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (unico) "Esperar disparo" else "Iniciar monitoreo")
                }
            }

            estado.ultima?.let { lectura ->
                HorizontalDivider(color = colores.outlineVariant)
                ValoresLectura(elemento, lectura.valores)
                FilaDato("Precisión reportada", FormatoSensor.precision(estado.precision) ?: "No reportada")
                val hora = FormatoHora.format(
                    Instant.ofEpochMilli(lectura.horaEpochMs).atZone(ZoneId.systemDefault())
                )
                FilaDato(
                    "Última lectura",
                    if (estado.activo) "$hora · ${FormatoSensor.antiguedad((ahora - lectura.horaEpochMs).coerceAtLeast(0))}"
                    else hora
                )
                FilaDato("Marca del evento", "${lectura.marcaTiempoNs} ns")
                FilaDato("Eventos recibidos", estado.eventos.toString())
            }

            val mostrarGrafica = estado.activo || estado.fase == FaseMonitoreo.PAUSADO || estado.muestras.tamano >= 2
            if (graficable && mostrarGrafica) {
                HorizontalDivider(color = colores.outlineVariant)
                SensorChart(
                    muestras = estado.muestras,
                    nombres = descriptor.ejes.take(descriptor.seriesGrafica).map { it.nombre },
                    unidad = descriptor.unidadGrafica,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ValoresLectura(elemento: ElementoSensor, valores: List<Float>) {
    val info = elemento.info
    val mostrados = FormatoSensor.valores(elemento.descriptor, valores)
    if (mostrados.isEmpty()) {
        FilaDato("Valores", "El evento no incluye valores")
        return
    }
    mostrados.forEach { v ->
        FilaDato(v.nombre, FormatoSensor.conUnidad(v.valor, v.unidad), destacado = true)
    }
    if (info.tipo == CatalogoSensores.TYPE_PROXIMITY) {
        FormatoSensor.proximidad(valores.firstOrNull() ?: Float.NaN, info.rangoMaximo)?.let {
            FilaDato("Interpretación cerca/lejos", it)
        }
    }
    if (elemento.descriptor == null) {
        Text(
            text = "Tipo no catalogado: se muestran los valores sin unidad.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FilaDato(titulo: String, valor: String, destacado: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = valor,
            style = if (destacado) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun etiquetaFase(fase: FaseMonitoreo): Triple<String, Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (fase) {
        FaseMonitoreo.INACTIVO -> Triple("Inactivo", c.surfaceContainerHighest, c.onSurfaceVariant)
        FaseMonitoreo.ESPERANDO -> Triple("Esperando eventos", c.tertiaryContainer, c.onTertiaryContainer)
        FaseMonitoreo.RECIBIENDO -> Triple("Recibiendo eventos", c.primaryContainer, c.onPrimaryContainer)
        FaseMonitoreo.PAUSADO -> Triple("En pausa", c.secondaryContainer, c.onSecondaryContainer)
        FaseMonitoreo.DISPARADO -> Triple("Evento recibido", c.primaryContainer, c.onPrimaryContainer)
        FaseMonitoreo.ERROR -> Triple("Error", c.errorContainer, c.onErrorContainer)
    }
}

private fun textoFase(estado: EstadoMonitoreo, modo: ModoReporte, ahora: Long): String? = when (estado.fase) {
    FaseMonitoreo.INACTIVO ->
        if (estado.ultima == null) "El sensor no está registrado. Pulsa iniciar para recibir lecturas."
        else "Monitoreo detenido. Se conserva la última lectura recibida."
    FaseMonitoreo.ESPERANDO -> when (modo) {
        ModoReporte.AL_CAMBIO -> "Listener registrado. Este sensor solo emite cuando su valor cambia."
        ModoReporte.UNICO -> "Esperando el disparo. Android desactivará el sensor tras el primer evento."
        ModoReporte.ESPECIAL -> "Listener registrado. Este sensor solo emite ante eventos concretos."
        else -> "Listener registrado. Aún no se ha recibido ningún evento."
    }
    FaseMonitoreo.RECIBIENDO -> estado.ultima?.let { l ->
        val silencio = ahora - l.horaEpochMs
        if (silencio > SILENCIO_AVISO_MS) {
            "Sin eventos nuevos ${FormatoSensor.antiguedad(silencio)}" +
                if (modo == ModoReporte.CONTINUO) "." else " (normal si el valor no cambia)."
        } else null
    }
    FaseMonitoreo.PAUSADO -> "La aplicación no está visible: el listener se liberó y se reanudará al volver."
    FaseMonitoreo.DISPARADO -> "El sensor se disparó y Android lo desactivó. Puedes volver a esperar otro evento."
    FaseMonitoreo.ERROR -> estado.error
}

private fun datosFabricante(e: ElementoSensor): List<DatoInfo> {
    val i = e.info
    val unidad = FormatoSensor.unidadPrincipal(e.descriptor)
    return listOf(
        DatoInfo(Icons.Outlined.Factory, "Fabricante", i.fabricante),
        DatoInfo(Icons.Outlined.Numbers, "Versión", i.version.toString()),
        DatoInfo(
            Icons.Outlined.SquareFoot, "Unidades",
            FormatoSensor.unidades(e.descriptor) ?: "No documentadas por Android para este tipo"
        ),
        DatoInfo(Icons.Outlined.Straighten, "Resolución", FormatoSensor.conUnidad(i.resolucion, unidad)),
        DatoInfo(Icons.Outlined.OpenInFull, "Rango máximo", FormatoSensor.conUnidad(i.rangoMaximo, unidad)),
        DatoInfo(Icons.Outlined.BatteryChargingFull, "Consumo reportado", FormatoSensor.consumo(i.consumoMa)),
        DatoInfo(Icons.Outlined.Timer, "Retardo mínimo", FormatoSensor.retardoMinimo(i.retardoMinimoUs, i.modoReporte)),
        DatoInfo(Icons.Outlined.HourglassEmpty, "Retardo máximo", FormatoSensor.retardoMaximo(i.retardoMaximoUs)),
        DatoInfo(Icons.Outlined.Sync, "Modo de reporte", FormatoSensor.modoReporte(i.modoReporte)),
        DatoInfo(Icons.Outlined.Bolt, "Activación", FormatoSensor.activacion(i.modoReporte)),
        DatoInfo(Icons.Outlined.Alarm, "Despierta el dispositivo", FormatoSistema.siNo(i.despiertaDispositivo)),
        DatoInfo(Icons.Outlined.Storage, "FIFO por lotes", FormatoSensor.fifo(i.fifoMaximo, i.fifoReservado))
    )
}

private fun datosIdentificacion(e: ElementoSensor): List<DatoInfo> {
    val i = e.info
    val tipo = if (CatalogoSensores.esPrivadoFabricante(i.tipo)) "${i.tipo} (privado del fabricante)" else i.tipo.toString()
    val permiso = PermisoSensor.entries.firstOrNull { it.permiso == i.permisoRequerido }
    return buildList {
        add(DatoInfo(Icons.Outlined.Category, "Tipo", e.nombreTipo))
        add(DatoInfo(Icons.Outlined.Tag, "Identificador de tipo", tipo))
        add(DatoInfo(Icons.Outlined.Code, "stringType", i.stringType, copiable = true))
        add(DatoInfo(Icons.Outlined.Folder, "Categoría", e.categoria.titulo))
        if (e.descriptor?.derivado == true) {
            add(
                DatoInfo(
                    Icons.Outlined.Layers, "Naturaleza",
                    "Tipo compuesto: Android lo define a partir de otros sensores"
                )
            )
        }
        add(DatoInfo(Icons.Outlined.Badge, "Id. del sensor", i.idSensor?.toString() ?: "No definido por el fabricante"))
        add(DatoInfo(Icons.Outlined.Usb, "Sensor dinámico", FormatoSistema.siNo(i.dinamico)))
        add(
            DatoInfo(
                Icons.Outlined.Lock, "Permiso requerido",
                permiso?.let { "${it.descripcion} (${it.permiso.substringAfterLast('.')})" } ?: "Ninguno"
            )
        )
    }
}

private fun tienePermiso(contexto: Context, permiso: String): Boolean =
    ContextCompat.checkSelfPermission(contexto, permiso) == PackageManager.PERMISSION_GRANTED

/** Un permiso no declarado en el manifiesto se deniega siempre sin mostrar diálogo. */
private fun permisoDeclarado(contexto: Context, permiso: String): Boolean = try {
    @Suppress("DEPRECATION")
    contexto.packageManager
        .getPackageInfo(contexto.packageName, PackageManager.GET_PERMISSIONS)
        .requestedPermissions?.contains(permiso) == true
} catch (e: Exception) {
    false
}
