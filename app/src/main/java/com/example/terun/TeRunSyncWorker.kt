package com.example.terun

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * TeRunSyncWorker — Garantierte Synchronisation von Offline-Daten mit Firestore (VL 6: WorkManager).
 * Wird ausgeführt, sobald wieder eine Internetverbindung besteht. Lädt alle lokal erstellten
 * Duelle und geänderten Profilwerte automatisch im Hintergrund auf den Server hoch.
 */
class TeRunSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val repository = SpielRepository(applicationContext)
            repository.synchronisiereLokaleDaten()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
