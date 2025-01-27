package com.example.buildingmenegamentapp.userinterface

import android.annotation.SuppressLint
import android.health.connect.datatypes.units.Power
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.buildingmenegamentapp.ADD_COND_VIEW
import com.example.buildingmenegamentapp.ADD_DEV_VIEW
import com.example.buildingmenegamentapp.ADD_SERVER_VIEW
import com.example.buildingmenegamentapp.CONDS_VIEW
import com.example.buildingmenegamentapp.DEV_VIEW
import com.example.buildingmenegamentapp.EDIT_COND_VIEW
import com.example.buildingmenegamentapp.EDIT_DEVICE_VIEW
import com.example.buildingmenegamentapp.EDIT_GPIO_VIEW
import com.example.buildingmenegamentapp.EDIT_SERV_VIEW
import com.example.buildingmenegamentapp.EndDevice
import com.example.buildingmenegamentapp.Gpio
import com.example.buildingmenegamentapp.HOME_VIEW
import com.example.buildingmenegamentapp.MqttHandler
import com.example.buildingmenegamentapp.MyViewModel
import com.example.buildingmenegamentapp.SERVER_VIEW
import com.theapache64.rebugger.Rebugger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow
import androidx.compose.runtime.LaunchedEffect

@Composable
fun Home(myViewModel: MyViewModel = viewModel()) {
    Rebugger(
        trackMap = mapOf(
            "myViewModel" to myViewModel,
        ),
        composableName = "Home"
    )
    LazyColumn {
        myViewModel.listOfLocalServers.forEach{ server ->
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            myViewModel.servInd.intValue = server.id
                            myViewModel.currentScreen.intValue = SERVER_VIEW
                        }
                ) {
                    Text(
                        text = "Server Name: ${server.name.value}",
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    )
                    Text(
                        text = "IP Address: ${server.ip}",
                        style = TextStyle(
                            fontSize = 16.sp,
                        )
                    )
                    Text(
                        text = "MAC: ${server.mac}",
                        style = TextStyle(
                            fontSize = 16.sp,
                        )
                    )
                }

                Divider(color = Color.Gray, modifier = Modifier.padding(10.dp))
            }
        }
        item{
            Button(onClick = {
                myViewModel.currentScreen.intValue = ADD_SERVER_VIEW
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add")

            }
        }

    }

}



@Composable
fun Server(myViewModel: MyViewModel = viewModel()) {
    val server = remember { myViewModel.listOfLocalServers.find { u -> u.id == myViewModel.servInd.intValue } }
    val devices = remember { myViewModel.listOfEndDevices }
    val gpios = remember { myViewModel.listOfGpios }

    if (server == null) {
        myViewModel.currentScreen.intValue = HOME_VIEW
        return
    }

    val devicesCopy = devices.filter { d -> d.localServerId == server.id }.toList()
    val gpiosCopy = gpios.filter { g -> g.visibility }.toList()

    if (!myViewModel.isConnected.value) myViewModel.mqttHandler.connect(myViewModel)
    myViewModel.mqttHandler.subscribe(server.mac)

    Rebugger(
        trackMap = mapOf(
            "myViewModel" to myViewModel,
            "server" to server,
            "devices" to devices,
            "gpios" to gpios,
            "devicesCopy" to devicesCopy,
            "gpiosCopy" to gpiosCopy,
        ),
        composableName = "Server"
    )

    LazyColumn {
        // Display Server Information with Online Indicator
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Server Name: ${server.name.value}",
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "IP Address: ${server.ip}",
                    style = TextStyle(fontSize = 16.sp)
                )
                Text(
                    text = "MAC: ${server.mac}",
                    style = TextStyle(fontSize = 16.sp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    if (server.online.value && myViewModel.isConnected.value) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Server Online",
                            tint = Color.Green,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Online",
                            modifier = Modifier.padding(start = 8.dp),
                            color = Color.Green,
                            fontSize = 14.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Server Offline",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Offline",
                            modifier = Modifier.padding(start = 8.dp),
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            Divider(color = Color.Gray, modifier = Modifier
                .padding(10.dp, 0.dp, 10.dp, 0.dp)
                .height(5.dp))
        }

        // Display Devices and GPIOs, conditionally based on device online status
        devicesCopy.forEach outer@{ dev ->
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp, 16.dp, 16.dp, 0.dp)
                        .clickable {
                            myViewModel.devInd.intValue = dev.id
                            myViewModel.currentScreen.intValue = DEV_VIEW
                        }
                ) {
                    Text(
                        text = "Device Name: ${dev.name.value}",
                        style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "MAC: ${dev.mac}\t Id: ${dev.devIdLocal}",
                        style = TextStyle(fontSize = 16.sp)
                    )

                    // Device Online Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        if (dev.online.value && server.online.value && myViewModel.isConnected.value) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Device Online",
                                tint = Color.Green,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Online",
                                modifier = Modifier.padding(start = 8.dp),
                                color = Color.Green,
                                fontSize = 14.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Device Offline",
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Offline",
                                modifier = Modifier.padding(start = 8.dp),
                                color = Color.Red,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                if (!dev.online.value) return@item // Skip if device is offline

                // Display GPIOs for Online Devices
                gpiosCopy.forEach inner@{ gpio ->
                    if (gpio.devId != dev.id) {
                        return@inner
                    }

                    Divider(color = Color.Gray, modifier = Modifier.padding(40.dp, 10.dp))
                    Column(modifier = Modifier.padding(25.dp, 0.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            when (gpio.gpioType) {
                                'd' -> {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)  // Circle size
                                            .background(
                                                color = if (gpio.state.value >= 1) Color.Yellow else Color.Black,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    if (!gpio.input) {
                                                        val str =
                                                            if (gpio.state.value == 0) "FF" else "00"
                                                        myViewModel.mqttHandler.publish(
                                                            server.mac,
                                                            "D" + dev.devIdLocal.toString() + ":00FFFF0" + 2.0
                                                                .pow(gpio.id.toDouble() - 5)
                                                                .toInt() + str,
                                                            myViewModel
                                                        );
                                                    }
                                                }
                                            }
                                    )
                                }
                                'a' -> {
                                    val text = remember{mutableStateOf(gpio.state.value.toString())}
                                    var isEditing by remember { mutableStateOf(false) }

                                    if (!gpio.input) {
                                        LaunchedEffect(gpio.state) {
                                            // Synchronize with gpio.state only if not actively editing
                                            if (!isEditing) {
                                                text.value = gpio.state.value.toString()
                                            }
                                        }

                                        TextField(
                                            value = text.value,
                                            onValueChange = {
                                                if (it.toIntOrNull() in 0..10000 || it == "") {
                                                    text.value = it
                                                    isEditing = true;
                                                }
                                            },
                                            label = { Text("Value (0-10000)") },
                                            modifier = Modifier
                                                .width(150.dp)
                                                .align(Alignment.CenterVertically),
                                            keyboardOptions = KeyboardOptions.Default.copy(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(
                                                onDone = {
                                                    println("helo")
                                                    CoroutineScope(Dispatchers.IO).launch {
                                                        if(text.value != ""){
                                                            val message = "D${dev.devIdLocal}:FFFFFF${if (gpio.id == 3) "04" else "06"}${
                                                                (text.value.toIntOrNull()!!.toDouble() * 4095.0 / 10000.0).toInt().toString(16)
                                                                    .padStart(4, '0').uppercase()
                                                            }"
                                                            println(message)
                                                            while (!myViewModel.isConnected.value) {
                                                                myViewModel.mqttHandler.connect(myViewModel)
                                                                delay(50)
                                                            }
                                                            myViewModel.mqttHandler.subscribe(server.mac)
                                                            myViewModel.mqttHandler.publish(server.mac, message.trim(), myViewModel)
                                                        }
                                                        isEditing = false;

                                                    }
                                                }
                                            )
                                        )
                                    } else {
                                        val scaledValue = remember(gpio.state.value) {
                                            if(gpio.slope != 0f) gpio.startValue + (gpio.state.value.toFloat()) * gpio.slope
                                            else gpio.state.value;
                                        }
                                        Text(
                                            text = if(gpio.slope != 0f) String.format("%.2f %s", scaledValue, gpio.unit) else String.format("%d mV", scaledValue), // Format with two decimals and append the unit
                                            modifier = Modifier.align(Alignment.CenterVertically)
                                        )
                                    }
                                }
                            }
                            Text(text = gpio.name.value, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Divider(color = Color.Gray, modifier = Modifier.padding(10.dp, 10.dp, 10.dp, 0.dp))
            }
        }
        item{
            Divider(color = Color.Gray, modifier = Modifier.padding(10.dp, 10.dp, 10.dp, 0.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable {
                        myViewModel.currentScreen.intValue = CONDS_VIEW
                    }
            ) {
                Text(
                    text = "CONDS HERE",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                )
            }

            Divider(color = Color.Gray, modifier = Modifier.padding(10.dp))
        }

        // Conditionally show buttons only when devices are online
        item {
            Row {
                Button(onClick = {
                    myViewModel.currentScreen.intValue = ADD_DEV_VIEW
                }) {
                    Text(text = "ADD DEVICE")
                }
                Button(onClick = {
                    myViewModel.currentScreen.intValue = EDIT_SERV_VIEW
                }) {
                    Text(text = "EDIT SERVER")
                }
            }
        }
    }
}


@Composable
fun Dev(myViewModel: MyViewModel = viewModel()) {
    val server = remember { myViewModel.listOfLocalServers.find { u -> u.id == myViewModel.servInd.intValue } }
    val dev = remember { myViewModel.listOfEndDevices.find { u -> u.id == myViewModel.devInd.intValue } }
    val gpios = remember { myViewModel.listOfGpios }



    if (server == null) {
        myViewModel.currentScreen.intValue = HOME_VIEW
        return
    }
    if (dev == null) {
        myViewModel.currentScreen.intValue = HOME_VIEW
        return
    }
    val gpiosCopy = gpios.filter { g -> g.devId == myViewModel.devInd.intValue }


    Rebugger(
        trackMap = mapOf(
            "myViewModel" to myViewModel,
            "server" to server,
            "gpios" to gpios,
            "gpiosCopy" to gpiosCopy,
        ),
    )
    LazyColumn(modifier = Modifier.padding(25.dp, 0.dp)) {
        item{
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 16.dp, 16.dp, 0.dp)
                    .clickable {
                        myViewModel.devInd.intValue = dev.id
                        myViewModel.currentScreen.intValue = DEV_VIEW
                    }
            ) {
                Text(
                    text = "Device Name: ${dev.name.value}",
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "MAC: ${dev.mac}\t Id: ${dev.devIdLocal}",
                    style = TextStyle(fontSize = 16.sp)
                )

                // Device Online Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    if (dev.online.value && server.online.value && myViewModel.isConnected.value) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Device Online",
                            tint = Color.Green,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Online",
                            modifier = Modifier.padding(start = 8.dp),
                            color = Color.Green,
                            fontSize = 14.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Device Offline",
                            tint = Color.Red,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Offline",
                            modifier = Modifier.padding(start = 8.dp),
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Divider(color = Color.Gray, modifier = Modifier
                .padding(10.dp, 0.dp, 10.dp, 0.dp)
                .height(5.dp))
        }
        gpiosCopy.forEach { gpio ->
            item {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp) // Add vertical spacing between rows
                ) {
                    // Left side: Icon and GPIO name
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (gpio.gpioType) {
                            'd' -> {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)  // Circle size
                                        .background(
                                            color = if (gpio.state.value >= 1) Color.Yellow else Color.Black,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            CoroutineScope(Dispatchers.IO).launch {
                                                if (!gpio.input) {
                                                    val str =
                                                        if (gpio.state.value == 0) "FF" else "00"
                                                    myViewModel.mqttHandler.publish(
                                                        server.mac,
                                                        "D" + dev.devIdLocal.toString() + ":00FFFF0" + 2.0
                                                            .pow(gpio.id.toDouble() - 5)
                                                            .toInt() + str,
                                                        myViewModel
                                                    );
                                                }
                                            }
                                        }
                                )
                                Text(
                                    text = if (gpio.input) "Input" else "Output",
                                    color = if (gpio.input) Color.Blue else Color.Green,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }

                            'a' -> {
                                val text = remember{mutableStateOf(gpio.state.value.toString())}
                                var isEditing by remember { mutableStateOf(false) }

                                if (!gpio.input) {
                                    LaunchedEffect(gpio.state) {
                                        // Synchronize with gpio.state only if not actively editing
                                        if (!isEditing) {
                                            text.value = gpio.state.value.toString()
                                        }
                                    }

                                    TextField(
                                        value = text.value,
                                        onValueChange = {
                                            if (it.toIntOrNull() in 0..10000 || it == "") {
                                                text.value = it
                                                isEditing = true;
                                            }
                                        },
                                        label = { Text("Value (0-10000)") },
                                        modifier = Modifier
                                            .width(150.dp)
                                            .align(Alignment.CenterVertically),
                                        keyboardOptions = KeyboardOptions.Default.copy(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                println("helo")
                                                CoroutineScope(Dispatchers.IO).launch {
                                                    if(text.value != ""){
                                                        val value =
                                                            (text.value.toIntOrNull()!!.toDouble() * 4095.0 / 10000.0).toInt()
                                                        val message = "D${dev.devIdLocal}:FFFFFF${if (gpio.id == 3) "04" else "06"}" + value.toString(16)
                                                            .padStart(4, '0').uppercase().trim()
                                                        println(message)
                                                        while (!myViewModel.isConnected.value) {
                                                            myViewModel.mqttHandler.connect(myViewModel)
                                                            delay(50)
                                                        }
                                                        myViewModel.mqttHandler.subscribe(server.mac)
                                                        myViewModel.mqttHandler.publish(server.mac, message, myViewModel)
                                                    }
                                                    isEditing = false;

                                                }
                                            }
                                        )
                                    )
                                } else {
                                    val scaledValue = remember(gpio.state.value) {
                                        if(gpio.slope != 0f) gpio.startValue + (gpio.state.value.toFloat()) * gpio.slope
                                        else gpio.state.value;
                                    }
                                    Text(
                                        text = if(gpio.slope != 0f) String.format("%.2f %s", scaledValue, gpio.unit) else String.format("%d mV", scaledValue), // Format with two decimals and append the unit
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                }
                            }
                        }
                        Text(
                            text = gpio.name.value,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }

                    // Right side: Edit GPIO button
                    Button(
                        onClick = {
                            myViewModel.gpioInd.intValue = gpio.id
                            myViewModel.gpioType.value = gpio.gpioType
                            myViewModel.currentScreen.intValue = EDIT_GPIO_VIEW
                        },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 100.dp) // Ensure consistent button size
                            .align(Alignment.CenterVertically)
                    ) {
                        Text(text = "Edit")
                    }
                }
                Divider(
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
                )
            }
        }

        item {
            Button(onClick = {
                myViewModel.currentScreen.intValue = EDIT_DEVICE_VIEW
            }) {
                Text(text = "EDIT DEVICE")
            }
        }

    }
}


@Composable
fun Conds(myViewModel: MyViewModel = viewModel()) {
    val server = remember{myViewModel.listOfLocalServers.find{u->u.id == myViewModel.servInd.intValue}}
    val conds = remember{myViewModel.listOfConds}
    println("Conds")

    if(server == null){
        myViewModel.currentScreen.intValue = HOME_VIEW
        return
    }
    val condsCopy = conds.filter{c-> c.localServerId == server.id}.toList()




    LazyColumn {
        condsCopy.forEach{ cond ->
            item{
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            myViewModel.condInd.intValue = cond.id
                            myViewModel.currentScreen.intValue = EDIT_COND_VIEW
                        }
                ) {
                    Text(
                        text = "Cond Name: ${cond.name}",
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    )
                    Text(
                        text = "Lside: ${cond.lside}",
                        style = TextStyle(
                            fontSize = 16.sp,
                        )
                    )
                    Text(
                        text = "Rside: ${cond.rside}",
                        style = TextStyle(
                            fontSize = 16.sp,
                        )
                    )
                    Text(
                        text = "Enabled: ${cond.enabled}",
                        style = TextStyle(
                            fontSize = 16.sp,
                        )
                    )
                }

                Divider(color = Color.Gray, modifier = Modifier.padding(10.dp))
            }
        }
        item{
            Row {
                Button(onClick = {
                    myViewModel.currentScreen.intValue = ADD_COND_VIEW
                }) {
                    Text("ADD COND")
                }
                Button(onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        val message = "SYNCCOND"
                        while(myViewModel.isConnected.value == false) {
                            myViewModel.mqttHandler.connect(myViewModel)
                            delay(50)
                        }
                        myViewModel.mqttHandler.subscribe(server.mac)
                        myViewModel.mqttHandler.publish(server.mac,message,myViewModel)
                    }
                }) {
                    Text("Sync Cond")
                }
            }

        }
    }




}



