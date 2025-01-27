package com.example.buildingmenegamentapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

const val DATABASE_NAME = "BuildingManagement.db"
const val DATABASE_VERSION = 5

// Table names and column names
const val TABLE_LOCALSERVER = "LocalServer"
const val COLUMN_LOCALSERVER_ID = "id"
const val COLUMN_LOCALSERVER_NAME = "name"
const val COLUMN_LOCALSERVER_IP = "ip"
const val COLUMN_LOCALSERVER_MAC = "mac"

const val TABLE_ENDDEVICE = "EndDevice"
const val COLUMN_ENDDEVICE_ID = "id"
const val COLUMN_ENDDEVICE_LOCALSERVERID = "localServerId"
const val COLUMN_ENDDEVICE_DEVIDLokal = "devIdLocal"
const val COLUMN_ENDDEVICE_NAME = "name"
const val COLUMN_ENDDEVICE_MAC = "mac"

const val TABLE_GPIO = "Gpio"
const val COLUMN_GPIO_ID = "id"
const val COLUMN_GPIO_GPIOTYPE = "gpioType"
const val COLUMN_GPIO_DEV_ID = "devId"
const val COLUMN_GPIO_NAME = "name"
const val COLUMN_GPIO_RANGE = "range"
const val COLUMN_GPIO_UNIT = "unit"
const val COLUMN_GPIO_STARTVALUE = "startValue"
const val COLUMN_GPIO_SLOPE = "slope"
const val COLUMN_GPIO_VISIBILITY = "visibility"
const val COLUMN_GPIO_INPUT = "input"

const val TABLE_COND = "Cond"
const val COLUMN_COND_ID = "id"
const val COLUMN_COND_CONDIDLOCAL = "condIdLocal"
const val COLUMN_COND_LOCALSERVERID = "localServerId"
const val COLUMN_COND_NAME = "name"
const val COLUMN_COND_LSIDE = "lside"
const val COLUMN_COND_RSIDE = "rside"
const val COLUMN_COND_ENABLED = "enabled"

class SQLiteHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true) // Enables foreign key constraints
    }
    override fun onCreate(db: SQLiteDatabase) {
        val createLocalServerTable = """
            CREATE TABLE $TABLE_LOCALSERVER (
                $COLUMN_LOCALSERVER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_LOCALSERVER_NAME TEXT,
                $COLUMN_LOCALSERVER_IP TEXT,
                $COLUMN_LOCALSERVER_MAC TEXT
            )
        """.trimIndent()

        val createEndDeviceTable = """
            CREATE TABLE $TABLE_ENDDEVICE (
                $COLUMN_ENDDEVICE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ENDDEVICE_LOCALSERVERID INTEGER,
                $COLUMN_ENDDEVICE_DEVIDLokal INTEGER,
                $COLUMN_ENDDEVICE_NAME TEXT,
                $COLUMN_ENDDEVICE_MAC TEXT,
                FOREIGN KEY($COLUMN_ENDDEVICE_LOCALSERVERID) REFERENCES $TABLE_LOCALSERVER($COLUMN_LOCALSERVER_ID) 
                ON DELETE CASCADE
            )
        """.trimIndent()

                val createGpioTable = """
            CREATE TABLE $TABLE_GPIO (
                $COLUMN_GPIO_ID INTEGER,
                $COLUMN_GPIO_GPIOTYPE TEXT,
                $COLUMN_GPIO_DEV_ID INTEGER,
                $COLUMN_GPIO_NAME TEXT,
                $COLUMN_GPIO_RANGE INTEGER,
                $COLUMN_GPIO_UNIT TEXT,
                $COLUMN_GPIO_STARTVALUE DECIMAL(12,4),
                $COLUMN_GPIO_SLOPE DECIMAL(12,4),
                $COLUMN_GPIO_VISIBILITY BOOLEAN,
                $COLUMN_GPIO_INPUT BOOLEAN,
                FOREIGN KEY($COLUMN_GPIO_DEV_ID) REFERENCES $TABLE_ENDDEVICE($COLUMN_ENDDEVICE_ID) 
                ON DELETE CASCADE
            )
        """.trimIndent()

                val createCondTable = """
            CREATE TABLE $TABLE_COND (
                $COLUMN_COND_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_COND_CONDIDLOCAL INT,
                $COLUMN_COND_LOCALSERVERID INTEGER,
                $COLUMN_COND_NAME TEXT,
                $COLUMN_COND_LSIDE TEXT,
                $COLUMN_COND_RSIDE TEXT,
                $COLUMN_COND_ENABLED BOOLEAN,
                FOREIGN KEY($COLUMN_COND_LOCALSERVERID) REFERENCES $TABLE_LOCALSERVER($COLUMN_LOCALSERVER_ID) 
                ON DELETE CASCADE
            )
        """.trimIndent()


        db.execSQL(createLocalServerTable)
        db.execSQL(createEndDeviceTable)
        db.execSQL(createGpioTable)
        db.execSQL(createCondTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOCALSERVER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ENDDEVICE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GPIO")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_COND")
        onCreate(db)
    }

    // Insert LocalServer
    fun insertLocalServer(localServer: LocalServer) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_LOCALSERVER_NAME, localServer.name.value)
            put(COLUMN_LOCALSERVER_IP, localServer.ip)
            put(COLUMN_LOCALSERVER_MAC, localServer.mac)
        }
        db.insert(TABLE_LOCALSERVER, null, values)
    }

    // Insert EndDevice
    fun insertEndDevice(endDevice: EndDevice) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ENDDEVICE_LOCALSERVERID, endDevice.localServerId)
            put(COLUMN_ENDDEVICE_DEVIDLokal, endDevice.devIdLocal)
            put(COLUMN_ENDDEVICE_NAME, endDevice.name.value)
            put(COLUMN_ENDDEVICE_MAC, endDevice.mac)
        }
        db.insert(TABLE_ENDDEVICE, null, values)
    }

    // Insert Gpio
    fun insertGpio(gpio: Gpio) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_GPIO_GPIOTYPE, gpio.gpioType.toString())
            put(COLUMN_GPIO_DEV_ID, gpio.devId)
            put(COLUMN_GPIO_NAME, gpio.name.value)
            put(COLUMN_GPIO_RANGE, gpio.range)
            put(COLUMN_GPIO_UNIT, gpio.unit)
            put(COLUMN_GPIO_STARTVALUE, gpio.startValue)
            put(COLUMN_GPIO_SLOPE, gpio.slope)
            put(COLUMN_GPIO_VISIBILITY, gpio.visibility)
            put(COLUMN_GPIO_INPUT, gpio.input)

        }
        db.insert(TABLE_GPIO, null, values)
    }

    // Insert Cond
    fun insertCond(cond: Cond) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_COND_NAME, cond.name)
            put(COLUMN_COND_LSIDE, cond.lside)
            put(COLUMN_COND_CONDIDLOCAL, cond.condIdLocal)
            put(COLUMN_COND_RSIDE, cond.rside)
            put(COLUMN_COND_ENABLED, cond.enabled)
            put(COLUMN_COND_LOCALSERVERID, cond.localServerId)
        }
        db.insert(TABLE_COND, null, values)
    }

    // Fetch LocalServers
    fun getLocalServers(): List<LocalServer> {
        val list = mutableListOf<LocalServer>()
        val db = writableDatabase
        val sql = "SELECT * FROM $TABLE_LOCALSERVER"
        val cursor = db.rawQuery(sql, null)
        with(cursor) {
            while (moveToNext()) {
                val id = getInt(getColumnIndexOrThrow(COLUMN_LOCALSERVER_ID))
                val name = getString(getColumnIndexOrThrow(COLUMN_LOCALSERVER_NAME))
                val ip = getString(getColumnIndexOrThrow(COLUMN_LOCALSERVER_IP))
                val mac = getString(getColumnIndexOrThrow(COLUMN_LOCALSERVER_MAC))
                list.add(LocalServer(id, mutableStateOf(name), ip, mac, mutableStateOf(false)))
            }
        }
        cursor.close()
        return list
    }

    // Fetch EndDevices
    fun getEndDevices(): List<EndDevice> {
        val list = mutableListOf<EndDevice>()
        val db = writableDatabase
        val sql = "SELECT * FROM $TABLE_ENDDEVICE"
        val cursor = db.rawQuery(sql, null)
        with(cursor) {
            while (moveToNext()) {
                val id = getInt(getColumnIndexOrThrow(COLUMN_ENDDEVICE_ID))
                val localServerId = getInt(getColumnIndexOrThrow(COLUMN_ENDDEVICE_LOCALSERVERID))
                val devIdLocal = getInt(getColumnIndexOrThrow(COLUMN_ENDDEVICE_DEVIDLokal))
                val name = getString(getColumnIndexOrThrow(COLUMN_ENDDEVICE_NAME))
                val mac = getString(getColumnIndexOrThrow(COLUMN_ENDDEVICE_MAC))
                list.add(EndDevice(id, localServerId, devIdLocal, mutableStateOf(name), mac, mutableStateOf(false)))
            }
        }
        cursor.close()
        return list
    }

    // Fetch Gpios
    fun getGpios(): List<Gpio> {
        val list = mutableListOf<Gpio>()
        val db = writableDatabase
        val sql = "SELECT * FROM $TABLE_GPIO"
        val cursor = db.rawQuery(sql, null)
        with(cursor) {
            while (moveToNext()) {
                val id = getInt(getColumnIndexOrThrow(COLUMN_GPIO_ID))
                val gpioType = getString(getColumnIndexOrThrow(COLUMN_GPIO_GPIOTYPE))[0] // 'D' or 'A'
                val devId = getInt(getColumnIndexOrThrow(COLUMN_GPIO_DEV_ID))
                val name = getString(getColumnIndexOrThrow(COLUMN_GPIO_NAME))
                val range = getInt(getColumnIndexOrThrow(COLUMN_GPIO_RANGE))
                val unit = getString(getColumnIndexOrThrow(COLUMN_GPIO_UNIT))
                val startValue = getFloat(getColumnIndexOrThrow(COLUMN_GPIO_STARTVALUE))
                val slope = getFloat(getColumnIndexOrThrow(COLUMN_GPIO_SLOPE))
                val visibility = getInt(getColumnIndexOrThrow(COLUMN_GPIO_VISIBILITY)) == 1
                val input = getInt(getColumnIndexOrThrow(COLUMN_GPIO_INPUT)) == 1
                list.add(Gpio(id, gpioType, devId, mutableStateOf(name), range, unit, startValue, slope, visibility, input,
                    mutableIntStateOf(0)
                ))
            }
        }
        cursor.close()
        return list
    }

    // Fetch Conds
    fun getConds(): List<Cond> {
        val list = mutableListOf<Cond>()
        val db = writableDatabase
        val sql = "SELECT * FROM $TABLE_COND"
        val cursor = db.rawQuery(sql, null)
        with(cursor) {
            while (moveToNext()) {
                val id = getInt(getColumnIndexOrThrow(COLUMN_COND_ID))
                val condIdLocal = getInt(getColumnIndexOrThrow(COLUMN_COND_CONDIDLOCAL))
                val locServerId = getInt(getColumnIndexOrThrow(COLUMN_COND_LOCALSERVERID))
                val name = getString(getColumnIndexOrThrow(COLUMN_COND_NAME))
                val lside = getString(getColumnIndexOrThrow(COLUMN_COND_LSIDE))
                val rside = getString(getColumnIndexOrThrow(COLUMN_COND_RSIDE))
                val enabled = getInt(getColumnIndexOrThrow(COLUMN_COND_ENABLED)) == 1
                list.add(Cond(id,condIdLocal,locServerId , name, lside, rside, enabled))
            }
        }
        cursor.close()
        return list
    }

    fun deleteLocalServer(id: Int) {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_LOCALSERVER WHERE $COLUMN_LOCALSERVER_ID = $id")
    }

    // Delete by ID
    fun deleteEndDevice(id: Int) {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_ENDDEVICE WHERE $COLUMN_ENDDEVICE_ID = $id")
    }

    fun deleteGpio(id: Int) {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_GPIO WHERE $COLUMN_GPIO_ID = $id")
    }

    fun deleteCond(id: Int) {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_COND WHERE $COLUMN_COND_ID = $id")
    }


    fun updateServer(server: LocalServer) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_LOCALSERVER_NAME, server.name.value)
            put(COLUMN_LOCALSERVER_IP, server.ip)
            put(COLUMN_LOCALSERVER_MAC, server.mac)
        }

        // Use db.update to update the row with the matching ID
        db.update(
            TABLE_LOCALSERVER,
            values,
            "$COLUMN_LOCALSERVER_ID = ?",
            arrayOf(server.id.toString())
        )
    }
    fun updateEndDevice(endDevice: EndDevice) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ENDDEVICE_LOCALSERVERID, endDevice.localServerId)
            put(COLUMN_ENDDEVICE_DEVIDLokal, endDevice.devIdLocal)
            put(COLUMN_ENDDEVICE_NAME, endDevice.name.value)
            put(COLUMN_ENDDEVICE_MAC, endDevice.mac)
        }

        db.update(
            TABLE_ENDDEVICE,
            values,
            "$COLUMN_ENDDEVICE_ID = ?",
            arrayOf(endDevice.id.toString())
        )
    }
    fun updateGpio(gpio: Gpio) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_GPIO_GPIOTYPE, gpio.gpioType.toString())
            put(COLUMN_GPIO_DEV_ID, gpio.devId)
            put(COLUMN_GPIO_NAME, gpio.name.value)
            put(COLUMN_GPIO_RANGE, gpio.range)
            put(COLUMN_GPIO_UNIT, gpio.unit)
            put(COLUMN_GPIO_STARTVALUE, gpio.startValue)
            put(COLUMN_GPIO_SLOPE, gpio.slope)
            put(COLUMN_GPIO_VISIBILITY, gpio.visibility)
            put(COLUMN_GPIO_INPUT, gpio.input)
        }

        db.update(
            TABLE_GPIO,
            values,
            "$COLUMN_GPIO_ID = ? AND $COLUMN_GPIO_DEV_ID = ? AND $COLUMN_GPIO_GPIOTYPE = ?",
            arrayOf(gpio.id.toString(), gpio.devId.toString(), gpio.gpioType.toString())
        )
    }
    fun updateCond(cond: Cond) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_COND_LOCALSERVERID, cond.localServerId)
            put(COLUMN_COND_NAME, cond.name)
            put(COLUMN_COND_LSIDE, cond.lside)
            put(COLUMN_COND_RSIDE, cond.rside)
            put(COLUMN_COND_ENABLED, cond.enabled)
        }

        db.update(
            TABLE_COND,
            values,
            "$COLUMN_COND_ID = ?",
            arrayOf(cond.id.toString())
        )
    }



    fun insertEndDeviceWithGpios(endDevice: EndDevice) {
        val db = writableDatabase
        db.beginTransaction() // Start a transaction
        try {
            // Insert the device into the table
            val values = ContentValues().apply {
                put(COLUMN_ENDDEVICE_LOCALSERVERID, endDevice.localServerId)
                put(COLUMN_ENDDEVICE_DEVIDLokal, endDevice.devIdLocal)
                put(COLUMN_ENDDEVICE_NAME, endDevice.name.value)
                put(COLUMN_ENDDEVICE_MAC, endDevice.mac)
            }
            val newDevId = db.insert(TABLE_ENDDEVICE, null, values) // Insert the device and get the ID

            if (newDevId == -1L) {
                throw Exception("Failed to insert end device")
            }

            // Insert digital GPIOs (8 entries)
            for (gpioId in 1..8) {
                val gpioValues = ContentValues().apply {
                    put(COLUMN_GPIO_ID, gpioId)
                    put(COLUMN_GPIO_GPIOTYPE, "d")
                    put(COLUMN_GPIO_DEV_ID, newDevId)
                    put(COLUMN_GPIO_NAME, "") // Empty name as in procedure
                    put(COLUMN_GPIO_RANGE, 0) // EndRange
                    put(COLUMN_GPIO_UNIT, "") // Unit
                    put(COLUMN_GPIO_STARTVALUE, 0) // StartValue
                    put(COLUMN_GPIO_SLOPE, 0) // Slope
                    put(COLUMN_GPIO_VISIBILITY, false) // Assuming digital GPIOs are visible
                    put(COLUMN_GPIO_INPUT, gpioId <= 4) // Assuming GPIO input is true by default
                }
                db.insert(TABLE_GPIO, null, gpioValues)
            }

            // Insert analog GPIOs (4 entries)
            for (gpioId in 1..4) {
                val gpioValues = ContentValues().apply {
                    put(COLUMN_GPIO_ID, gpioId)
                    put(COLUMN_GPIO_GPIOTYPE, "a")
                    put(COLUMN_GPIO_DEV_ID, newDevId)
                    put(COLUMN_GPIO_NAME, "") // Empty name as in procedure
                    put(COLUMN_GPIO_RANGE, 0) // EndRange
                    put(COLUMN_GPIO_UNIT, "") // Unit
                    put(COLUMN_GPIO_STARTVALUE, 0) // StartValue
                    put(COLUMN_GPIO_SLOPE, 0) // Slope
                    put(COLUMN_GPIO_VISIBILITY, false) // Assuming analog GPIOs are visible
                    put(COLUMN_GPIO_INPUT, gpioId <= 2) // Assuming GPIO input is false for analog
                }
                db.insert(TABLE_GPIO, null, gpioValues)
            }

            db.setTransactionSuccessful() // Commit transaction if all inserts are successful
        } catch (e: Exception) {
            e.printStackTrace() // Log any errors
        } finally {
            db.endTransaction() // End the transaction
        }
    }





}
