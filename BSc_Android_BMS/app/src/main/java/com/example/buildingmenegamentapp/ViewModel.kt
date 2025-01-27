package com.example.buildingmenegamentapp

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlin.Exception


class MyViewModel : ViewModel() {

    lateinit var sqLiteHelper: SQLiteHelper
    var initialized = false

    lateinit var mqttHandler: MqttHandler
    var isConnected = mutableStateOf(false);

    var listOfLocalServers = mutableStateListOf<LocalServer>()
    var listOfEndDevices = mutableStateListOf<EndDevice>()
    var listOfGpios = mutableStateListOf<Gpio>()
    var listOfConds = mutableStateListOf<Cond>()

    var currentScreen = mutableIntStateOf(HOME_VIEW)
    var servInd =  mutableIntStateOf(-1)
    var devInd =  mutableIntStateOf(-1)
    var gpioInd =  mutableIntStateOf(-1)
    var gpioType = mutableStateOf('0')
    var condInd =  mutableIntStateOf(-1)


    fun readDb(){
        listOfLocalServers.clear()
        listOfConds.clear()
        listOfGpios.clear()
        listOfEndDevices.clear()


        sqLiteHelper.getLocalServers().forEach{s->
            listOfLocalServers.add(s)
        }

        sqLiteHelper.getEndDevices().forEach{d->
            listOfEndDevices.add(d)
        }

        sqLiteHelper.getGpios().forEach{g->
            listOfGpios.add(g)
        }

        sqLiteHelper.getConds().forEach{c->
            listOfConds.add(c)
        }
    }

    fun readDbServer(){
        listOfLocalServers.clear()
        sqLiteHelper.getLocalServers().forEach{s->
            listOfLocalServers.add(s)
        }
    }
    fun readDbDevice(){
        listOfEndDevices.clear()
        sqLiteHelper.getEndDevices().forEach{e->
            listOfEndDevices.add(e)
        }
        readDbGpio()

    }
    fun readDbCond(){
        listOfConds.clear()
        sqLiteHelper.getConds().forEach{c->
            listOfConds.add(c)
        }
    }

    fun readDbGpio(){
        listOfGpios.clear()
        sqLiteHelper.getGpios().forEach{g->
            listOfGpios.add(g)
        }
    }

    fun setMqttOnline(online: Boolean){
        isConnected.value = online;
    }
    fun setServerOnline(mac: String, online: Boolean) {
        val server = listOfLocalServers.first{ it.mac == mac }
        if(server.online.value != online)
            server.online.value = online;
    }

    fun syncDevices(device: String = "D9;40:4c:ca:43:da:b0;name;D10;40:4a:ca:43:da:b0;name;", macServer:String) {
        // Split the input string by semicolons
        try {

            val deviceEntries = device.split(";")
            val server = listOfLocalServers.first { it.mac == macServer }
            println(device)

            for (i in deviceEntries.indices step 3) {
                val devIdLocal = deviceEntries[i].removePrefix("D").toInt()
                val mac = deviceEntries[i + 1]  // Get the MAC address

                val existingDevice = listOfEndDevices.find { it.mac == mac }

                if (existingDevice == null) {
                    val newDevice = EndDevice(
                        id = -1,
                        localServerId = server.id,
                        devIdLocal = devIdLocal,
                        name = mutableStateOf(deviceEntries[i + 2]),
                        mac = mac,
                        online = mutableStateOf(false)
                    )
                    sqLiteHelper.insertEndDeviceWithGpios(newDevice)
                } else {
                    existingDevice.name = mutableStateOf(deviceEntries[i + 2])
                    sqLiteHelper.updateEndDevice(existingDevice)
                }
                readDbDevice()
            }
        }catch(ex:Exception)
        {
            return;
        }

    }

    fun syncCond(conds: String = "C1;name;lside;rside;C4:name;lside;rside;", macServer: String) {
        // Split the input string by semicolons
        try{
            val condEntries = conds.split(";")
            val server = listOfLocalServers.first{it.mac == macServer}
            println(conds)

            for (i in condEntries.indices step 5) {
                if(condEntries.count() == i + 1) break;
                val condIdLocal = condEntries[i].removePrefix("C").toInt()
                val lside = condEntries[i + 2]  // Get the MAC address
                val rside = condEntries[i + 3]  // Get the MAC address

                val existingCond = listOfConds.find { it.lside == lside && it.rside == rside && it.localServerId == server.id }

                if (existingCond == null) {
                    val newCond = Cond(
                        id = -1,
                        condIdLocal = condIdLocal,
                        localServerId = server.id,
                        name = (condEntries[i+1]),
                        lside = lside,
                        rside = rside,
                        enabled = if(condEntries[i+4] == "1") true else false
                    )
                    sqLiteHelper.insertCond(newCond)
                }
                else{
                    existingCond.condIdLocal = condIdLocal;
                    existingCond.name = (condEntries[i+1])
                    existingCond.lside = lside
                    existingCond.rside = rside
                    existingCond.enabled = if(condEntries[i+4] == "1") true else false
                    sqLiteHelper.updateCond(existingCond)
                }
                readDbCond()
            }
        }catch(ex:Exception)
        {
            return;
        }

    }
    fun syncState(state: ByteArray, stateString: String, mac: String) {
        //LOC:D15:fffffffff
        //D15:fffffffffD16
        //D15:OFFLINED16:
        try{
            var h = 0
            var subState = state
            var subStateString = stateString;
            subState = subState.copyOfRange(4, state.size);
//            println(subState.size.toString() + " " + stateString.count().toString())


            while(true) {
                var t = 1;

//                println(subState.map { it.toInt().toChar() })
//                println(subStateString)

                h = subStateString.indexOf(':')
                val ind = subStateString.substring(1, h).toInt()
//                println("NEW ITERATION $ind")
                subState = subState.copyOfRange(h + 1, subState.size)
                subStateString = subStateString.substring(h + 1)

//                println(subState.map { it.toInt().toChar() })
//                println(subStateString)
                if (subStateString.substring(0, 7) == "OFFLINE") {
//                    println("OFFLINE $ind")
                    setEndDeviceOnline(ind, mac, false);
                    if(8 > subState.size)
                        break;
                    subState = subState.copyOfRange(7, subState.size);
                    subStateString = subStateString.substring(7);
                    continue;
                }
                setEndDeviceOnline(ind, mac, true);

//                println("DEVICE ONLINE UPDATED $ind")
                for (i in 1..8) {
                    setGpioState(ind, i, mac, subState[0].toInt().and(t), true)
                    t *= 2
                }
//                println("GPIOS STATE UPDATED $ind")
                //HERE ANALOG
                for(i in 1 .. 4){
                    val value = subState[i*2 - 1].toInt().shl(8).and(255.shl(8)) + subState[i * 2].toInt().and(255)
//                    setGpioState(ind, i, mac, subState[i*2 - 1].toInt().shl(8).and(255.shl(8)) + subState[i * 2].toInt().and(255),false);
                    setGpioState(ind, i, mac, (value.toDouble()*10000.0/4095.0).toInt(),false);

                }
//                println(subState.map { it.toInt().toChar() })
//                println(subStateString)

                if(10 > subState.size)
                    break;
                subState = subState.copyOfRange(9, subState.size)
//                println(subState.map { it.toInt().toChar() })

                subStateString = subStateString.substring(9);
//                println(subStateString)
//                println("FINITO")
            }
        }catch(ex:Exception)
        {
            return;
        }

    }
    fun setGpioState(localId: Int, GpioId: Int, mac: String , state: Int, digital: Boolean) {
        try{
            val server = listOfLocalServers.first{ it.mac == mac }
            val device = listOfEndDevices.first{ it.localServerId == server.id && it.devIdLocal == localId }
            if(digital)
            {
                val gpio = listOfGpios.first{it.id == GpioId && it.devId == device.id && it.gpioType == 'd'}
                gpio.state.value = if(state >= 1) 1 else 0;
            }
            else{
                val gpio = listOfGpios.first{it.id == GpioId && it.devId == device.id && it.gpioType == 'a'}
                gpio.state.value = state;
            }
        }
        catch(ex:Exception){return;}


    }

    fun setEndDeviceOnline(localId: Int, mac: String , online: Boolean) {
        val server = listOfLocalServers.first{ it.mac == mac }
        var device : EndDevice
        try{
             device = listOfEndDevices.first{ it.localServerId == server.id && it.devIdLocal == localId }

        }
        catch (ex:Exception){
            mqttHandler.publish(mac,"SYNCDEV",mqttHandler.viewModel);
            return
        }
        device = listOfEndDevices.first{ it.localServerId == server.id && it.devIdLocal == localId }
        if(device.online.value != online)
            device.online.value = online;
    }





}