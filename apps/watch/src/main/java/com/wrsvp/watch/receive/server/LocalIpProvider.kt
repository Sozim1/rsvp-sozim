package com.wrsvp.watch.receive.server

import java.net.NetworkInterface
import javax.inject.Inject

class LocalIpProvider @Inject constructor() {
    fun localIp(): String {
        return localIps().firstOrNull { it.startsWith("192.168.") || it.startsWith("10.") || it.startsWith("172.") }
            ?: localIps().firstOrNull()
            ?: "0.0.0.0"
    }

    private fun localIps(): List<String> {
        return NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { network -> network.inetAddresses.toList() }
            .mapNotNull { address -> address.hostAddress?.takeIf { !address.isLoopbackAddress && it.indexOf(':') < 0 } }
            .distinct()
    }
}
