package net.ankio.bluetooth.utils

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import net.ankio.bluetooth.ble.BluetoothData
import net.ankio.webdav.lib.WebDav
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WebdavUtils(private val context: Context) {

    private val client = WebDav.client(context)

    suspend fun sendToServer(bluetoothData: BluetoothData) {
        Log.i(TAG, "Sending Bluetooth data to WebDAV")
        ensureBluetoothDir()
        val json = Gson().toJson(bluetoothData)
        if (client.exists(BLUETOOTH_FILE)) {
            client.delete(BLUETOOTH_FILE)
        }
        client.writeBytes(BLUETOOTH_FILE, json.toByteArray(), "application/json")
        SpUtils.putString(PrefKeys.WEBDAV_LAST, now())
    }

    suspend fun getFromServer(): BluetoothData? {
        if (!client.exists(BLUETOOTH_FILE)) return null
        SpUtils.putString(PrefKeys.WEBDAV_LAST, now())
        return Gson().fromJson(client.readText(BLUETOOTH_FILE), BluetoothData::class.java)
    }

    private suspend fun ensureBluetoothDir() {
        if (!client.exists(BLUETOOTH_DIR)) {
            client.mkdirs(BLUETOOTH_DIR)
        }
    }

    private fun now(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatter.format(Date())
    }

    companion object {
        private const val TAG = "WebdavUtils"
        private const val BLUETOOTH_DIR = "/bluetooth"
        private const val BLUETOOTH_FILE = "/bluetooth/bluetooth.json"
    }
}
