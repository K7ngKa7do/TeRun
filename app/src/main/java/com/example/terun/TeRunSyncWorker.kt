package com.example.terun

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * =====================================================================
 * TeRunSyncWorker – Hintergrundaufgabe für die Offline-Synchronisation
 * =====================================================================
 *
 * VORLESUNG 27 – WorkManager (Hintergrundaufgaben planen):
 * WorkManager ist die empfohlene Lösung für Aufgaben, die:
 * - garantiert ausgeführt werden müssen (auch wenn die App geschlossen wird)
 * - von bestimmten Bedingungen abhängen (z.B. "nur wenn Internet vorhanden")
 * - persistiert werden sollen (überleben App-Neustarts und Geräte-Neustarts)
 *
 * CoroutineWorker (statt einfachem Worker):
 * - Unterstützt Kotlin Coroutines nativ (suspend-Funktionen möglich)
 * - doWork() läuft automatisch auf einem Hintergrundthread (kein UI-Thread blockiert)
 * - Gibt Result.success() zurück wenn erfolgreich, Result.retry() bei Fehler
 *
 * Was macht dieser Worker?
 * Wenn das Gerät offline war und wieder eine Internetverbindung bekommt,
 * lädt dieser Worker alle lokal erstellten Duelle und Profildaten
 * automatisch in die Firestore-Datenbank hoch (Offline-First-Strategie, VL 48).
 *
 * Wann wird dieser Worker ausgeführt?
 * Er wird in SpielRepository.scheduleOfflineSync() geplant mit der Einschränkung:
 * "Nur ausführen wenn eine Netzwerkverbindung besteht" (NetworkType.CONNECTED)
 *
 * Hinweis: WorkManager.getInstance(context) wird vom Framework verwaltet.
 * Der Worker bekommt einen App-Kontext übergeben, keinen Activity-Kontext.
 */
class TeRunSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    /**
     * Hier wird die eigentliche Arbeit definiert (VL 27: "override doWork()").
     * withContext(Dispatchers.IO) → Datenbankoperationen auf IO-Thread ausführen
     * Gibt Result.success() zurück wenn alles geklappt hat.
     * Gibt Result.retry() zurück wenn ein Fehler aufgetreten ist
     * → WorkManager versucht es später automatisch erneut.
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Repository erstellen und Offline-Daten mit Firestore synchronisieren
            val repository = SpielRepository(applicationContext)
            repository.synchronisiereLokaleDaten()

            Result.success() // Erfolgreich abgeschlossen
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry() // Fehler aufgetreten → WorkManager versucht es erneut
        }
    }
}
