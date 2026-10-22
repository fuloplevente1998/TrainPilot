package com.repforge.app

import java.util.UUID

data class RdfitProbeResult(
    val stepsCount: Int? = null,
    val batteryPercent: Int? = null
)

/**
 * Szabványos Bluetooth SIG és RDFit specifikus UUID-k és adatcsomag elemzők.
 */
object BleWatchAndProtocols {
    // Bluetooth SIG szabványos Heart Rate Service (0x180D) és Measurement (0x2A37)
    val HEART_RATE_SERVICE_UUID: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
    val HEART_RATE_MEASUREMENT_UUID: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // RDFit okosóra vendor szolgáltatás és karakterisztikák (0xED)
    val RDFIT_SERVICE_UUID: UUID = UUID.fromString("0000fee7-0000-1000-8000-00805f9b34fb")
    val RDFIT_WRITE_CHAR_UUID: UUID = UUID.fromString("0000fea1-0000-1000-8000-00805f9b34fb")
    val RDFIT_NOTIFY_CHAR_UUID: UUID = UUID.fromString("0000fea2-0000-1000-8000-00805f9b34fb")

    /**
     * 0x2A37 Heart Rate Measurement csomag dekódolása.
     * Flag byte: bit 0 = 0 -> UINT8 pulzus, bit 0 = 1 -> UINT16 pulzus.
     */
    fun parseHeartRate(data: ByteArray?): Int? {
        if (data == null || data.isEmpty()) return null
        val flags = data[0].toInt()
        val is16Bit = (flags and 0x01) != 0

        return if (is16Bit) {
            if (data.size < 3) return null
            val lsb = data[1].toInt() and 0xFF
            val msb = data[2].toInt() and 0xFF
            (msb shl 8) or lsb
        } else {
            if (data.size < 2) return null
            data[1].toInt() and 0xFF
        }
    }

    /**
     * RDFit mérési adatok (0xED) lekérdező parancs összeállítása.
     */
    fun buildRdfitQueryPacket(): ByteArray {
        return byteArrayOf(
            0xAB.toByte(), 0x00.toByte(), 0x04.toByte(), 0xFF.toByte(),
            0x31.toByte(), 0x80.toByte(), 0x01.toByte(), 0xED.toByte()
        )
    }

    /**
     * RDFit válaszcsomag dekódolása (lépések és akkumulátor töltöttség).
     */
    fun parseRdfitResponse(data: ByteArray?): RdfitProbeResult {
        if (data == null || data.size < 6) return RdfitProbeResult()

        var steps: Int? = null
        var battery: Int? = null

        try {
            // Lépésszám és teleptöltöttség kinyerése a válaszból
            if ((data[0].toInt() and 0xFF) == 0xAB) {
                if (data.size >= 8) {
                    val b1 = data[4].toInt() and 0xFF
                    val b2 = data[5].toInt() and 0xFF
                    val b3 = data[6].toInt() and 0xFF
                    val calculatedSteps = (b1 shl 16) or (b2 shl 8) or b3
                    if (calculatedSteps in 0..200000) {
                        steps = calculatedSteps
                    }
                }
                if (data.size >= 9) {
                    val bat = data[7].toInt() and 0xFF
                    if (bat in 0..100) {
                        battery = bat
                    }
                }
            }
        } catch (_: Exception) {}

        return RdfitProbeResult(stepsCount = steps, batteryPercent = battery)
    }
}
