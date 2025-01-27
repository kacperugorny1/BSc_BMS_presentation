package com.example.buildingmenegamentapp.userinterface

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.buildingmenegamentapp.CONDS_VIEW
import com.example.buildingmenegamentapp.DEV_VIEW
import com.example.buildingmenegamentapp.HOME_VIEW
import com.example.buildingmenegamentapp.MyViewModel
import com.example.buildingmenegamentapp.SERVER_VIEW
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun EditServer(myViewModel: MyViewModel = viewModel()) {
    val localServer = myViewModel.listOfLocalServers.first { u -> u.id == myViewModel.servInd.intValue }
    var name by remember { mutableStateOf(localServer.name.value) }
    var ip by remember { mutableStateOf(localServer.ip) }
    var mac by remember { mutableStateOf(localServer.mac) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text("Edit Local Server")

        Spacer(modifier = Modifier.height(8.dp))

        Text("ID: ${localServer.id}")
        Text("MAC Address: ${localServer.mac}")

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = ip,
            onValueChange = { ip = it },
            label = { Text("IP Address") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = mac,
            onValueChange = { mac = it },
            label = { Text("MAC Address") },
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = {
                    localServer.name.value = name
                    localServer.ip = ip
                    localServer.mac = mac
                    myViewModel.sqLiteHelper.updateServer(localServer)
                    myViewModel.currentScreen.intValue = SERVER_VIEW
                }
            ) {
                Text("Save")
            }

            Button(
                onClick = {
                    myViewModel.sqLiteHelper.deleteLocalServer(localServer.id)
                    myViewModel.currentScreen.intValue = HOME_VIEW
                    myViewModel.listOfLocalServers.remove(localServer)


                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            ) {
                Text("Delete", color = Color.White)
            }
        }
    }
}


@Composable
fun EditDev(myViewModel: MyViewModel = viewModel()) {
    val endDevice = myViewModel.listOfEndDevices.first { u -> u.id == myViewModel.devInd.intValue }
    val server = myViewModel.listOfLocalServers.first { s -> s.id == myViewModel.servInd.intValue }

    var name by remember { mutableStateOf(endDevice.name.value) }
    var enabled by remember { mutableStateOf(true)}

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text("Edit End Device")

        Spacer(modifier = Modifier.height(8.dp))

        Text("ID: ${endDevice.id}")
        Text("Local Server ID: ${endDevice.localServerId}")
        Text("Device Local ID: ${endDevice.devIdLocal}")
        Text("MAC Address: ${endDevice.mac}")

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = {
                    var message = ""
                    CoroutineScope(Dispatchers.IO).launch {
                        message = "ED:${endDevice.devIdLocal};${name}"
                        endDevice.name.value = name
                        while(!myViewModel.isConnected.value) {
                            myViewModel.mqttHandler.connect(myViewModel)
                            delay(50)
                        }
                        myViewModel.mqttHandler.subscribe(server.mac)
                        myViewModel.mqttHandler.publish(server.mac,message,myViewModel)


                        myViewModel.sqLiteHelper.updateEndDevice(endDevice)
                        myViewModel.currentScreen.intValue = DEV_VIEW
                    }
                }
            ) {
                Text("Save")
            }

            Button(
                onClick = {
                    var message = ""
                    CoroutineScope(Dispatchers.IO).launch{
                        enabled = false

                        message = "RD:${endDevice.devIdLocal}"
                        while(!myViewModel.isConnected.value) {
                            myViewModel.mqttHandler.connect(myViewModel)
                            delay(50)
                        }
                        myViewModel.mqttHandler.subscribe(server.mac)
                        myViewModel.mqttHandler.publish(server.mac,message,myViewModel)
                        myViewModel.sqLiteHelper.deleteEndDevice(endDevice.id)
                        myViewModel.currentScreen.intValue = SERVER_VIEW
                        myViewModel.listOfEndDevices.remove(endDevice)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                enabled = enabled
            ) {
                Text("Delete", color = Color.White)
            }
        }
    }
}


@Composable
fun EditCond(myViewModel: MyViewModel = viewModel()) {
    val server = myViewModel.listOfLocalServers.first { s -> s.id == myViewModel.servInd.intValue }
    val cond = myViewModel.listOfConds.first { u -> u.id == myViewModel.condInd.intValue }
    var name by remember { mutableStateOf(cond.name) }
    var enabled by remember { mutableStateOf(cond.enabled) }
    var enabledBut by remember { mutableStateOf(true)}
    var lside by remember {mutableStateOf(cond.lside)}
    var rside by remember {mutableStateOf(cond.rside)}


    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text("Edit Condition")

        Spacer(modifier = Modifier.height(8.dp))

        Text("ID: ${cond.id}")
        Text("Local Server ID: ${cond.localServerId}")
        Text("Left-Side Expression: ${cond.lside}")
        Text("Right-Side Expression: ${cond.rside}")

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = lside,
            onValueChange = { lside = it },
            label = { Text("lside") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = rside,
            onValueChange = { rside = it },
            label = { Text("rside") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Enabled:")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = enabled,
                onCheckedChange = { enabled = it }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = {
                    var message = ""
                    CoroutineScope(Dispatchers.IO).launch {
                        enabledBut = false
                        message = "EC:${cond.condIdLocal};${name};${lside.replace("\\", "\\\\")};${rside.replace("\\", "\\\\")};${if(enabled)1 else 0}"
                        while(!myViewModel.isConnected.value) {
                            myViewModel.mqttHandler.connect(myViewModel)
                            delay(50)
                        }
                        myViewModel.mqttHandler.subscribe(server.mac)
                        myViewModel.mqttHandler.publish(server.mac,message,myViewModel)
                        cond.name = name
                        cond.enabled = enabled
                        cond.lside = lside
                        cond.rside = rside

                        myViewModel.sqLiteHelper.updateCond(cond)
                        myViewModel.currentScreen.intValue = CONDS_VIEW

                    }
                },
                enabled = enabledBut
            ) {
                Text("Save")
            }

            Button(
                onClick = {
                    var message = ""
                    CoroutineScope(Dispatchers.IO).launch {
                        enabledBut = false

                        message = "RC:${cond.condIdLocal}"
                        while(!myViewModel.isConnected.value) {
                            myViewModel.mqttHandler.connect(myViewModel)
                            delay(50)
                        }
                        myViewModel.mqttHandler.subscribe(server.mac)
                        myViewModel.mqttHandler.publish(server.mac,message,myViewModel)

                        myViewModel.sqLiteHelper.deleteCond(cond.id)
                        myViewModel.currentScreen.intValue = CONDS_VIEW
                        myViewModel.listOfConds.remove(cond)

                    }

                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                enabled = enabledBut
            ) {
                Text("Delete", color = Color.White)
            }
        }
    }
}


@Composable
fun EditGpio(myViewModel: MyViewModel = viewModel()) {
    val gpio = myViewModel.listOfGpios.first { u -> u.id == myViewModel.gpioInd.intValue && u.devId == myViewModel.devInd.intValue && u.gpioType == myViewModel.gpioType.value}
    var name by remember { mutableStateOf(gpio.name.value) }
    var range by remember { mutableStateOf(gpio.range) }
    var unit by remember { mutableStateOf(gpio.unit) }
    var startValue by remember { mutableStateOf(gpio.startValue) }
    var slope by remember { mutableStateOf(gpio.slope) }
    var visibility by remember { mutableStateOf(gpio.visibility) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text("Edit GPIO")
        Text("GPIO ID: " + gpio.id)

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = range.toString(),
            onValueChange = { it.toIntOrNull()?.let { newValue -> range = newValue } },
            label = { Text("Range") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = unit,
            onValueChange = { unit = it },
            label = { Text("Unit") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = startValue.toString(),
            onValueChange = { input ->
                // Allow negative numbers and validate input
                val parsedValue = input.toFloatOrNull()
                if (parsedValue != null || input == "-" || input.isEmpty()) {
                    startValue = parsedValue ?: 0f // Retain partial input like "-" or reset to 0
                }
            },
            label = { Text("Start Value") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), // Use Text keyboard to allow "-"
            singleLine = true // Optional: Restricts the input to a single line
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = slope.toString(),
            onValueChange = { input ->
                // Allow negative numbers and validate input
                val parsedValue = input.toFloatOrNull()
                if (parsedValue != null || input == "-" || input.isEmpty()) {
                    slope = parsedValue ?: 0f // Retain partial input like "-" or reset to 0
                }
            },
            label = { Text("Slope value") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), // Use Text keyboard to allow "-"
            singleLine = true // Optional: Restricts the input to a single line
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Visibility:")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = visibility,
                onCheckedChange = { visibility = it }
            )
        }
        Button(
            onClick = {
                gpio.visibility = visibility
                gpio.name.value = name
                gpio.range = range
                gpio.unit = unit
                gpio.startValue = startValue
                gpio.slope = slope
                gpio.visibility = visibility
                myViewModel.sqLiteHelper.updateGpio(gpio)
                myViewModel.currentScreen.intValue = DEV_VIEW;
            }
        ) {
            Text("Save")
        }


    }

}
