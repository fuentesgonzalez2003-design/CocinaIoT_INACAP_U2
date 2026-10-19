package cl.inacap.cocinaiot

import cl.inacap.cocinaiot.data.SensorReading
import cl.inacap.cocinaiot.data.TelemetryCodec
import org.junit.Assert.*
import org.junit.Test

class TelemetryCodecTest {
    @Test fun thresholdIsCriticalAt50() {
        assertEquals("NORMAL", SensorReading(49.9, 22.0, "Test").condition)
        assertEquals("CRÍTICO", SensorReading(50.0, 22.0, "Test").condition)
    }
    @Test fun parsesDocumentedArduinoFrame() {
        val reading = TelemetryCodec.parseArduino("H: 54.00% | T: 24.00C | Estado: CRITICO")
        assertNotNull(reading)
        assertEquals(54.0, reading!!.humidity, 0.001)
        assertEquals("CRÍTICO", reading.condition)
    }
    @Test fun rejectsInvalidWifiFrame() {
        assertNull(TelemetryCodec.parseWifi("DATA|123.0|25|CRITICO"))
        assertNotNull(TelemetryCodec.parseWifi("DATA|46.0|22.5|NORMAL"))
    }
}
