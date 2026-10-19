package cl.inacap.cocinaiot.network

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.util.UUID
import kotlin.concurrent.thread

/** HC-05/HC-06 reales: Bluetooth Classic SPP, requiere equipo Android físico. */
class BluetoothSpp(private val context: Context) {
    private val adapter get() = context.getSystemService(BluetoothManager::class.java)?.adapter
    private var socket: BluetoothSocket? = null
    private var writer: PrintWriter? = null
    private val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun hasPermission(): Boolean = Build.VERSION.SDK_INT < 31 ||
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun paired(): List<Pair<String, String>> {
        if (!hasPermission()) return emptyList()
        return adapter?.bondedDevices?.map { device ->
            (device.name ?: "Dispositivo") to device.address
        }?.sortedBy { it.first } ?: emptyList()
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String, onLine: (String) -> Unit, onStatus: (String) -> Unit) {
        if (!hasPermission()) { onStatus("Autoriza BLUETOOTH_CONNECT primero"); return }
        close()
        thread(name = "iot-bluetooth") {
            try {
                val bluetooth = adapter ?: error("Bluetooth no disponible")
                if (!bluetooth.isEnabled) error("Activa Bluetooth en Android")
                val device: BluetoothDevice = bluetooth.getRemoteDevice(address)
                bluetooth.cancelDiscovery()
                val connected = device.createRfcommSocketToServiceRecord(sppUuid)
                socket = connected
                connected.connect()
                writer = PrintWriter(connected.outputStream, true)
                onStatus("Bluetooth HC-06 conectado")
                BufferedReader(InputStreamReader(connected.inputStream)).use { input ->
                    while (connected.isConnected) {
                        val line = input.readLine() ?: break
                        onLine(line)
                    }
                }
                if (socket === connected) onStatus("Bluetooth desconectado")
            } catch (e: Exception) {
                onStatus("Error Bluetooth: ${e.message}")
            } finally { close() }
        }
    }

    fun send(command: String) {
        synchronized(this) { writer?.apply { print(command); flush() } }
    }

    fun close() {
        val old = socket
        socket = null
        writer = null
        runCatching { old?.close() }
    }
}
