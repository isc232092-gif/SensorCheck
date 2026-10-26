
package com.example.sensorcheck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.sensorcheck.data.device.AndroidDeviceRepository
import com.example.sensorcheck.ui.device.DeviceInfoScreen
import com.example.sensorcheck.ui.theme.SensorCheckTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Obtener información real del teléfono
        val repository = AndroidDeviceRepository()
        val dispositivo = repository.obtenerInformacion()

        // Mostrar la información con Jetpack Compose
        setContent {

            SensorCheckTheme {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    DeviceInfoScreen(
                        info = dispositivo
                    )
                }
            }
        }
    }
}
