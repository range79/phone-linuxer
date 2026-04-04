package com.range.rangeEmulator.util

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.HardwarePropertiesManager
import android.os.StatFs
import java.io.File

object HardwareUtil {

    fun isKvmSupported(): Boolean {
        return try {
            val kvmDevice = File("/dev/kvm")
            kvmDevice.exists() && kvmDevice.canRead()
        } catch (e: Exception) {
            false
        }
    }

    fun isGpuAccelerationSupported(context: Context): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val configurationInfo = activityManager.deviceConfigurationInfo
            configurationInfo.reqGlEsVersion >= 0x30000
        } catch (e: Exception) {
            false
        }
    }

    fun getTotalRamMB(context: Context): Int {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            (memInfo.totalMem / (1024 * 1024)).toInt()
        } catch (e: Exception) {
            2048
        }
    }

    fun getTotalCores(): Int {
        return try {
            Runtime.getRuntime().availableProcessors()
        } catch (e: Exception) {
            4
        }
    }

    fun getAvailableInternalStorageGB(context: Context): Float {
        return try {
            val stat = StatFs(context.filesDir.absolutePath)
            val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
            bytesAvailable / (1024f * 1024f * 1024f)
        } catch (e: Exception) {
            0f
        }
    }

    fun getSafeStorageLimitGB(context: Context): Float {
        val available = getAvailableInternalStorageGB(context)
        return (available * 0.9f).coerceAtLeast(0f)
    }

    fun isEngineVirglSupported(context: Context): Boolean {
        return try {
            val engineFile = File(context.filesDir, "libqemu_system.so")
            if (!engineFile.exists()) return true

            val searchStr = "virtio-gpu-gl".toByteArray()
            engineFile.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    for (i in 0 until bytesRead - searchStr.size) {
                        var found = true
                        for (j in searchStr.indices) {
                            if (buffer[i + j] != searchStr[j]) {
                                found = false
                                break
                            }
                        }
                        if (found) return true
                    }
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    fun getMemoryInfo(context: Context): Pair<Long, Long> {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return (memoryInfo.totalMem - memoryInfo.availMem) to memoryInfo.totalMem
    }

    fun getBatteryTemperature(context: Context): Int {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            if (temp > 0) (temp / 10) else 0
        } catch (t: Throwable) { 0 }
    }
}