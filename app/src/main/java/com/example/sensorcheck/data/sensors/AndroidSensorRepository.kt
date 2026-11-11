package com.example.sensorcheck.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import android.os.Build
import android.os.SystemClock
import com.example.sensorcheck.domain.model.EventoSensor
import com.example.sensorcheck.domain.model.ModoReporte
import com.example.sensorcheck.domain.model.SensorInfo
import com.example.sensorcheck.domain.model.SensorReading
import com.example.sensorcheck.domain.repository.SensorRepository
import com.example.sensorcheck.domain.sensors.CatalogoSensores
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Adapta SensorManager al dominio (GoF: Adapter).
 * Solo registra el sensor que se pide y lo libera al cancelar el Flow.
 */
class AndroidSensorRepository(
    context: Context
) : SensorRepository {

    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    /** Relación clave -> Sensor de Android de la última consulta. */
    @Volatile
    private var porClave: Map<String, Sensor> = emptyMap()

    override fun obtenerSensores(): List<SensorInfo> {
        val lista = sensorManager?.getSensorList(Sensor.TYPE_ALL).orEmpty()
        val usadas = HashMap<String, Int>()
        val mapa = LinkedHashMap<String, Sensor>()

        val sensores = lista.map { sensor ->
            val base = "${sensor.type}|${sensor.name}|${sensor.vendor}|${sensor.version}"
            val repeticion = usadas.merge(base, 1, Int::plus) ?: 1
            val clave = if (repeticion == 1) base else "$base#$repeticion"
            mapa[clave] = sensor
            convertir(sensor, clave)
        }
        porClave = mapa
        return sensores
    }

    override fun monitorear(clave: String, periodoUs: Int): Flow<EventoSensor> = callbackFlow {
        val manager = sensorManager
        val sensor = porClave[clave] ?: run {
            obtenerSensores()
            porClave[clave]
        }

        if (manager == null || sensor == null) {
            trySend(EventoSensor.Error("El sensor ya no está disponible."))
            close()
            awaitClose { }
            return@callbackFlow
        }

        if (sensor.reportingMode == Sensor.REPORTING_MODE_ONE_SHOT) {
            // Sensores de activación única: TriggerEventListener
            val listener = object : TriggerEventListener() {
                override fun onTrigger(event: TriggerEvent) {
                    trySend(EventoSensor.Disparo(lectura(event.values, event.timestamp, null)))
                    // Android desactiva el sensor tras el disparo
                    channel.close()
                }
            }
            val solicitado = try {
                manager.requestTriggerSensor(listener, sensor)
            } catch (e: SecurityException) {
                trySend(EventoSensor.Error("Android denegó el acceso al sensor: ${e.message}"))
                false
            } catch (e: IllegalArgumentException) {
                trySend(EventoSensor.Error("El sensor no admite activación única: ${e.message}"))
                false
            }
            if (!solicitado) {
                trySend(EventoSensor.Error("No se pudo activar el sensor."))
                close()
            }
            awaitClose { manager.cancelTriggerSensor(listener, sensor) }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    trySend(EventoSensor.Lectura(lectura(event.values, event.timestamp, event.accuracy)))
                }

                override fun onAccuracyChanged(s: Sensor, accuracy: Int) {
                    trySend(EventoSensor.CambioPrecision(accuracy))
                }
            }
            val registrado = try {
                manager.registerListener(listener, sensor, periodoUs)
            } catch (e: SecurityException) {
                trySend(EventoSensor.Error("Android denegó el acceso al sensor: ${e.message}"))
                false
            }
            if (!registrado) {
                trySend(EventoSensor.Error("No se pudo registrar el sensor (posible restricción del fabricante o permiso)."))
                close()
            }
            awaitClose { manager.unregisterListener(listener, sensor) }
        }
    }

    private fun convertir(sensor: Sensor, clave: String): SensorInfo {
        val descriptor = CatalogoSensores.describir(sensor.type)
        val permiso = descriptor?.permiso?.takeIf { Build.VERSION.SDK_INT >= it.apiMinima }?.permiso

        return SensorInfo(
            clave = clave,
            nombre = sensor.name,
            tipo = sensor.type,
            stringType = sensor.stringType?.takeIf { it.isNotBlank() },
            fabricante = sensor.vendor?.takeIf { it.isNotBlank() },
            version = sensor.version,
            resolucion = sensor.resolution,
            rangoMaximo = sensor.maximumRange,
            consumoMa = sensor.power,
            retardoMinimoUs = sensor.minDelay,
            retardoMaximoUs = sensor.maxDelay,
            modoReporte = when (sensor.reportingMode) {
                Sensor.REPORTING_MODE_CONTINUOUS -> ModoReporte.CONTINUO
                Sensor.REPORTING_MODE_ON_CHANGE -> ModoReporte.AL_CAMBIO
                Sensor.REPORTING_MODE_ONE_SHOT -> ModoReporte.UNICO
                Sensor.REPORTING_MODE_SPECIAL_TRIGGER -> ModoReporte.ESPECIAL
                else -> ModoReporte.DESCONOCIDO
            },
            despiertaDispositivo = sensor.isWakeUpSensor,
            fifoMaximo = sensor.fifoMaxEventCount,
            fifoReservado = sensor.fifoReservedEventCount,
            dinamico = sensor.isDynamicSensor,
            idSensor = sensor.id.takeIf { it > 0 },
            permisoRequerido = permiso
        )
    }

    private fun lectura(valores: FloatArray, marcaNs: Long, precision: Int?): SensorReading {
        // La marca del evento usa la base de elapsedRealtimeNanos
        val ahoraMs = System.currentTimeMillis()
        val atrasoMs = (SystemClock.elapsedRealtimeNanos() - marcaNs) / 1_000_000
        val hora = if (atrasoMs in 0..86_400_000) ahoraMs - atrasoMs else ahoraMs
        return SensorReading(
            valores = valores.toList(),
            marcaTiempoNs = marcaNs,
            horaEpochMs = hora,
            precision = precision
        )
    }
}
