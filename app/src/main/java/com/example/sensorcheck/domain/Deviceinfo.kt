
package com.example.sensorcheck.domain.model

data class DeviceInfo(
    val fabricante: String,
    val marca: String,
    val modelo: String,
    val dispositivo: String,
    val versionAndroid: String,
    val nivelApi: Int,
    val placa: String,
    val hardware: String,
    val producto: String,
    val arquitecturasCpu: List<String>
)
