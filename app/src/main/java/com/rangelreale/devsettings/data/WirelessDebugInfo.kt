package com.rangelreale.devsettings.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.Inet4Address
import kotlin.coroutines.resume

/** Finds the phone's Wi-Fi address and the port that wireless debugging listens on. */
object WirelessDebugInfo {

    /** mDNS service type that adbd advertises while wireless debugging is on. */
    private const val ADB_TLS_SERVICE = "_adb-tls-connect._tcp"

    /** IPv4 address of the current Wi-Fi network, or null when not on Wi-Fi. */
    fun wifiIpAddress(context: Context): String? {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val network = cm.activeNetwork ?: return null
        val caps = cm.getNetworkCapabilities(network) ?: return null
        if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null
        return cm.getLinkProperties(network)?.linkAddresses
            ?.map { it.address }
            ?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress
    }

    /**
     * Port of the wireless debugging service on this phone, or null when it cannot be found.
     * Reads the adbd system property first, then falls back to mDNS discovery.
     */
    suspend fun findPort(context: Context, ipAddress: String): Int? =
        readPortProperty() ?: discoverPort(context, ipAddress)

    private suspend fun readPortProperty(): Int? = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("getprop", "service.adb.tls.port").start()
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            process.waitFor()
            output.toIntOrNull()?.takeIf { it > 0 }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun discoverPort(context: Context, ipAddress: String): Int? {
        val nsd = context.getSystemService(NsdManager::class.java) ?: return null
        val found = Channel<NsdServiceInfo>(Channel.UNLIMITED)
        val listener = object : NsdManager.DiscoveryListener {
            override fun onServiceFound(info: NsdServiceInfo) {
                found.trySend(info)
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                found.close()
            }

            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onServiceLost(info: NsdServiceInfo) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        }

        nsd.discoverServices(ADB_TLS_SERVICE, NsdManager.PROTOCOL_DNS_SD, listener)
        return try {
            withTimeoutOrNull(5_000) {
                // Other devices on the network may advertise the same service. Keep the one on this phone.
                for (info in found) {
                    val resolved = resolve(nsd, info) ?: continue
                    @Suppress("DEPRECATION")
                    if (resolved.host?.hostAddress == ipAddress) return@withTimeoutOrNull resolved.port
                }
                null
            }
        } finally {
            try {
                nsd.stopServiceDiscovery(listener)
            } catch (e: IllegalArgumentException) {
                // Discovery never started.
            }
        }
    }

    @Suppress("DEPRECATION") // resolveService still works and keeps the code the same on API 30 to 36.
    private suspend fun resolve(nsd: NsdManager, info: NsdServiceInfo): NsdServiceInfo? =
        suspendCancellableCoroutine { cont ->
            nsd.resolveService(
                info,
                object : NsdManager.ResolveListener {
                    override fun onServiceResolved(resolved: NsdServiceInfo) {
                        if (cont.isActive) cont.resume(resolved)
                    }

                    override fun onResolveFailed(failed: NsdServiceInfo, errorCode: Int) {
                        if (cont.isActive) cont.resume(null)
                    }
                },
            )
        }
}
