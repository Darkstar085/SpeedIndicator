package com.sipun.netspeedindicator.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NetworkMonitor @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private const val TAG = "NetworkMonitor"
        private val MONITORED_TRANSPORTS = intArrayOf(
            NetworkCapabilities.TRANSPORT_WIFI,
            NetworkCapabilities.TRANSPORT_CELLULAR,
            NetworkCapabilities.TRANSPORT_ETHERNET,
            NetworkCapabilities.TRANSPORT_BLUETOOTH,
            NetworkCapabilities.TRANSPORT_USB
        )
    }

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    @Volatile
    private var defaultNetwork: Network? = null

    @Volatile
    private var defaultCapabilities: NetworkCapabilities? = null

    fun start(
        onNetworkUnavailable: () -> Unit,
        onNetworkAvailable: () -> Unit = {}
    ) {
        stop()

        defaultNetwork = connectivityManager.activeNetwork
        defaultCapabilities = defaultNetwork?.let {
            runCatching { connectivityManager.getNetworkCapabilities(it) }.getOrNull()
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                defaultNetwork = network
                defaultCapabilities = runCatching {
                    connectivityManager.getNetworkCapabilities(network)
                }.getOrNull()
                if (isNetworkUsable(defaultCapabilities)) {
                    onNetworkAvailable()
                }
            }

            override fun onUnavailable() {
                defaultNetwork = null
                defaultCapabilities = null
                onNetworkUnavailable()
            }

            override fun onLost(network: Network) {
                if (defaultNetwork == network) {
                    defaultNetwork = null
                    defaultCapabilities = null
                }
                if (!hasValidatedNetwork()) {
                    onNetworkUnavailable()
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                defaultNetwork = network
                defaultCapabilities = networkCapabilities
                if (isNetworkUsable(networkCapabilities)) {
                    onNetworkAvailable()
                } else {
                    onNetworkUnavailable()
                }
            }
        }

        networkCallback = callback
        try {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } catch (error: Exception) {
            networkCallback = null
            Log.w(TAG, "Unable to register network callback", error)
        }
    }

    fun stop() {
        networkCallback?.let { callback ->
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {
            }
        }
        networkCallback = null
        defaultNetwork = null
        defaultCapabilities = null
    }

    fun hasValidatedNetwork(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork
        if (activeNetwork != null) {
            val activeCaps = runCatching {
                connectivityManager.getNetworkCapabilities(activeNetwork)
            }.getOrNull()
            if (isNetworkUsable(activeCaps)) return true
            if (defaultNetwork != null && defaultNetwork != activeNetwork) {
                defaultNetwork = null
                defaultCapabilities = null
            }
        } else if (defaultNetwork != null) {
            val trackedCaps = runCatching {
                connectivityManager.getNetworkCapabilities(defaultNetwork)
            }.getOrNull()
            if (isNetworkUsable(trackedCaps)) return true
            defaultNetwork = null
            defaultCapabilities = null
        }

        return isNetworkUsable(defaultCapabilities)
    }

    private fun isNetworkUsable(capabilities: NetworkCapabilities?): Boolean {
        if (capabilities == null) return false
        val hasRealTransport = MONITORED_TRANSPORTS.any { capabilities.hasTransport(it) }
        if (!hasRealTransport) return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
