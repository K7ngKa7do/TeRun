package com.example.terun

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * NetworkMonitor — Überwacht die Netzwerkverbindung in Echtzeit (basiert auf VL 11: Wireless Networks).
 * Nutzt den modernen ConnectivityManager.NetworkCallback statt des veralteten NetworkInfo,
 * um Batterieladung zu schonen und Reaktivität zu erhöhen. Exponiert den On-/Offline-Status als Flow.
 */
class NetworkMonitor(context: Context) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        // Initialen Status beim Erstellen ermitteln
        _isOnline.value = checkCurrentlyOnline()

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.value = true
                }

                override fun onLost(network: Network) {
                    _isOnline.value = checkCurrentlyOnline()
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Hilfsfunktion zur Ermittlung des aktuellen Status
    private fun checkCurrentlyOnline(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
