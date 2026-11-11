package com.example.sensorcheck.ui.diagnostico.ejecutores

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.sensorcheck.domain.diagnostico.CatalogoPruebas
import com.example.sensorcheck.domain.diagnostico.EvaluadorAcelerometro
import com.example.sensorcheck.domain.diagnostico.EvaluadorGiroscopio
import com.example.sensorcheck.domain.diagnostico.EvaluadorLuz
import com.example.sensorcheck.domain.diagnostico.EvaluadorMagnetometro
import com.example.sensorcheck.domain.diagnostico.EvaluadorProximidad
import com.example.sensorcheck.domain.diagnostico.EvaluadorSensor
import com.example.sensorcheck.domain.diagnostico.FinPrueba
import com.example.sensorcheck.domain.diagnostico.ResultadoEvaluado
import com.example.sensorcheck.domain.diagnostico.ResultadoPrueba
import com.example.sensorcheck.domain.model.EventoSensor
import com.example.sensorcheck.domain.model.RitmoMuestreo
import com.example.sensorcheck.domain.model.SensorInfo
import com.example.sensorcheck.domain.sensors.CatalogoSensores
import com.example.sensorcheck.domain.sensors.FormatoSensor
import com.example.sensorcheck.ui.components.Espaciado
import com.example.sensorcheck.ui.components.TarjetaGrupo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Tiempo máximo de una prueba guiada de sensor. */
private const val LIMITE_S = 45

/** Estrategia de evaluación según el tipo de sensor. */
private fun crearEvaluador(tipo: Int, sensor: SensorInfo): EvaluadorSensor? = when (tipo) {
    CatalogoSensores.TYPE_ACCELEROMETER -> EvaluadorAcelerometro()
    CatalogoSensores.TYPE_GYROSCOPE -> EvaluadorGiroscopio()
    CatalogoSensores.TYPE_MAGNETIC_FIELD -> EvaluadorMagnetometro()
    CatalogoSensores.TYPE_PROXIMITY -> EvaluadorProximidad(sensor.rangoMaximo)
    CatalogoSensores.TYPE_LIGHT -> EvaluadorLuz()
    else -> null
}

/**
 * Prueba guiada de un sensor: reutiliza SensorRepository.monitorear, que
 * registra un único listener y lo libera al cancelar la recolección
 * (al terminar, cancelar o pasar la app a segundo plano).
 */
@Composable
fun PruebaSensorGuiada(ctx: ContextoEjecucion, tipo: Int) {
    var sensor by remember { mutableStateOf<SensorInfo?>(null) }
    var evaluador by remember { mutableStateOf<EvaluadorSensor?>(null) }
    var valores by remember { mutableStateOf<List<Float>>(emptyList()) }
    var restante by remember { mutableIntStateOf(LIMITE_S) }
    val ciclo = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(tipo) {
        val encontrado = withContext(Dispatchers.Default) {
            // Se prefiere la variante que no despierta el dispositivo, como getDefaultSensor()
            ctx.sensores.obtenerSensores().filter { it.tipo == tipo }.minByOrNull { if (it.despiertaDispositivo) 1 else 0 }
        }
        val ev = encontrado?.let { crearEvaluador(tipo, it) }
        if (encontrado == null || ev == null) {
            ctx.terminar(
                ResultadoEvaluado(
                    ResultadoPrueba.NO_COMPATIBLE,
                    listOf("Android no expone este sensor en el momento de la prueba"),
                    CatalogoPruebas.recomendacion(ctx.definicion.id, ResultadoPrueba.NO_COMPATIBLE)
                )
            )
            return@LaunchedEffect
        }
        sensor = encontrado
        evaluador = ev
    }

    val s = sensor
    val ev = evaluador

    if (s != null && ev != null) {
        LaunchedEffect(s.clave) {
            ciclo.repeatOnLifecycle(Lifecycle.State.STARTED) {
                var ultimaPantalla = 0L
                ctx.sensores.monitorear(s.clave, RitmoMuestreo.JUEGO.periodoUs).collect { evento ->
                    when (evento) {
                        is EventoSensor.Lectura -> {
                            ev.registrar(evento.lectura.valores, evento.lectura.marcaTiempoNs)
                            val ahora = System.currentTimeMillis()
                            if (ahora - ultimaPantalla >= 100) { // ~10 actualizaciones por segundo
                                ultimaPantalla = ahora
                                valores = evento.lectura.valores
                            }
                            if (ev.completo) ctx.terminar(ev.evaluar(FinPrueba.COMPLETADA))
                        }
                        is EventoSensor.Error -> ctx.terminar(
                            ResultadoEvaluado(
                                ResultadoPrueba.INDETERMINADO,
                                listOf("Android no permitió leer el sensor: ${evento.mensaje}"),
                                CatalogoPruebas.recomendacion(ctx.definicion.id, ResultadoPrueba.INDETERMINADO)
                            )
                        )
                        else -> Unit
                    }
                }
            }
        }
        LaunchedEffect(s.clave) {
            // El tiempo solo corre con la app en primer plano
            ciclo.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (restante > 0) {
                    delay(1_000)
                    restante--
                }
                ctx.terminar(ev.evaluar(FinPrueba.TIEMPO_AGOTADO))
            }
        }
    }

    MarcoPrueba(
        onCancelar = ctx::cancelar,
        opcionFallo = "Seguí las instrucciones y no responde",
        onFallo = { evaluador?.let { ctx.terminar(it.evaluar(FinPrueba.USUARIO_REPORTA_FALLO)) } }
    ) {
        if (s == null || ev == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
            return@MarcoPrueba
        }
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(Espaciado.InternoTarjeta),
            verticalArrangement = Arrangement.spacedBy(Espaciado.Elementos)
        ) {
            Text(ctx.definicion.nombre, style = MaterialTheme.typography.headlineSmall)
            Text(s.nombre, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            ctx.definicion.instrucciones.forEachIndexed { i, paso ->
                Text("${i + 1}. $paso", style = MaterialTheme.typography.bodyLarge)
            }

            TarjetaGrupo {
                Column(Modifier.padding(Espaciado.InternoTarjeta), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Lectura actual", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (valores.isEmpty()) {
                        Text("Esperando lecturas del sensor…", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        FormatoSensor.valores(CatalogoSensores.describir(tipo), valores).take(3).forEach {
                            Row {
                                Text(it.nombre, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Text(FormatoSensor.conUnidad(it.valor, it.unidad), style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }

            TarjetaGrupo {
                Column(Modifier.padding(Espaciado.InternoTarjeta), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Criterios", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ev.criterios().forEach { c ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (c.cumplido) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = if (c.cumplido) "Cumplido" else "Pendiente",
                                tint = if (c.cumplido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(c.texto, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Text(
                "Tiempo restante: $restante s",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = { ctx.terminar(ev.evaluar(FinPrueba.USUARIO_REPORTA_FALLO)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) { Text("Seguí las instrucciones y no responde") }
            TextButton(onClick = ctx::cancelar, modifier = Modifier.fillMaxWidth()) { Text("Cancelar prueba") }
        }
    }
}
