package com.example.sensorcheck.ui.sensors

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sensorcheck.domain.model.CategoriaSensor
import com.example.sensorcheck.domain.sensors.CatalogoSensores
import com.example.sensorcheck.domain.sensors.ElementoSensor
import com.example.sensorcheck.ui.components.ContenidoPantalla
import com.example.sensorcheck.ui.components.Espaciado
import com.example.sensorcheck.ui.components.EtiquetaEstado
import com.example.sensorcheck.ui.components.SeccionExpandible
import com.example.sensorcheck.ui.components.TarjetaGrupo
import com.example.sensorcheck.ui.components.TarjetaPrincipal
import com.example.sensorcheck.ui.components.TituloSeccion

private val RadioGrupo = 16.dp

/** Listado de sensores detectados. [onAbrir] null = filas no navegables. */
@Composable
fun SensorsScreen(
    onAbrir: ((String) -> Unit)?,
    viewModel: SensorsViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val vista by viewModel.vista.collectAsStateWithLifecycle()
    val busqueda by viewModel.busqueda.collectAsStateWithLifecycle()
    val categoria by viewModel.categoria.collectAsStateWithLifecycle()

    when (val e = estado) {
        EstadoInventario.Cargando -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator()
        }

        is EstadoInventario.Error -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No se pudo consultar los sensores:\n${e.mensaje}",
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = viewModel::cargar) { Text("Reintentar") }
        }

        is EstadoInventario.Listo -> vista?.let {
            ListadoSensores(
                vista = it,
                busqueda = busqueda,
                categoria = categoria,
                onBuscar = viewModel::buscar,
                onFiltrar = viewModel::filtrar,
                onAbrir = onAbrir
            )
        }
    }
}

@Composable
private fun ListadoSensores(
    vista: VistaInventario,
    busqueda: String,
    categoria: CategoriaSensor?,
    onBuscar: (String) -> Unit,
    onFiltrar: (CategoriaSensor?) -> Unit,
    onAbrir: ((String) -> Unit)?
) {
    var noDetectadosAbierto by rememberSaveable { mutableStateOf(false) }

    // Separación 0: las filas de un mismo grupo forman una sola tarjeta
    ContenidoPantalla(separacion = 0.dp) {

        item(key = "cabecera") {
            TarjetaPrincipal(
                icono = Icons.Outlined.Sensors,
                etiqueta = "Sensores detectados",
                titulo = vista.total.toString(),
                subtitulo = "Inventario expuesto por Android en este dispositivo"
            )
        }

        item(key = "busqueda") {
            CampoBusqueda(
                texto = busqueda,
                onCambiar = onBuscar,
                modifier = Modifier.padding(top = Espaciado.Elementos)
            )
        }

        item(key = "filtros") {
            FiltrosCategoria(
                seleccion = categoria,
                total = vista.total,
                conteo = vista.conteoPorCategoria,
                onFiltrar = onFiltrar
            )
        }

        if (vista.grupos.isEmpty()) {
            item(key = "vacio") {
                Text(
                    text = if (vista.hayFiltro) "Ningún sensor coincide con la búsqueda."
                    else "Android no informa sensores en este dispositivo.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp)
                )
            }
        }

        vista.grupos.forEach { (cat, lista) ->
            item(key = "titulo_${cat.name}") {
                Box(Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                    TituloSeccion("${cat.titulo} · ${lista.size}")
                }
            }
            itemsIndexed(lista, key = { _, e -> e.info.clave }) { indice, elemento ->
                FilaSensor(
                    elemento = elemento,
                    forma = formaEnGrupo(indice, lista.size),
                    mostrarDivisor = indice < lista.lastIndex,
                    onClick = onAbrir?.let { abrir -> { abrir(elemento.info.clave) } }
                )
            }
        }

        if (vista.tiposNoDetectados.isNotEmpty() && busqueda.isBlank()) {
            item(key = "no_detectados") {
                Box(Modifier.padding(top = Espaciado.Elementos)) {
                    SeccionExpandible(
                        titulo = "Habituales no detectados · ${vista.tiposNoDetectados.size}",
                        expandida = noDetectadosAbierto,
                        onCambiar = { noDetectadosAbierto = !noDetectadosAbierto }
                    ) {
                        ListaNoDetectados(vista.tiposNoDetectados)
                    }
                }
            }
        }
    }
}

@Composable
private fun CampoBusqueda(
    texto: String,
    onCambiar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val teclado = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = texto,
        onValueChange = onCambiar,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(RadioGrupo),
        placeholder = { Text("Buscar por nombre, tipo o fabricante") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (texto.isNotEmpty()) {
                IconButton(onClick = { onCambiar("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { teclado?.hide() })
    )
}

@Composable
private fun FiltrosCategoria(
    seleccion: CategoriaSensor?,
    total: Int,
    conteo: Map<CategoriaSensor, Int>,
    onFiltrar: (CategoriaSensor?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = seleccion == null,
            onClick = { onFiltrar(null) },
            label = { Text("Todos · $total") }
        )
        CategoriaSensor.entries.forEach { cat ->
            val n = conteo[cat] ?: 0
            if (n > 0) {
                FilterChip(
                    selected = seleccion == cat,
                    onClick = { onFiltrar(if (seleccion == cat) null else cat) },
                    label = { Text("${cat.titulo} · $n") }
                )
            }
        }
    }
}

@Composable
private fun FilaSensor(
    elemento: ElementoSensor,
    forma: Shape,
    mostrarDivisor: Boolean,
    onClick: (() -> Unit)?
) {
    val colores = MaterialTheme.colorScheme
    val info = elemento.info
    val contenido: @Composable () -> Unit = {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Espaciado.InternoTarjeta, end = 8.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = iconoSensor(info.tipo),
                    contentDescription = null,
                    tint = colores.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(Espaciado.InternoTarjeta))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = info.nombre,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colores.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOfNotNull(elemento.nombreTipo, info.fabricante, elemento.categoria.titulo)
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = colores.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                EtiquetaEstado(
                    texto = "Detectado",
                    contenedor = colores.primaryContainer,
                    contenido = colores.onPrimaryContainer
                )
                if (onClick != null) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = colores.onSurfaceVariant
                    )
                }
            }
            if (mostrarDivisor) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 54.dp),
                    color = colores.outlineVariant
                )
            }
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = forma,
            color = colores.surfaceContainerLow,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
            content = contenido
        )
    } else {
        Surface(
            shape = forma,
            color = colores.surfaceContainerLow,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
            content = contenido
        )
    }
}

@Composable
private fun ListaNoDetectados(tipos: List<Int>) {
    val colores = MaterialTheme.colorScheme
    TarjetaGrupo {
        tipos.forEachIndexed { i, tipo ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Espaciado.InternoTarjeta, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = iconoSensor(tipo),
                    contentDescription = null,
                    tint = colores.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(Espaciado.InternoTarjeta))
                Text(
                    text = CatalogoSensores.describir(tipo)?.nombre ?: "Tipo $tipo",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colores.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "No detectado",
                    style = MaterialTheme.typography.labelMedium,
                    color = colores.outline
                )
            }
            if (i < tipos.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 54.dp),
                    color = colores.outlineVariant
                )
            }
        }
    }
}

/** Esquinas redondeadas solo en los extremos del grupo. */
private fun formaEnGrupo(indice: Int, total: Int): Shape {
    val arriba = if (indice == 0) RadioGrupo else 0.dp
    val abajo = if (indice == total - 1) RadioGrupo else 0.dp
    return RoundedCornerShape(topStart = arriba, topEnd = arriba, bottomStart = abajo, bottomEnd = abajo)
}
