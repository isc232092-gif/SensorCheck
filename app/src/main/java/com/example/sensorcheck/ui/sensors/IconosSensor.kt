package com.example.sensorcheck.ui.sensors

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.sensorcheck.domain.sensors.CatalogoSensores as C

/** Icono Material para cada tipo de sensor. */
fun iconoSensor(tipo: Int): ImageVector = when (tipo) {
    C.TYPE_ACCELEROMETER, C.TYPE_ACCELEROMETER_UNCALIBRATED,
    C.TYPE_ACCELEROMETER_LIMITED_AXES, C.TYPE_ACCELEROMETER_LIMITED_AXES_UNCALIBRATED,
    C.TYPE_LINEAR_ACCELERATION -> Icons.Outlined.Speed

    C.TYPE_GYROSCOPE, C.TYPE_GYROSCOPE_UNCALIBRATED,
    C.TYPE_GYROSCOPE_LIMITED_AXES, C.TYPE_GYROSCOPE_LIMITED_AXES_UNCALIBRATED -> Icons.Outlined._3dRotation

    C.TYPE_GRAVITY -> Icons.Outlined.ArrowDownward
    C.TYPE_ROTATION_VECTOR, C.TYPE_GAME_ROTATION_VECTOR, C.TYPE_ORIENTATION -> Icons.Outlined.ScreenRotation
    C.TYPE_SIGNIFICANT_MOTION, C.TYPE_MOTION_DETECT, C.TYPE_STATIONARY_DETECT -> Icons.AutoMirrored.Outlined.DirectionsRun
    C.TYPE_STEP_DETECTOR, C.TYPE_STEP_COUNTER -> Icons.AutoMirrored.Outlined.DirectionsWalk

    C.TYPE_LIGHT -> Icons.Outlined.LightMode
    C.TYPE_PROXIMITY -> Icons.Outlined.Sensors
    C.TYPE_PRESSURE -> Icons.Outlined.Compress
    C.TYPE_AMBIENT_TEMPERATURE, C.TYPE_TEMPERATURE -> Icons.Outlined.Thermostat
    C.TYPE_RELATIVE_HUMIDITY -> Icons.Outlined.WaterDrop

    C.TYPE_MAGNETIC_FIELD, C.TYPE_MAGNETIC_FIELD_UNCALIBRATED,
    C.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> Icons.Outlined.Explore
    C.TYPE_HEADING -> Icons.Outlined.Navigation
    C.TYPE_HINGE_ANGLE -> Icons.Outlined.Straighten

    C.TYPE_HEART_RATE, C.TYPE_HEART_BEAT -> Icons.Outlined.MonitorHeart
    else -> Icons.Outlined.Sensors
}
