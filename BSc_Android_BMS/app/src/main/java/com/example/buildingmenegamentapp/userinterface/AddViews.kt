package com.example.buildingmenegamentapp.userinterface

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.buildingmenegamentapp.CONDS_VIEW
import com.example.buildingmenegamentapp.HOME_VIEW
import com.example.buildingmenegamentapp.LocalServer
import com.example.buildingmenegamentapp.MyViewModel
import com.example.buildingmenegamentapp.SERVER_VIEW
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import com.theapache64.rebugger.Rebugger
import java.net.URL


@SuppressLint("UnrememberedMutableState")
@Composable
fun AddServ(myViewModel: MyViewModel = viewModel()) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var mac by remember { mutableStateOf("") }
    println("AddServ")
    Rebugger(
        trackMap = mapOf(
            "myViewModel" to myViewModel,
            "name" to name,
            "ip" to ip,
            "mac" to mac,
        ),
        composableName = "AddServ"
    )
    var server = LocalServer(-1,mutableStateOf(""),"","", mutableStateOf(false))

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text("Add SERVER")

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = ip,
            onValueChange = { ip = it },
            label = { Text("Ip") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = mac,
            onValueChange = { mac = it },
            label = { Text("mac") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {

                server.name.value = name
                server.ip = ip
                server.mac = mac
                myViewModel.sqLiteHelper.insertLocalServer(server)
                myViewModel.readDb()
                myViewModel.currentScreen.intValue = HOME_VIEW;
            }
        ) {
            Text("Save")
        }
    }
}
/*

val localServerId: Int,
    var name: String,                // Name of the condition (from COND.Name)
    val lside: String,               // Left-side expression (from COND.Lside)
    val rside: String,               // Right-side expression (from COND.Rside)
    var enabled: Boolean             // Condition enabled/disabled (from COND.Enabled)
 */
@Composable
fun AddCond(myViewModel: MyViewModel = viewModel()) {
    val server = myViewModel.listOfLocalServers.first { s -> s.id == myViewModel.servInd.intValue }
    var name by remember { mutableStateOf("") }
    var lside by remember { mutableStateOf("") }
    var rside by remember { mutableStateOf("") } // New state for device name
    var isSubmitting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    Rebugger(
        trackMap = mapOf(
            "myViewModel" to myViewModel,
            "server" to server,
            "name" to name,
            "lside" to lside,
            "rside" to rside,
            "isSubmitting" to isSubmitting,
            "coroutineScope" to coroutineScope,
            "context" to context,
        ),
        composableName = "AddCond"
    )
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text("Configure Cond")

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("NAME") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = lside,
            onValueChange = { lside = it },
            label = { Text("LEFT SIDE") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // New TextField for device name
        TextField(
            value = rside,
            onValueChange = { rside = it },
            label = { Text("RIGHT SIDE") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                var message = ""
                isSubmitting = true
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        lside = lside.replace("\\", "\\\\")
                        rside = rside.replace("\\", "\\\\")
                        message = "AC:$name;$lside;$rside"
                        while(myViewModel.isConnected.value == false) {
                            myViewModel.mqttHandler.connect(myViewModel)
                            delay(50)
                        }
                        myViewModel.mqttHandler.subscribe(server.mac)
                        myViewModel.mqttHandler.publish(server.mac,message,myViewModel)
                        myViewModel.currentScreen.intValue = CONDS_VIEW

                    } catch (e: Exception) {
                        Log.e("AddDev", "Error configuring device", e)
                    } finally {
                        isSubmitting = false
                    }
                }
            },
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSubmitting) "Submitting..." else "Submit")
        }
    }
}

@Composable
fun AddDev(myViewModel: MyViewModel = viewModel()) {
    val server = myViewModel.listOfLocalServers.first { s -> s.id == myViewModel.servInd.intValue }
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("") } // New state for device name
    var isSubmitting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val mqttIp = server.ip
    val context = LocalContext.current
    Rebugger(
        trackMap = mapOf(
            "myViewModel" to myViewModel,
            "server" to server,
            "ssid" to ssid,
            "password" to password,
            "deviceName" to deviceName,
            "isSubmitting" to isSubmitting,
            "coroutineScope" to coroutineScope,
            "mqttIp" to mqttIp,
            "context" to context,
        ),
        composableName = "AddDev"
    )
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text("Configure Device")

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = ssid,
            onValueChange = { ssid = it },
            label = { Text("SSID") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // New TextField for device name
        TextField(
            value = deviceName,
            onValueChange = { deviceName = it },
            label = { Text("Device Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                var message = ""
                isSubmitting = true
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val response = configureDevice(ssid, password, mqttIp)
                        Log.d("AddDev", "Response: $response")
                        message = "AD:$response;$deviceName"
                        while(myViewModel.isConnected.value == false) {
                            myViewModel.mqttHandler.connect(myViewModel)
                            delay(50)
                        }
                        myViewModel.mqttHandler.subscribe(server.mac)
                        myViewModel.mqttHandler.publish(server.mac,message,myViewModel)
                        myViewModel.currentScreen.intValue = SERVER_VIEW

                    } catch (e: Exception) {
                        Log.e("AddDev", "Error configuring device", e)
                    } finally {
                        isSubmitting = false
                    }
                }
            },
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSubmitting) "Submitting..." else "Submit")
        }
    }
}

suspend fun configureDevice(ssid: String, password: String, mqttIp: String): String {
    val url = URL("http://192.168.4.1/configure")
    val postData = "ssid=$ssid&password=$password&mqtt=$mqttIp"

    with(withContext(Dispatchers.IO) {
        url.openConnection()
    } as HttpURLConnection) {
        requestMethod = "POST"
        doOutput = true
        outputStream.write(postData.toByteArray())
        var response = inputStream.bufferedReader().use { it.readText() }
        // Return the substring starting from index 9
        return if (response.length > 9) {
            response.substring(9)
        } else {
            response
        }
    }
}




