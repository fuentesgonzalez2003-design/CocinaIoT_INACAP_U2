package cl.inacap.cocinaiot.data

import java.util.Locale

const val HUMIDITY_LIMIT = 50.0
const val WIFI_PORT = 8765

data class SensorReading(
    val humidity: Double,
    val temperature: Double,
    val source: String,
    val receivedAt: Long = System.currentTimeMillis()
) {
    val condition: String get() = if (humidity >= HUMIDITY_LIMIT) "CRÍTICO" else "NORMAL"
    val humidityText: String get() = String.format(Locale.US, "%.1f %%", humidity)
    val temperatureText: String get() = String.format(Locale.US, "%.1f °C", temperature)
}

data class ReadingRecord(
    val dateTime: Long,
    val humidity: Double,
    val temperature: Double,
    val condition: String,
    val source: String
)

object TelemetryCodec {
    // Trama para la simulación entre emuladores (TCP por Wi-Fi virtual).
    fun wifiFrame(reading: SensorReading): String =
        "DATA|${reading.humidity}|${reading.temperature}|${reading.condition}"

    fun parseWifi(line: String): SensorReading? {
        val pieces = line.trim().split('|')
        if (pieces.size != 4 || pieces[0] != "DATA") return null
        val h = pieces[1].toDoubleOrNull() ?: return null
        val t = pieces[2].toDoubleOrNull() ?: return null
        if (!valid(h, t)) return null
        return SensorReading(h, t, "Wi-Fi (emulador)")
    }

    // Formato documentado en el código Arduino de la Unidad 1:
    // H: 48.00% | T: 23.00C | Estado: NORMAL
    private val arduinoPattern = Regex(
        """H:\s*(-?\d+(?:\.\d+)?)%\s*\|\s*T:\s*(-?\d+(?:\.\d+)?)C\s*\|\s*Estado:\s*(NORMAL|CRITICO|CRÍTICO)""",
        RegexOption.IGNORE_CASE
    )

    fun parseArduino(line: String): SensorReading? {
        val match = arduinoPattern.find(line.trim()) ?: return null
        val h = match.groupValues[1].toDoubleOrNull() ?: return null
        val t = match.groupValues[2].toDoubleOrNull() ?: return null
        if (!valid(h, t)) return null
        return SensorReading(h, t, "Bluetooth HC-06")
    }

    private fun valid(h: Double, t: Double) =
        h.isFinite() && h in 0.0..100.0 && t.isFinite() && t in -40.0..85.0
}
