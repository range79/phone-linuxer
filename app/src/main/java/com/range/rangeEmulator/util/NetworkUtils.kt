package com.range.rangeEmulator.util

import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {
    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in Collections.list(interfaces)) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    val host = addr.hostAddress
                    if (!addr.isLoopbackAddress && host != null && host.contains(".")) {
                        return host
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return "127.0.0.1"
    }
}