package com.range.rangeEmulator.util

import android.content.Context
import android.os.Build
import android.os.HardwarePropertiesManager
import java.io.File
import java.io.InputStreamReader

object SystemMonitor {

    private val CPU_TEMP_TYPES = listOf(
        "tsens_tz_sensor",
        "cpu-",
        "soc_thermal",
        "mtktscpu",
        "exynos-therm"
    )

    fun getCpuUsage(): Int {
        return try {
            val process = Runtime.getRuntime().exec("top -b -n 1")
            val reader = InputStreamReader(process.inputStream).buffered()
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                if (line?.contains("User") == true && line?.contains("System") == true) {
                    val user = parsePercent(line!!, "User")
                    val system = parsePercent(line!!, "System")
                    return (user + system).coerceIn(0, 100)
                }
            }
            0
        } catch (e: Exception) {
            0
        }
    }

    private fun parsePercent(line: String, label: String): Int {
        return try {
            val startIndex = line.indexOf(label) + label.length
            val part = line.substring(startIndex).trim()
            val endIndex = part.indexOf("%")
            part.substring(0, endIndex).trim().toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun getCpuTemperature(context: Context): Int {
        return try {
            val thermalDir = File("/sys/class/thermal")
            val zones = thermalDir.listFiles { _, name -> name.startsWith("thermal_zone") } ?: emptyArray()
            
            var bestTypeRank = Int.MAX_VALUE
            var bestTemp = -1

            for (zone in zones) {
                try {
                    val type = File(zone, "type").readText().trim().lowercase()
                    val temp = File(zone, "temp").readText().trim().toInt()
                    
                    val rank = CPU_TEMP_TYPES.indexOfFirst { type.contains(it) }
                    if (rank != -1 && rank < bestTypeRank) {
                        bestTypeRank = rank
                        bestTemp = if (temp > 1000) temp / 1000 else temp
                    } else if (bestTemp == -1 && (type.contains("cpu") || type.contains("soc"))) {
                        bestTemp = if (temp > 1000) temp / 1000 else temp
                    }
                } catch (e: Exception) { continue }
            }

            if (bestTemp > 0) return bestTemp

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val hpm = context.getSystemService(Context.HARDWARE_PROPERTIES_SERVICE) as? HardwarePropertiesManager
                val temps = hpm?.getDeviceTemperatures(HardwarePropertiesManager.DEVICE_TEMPERATURE_CPU, HardwarePropertiesManager.TEMPERATURE_CURRENT)
                if (temps != null && temps.isNotEmpty() && temps[0] > 0) return temps[0].toInt()
            }

            HardwareUtil.getBatteryTemperature(context)
        } catch (e: Exception) {
            0
        }
    }

    fun getThermalStatus(context: Context): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                when (powerManager.currentThermalStatus) {
                    android.os.PowerManager.THERMAL_STATUS_NONE -> "Cool"
                    android.os.PowerManager.THERMAL_STATUS_LIGHT -> "Warm"
                    android.os.PowerManager.THERMAL_STATUS_MODERATE -> "Moderate"
                    android.os.PowerManager.THERMAL_STATUS_SEVERE -> "Throttling"
                    android.os.PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
                    android.os.PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
                    android.os.PowerManager.THERMAL_STATUS_SHUTDOWN -> "Overheat"
                    else -> "Safe"
                }
            } else "Safe"
        } catch (e: Exception) { "Unknown" }
    }
}
