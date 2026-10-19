package cl.inacap.cocinaiot.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.inacap.cocinaiot.data.HistoryDb
import cl.inacap.cocinaiot.data.LocalAuth
import cl.inacap.cocinaiot.data.ReadingRecord
import cl.inacap.cocinaiot.data.SensorReading
import cl.inacap.cocinaiot.data.TelemetryCodec
import cl.inacap.cocinaiot.network.BluetoothSpp
import cl.inacap.cocinaiot.network.WifiTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.sin

/** Estado único para la interfaz: presentación, acceso, información, monitor y control. */
enum class Page { START, LOGIN, INFO, MONITOR, CONTROL }
enum class LinkMode { NONE, SIMULATOR, WIFI, BLUETOOTH }

data class AppState(
    val page: Page = Page.START,
    val registered: Boolean = false,
    val authenticated: Boolean = false,
    val username: String = "",
    val authMessage: String = "",
    val mode: LinkMode = LinkMode.NONE,
    val connectionMessage: String = "Sin conexión",
    val ipAddress: String = "",
    val wifiTarget: String = "",
    val bluetoothDevices: List<Pair<String, String>> = emptyList(),
    val reading: SensorReading? = null,
    val greenLedOn: Boolean = false,
    val history: List<ReadingRecord> = emptyList()
)

class CocinaViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = LocalAuth(application)
    private val historyDb = HistoryDb(application)
    private val wifi = WifiTransport()
    private val bluetooth = BluetoothSpp(application)
    private var simulatorJob: Job? = null

    private val mutableState = MutableStateFlow(
        AppState(registered = auth.hasUser, username = auth.username, ipAddress = WifiTransport.wifiIp())
    )
    val state: StateFlow<AppState> = mutableState.asStateFlow()

    init { refreshHistory() }

    fun open(page: Page) {
        if (!state.value.authenticated && page !in setOf(Page.START, Page.LOGIN)) return
        mutableState.update { it.copy(page = page, authMessage = "") }
    }

    fun register(user: String, pass: String) {
        viewModelScope.launch {
            val error = withContext(Dispatchers.IO) { auth.register(user, pass) }
            mutableState.update {
                it.copy(registered = auth.hasUser, username = auth.username,
                    authMessage = error ?: "Usuario creado. Ingresa con tus credenciales.")
            }
        }
    }

    fun login(user: String, pass: String) {
        viewModelScope.launch {
            val valid = withContext(Dispatchers.Default) { auth.login(user, pass) }
            mutableState.update {
                if (valid) it.copy(authenticated = true, page = Page.INFO,
                    username = auth.username, authMessage = "")
                else it.copy(authMessage = "Usuario o contraseña incorrectos.")
            }
        }
    }

    fun logout() {
        stopAll()
        mutableState.update {
            it.copy(page = Page.START, authenticated = false, mode = LinkMode.NONE,
                connectionMessage = "Sin conexión", reading = null, greenLedOn = false,
                bluetoothDevices = emptyList())
        }
    }

    fun setWifiTarget(text: String) {
        mutableState.update { it.copy(wifiTarget = text) }
    }

    /** El segundo emulador hace de Arduino + sensor. */
    fun startSimulator() {
        stopAll()
        mutableState.update {
            it.copy(mode = LinkMode.SIMULATOR, connectionMessage = "Preparando simulador...",
                greenLedOn = false, ipAddress = WifiTransport.wifiIp())
        }
        wifi.startServer(onCommand = { value ->
            if (value == "E" || value == "A") {
                val on = value == "E"
                mutableState.update { it.copy(greenLedOn = on) }
                wifi.broadcast("LED|${if (on) "ON" else "OFF"}")
            }
        }, onStatus = ::setConnectionStatus)
        simulatorJob = viewModelScope.launch(Dispatchers.IO) {
            var tick = 0
            while (isActive) {
                // Datos DEMO: no son medidas reales del DHT11.
                val h = (49.0 + 14.0 * sin(tick / 5.0)).coerceIn(15.0, 85.0)
                val t = 23.0 + 2.2 * sin(tick / 7.0)
                val reading = SensorReading(
                    String.format(Locale.US, "%.1f", h).toDouble(),
                    String.format(Locale.US, "%.1f", t).toDouble(),
                    "Simulado"
                )
                acceptReading(reading)
                wifi.broadcast(TelemetryCodec.wifiFrame(reading))
                tick++
                delay(2000)
            }
        }
    }

    fun connectWifi() {
        val target = state.value.wifiTarget.trim()
        val valid = Regex("(?:\\d{1,3}\\.){3}\\d{1,3}").matches(target) &&
            target.split('.').all { (it.toIntOrNull() ?: 256) in 0..255 }
        if (!valid) {
            setConnectionStatus("Escribe una dirección IPv4 válida del simulador.")
            return
        }
        stopAll()
        mutableState.update { it.copy(mode = LinkMode.WIFI, connectionMessage = "Conectando a $target...") }
        wifi.connect(target, onLine = { line ->
            when {
                line.startsWith("DATA|") -> TelemetryCodec.parseWifi(line)?.let(::acceptReading)
                line == "LED|ON" -> mutableState.update { it.copy(greenLedOn = true) }
                line == "LED|OFF" -> mutableState.update { it.copy(greenLedOn = false) }
            }
        }, onStatus = ::setConnectionStatus)
    }

    fun bluetoothPermissionGranted(): Boolean = bluetooth.hasPermission()

    fun prepareBluetooth() {
        if (!bluetooth.hasPermission()) {
            setConnectionStatus("Autoriza el permiso de Bluetooth primero.")
            return
        }
        val paired = bluetooth.paired()
        mutableState.update {
            it.copy(bluetoothDevices = paired,
                connectionMessage = if (paired.isEmpty()) "No hay dispositivos emparejados. Empareja el HC-06 en Ajustes de Android." else "Selecciona el HC-06 emparejado.")
        }
    }

    fun connectBluetooth(mac: String) {
        stopAll()
        mutableState.update { it.copy(mode = LinkMode.BLUETOOTH, connectionMessage = "Conectando al HC-06...") }
        bluetooth.connect(mac, onLine = { raw ->
            TelemetryCodec.parseArduino(raw)?.let(::acceptReading)
        }, onStatus = ::setConnectionStatus)
    }

    fun sendLed(on: Boolean) {
        when (state.value.mode) {
            LinkMode.WIFI -> viewModelScope.launch(Dispatchers.IO) { wifi.sendCommand(if (on) "E" else "A") }
            LinkMode.BLUETOOTH -> viewModelScope.launch(Dispatchers.IO) { bluetooth.send(if (on) "E" else "A") }
            LinkMode.SIMULATOR -> mutableState.update { it.copy(greenLedOn = on) }
            LinkMode.NONE -> setConnectionStatus("Primero conecta un monitor o inicia el simulador.")
        }
    }

    fun disconnect() {
        stopAll()
        mutableState.update { it.copy(mode = LinkMode.NONE, connectionMessage = "Desconectado", greenLedOn = false) }
    }

    private fun setConnectionStatus(text: String) {
        mutableState.update { it.copy(connectionMessage = text) }
    }

    private fun acceptReading(reading: SensorReading) {
        mutableState.update { it.copy(reading = reading) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                historyDb.insert(reading)
                val newHistory = historyDb.latest()
                mutableState.update { it.copy(history = newHistory) }
            } catch (e: Exception) {
                setConnectionStatus("No se pudo guardar la lectura: ${e.message}")
            }
        }
    }

    private fun refreshHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { historyDb.latest() }.onSuccess { readings ->
                mutableState.update { it.copy(history = readings) }
            }
        }
    }

    private fun stopAll() {
        simulatorJob?.cancel()
        simulatorJob = null
        wifi.close()
        bluetooth.close()
    }

    override fun onCleared() {
        stopAll()
        historyDb.close()
        super.onCleared()
    }
}
