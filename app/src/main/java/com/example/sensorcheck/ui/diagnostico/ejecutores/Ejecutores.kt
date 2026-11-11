package com.example.sensorcheck.ui.diagnostico.ejecutores

import android.os.Build
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.sensorcheck.domain.diagnostico.CatalogoPruebas
import com.example.sensorcheck.domain.diagnostico.DefinicionPrueba
import com.example.sensorcheck.domain.diagnostico.IdPrueba
import com.example.sensorcheck.domain.diagnostico.ResultadoEvaluado
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.sensorcheck.domain.model.OrientacionCamara
import com.example.sensorcheck.domain.repository.CameraRepository
import com.example.sensorcheck.domain.repository.SensorRepository
import com.example.sensorcheck.domain.sensors.CatalogoSensores

/**
 * Lo que un ejecutor necesita para correr una prueba. [terminar] solo
 * tiene efecto la primera vez, para no registrar resultados duplicados.
 */
class ContextoEjecucion(
    val definicion: DefinicionPrueba,
    val sensores: SensorRepository,
    val camaras: CameraRepository,
    val contactosDeclarados: Int,
    private val alTerminar: (ResultadoEvaluado) -> Unit
) {
    private var terminado = false

    fun terminar(resultado: ResultadoEvaluado) {
        if (terminado) return
        terminado = true
        alTerminar(resultado)
    }

    /** Cancelar nunca cuenta como aprobado. */
    fun cancelar() = terminar(
        CatalogoPruebas.noEjecutada(definicion.id, "Cancelada por el usuario durante la ejecución")
    )
}

/** Estrategia de ejecución de una prueba (GoF: Strategy). */
typealias EjecutorPrueba = @Composable (ContextoEjecucion) -> Unit

/** Registro de estrategias por prueba. null = sin ejecutor (no se ofrece como ejecutable). */
object Ejecutores {
    fun para(id: IdPrueba): EjecutorPrueba? = when (id) {
        IdPrueba.TACTIL -> { ctx -> PruebaTactil(ctx) }
        IdPrueba.MULTITACTIL -> { ctx -> PruebaMultitactil(ctx) }
        IdPrueba.COLORES -> { ctx -> PruebaColores(ctx) }
        IdPrueba.PIXELES -> { ctx -> PruebaPixeles(ctx) }
        IdPrueba.ACELEROMETRO -> { ctx -> PruebaSensorGuiada(ctx, CatalogoSensores.TYPE_ACCELEROMETER) }
        IdPrueba.GIROSCOPIO -> { ctx -> PruebaSensorGuiada(ctx, CatalogoSensores.TYPE_GYROSCOPE) }
        IdPrueba.MAGNETOMETRO -> { ctx -> PruebaSensorGuiada(ctx, CatalogoSensores.TYPE_MAGNETIC_FIELD) }
        IdPrueba.PROXIMIDAD -> { ctx -> PruebaSensorGuiada(ctx, CatalogoSensores.TYPE_PROXIMITY) }
        IdPrueba.LUZ -> { ctx -> PruebaSensorGuiada(ctx, CatalogoSensores.TYPE_LIGHT) }
        IdPrueba.CAMARA_TRASERA -> { ctx -> PruebaCamara(ctx, OrientacionCamara.TRASERA) }
        IdPrueba.CAMARA_FRONTAL -> { ctx -> PruebaCamara(ctx, OrientacionCamara.FRONTAL) }
        IdPrueba.FLASH -> { ctx -> PruebaFlash(ctx) }
        IdPrueba.ALTAVOZ -> { ctx -> PruebaAltavoz(ctx) }
        IdPrueba.MICROFONO -> { ctx -> PruebaMicrofono(ctx) }
        IdPrueba.VIBRACION -> { ctx -> PruebaVibracion(ctx) }
        IdPrueba.WIFI -> { ctx -> PruebaWifi(ctx) }
        IdPrueba.BLUETOOTH -> { ctx -> PruebaBluetooth(ctx) }
        IdPrueba.GPS -> { ctx -> PruebaGps(ctx) }
        IdPrueba.BATERIA -> { ctx -> PruebaBateria(ctx) }
        IdPrueba.ALMACENAMIENTO -> { ctx -> PruebaAlmacenamiento(ctx) }
        IdPrueba.RAM -> { ctx -> PruebaRam(ctx) }
        IdPrueba.BOTONES -> { ctx -> PruebaBotones(ctx) }
    }
}

/** Contenido desplazable común para pruebas no inmersivas. */
@Composable
fun ColumnaPrueba(ctx: ContextoEjecucion, contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(ctx.definicion.nombre, style = MaterialTheme.typography.headlineSmall)
        contenido()
    }
}

/** Respuesta de conteo con botones grandes; 0 = «No percibí nada». */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectorConteo(pregunta: String, maximo: Int = 6, onRespuesta: (Int) -> Unit, onRepetirEstimulo: () -> Unit) {
    Text(pregunta, style = MaterialTheme.typography.titleLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..maximo).forEach { n ->
            FilledTonalButton(onClick = { onRespuesta(n) }, modifier = Modifier.size(64.dp)) {
                Text("$n", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
    OutlinedButton(onClick = { onRespuesta(0) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
        Text("No percibí nada")
    }
    TextButton(onClick = onRepetirEstimulo, modifier = Modifier.fillMaxWidth()) { Text("Repetir") }
}

/**
 * Ventana a pantalla completa para ejecutar una prueba. Tapa la navegación
 * inferior y el gesto Atrás abre una confirmación en lugar de salir.
 */
@Composable
fun MarcoPrueba(
    onCancelar: () -> Unit,
    inmersiva: Boolean = false,
    brilloMaximo: Boolean = false,
    fondo: Color = MaterialTheme.colorScheme.surface,
    opcionFallo: String? = null,
    onFallo: () -> Unit = {},
    contenido: @Composable BoxScope.() -> Unit
) {
    var confirmar by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { confirmar = true },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false
        )
    ) {
        val ventana = (LocalView.current.parent as? DialogWindowProvider)?.window
        DisposableEffect(ventana, inmersiva, brilloMaximo) {
            ventana?.let { w ->
                w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                w.attributes = w.attributes.apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
                    // Solo afecta a esta ventana: se restaura al cerrarla
                    screenBrightness = if (brilloMaximo) 1f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
                val control = WindowCompat.getInsetsController(w, w.decorView)
                if (inmersiva) {
                    control.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    control.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    control.show(WindowInsetsCompat.Type.systemBars())
                }
            }
            onDispose { }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = fondo) {
            Box(Modifier.fillMaxSize(), content = contenido)
        }

        if (confirmar) {
            AlertDialog(
                onDismissRequest = { confirmar = false },
                title = { Text("¿Salir de la prueba?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Si la cancelas se registrará como «No ejecutado».")
                        if (opcionFallo != null) {
                            OutlinedButton(
                                onClick = { confirmar = false; onFallo() },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(opcionFallo) }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { confirmar = false }) { Text("Seguir con la prueba") } },
                dismissButton = {
                    TextButton(onClick = { confirmar = false; onCancelar() }) { Text("Cancelar prueba") }
                }
            )
        }
    }
}
