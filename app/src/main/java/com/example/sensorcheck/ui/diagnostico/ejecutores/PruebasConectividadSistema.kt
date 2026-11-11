package com.example.sensorcheck.ui.diagnostico.ejecutores

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.sensorcheck.data.diagnostico.EventoBle
import com.example.sensorcheck.data.diagnostico.EventoGps
import com.example.sensorcheck.data.diagnostico.bluetoothActivado
import com.example.sensorcheck.data.diagnostico.escanearBle
import com.example.sensorcheck.data.diagnostico.escucharGps
import com.example.sensorcheck.data.diagnostico.leerBateria
import com.example.sensorcheck.data.diagnostico.leerWifi
import com.example.sensorcheck.data.diagnostico.probarAlmacenamiento
import com.example.sensorcheck.data.diagnostico.probarRam
import com.example.sensorcheck.data.diagnostico.ubicacionActivada
import com.example.sensorcheck.domain.diagnostico.EvaluadorAlmacenamiento
import com.example.sensorcheck.domain.diagnostico.EvaluadorBateria
import com.example.sensorcheck.domain.diagnostico.EvaluadorBotones
import com.example.sensorcheck.domain.diagnostico.EvaluadorConectividad
import com.example.sensorcheck.domain.diagnostico.EvaluadorRam
import com.example.sensorcheck.domain.diagnostico.LecturaBateria
import kotlinx.coroutines.delay

/** Cuenta atrás que solo avanza con la app en primer plano. */
@Composable
private fun cuentaAtras(segundos: Int, alTerminar: () -> Unit): Int {
    var restante by remember { mutableIntStateOf(segundos) }
    val ciclo = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        ciclo.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (restante > 0) {
                delay(1_000)
                restante--
            }
            alTerminar()
        }
    }
    return restante
}

@Composable
private fun FilaEstado(texto: String, cumplido: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (cumplido) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = if (cumplido) "Sí" else "No",
            tint = if (cumplido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(texto, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun abrirAjustes(contexto: Context, accion: String) =
    runCatching { contexto.startActivity(Intent(accion).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }

// ---------------- Wi-Fi ----------------

@Composable
fun PruebaWifi(ctx: ContextoEjecucion) {
    val contexto = LocalContext.current
    var estado by remember { mutableStateOf(leerWifi(contexto)) }
    fun evaluar(reporta: Boolean) = ctx.terminar(
        EvaluadorConectividad.wifi(estado.activado, estado.conectado, estado.validado, estado.rssiDbm, reporta)
    )
    LaunchedEffect(Unit) {
        while (true) {
            estado = leerWifi(contexto)
            if (estado.conectado) {
                delay(1_500) // da tiempo a que Android valide el acceso a internet
                estado = leerWifi(contexto)
                evaluar(false)
            }
            delay(1_000)
        }
    }
    val restante = cuentaAtras(90) { evaluar(false) }

    MarcoPrueba(onCancelar = ctx::cancelar, opcionFallo = "No logro conectarme", onFallo = { evaluar(true) }) {
        ColumnaPrueba(ctx) {
            FilaEstado("Wi-Fi activado", estado.activado)
            FilaEstado("Conectado a una red Wi-Fi", estado.conectado)
            Button(
                onClick = {
                    abrirAjustes(
                        contexto,
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Settings.Panel.ACTION_WIFI else Settings.ACTION_WIFI_SETTINGS
                    )
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) { Text("Abrir ajustes de Wi-Fi") }
            Text("Tiempo restante: $restante s", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = { evaluar(true) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("No logro conectarme a una red conocida")
            }
        }
    }
}

// ---------------- Bluetooth ----------------

@Composable
fun PruebaBluetooth(ctx: ContextoEjecucion) {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current.lifecycle
    var activado by remember { mutableStateOf(bluetoothActivado(contexto) == true) }
    var encontrados by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    fun evaluar(reporta: Boolean) = ctx.terminar(EvaluadorConectividad.bluetooth(activado, encontrados, error, reporta))

    LaunchedEffect(Unit) {
        while (!activado) {
            delay(1_000)
            activado = bluetoothActivado(contexto) == true
        }
    }
    if (activado) {
        LaunchedEffect(Unit) {
            ciclo.repeatOnLifecycle(Lifecycle.State.STARTED) {
                escanearBle(contexto).collect { e ->
                    when (e) {
                        is EventoBle.Encontrados -> {
                            encontrados = e.total
                            evaluar(false)
                        }
                        is EventoBle.Fallo -> error = e.mensaje
                    }
                }
            }
        }
    }
    val restante = cuentaAtras(45) { evaluar(false) }

    MarcoPrueba(onCancelar = ctx::cancelar, opcionFallo = "Hay dispositivos cerca y no los encuentra", onFallo = { evaluar(true) }) {
        ColumnaPrueba(ctx) {
            FilaEstado("Bluetooth activado", activado)
            if (!activado) {
                Button(
                    onClick = { abrirAjustes(contexto, Settings.ACTION_BLUETOOTH_SETTINGS) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) { Text("Abrir ajustes de Bluetooth") }
            } else {
                Text("Buscando dispositivos Bluetooth cercanos…", style = MaterialTheme.typography.bodyLarge)
                LinearProgressIndicator(Modifier.fillMaxWidth())
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !ubicacionActivada(contexto)) {
                    Text(
                        "En esta versión de Android la ubicación debe estar activada para encontrar dispositivos.",
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Text("Tiempo restante: $restante s", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = { evaluar(true) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Hay dispositivos cerca y no los encuentra")
            }
        }
    }
}

// ---------------- GPS ----------------

@Composable
fun PruebaGps(ctx: ContextoEjecucion) {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current.lifecycle
    var activado by remember { mutableStateOf(ubicacionActivada(contexto)) }
    var vistos by remember { mutableIntStateOf(0) }
    var enUso by remember { mutableIntStateOf(0) }
    fun evaluar(reporta: Boolean, posicion: Boolean = false, precision: Float? = null) =
        ctx.terminar(EvaluadorConectividad.gps(activado, vistos, enUso, posicion, precision, reporta))

    LaunchedEffect(Unit) {
        while (!activado) {
            delay(1_000)
            activado = ubicacionActivada(contexto)
        }
    }
    if (activado) {
        LaunchedEffect(Unit) {
            ciclo.repeatOnLifecycle(Lifecycle.State.STARTED) {
                escucharGps(contexto).collect { e ->
                    when (e) {
                        is EventoGps.Satelites -> {
                            vistos = maxOf(vistos, e.vistos)
                            enUso = maxOf(enUso, e.enUso)
                        }
                        is EventoGps.Posicion -> evaluar(false, posicion = true, precision = e.precisionM.takeIf { it.isFinite() })
                        is EventoGps.Fallo -> evaluar(false)
                    }
                }
            }
        }
    }
    val restante = cuentaAtras(120) { evaluar(false) }

    MarcoPrueba(onCancelar = ctx::cancelar, opcionFallo = "Estoy al aire libre y no hay señal", onFallo = { evaluar(true) }) {
        ColumnaPrueba(ctx) {
            FilaEstado("Ubicación activada", activado)
            if (!activado) {
                Button(
                    onClick = { abrirAjustes(contexto, Settings.ACTION_LOCATION_SOURCE_SETTINGS) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) { Text("Abrir ajustes de ubicación") }
            } else {
                Text("Satélites visibles: $vistos · en uso: $enUso", style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(
                    "La posición no se guarda; solo se registra su precisión.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("Tiempo restante: $restante s", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = { evaluar(true) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Estoy al aire libre y no hay señal")
            }
        }
    }
}

// ---------------- Batería ----------------

@Composable
fun PruebaBateria(ctx: ContextoEjecucion) {
    val contexto = LocalContext.current
    val filtro = remember { IntentFilter(Intent.ACTION_BATTERY_CHANGED) }
    var lectura by remember {
        mutableStateOf(leerBateria(ContextCompat.registerReceiver(contexto, null, filtro, ContextCompat.RECEIVER_NOT_EXPORTED)))
    }
    val inicial = remember { lectura }
    val yaConectada = remember { inicial?.conectada == true }

    DisposableEffect(Unit) {
        val receptor = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                lectura = leerBateria(i)
            }
        }
        ContextCompat.registerReceiver(contexto, receptor, filtro, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { contexto.unregisterReceiver(receptor) }
    }

    fun evaluar(carga: Boolean?) {
        val l = lectura ?: inicial ?: LecturaBateria(null, EvaluadorBateria.SALUD_DESCONOCIDA, null, null, false)
        val r = EvaluadorBateria.evaluar(l, carga)
        ctx.terminar(if (yaConectada) r.copy(evidencia = r.evidencia + "El cargador ya estaba conectado al iniciar") else r)
    }
    LaunchedEffect(lectura?.conectada) {
        if (lectura?.conectada == true) {
            delay(1_000)
            evaluar(true)
        }
    }

    MarcoPrueba(onCancelar = ctx::cancelar) {
        ColumnaPrueba(ctx) {
            val l = lectura
            if (l == null) {
                Text("Android no entregó el estado de la batería.", color = MaterialTheme.colorScheme.error)
            } else {
                Text("Salud: ${EvaluadorBateria.nombreSalud(l.salud)}", style = MaterialTheme.typography.titleMedium)
                l.nivelPorcentaje?.let { Text("Nivel: $it %") }
                l.temperaturaC?.let { Text("Temperatura: $it °C") }
                EvaluadorBateria.normalizarMv(l.voltajeMv)?.let { Text("Voltaje: ${it / 1000f} V") }
            }
            Text("Conecta ahora el cargador.", style = MaterialTheme.typography.titleLarge)
            LinearProgressIndicator(Modifier.fillMaxWidth())
            OutlinedButton(onClick = { evaluar(false) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Lo conecté y no detecta la carga")
            }
            TextButton(onClick = { evaluar(null) }, modifier = Modifier.fillMaxWidth()) {
                Text("No tengo cargador ahora")
            }
        }
    }
}

// ---------------- Almacenamiento y RAM ----------------

@Composable
fun PruebaAlmacenamiento(ctx: ContextoEjecucion) {
    val contexto = LocalContext.current
    LaunchedEffect(Unit) {
        ctx.terminar(EvaluadorAlmacenamiento.evaluar(probarAlmacenamiento(contexto.cacheDir)))
    }
    PantallaEspera(ctx, "Escribiendo y verificando 32 MB…")
}

@Composable
fun PruebaRam(ctx: ContextoEjecucion) {
    val contexto = LocalContext.current
    LaunchedEffect(Unit) {
        val m = probarRam(contexto)
        ctx.terminar(EvaluadorRam.evaluar(m.mbVerificados, m.errores, m.sinMemoria, m.totalMb, m.disponibleMb))
    }
    PantallaEspera(ctx, "Escribiendo y verificando un bloque de memoria…")
}

@Composable
private fun PantallaEspera(ctx: ContextoEjecucion, texto: String) {
    MarcoPrueba(onCancelar = ctx::cancelar) {
        ColumnaPrueba(ctx) {
            Text(texto, style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

// ---------------- Botones ----------------

@Composable
fun PruebaBotones(ctx: ContextoEjecucion) {
    var subir by remember { mutableIntStateOf(0) }
    var bajar by remember { mutableIntStateOf(0) }
    val foco = remember { FocusRequester() }
    fun evaluar(reporta: Boolean) = ctx.terminar(EvaluadorBotones.evaluar(subir, bajar, reporta))

    LaunchedEffect(subir > 0 && bajar > 0) {
        if (subir > 0 && bajar > 0) {
            delay(600)
            evaluar(false)
        }
    }
    val restante = cuentaAtras(30) { evaluar(false) }

    MarcoPrueba(onCancelar = ctx::cancelar, opcionFallo = "Un botón no responde", onFallo = { evaluar(true) }) {
        Box(
            Modifier
                .fillMaxSize()
                .focusRequester(foco)
                .focusable()
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent e.key == Key.VolumeUp || e.key == Key.VolumeDown
                    when (e.key) {
                        Key.VolumeUp -> { subir++; true } // se consume: el volumen no cambia
                        Key.VolumeDown -> { bajar++; true }
                        else -> false
                    }
                }
        ) {
            ColumnaPrueba(ctx) {
                FilaEstado("Subir volumen (${subir})", subir > 0)
                FilaEstado("Bajar volumen (${bajar})", bajar > 0)
                Text("Tiempo restante: $restante s", style = MaterialTheme.typography.labelLarge)
                OutlinedButton(onClick = { evaluar(true) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text("Pulsé y un botón no responde")
                }
            }
        }
        LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }
    }
}
