
package com.example.sensorcheck
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                SensorCheckApp()
            }
        }
    }
}

enum class Modulo(val titulo: String) {
    RESUMEN("Resumen"),
    HARDWARE("Hardware"),
    SISTEMA("Sistema"),
    SENSORES("Sensores"),
    DIAGNOSTICO("Diagnóstico")
}

@Composable
fun SensorCheckApp() {
    var modulo by remember {
        mutableStateOf(Modulo.RESUMEN)
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(
                    text = "SensorCheck",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                )
            }
        },
        bottomBar = {
            NavigationBar {
                Modulo.entries.forEach { item ->
                    NavigationBarItem(
                        selected = modulo == item,
                        onClick = { modulo = item },
                        icon = {
                            Text(
                                when (item) {
                                    Modulo.RESUMEN -> "⌂"
                                    Modulo.HARDWARE -> "▣"
                                    Modulo.SISTEMA -> "⚙"
                                    Modulo.SENSORES -> "◉"
                                    Modulo.DIAGNOSTICO -> "✓"
                                }
                            )
                        },
                        label = { Text(item.titulo) },
                        alwaysShowLabel = false
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (modulo) {
                Modulo.RESUMEN -> PantallaResumen()
                Modulo.HARDWARE -> PantallaHardware()
                Modulo.SISTEMA -> PantallaSistema()
                Modulo.SENSORES -> PantallaSensores()
                Modulo.DIAGNOSTICO -> PantallaDiagnostico()
            }
        }
    }
}

@Composable
fun TarjetaDato(titulo: String, valor: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = titulo,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

fun consultarBateria(context: Context): String {
    val intent = context.registerReceiver(
        null,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    )

    val nivel = intent?.getIntExtra(
        BatteryManager.EXTRA_LEVEL, -1
    ) ?: -1

    val escala = intent?.getIntExtra(
        BatteryManager.EXTRA_SCALE, -1
    ) ?: -1

    return if (nivel >= 0 && escala > 0) {
        "${nivel * 100 / escala}%"
    } else {
        "No disponible"
    }
}

@Composable
fun PantallaResumen() {
    val context = LocalContext.current

    Text(
        "Resumen del dispositivo",
        style = MaterialTheme.typography.headlineSmall
    )

    TarjetaDato(
        "Dispositivo",
        "${Build.MANUFACTURER} ${Build.MODEL}"
    )

    TarjetaDato(
        "Sistema operativo",
        "Android ${Build.VERSION.RELEASE}"
    )

    TarjetaDato(
        "Nivel de batería",
        consultarBateria(context)
    )

    TarjetaDato(
        "Diagnóstico general",
        "Pendiente de evaluación"
    )
}

@Composable
fun PantallaHardware() {
    Text(
        "Hardware del dispositivo",
        style = MaterialTheme.typography.headlineSmall
    )

    TarjetaDato("Fabricante", Build.MANUFACTURER)
    TarjetaDato("Modelo", Build.MODEL)
    TarjetaDato("Placa", Build.BOARD)
    TarjetaDato("Hardware", Build.HARDWARE)

    TarjetaDato(
        "Arquitecturas de CPU",
        Build.SUPPORTED_ABIS.joinToString(", ")
    )
}

@Composable
fun PantallaSistema() {
    Text(
        "Información del sistema",
        style = MaterialTheme.typography.headlineSmall
    )

    TarjetaDato(
        "Versión de Android",
        Build.VERSION.RELEASE
    )

    TarjetaDato(
        "Nivel de API",
        Build.VERSION.SDK_INT.toString()
    )

    TarjetaDato(
        "Nombre del dispositivo",
        Build.DEVICE
    )

    TarjetaDato(
        "Producto",
        Build.PRODUCT
    )
}

@Composable
fun PantallaSensores() {
    val context = LocalContext.current

    val sensores = remember {
        val manager = context.getSystemService(
            Context.SENSOR_SERVICE
        ) as SensorManager

        manager.getSensorList(Sensor.TYPE_ALL)
            .toList()
    }

    Text(
        "Sensores disponibles",
        style = MaterialTheme.typography.headlineSmall
    )

    TarjetaDato(
        "Sensores detectados",
        sensores.size.toString()
    )

    sensores.forEach { sensor ->
        TarjetaDato(
            sensor.name,
            "Fabricante: ${sensor.vendor}\n" +
                    "Tipo: ${sensor.stringType}\n" +
                    "Resolución: ${sensor.resolution}\n" +
                    "Rango máximo: ${sensor.maximumRange}"
        )
    }
}

@Composable
fun PantallaDiagnostico() {
    Text(
        "Diagnóstico funcional",
        style = MaterialTheme.typography.headlineSmall
    )

    TarjetaDato(
        "Diagnóstico completo",
        "Próximamente se implementarán " +
                "las pruebas automáticas y guiadas."
    )

    TarjetaDato(
        "Pruebas individuales",
        "Pantalla, acelerómetro, proximidad, " +
                "vibración, audio y otros componentes."
    )

    TarjetaDato(
        "Estado actual",
        "Sin diagnósticos realizados"
    )
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    showSystemUi = true,
    name = "SensorCheck - Pantalla Principal"
)
@Composable
fun SensorCheckPreview() {
    MaterialTheme {
        SensorCheckApp()
    }
}
