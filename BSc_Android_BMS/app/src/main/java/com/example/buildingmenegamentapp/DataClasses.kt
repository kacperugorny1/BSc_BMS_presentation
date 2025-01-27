package com.example.buildingmenegamentapp

import android.text.BoringLayout
import androidx.compose.runtime.MutableState

data class LocalServer(
    val id: Int,
    var name: MutableState<String>,
    var ip: String,
    var mac: String,
    var online: MutableState<Boolean>
)
data class EndDevice(
    val id: Int,                     // Internal app-specific ID
    val localServerId: Int,          // Reference to LocalServer.id
    val devIdLocal: Int,             // Mirrors the exact ID from the DEVICES table
    var name: MutableState<String>,  // Device name (from DEVICES.Name)
    val mac: String,                 // MAC address (from DEVICES.Mac)
    var online: MutableState<Boolean>
)

data class Gpio(
    val id: Int,                     // Unique ID for the GPIO (from GPIO.GpioID)
    val gpioType: Char,              // 'D' for Digital, 'A' for Analog (from GPIO.Digital)
    val devId: Int,                  // Device ID (from GPIO.DevID)
    var name: MutableState<String>,  // GPIO name (from GPIO.Name)
    var range: Int,                  // Range (from GPIO.EndRange)
    var unit: String,                // Unit of measurement (from GPIO.Unit)
    var startValue: Float,           // Initial value (from GPIO.StartValue)
    var slope: Float,                // Slope for scaling (from GPIO.Slope)
    var visibility: Boolean,
    var input: Boolean,
    var state: MutableState<Int>
)

data class Cond(
    val id: Int,                     // Unique ID for the condition (from COND.CondID)
    var condIdLocal: Int,                     // Unique ID for the condition (from COND.CondID)
    val localServerId: Int,
    var name: String,                // Name of the condition (from COND.Name)
    var lside: String,               // Left-side expression (from COND.Lside)
    var rside: String,               // Right-side expression (from COND.Rside)
    var enabled: Boolean             // Condition enabled/disabled (from COND.Enabled)
)
