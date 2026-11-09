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
    }

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    fun start(onNetworkUnavailable: () -> Unit) {
        stop()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onLost(network: Network) {
                notifyIfUnavailable(onNetworkUnavailable)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                if (!networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    notifyIfUnavailable(onNetworkUnavailable)
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
    }

    fun hasValidatedNetwork(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun notifyIfUnavailable(onNetworkUnavailable: () -> Unit) {
        if (!hasValidatedNetwork()) onNetworkUnavailable()
    }
}
