package com.example.buildingmenegamentapp


import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.buildingmenegamentapp.ui.theme.BuildingMenegamentAppTheme
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.buildingmenegamentapp.userinterface.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.theapache64.rebugger.Rebugger
import java.util.ArrayList

const val HOME_VIEW = 0
const val ADD_SERVER_VIEW = 1
const val SERVER_VIEW = 2
const val ADD_DEV_VIEW = 3
const val ADD_COND_VIEW = 4
const val DEV_VIEW = 5
const val CONDS_VIEW = 6
const val EDIT_GPIO_VIEW = 7
const val EDIT_COND_VIEW = 8
const val EDIT_SERV_VIEW = 9
const val EDIT_DEVICE_VIEW = 10

const val cloud_ip = "tcp://217.182.75.141:1883"
//const val cloud_ip = "tcp://192.168.1.100:1883"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BuildingMenegamentAppTheme {
                    MainWindow()

            }
        }
    }
}
@Composable
fun MyApp(darkMode: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkMode) DarkColors else LightColors
    Rebugger(
        trackMap = mapOf(
            "darkMode" to darkMode,
            "content" to content,
            "colors" to colors,
        ),
        composableName = "MyApp"
    )
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun MainWindow(myViewModel: MyViewModel = viewModel(), darkMode: Boolean = isSystemInDarkTheme()) {
    MyApp(darkMode = darkMode) {
        var currentScreen by remember { myViewModel.currentScreen }
        val context = LocalContext.current
        var topBarText = "Home View"
        Rebugger(
            trackMap = mapOf(
                "currentScreen" to currentScreen,
                "topBarText" to topBarText,
                "myViewModel" to myViewModel,
                "darkMode" to darkMode,
            ),
            composableName = "MainWindow"
        )
        if (!myViewModel.initialized) {
            myViewModel.sqLiteHelper = SQLiteHelper(context)
            myViewModel.initialized = true;
            myViewModel.readDb()
            myViewModel.mqttHandler = MqttHandler(cloud_ip,"ID")
        }
        if(myViewModel.isConnected.value == false){
            try{
                myViewModel.mqttHandler.connect(myViewModel)
            } catch(ex:Exception) {
                ex.printStackTrace()
            }
        }




        BackHandler(enabled = true) {
            // Handle the back press logic here
            when (currentScreen) {
                HOME_VIEW -> (context as? Activity)?.finish()
                ADD_SERVER_VIEW -> currentScreen = HOME_VIEW
                SERVER_VIEW -> currentScreen = HOME_VIEW
                ADD_DEV_VIEW -> currentScreen = SERVER_VIEW
                DEV_VIEW -> currentScreen = SERVER_VIEW
                CONDS_VIEW -> currentScreen = SERVER_VIEW
                ADD_COND_VIEW -> currentScreen = CONDS_VIEW
                EDIT_GPIO_VIEW -> currentScreen = DEV_VIEW
                EDIT_COND_VIEW -> currentScreen = CONDS_VIEW
                EDIT_SERV_VIEW -> currentScreen = SERVER_VIEW
                EDIT_DEVICE_VIEW -> currentScreen = DEV_VIEW
            }
        }
        when (currentScreen) {
            HOME_VIEW -> topBarText = "Home View"
            ADD_SERVER_VIEW -> topBarText = "Add Server View"
            SERVER_VIEW -> topBarText = "Server View - " + myViewModel.listOfLocalServers.first { u -> u.id == myViewModel.servInd.intValue }.name.value
            EDIT_SERV_VIEW -> topBarText = "Edit server View - " + myViewModel.listOfLocalServers.first { u -> u.id == myViewModel.servInd.intValue }.name.value
            ADD_DEV_VIEW -> topBarText = "Add Device View - " + myViewModel.listOfLocalServers.first { u -> u.id == myViewModel.servInd.intValue }.name.value
            ADD_COND_VIEW -> topBarText = "Add Condition View - " + myViewModel.listOfLocalServers.first { u -> u.id == myViewModel.servInd.intValue }.name.value
            DEV_VIEW -> topBarText = "Device View - " +
                    myViewModel.listOfLocalServers.first { u -> u.id == myViewModel.servInd.intValue }.name.value +
                    " - " +
                    myViewModel.listOfEndDevices.first { u -> u.id == myViewModel.devInd.intValue }.name.value
            EDIT_DEVICE_VIEW -> topBarText = "Edit View - " +
                    myViewModel.listOfLocalServers.first { u -> u.id == myViewModel.servInd.intValue }.name.value +
                    " - " +
                    myViewModel.listOfEndDevices.first { u -> u.id == myViewModel.devInd.intValue }.name.value
            CONDS_VIEW -> topBarText = "Conds View - " +
                    myViewModel.listOfLocalServers.first { u -> u.id == myViewModel.servInd.intValue }.name.value
            EDIT_COND_VIEW -> topBarText = "Cond View - " +
                    myViewModel.listOfLocalServers.first { u -> u.id == myViewModel.servInd.intValue }.name.value +
                    " - " +
                    myViewModel.listOfConds.first { u -> u.id == myViewModel.condInd.intValue}.name
            EDIT_GPIO_VIEW -> topBarText = "GPIO View - " +
                    myViewModel.listOfLocalServers.first { u -> u.id == myViewModel.servInd.intValue }.name.value +
                    " - " +
                    myViewModel.listOfEndDevices.first { u -> u.id == myViewModel.devInd.intValue }.name.value +
                    " - " + myViewModel.listOfGpios.first { u -> u.id == myViewModel.gpioInd.intValue && u.devId == myViewModel.devInd.intValue}.name.value
        }




        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .drawBehind {
                            drawLine(
                                color = Color.Gray,
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 2.dp.toPx()
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Title with flexible width
                    Text(
                        modifier = Modifier
                            .weight(1f) // Ensures the text takes up the remaining space
                            .padding(15.dp),
                        text = topBarText.trimIndent(),
                        maxLines = 2, // Allows up to 2 lines
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, // Truncates additional text
                        style = TextStyle(
                            fontSize = 16.sp, // Adjust font size for better fit
                            fontWeight = FontWeight.Medium
                        )
                    )

                    // MQTT Connection Indicator and Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        if (myViewModel.isConnected.value) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Connected",
                                tint = Color.Green,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Connected",
                                modifier = Modifier.padding(start = 8.dp),
                                color = Color.Green,
                                fontSize = 14.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Disconnected",
                                tint = Color.Red,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Disconnected",
                                modifier = Modifier.padding(start = 8.dp),
                                color = Color.Red,
                                fontSize = 14.sp
                            )
                            Button(
                                onClick = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            myViewModel.mqttHandler.connect(myViewModel)
                                        } catch (ex: Exception) {
                                            ex.printStackTrace()
                                        }
                                    }
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text("Reconnect")
                            }
                        }
                    }
                }
            },
            bottomBar = {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    NavBar { currentScreen = it }
                }
            },

        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when(currentScreen){
                    HOME_VIEW -> Home()
                    ADD_SERVER_VIEW -> AddServ()
                    SERVER_VIEW -> Server()
                    EDIT_SERV_VIEW -> EditServer()
                    ADD_DEV_VIEW -> AddDev()
                    ADD_COND_VIEW -> AddCond()
                    DEV_VIEW -> Dev()
                    CONDS_VIEW -> Conds()
                    EDIT_GPIO_VIEW -> EditGpio()
                    EDIT_COND_VIEW -> EditCond()
                    EDIT_DEVICE_VIEW -> EditDev()
                }
            }
        }
    }
}

@Composable
fun NavBar(changeWindow: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize(),
        content = {
            Button( onClick = {changeWindow(HOME_VIEW)},
            ){
                Icon(Icons.Default.Home, contentDescription = "HomePage")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BuildingMenegamentAppTheme {
        MainWindow()
    }
}

