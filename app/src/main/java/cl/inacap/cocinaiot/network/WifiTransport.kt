package cl.inacap.cocinaiot.network

import cl.inacap.cocinaiot.data.WIFI_PORT
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/** TCP sencillo solo para demostración entre dos emuladores en Wi-Fi virtual. Sin TLS. */
class WifiTransport {
    private var server: ServerSocket? = null
    @Volatile private var peer: Socket? = null
    @Volatile private var client: Socket? = null
    @Volatile private var peerOutput: PrintWriter? = null
    @Volatile private var clientOutput: PrintWriter? = null
    private val serverRunning = AtomicBoolean(false)

    fun startServer(onCommand: (String) -> Unit, onStatus: (String) -> Unit) {
        if (!serverRunning.compareAndSet(false, true)) return
        thread(name = "iot-wifi-server") {
            try {
                ServerSocket(WIFI_PORT).use { socketServer ->
                    server = socketServer
                    onStatus("Servidor disponible en puerto $WIFI_PORT")
                    while (serverRunning.get()) {
                        val socket = socketServer.accept()
                        peer?.close()
                        peer = socket
                        peerOutput = PrintWriter(socket.getOutputStream(), true)
                        onStatus("Monitor conectado por Wi-Fi")
                        try {
                            BufferedReader(InputStreamReader(socket.getInputStream())).use { input ->
                                while (serverRunning.get()) {
                                    val command = input.readLine() ?: break
                                    onCommand(command.trim())
                                }
                            }
                        } catch (_: Exception) { /* Fin de sesión del monitor */ }
                        finally {
                            peerOutput = null
                            peer = null
                            runCatching { socket.close() }
                            if (serverRunning.get()) onStatus("Esperando conexión Wi-Fi")
                        }
                    }
                }
            } catch (e: Exception) {
                if (serverRunning.get()) onStatus("Error del servidor: ${e.message}")
            } finally {
                serverRunning.set(false)
                server = null
            }
        }
    }

    fun broadcast(message: String) {
        synchronized(this) { peerOutput?.println(message) }
    }

    fun connect(ip: String, onLine: (String) -> Unit, onStatus: (String) -> Unit) {
        closeClient()
        thread(name = "iot-wifi-client") {
            val socket = Socket()
            try {
                socket.connect(InetSocketAddress(ip, WIFI_PORT), 5000)
                client = socket
                clientOutput = PrintWriter(socket.getOutputStream(), true)
                onStatus("Conectado a $ip:$WIFI_PORT")
                BufferedReader(InputStreamReader(socket.getInputStream())).use { input ->
                    while (!socket.isClosed) {
                        val line = input.readLine() ?: break
                        onLine(line)
                    }
                }
                if (client === socket) onStatus("Conexión Wi-Fi cerrada")
            } catch (e: Exception) {
                if (client === socket || client == null) onStatus("No se pudo conectar: ${e.message}")
            } finally {
                if (client === socket) { client = null; clientOutput = null }
                runCatching { socket.close() }
            }
        }
    }

    fun sendCommand(command: String) {
        synchronized(this) { clientOutput?.println(command) }
    }

    fun closeClient() {
        val toClose = client
        client = null
        clientOutput = null
        runCatching { toClose?.close() }
    }

    fun stopServer() {
        serverRunning.set(false)
        runCatching { peer?.close() }
        runCatching { server?.close() }
        peer = null
        peerOutput = null
        server = null
    }

    fun close() { closeClient(); stopServer() }

    companion object {
        fun wifiIp(): String {
            return try {
                val network = NetworkInterface.getByName("wlan0")
                val ip = network?.inetAddresses?.toList()?.firstOrNull {
                    it is Inet4Address && !it.isLoopbackAddress
                }
                ip?.hostAddress ?: "IP Wi-Fi no disponible"
            } catch (_: Exception) { "IP Wi-Fi no disponible" }
        }
    }
}
