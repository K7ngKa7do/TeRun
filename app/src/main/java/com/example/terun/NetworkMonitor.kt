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
 * =====================================================================
 * NetworkMonitor – Echtzeit-Überwachung der Netzwerkverbindung
 * =====================================================================
 *
 * VORLESUNG 48 – Strategien für Datentransfer (Data Strategies):
 * Eine "Offline-First"-App funktioniert auch ohne Internet (durch lokale Daten),
 * synchronisiert aber sobald wieder eine Verbindung besteht.
 *
 * Diese Klasse erkennt in Echtzeit, ob das Gerät online oder offline ist.
 * Sobald das Gerät wieder online geht, kann die App automatisch synchronisieren
 * (WorkManager wird dann gestartet → VL 27).
 *
 * Moderner Ansatz mit ConnectivityManager.NetworkCallback:
 * - Android empfiehlt diesen Ansatz statt des veralteten BroadcastReceivers (VL 39)
 * - Der NetworkCallback wird vom System aufgerufen sobald sich der Netzwerkstatus ändert
 * - Spart Batterielaufzeit im Vergleich zu regelmäßigem Pollen (aktives Abfragen)
 *
 * StateFlow (Kotlin Coroutines):
 * - Hält den aktuellen Online-Status als beobachtbaren Wert
 * - Composables und das Repository können diesen Wert live beobachten
 * - MutableStateFlow = intern veränderbar, nach außen nur lesbar (asStateFlow)
 */
class NetworkMonitor(context: Context) {

    // System-Service für Netzwerkabfragen
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Interner (veränderbarer) Online-Status → wird vom NetworkCallback aktualisiert
    private val _isOnline = MutableStateFlow(false)

    // Öffentlicher (nur lesbarer) Online-Status für andere Klassen (Repository, ViewModel)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        // Beim Erstellen den aktuellen Status sofort ermitteln (nicht warten)
        _isOnline.value = checkCurrentlyOnline()

        // Netzwerk-Anforderung definieren: Wir wollen über jede Verbindung MIT Internet benachrichtigt werden
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            // NetworkCallback beim System anmelden
            // → onAvailable() wird aufgerufen wenn Netzwerk verbunden wird
            // → onLost() wird aufgerufen wenn Netzwerk getrennt wird
            connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {

                override fun onAvailable(network: Network) {
                    // Gerät ist wieder online
                    _isOnline.value = true
                }

                override fun onLost(network: Network) {
                    // Netzwerk verloren → erneut prüfen ob noch eine andere Verbindung aktiv ist
                    _isOnline.value = checkCurrentlyOnline()
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Prüft ob das Gerät gerade eine aktive Internetverbindung hat.
     * Gibt true zurück wenn ja, false wenn keine Verbindung besteht.
     */
    private fun checkCurrentlyOnline(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
