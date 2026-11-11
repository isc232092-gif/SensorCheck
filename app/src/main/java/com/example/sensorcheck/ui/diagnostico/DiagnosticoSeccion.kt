package com.example.sensorcheck.ui.diagnostico

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChecklistRtl
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sensorcheck.domain.diagnostico.CategoriaPrueba
import com.example.sensorcheck.domain.diagnostico.Compatibilidad
import com.example.sensorcheck.domain.diagnostico.IdPrueba
import com.example.sensorcheck.domain.diagnostico.ResultadoPrueba
import com.example.sensorcheck.domain.diagnostico.SesionDiagnostico
import com.example.sensorcheck.ui.components.ContenidoPantalla
import com.example.sensorcheck.ui.components.Espaciado
import com.example.sensorcheck.ui.components.EtiquetaEstado
import com.example.sensorcheck.ui.components.TarjetaGrupo
import com.example.sensorcheck.ui.components.TarjetaPrincipal
import com.example.sensorcheck.ui.components.TituloSeccion

/** Punto de entrada del módulo Diagnóstico. */
@Composable
fun DiagnosticoSeccion(vm: DiagnosticoViewModel = viewModel()) {
    val elementos by vm.elementos.collectAsStateWithLifecycle()
    val pantalla by vm.pantalla.collectAsStateWithLifecycle()
    val sesion by vm.sesion.collectAsStateWithLifecycle()
    val resultado by vm.resultado.collectAsStateWithLifecycle()
    val seleccion by vm.seleccion.collectAsStateWithLifecycle()

    val lista = elementos ?: run {
        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        return
    }

    when (val p = pantalla) {
        PantallaDiagnostico.Inicio -> {
            BackHandler(enabled = seleccion != null) { vm.alternarModoSeleccion() }
            InicioDiagnostico(
                elementos = lista,
                seleccion = seleccion,
                onCompleto = vm::iniciarCompleto,
                onModoSeleccion = vm::alternarModoSeleccion,
                onAlternar = vm::alternarSeleccion,
                onEjecutarSeleccion = vm::iniciarSeleccionadas,
                onAbrir = vm::abrirPrueba
            )
        }

        is PantallaDiagnostico.Individual -> {
            val elemento = lista.first { it.definicion.id == p.id }
            BackHandler { vm.volverAlInicio() }
            PruebaScreen(
                elemento = elemento,
                sensores = vm.sensores,
                camaras = vm.camaras,
                contactosDeclarados = vm.contactosDeclarados,
                resultado = resultado,
                onGuardar = { vm.guardar(p.id, it) },
                onRepetir = vm::repetir,
                onContinuar = vm::volverAlInicio,
                textoContinuar = "Volver al listado",
                onVolver = vm::volverAlInicio
            )
        }

        PantallaDiagnostico.Asistente -> sesion?.let { s ->
            Asistente(s, lista, vm)
        }
    }
}

@Composable
private fun Asistente(sesion: SesionDiagnostico, lista: List<ElementoPrueba>, vm: DiagnosticoViewModel) {
    var confirmarSalida by remember { mutableStateOf(false) }
    val resultado by vm.resultado.collectAsStateWithLifecycle()

    if (sesion.terminada) {
        BackHandler { vm.volverAlInicio() }
        ResumenSesion(sesion, lista, onRepetir = vm::repetirPendientes, onFinalizar = vm::volverAlInicio)
        return
    }

    BackHandler { confirmarSalida = true }
    val id = sesion.actual ?: return
    val elemento = lista.first { it.definicion.id == id }

    PruebaScreen(
        elemento = elemento,
        sensores = vm.sensores,
        camaras = vm.camaras,
        contactosDeclarados = vm.contactosDeclarados,
        resultado = resultado,
        onGuardar = { vm.guardar(id, it) },
        onRepetir = vm::repetir,
        onContinuar = vm::continuarAsistente,
        textoContinuar = if (sesion.indice + 1 < sesion.total) "Continuar con la siguiente" else "Ver resumen",
        onOmitir = vm::omitirActual,
        cabecera = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Diagnóstico completo · prueba ${sesion.indice + 1} de ${sesion.total}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { confirmarSalida = true }) { Text("Salir") }
                }
                LinearProgressIndicator(progress = { sesion.progreso }, modifier = Modifier.fillMaxWidth())
            }
        }
    )

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Salir del diagnóstico?") },
            text = { Text("Las pruebas restantes no se ejecutarán. Los resultados ya obtenidos se conservan en el historial.") },
            confirmButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Seguir") } },
            dismissButton = {
                TextButton(onClick = { confirmarSalida = false; vm.volverAlInicio() }) { Text("Salir") }
            }
        )
    }
}

@Composable
private fun InicioDiagnostico(
    elementos: List<ElementoPrueba>,
    seleccion: Set<IdPrueba>?,
    onCompleto: () -> Unit,
    onModoSeleccion: () -> Unit,
    onAlternar: (IdPrueba) -> Unit,
    onEjecutarSeleccion: () -> Unit,
    onAbrir: (IdPrueba) -> Unit
) {
    val ejecutables = elementos.count { it.ejecutable }
    val grupos = CategoriaPrueba.entries.associateWith { c -> elementos.filter { it.definicion.categoria == c } }
        .filterValues { it.isNotEmpty() }

    ContenidoPantalla {
        item(key = "cabecera") {
            TarjetaPrincipal(
                icono = Icons.Outlined.MonitorHeart,
                etiqueta = "Diagnóstico físico",
                titulo = "$ejecutables pruebas disponibles",
                subtitulo = "Cada resultado se basa en la ejecución y su evidencia"
            )
        }
        item(key = "acciones") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (seleccion == null) {
                    Button(onClick = onCompleto, enabled = ejecutables > 0, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Iniciar diagnóstico completo")
                    }
                    OutlinedButton(onClick = onModoSeleccion, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        Icon(Icons.Outlined.ChecklistRtl, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Seleccionar pruebas individuales")
                    }
                } else {
                    Button(
                        onClick = onEjecutarSeleccion,
                        enabled = seleccion.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    ) { Text("Ejecutar seleccionadas (${seleccion.size})") }
                    OutlinedButton(onClick = onModoSeleccion, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("Cancelar selección")
                    }
                }
            }
        }
        grupos.forEach { (categoria, lista) ->
            item(key = "t_${categoria.name}") { TituloSeccion(categoria.titulo) }
            item(key = "g_${categoria.name}") {
                TarjetaGrupo {
                    lista.forEachIndexed { i, e ->
                        FilaPrueba(
                            elemento = e,
                            seleccionada = seleccion?.let { e.definicion.id in it },
                            onClick = {
                                when {
                                    seleccion != null -> if (e.ejecutable) onAlternar(e.definicion.id)
                                    e.definicion.implementada -> onAbrir(e.definicion.id)
                                }
                            }
                        )
                        if (i < lista.lastIndex) {
                            HorizontalDivider(Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaPrueba(elemento: ElementoPrueba, seleccionada: Boolean?, onClick: () -> Unit) {
    val c = MaterialTheme.colorScheme
    val def = elemento.definicion
    val compat = when {
        !def.implementada -> "Pendiente de implementar"
        elemento.compatibilidad is Compatibilidad.NoCompatible -> "No compatible"
        else -> "Compatible"
    }
    val habilitada = if (seleccionada != null) elemento.ejecutable else def.implementada
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = habilitada, onClick = onClick)
            .padding(horizontal = Espaciado.InternoTarjeta, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            iconoPrueba(def.id), contentDescription = null,
            tint = if (habilitada) c.primary else c.outline,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(Espaciado.InternoTarjeta))
        Column(Modifier.weight(1f)) {
            Text(
                def.nombre, style = MaterialTheme.typography.bodyLarge,
                color = if (habilitada) c.onSurface else c.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Text(
                "${def.componente} · $compat", style = MaterialTheme.typography.bodySmall,
                color = c.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        if (seleccionada != null) {
            Checkbox(checked = seleccionada, onCheckedChange = { onClick() }, enabled = elemento.ejecutable)
        } else {
            elemento.ultimo?.let { ChipResultado(it.resultado) }
            if (def.implementada) Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = c.onSurfaceVariant)
        }
    }
}

@Composable
fun ChipResultado(r: ResultadoPrueba) {
    val (fondo, tinta) = coloresResultado(r)
    EtiquetaEstado(r.etiqueta, fondo, tinta)
}

@Composable
private fun ResumenSesion(
    sesion: SesionDiagnostico,
    lista: List<ElementoPrueba>,
    onRepetir: () -> Unit,
    onFinalizar: () -> Unit
) {
    val conteo = sesion.conteo()
    val pendientes = sesion.paraRepetir()
    ContenidoPantalla {
        item(key = "cabecera") {
            TarjetaPrincipal(
                icono = Icons.Outlined.MonitorHeart,
                etiqueta = "Diagnóstico terminado",
                titulo = "${conteo[ResultadoPrueba.CORRECTO] ?: 0} de ${sesion.total} correctas",
                subtitulo = ResultadoPrueba.entries.mapNotNull { r -> conteo[r]?.let { "${r.etiqueta}: $it" } }.joinToString(" · ")
            )
        }
        item(key = "t_lista") { TituloSeccion("Resultados") }
        item(key = "lista") {
            TarjetaGrupo {
                sesion.cola.forEachIndexed { i, id ->
                    val def = lista.first { it.definicion.id == id }.definicion
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = Espaciado.InternoTarjeta, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(iconoPrueba(id), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(Espaciado.InternoTarjeta))
                        Text(def.nombre, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        ChipResultado(sesion.resultados[id]?.resultado ?: ResultadoPrueba.NO_EJECUTADO)
                    }
                    if (i < sesion.cola.lastIndex) {
                        HorizontalDivider(Modifier.padding(start = 52.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
        item(key = "acciones") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pendientes.isNotEmpty()) {
                    Button(onClick = onRepetir, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        Text("Repetir fallidas, indeterminadas u omitidas (${pendientes.size})")
                    }
                }
                OutlinedButton(onClick = onFinalizar, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text("Finalizar")
                }
            }
        }
    }
}
