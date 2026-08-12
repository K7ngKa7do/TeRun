package com.example.terun

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * =====================================================================
 * SpielRepository – Datenschicht der App (MVVM-Model)
 * =====================================================================
 *
 * VORLESUNG 18 – MVVM (Model-View-ViewModel):
 * Das Repository ist die unterste Schicht im MVVM-Muster – das "Model".
 * Es ist die einzige Stelle, die direkt auf Datenquellen zugreift.
 * Das ViewModel kennt NUR das Repository, nie direkt DAO, Prefs oder Firebase.
 *
 * Dieses Repository verwaltet DREI Datenquellen:
 * 1. Room-Datenbank (SQLite) – lokale, persistente Speicherung (VL 33–36)
 * 2. SharedPreferences – einfache Schlüssel-Wert-Paare (VL 32)
 * 3. Firebase Auth + Firestore – cloudbasierte Authentifizierung und Datenbank (VL 46)
 *
 * VORLESUNG 48 – Data Strategies (Offline-First):
 * Die App arbeitet nach dem Offline-First-Prinzip:
 * - Daten werden zuerst lokal (Room) gespeichert
 * - Sobald Netzwerk verfügbar ist, werden sie mit Firestore synchronisiert
 * - WorkManager erledigt die Synchronisation im Hintergrund (VL 27)
 *
 * VORLESUNG 30 – Dispatcher:
 * Alle Datenbankoperationen laufen mit withContext(Dispatchers.IO),
 * damit der Haupt-UI-Thread nicht blockiert wird.
 */
class SpielRepository(private val context: Context) {

    // DAO: Datenbankzugriffsobjekt für Room (public damit TeRunSyncWorker darauf zugreifen kann)
    val dao = TeRunDatabase.getDatabase(context).teRunDao()
    // Wrapper für SharedPreferences-Zugriffe (Key-Value Speicherung, VL 32)
    private val prefs = PreferencesManager(context)
    // Netzwerk-Monitor: erkennt ob Gerät online oder offline ist (VL 48)
    val networkMonitor = NetworkMonitor(context)
    // Firebase Firestore Datenbank (lazy = wird erst beim ersten Zugriff erzeugt)
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    init {
        // Wenn das Gerät von offline auf online wechselt: Offline-Sync starten (VL 48 – Data Strategies)
        // collect { } = Coroutine-Operator: hört permanent auf Änderungen des NetworkMonitor StateFlow
        CoroutineScope(Dispatchers.IO).launch {
            networkMonitor.isOnline.collect { online ->
                if (online) {
                    scheduleOfflineSync() // WorkManager-Job starten (VL 27)
                }
            }
        }
    }

    /**
     * Offline-Synchronisation mit WorkManager planen (VL 27 – WorkManager).
     * Erstellt eine einmalige Hintergrundaufgabe (OneTimeWorkRequest) die
     * nur ausgeführt wird wenn eine Netzwerkverbindung besteht.
     *
     * ExistingWorkPolicy.KEEP:
     * Falls bereits ein Sync-Job läuft, wird kein weiterer hinzugefügt.
     * Verhindert doppelte Synchronisationen wenn das Netzwerk schnell wechselt.
     */
    fun scheduleOfflineSync() {
        // Bedingung: nur wenn Netzwerk verfügbar ist (laut VL 27-Beispiel)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Einmalige Arbeitsanfrage für TeRunSyncWorker erstellen
        val workRequest = OneTimeWorkRequestBuilder<TeRunSyncWorker>()
            .setConstraints(constraints)
            .build()

        try {
            // Beim System einreihen: eindeutiger Name verhindert doppelte Jobs
            WorkManager.getInstance(context).enqueueUniqueWork(
                "TeRunOfflineSync",
                ExistingWorkPolicy.KEEP, // Nicht ersetzen wenn bereits läuft
                workRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildInvitationsMap(gegner: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        gegner.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { name ->
            map[name] = "PENDING"
        }
        return map
    }

    /**
     * Alle lokal gespeicherten Duelle und das Profil mit Firestore hochladen.
     * Wird vom TeRunSyncWorker aufgerufen (VL 48 – Batching-Strategie):
     * "Daten zuerst lokal speichern, dann als Block hochladen wenn Netzwerk verfügbar"
     *
     * withContext(Dispatchers.IO): läuft auf dem IO-Thread (kein UI-Thread blockiert, VL 30)
     */
    suspend fun synchronisiereLokaleDaten() = withContext(Dispatchers.IO) {
        val localDuelle = dao.getAlleDuelle().map { it.toDuell() }
        for (duell in localDuelle) {
            val duelMap = mapOf(
                "id" to duell.id,
                "name" to duell.name,
                "spotsAnzahl" to duell.spotsAnzahl,
                "zeitLimitMinuten" to duell.zeitLimitMinuten,
                "spot1Lat" to duell.spot1Lat, "spot1Lng" to duell.spot1Lng,
                "spot2Lat" to duell.spot2Lat, "spot2Lng" to duell.spot2Lng,
                "spot3Lat" to duell.spot3Lat, "spot3Lng" to duell.spot3Lng,
                "spot4Lat" to duell.spot4Lat, "spot4Lng" to duell.spot4Lng,
                "spot5Lat" to duell.spot5Lat, "spot5Lng" to duell.spot5Lng,
                "gegner" to duell.gegner,
                "creator" to getAccountKey(),
                "invitations" to buildInvitationsMap(duell.gegner)
            )
            try {
                firestore.collection("duels").document(duell.id).set(duelMap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val key = getAccountKey()
        val name = ladeSpielerName()
        if (key.isNotBlank() && name.isNotBlank() && name != "Spieler") {
            speichereProfilFirestore(key, name)
        }
    }

    // ==========================================================================
    // Authentifizierung (Firebase Auth + Room Offline-Fallback) – VL 46
    // ==========================================================================

    // Firebase Auth Instanz (lazy = erst beim ersten Zugriff initialisiert)
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    /**
     * Benutzer mit E-Mail und Passwort anmelden.
     *
     * VORLESUNG 46 – Firebase:
     * Firebase Auth erledigt die Sicherheit im Hintergrund (Passwort-Hash, Token-Verwaltung).
     * Wir rufen nur signInWithEmailAndPassword() auf – Firebase macht den Rest.
     *
     * Offline-Fallback (VL 48 – Data Strategies):
     * Falls Firebase nicht erreichbar ist (kein Internet / falscher API-Key),
     * prüft die App das Passwort gegen die lokale Room-Datenbank.
     * So kann der Spieler auch offline spielen.
     *
     * suspendCancellableCoroutine:
     * Wandelt den Firebase-Callback (addOnCompleteListener) in eine Coroutine um.
     * So kann das Repository await-artig auf das Firebase-Ergebnis warten
     * ohne den UI-Thread zu blockieren.
     */
    suspend fun anmelden(email: String, passwort: String): Result<String> {
        val cleanEmail = email.trim().lowercase()
        val result = try {
            suspendCancellableCoroutine<Result<String>> { continuation ->
                auth.signInWithEmailAndPassword(cleanEmail, passwort)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            continuation.resume(Result.success(auth.currentUser?.email ?: cleanEmail))
                        } else {
                            val exception = task.exception ?: Exception("Anmeldung fehlgeschlagen")
                            continuation.resume(Result.failure(exception))
                        }
                    }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

        // Falls Firebase fehlschlägt (z. B. wegen ungültigem API-Key oder fehlender Internetverbindung),
        // führen wir den Offline-Fallback auf die lokale Room-DB aus.
        return if (result.isFailure) {
            val lokalerUser = holeBenutzer(cleanEmail)
            if (lokalerUser != null) {
                if (lokalerUser.passwort == passwort) {
                    Result.success(cleanEmail)
                } else {
                    Result.failure(Exception("WRONG_PASSWORD"))
                }
            } else {
                Result.failure(Exception("USER_NOT_FOUND"))
            }
        } else {
            result
        }
    }

    /**
     * Neues Konto mit E-Mail, Spielername und Passwort erstellen.
     *
     * VORLESUNG 46 – Firebase:
     * Firebase erstellt das Konto im Authentifizierungs-Backend.
     * Zusätzlich wird der Spieler in Room (lokal) und Firestore (cloud) gespeichert.
     *
     * Offline-Fallback:
     * Falls Firebase nicht erreichbar, wird der Benutzer nur lokal gespeichert.
     * Bei nächster Verbindung synchronisiert der SyncWorker die Daten (VL 27 + VL 48).
     */
    suspend fun registrieren(email: String, name: String, passwort: String): Result<String> {
        val cleanEmail = email.trim().lowercase()
        val result = try {
            suspendCancellableCoroutine<Result<String>> { continuation ->
                auth.createUserWithEmailAndPassword(cleanEmail, passwort)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            continuation.resume(Result.success(auth.currentUser?.email ?: cleanEmail))
                        } else {
                            val exception = task.exception ?: Exception("Registrierung fehlgeschlagen")
                            continuation.resume(Result.failure(exception))
                        }
                    }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

        return if (result.isFailure) {
            // Offline/Fallback: Lokal in Room registrieren
            val existing = holeBenutzer(cleanEmail)
            if (existing != null) {
                Result.failure(Exception("USER_ALREADY_EXISTS"))
            } else {
                val lokalerUser = BenutzerEntity(cleanEmail, name, passwort)
                speichereBenutzer(lokalerUser)
                Result.success(cleanEmail)
            }
        } else {
            if (result.isSuccess) {
                speichereBenutzer(BenutzerEntity(cleanEmail, name, passwort))
                speichereProfilFirestore(cleanEmail, name)
            }
            result
        }
    }

    /**
     * Profildaten (Name, Distanz, Duellanzahl) in Firestore hochladen.
     * Wird nach jeder Profiländerung aufgerufen (Name, Statistiken).
     * Kein Update wenn offline (networkMonitor.isOnline.value = false).
     */
    fun speichereProfilFirestore(email: String, name: String) {
        if (!networkMonitor.isOnline.value) return
        val userMap = mapOf(
            "email" to email.trim().lowercase(),
            "name" to name,
            "name_lowercase" to name.trim().lowercase(),
            "gesamtDistanz" to ladeGesamtDistanz(),
            "absolvierteDuelleCount" to ladeAbsolvierteDuelleCount()
        )
        try {
            firestore.collection("users").document(email.trim().lowercase()).set(userMap)
                .addOnFailureListener { it.printStackTrace() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Synchronisiert das Benutzerprofil von Firestore nach dem Login.
    // Stellt sicher, dass der Anzeigename und Statistiken auf allen Geräten korrekt sind.
    // Priorität: 1. Firestore (Single Source of Truth) → 2. Lokale Room-DB → 3. E-Mail-Präfix
    suspend fun synchronisiereProfilBeiLogin(email: String) = withContext(Dispatchers.IO) {
        var profilName: String? = null

        // 1. Versuche den Profilnamen und Statistiken von Firestore zu laden
        if (networkMonitor.isOnline.value) {
            try {
                val snapshot = suspendCancellableCoroutine<com.google.firebase.firestore.DocumentSnapshot?> { continuation ->
                    firestore.collection("users").document(email).get()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful && task.result != null && task.result.exists()) {
                                continuation.resume(task.result)
                            } else {
                                continuation.resume(null)
                            }
                        }
                }
                if (snapshot != null) {
                    profilName = snapshot.getString("name")
                    // Statistiken ebenfalls synchronisieren (Distanz, Duell-Anzahl)
                    val distanz = snapshot.getDouble("gesamtDistanz")
                    val duelleCount = snapshot.getLong("absolvierteDuelleCount")?.toInt()
                    if (distanz != null) prefs.saveSpielerGesamtDistanz(distanz)
                    if (duelleCount != null) prefs.saveAbsolvierteDuelleCount(duelleCount)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Fallback: Lokalen DB-Eintrag verwenden
        if (profilName.isNullOrBlank()) {
            profilName = dao.getBenutzerByEmail(email)?.name
        }

        // 3. Letzter Fallback: E-Mail-Präfix verwenden (z.B. "sami" aus "sami@mail.de")
        if (profilName.isNullOrBlank()) {
            profilName = email.substringBefore("@")
        }

        // Lokale Daten aktualisieren: Room-DB + SharedPreferences
        val existingUser = dao.getBenutzerByEmail(email)
        if (existingUser != null) {
            // Vorhandenen Eintrag nur umbenennen (Passwort bleibt erhalten)
            dao.updateBenutzerName(email, profilName)
        } else {
            // Neuen lokalen Eintrag anlegen (Passwort leer, da Firebase Auth zuständig)
            dao.insertBenutzer(BenutzerEntity(email, profilName, ""))
        }
        prefs.saveDisplayName(email, profilName)
    }

    // Account-Key = E-Mail-Adresse des eingeloggten Nutzers; wird beim Login gesetzt
    fun setAccountKey(email: String) = prefs.saveAccountKey(email)
    fun getAccountKey(): String = prefs.getAccountKey()

    // ==========================================================================
    // Profil-Verwaltung (SharedPreferences + Room, VL 32 + VL 35)
    // ==========================================================================

    /**
     * Anzeigenamen des eingeloggten Spielers laden.
     * Quelle: SharedPreferences (VL 32) – schnell, kein Hintergrundthread nötig.
     * Fallback: E-Mail-Präfix wenn noch kein Name gespeichert ist (z.B. "max" aus "max@test.de")
     */
    fun ladeSpielerName(): String {
        val key = prefs.getAccountKey()
        return if (key.isBlank()) "Spieler" else prefs.getDisplayName(key, key.substringBefore("@"))
    }

    /**
     * Anzeigenamen ändern und sofort dreifach persistieren:
     * 1. SharedPreferences (lokal, sofort verfügbar)
     * 2. Room-Datenbank (SQLite, persistent nach Neustart)
     * 3. Firestore (cloud, für andere Spieler sichtbar)
     * Punkt 2+3 laufen im IO-Thread (CoroutineScope + Dispatchers.IO, VL 30)
     */
    fun speichereSpielerName(name: String) {
        val key = prefs.getAccountKey()
        if (key.isNotBlank()) {
            prefs.saveDisplayName(key, name)
            // DB-Update im IO-Thread (Fire-and-Forget, kein Rückgabewert benötigt)
            CoroutineScope(Dispatchers.IO).launch {
                dao.updateBenutzerName(key, name)
                speichereProfilFirestore(key, name)
            }
        }
    }

    fun ladeGesamtDistanz(): Double = prefs.getSpielerGesamtDistanz()
    fun speichereGesamtDistanz(distanz: Double) = prefs.saveSpielerGesamtDistanz(distanz)

    fun ladeAbsolvierteDuelleCount(): Int = prefs.getAbsolvierteDuelleCount()
    fun speichereAbsolvierteDuelleCount(count: Int) = prefs.saveAbsolvierteDuelleCount(count)

    // ==========================================================================
    // Duell-Verwaltung (Room + Firestore, VL 33–36 + VL 46)
    // ==========================================================================

    // Alle Duelle vom Firestore-Server laden
    // Filtert nur Duelle bei denen der Spieler Ersteller oder Gegner ist
    suspend fun ladeDuelleVomServer(): List<Duell> = withContext(Dispatchers.IO) {
        if (!networkMonitor.isOnline.value) return@withContext emptyList()
        try {
            suspendCancellableCoroutine { continuation ->
                firestore.collection("duels").get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val list = mutableListOf<Duell>()
                            val myEmail = getAccountKey()
                            val myName = ladeSpielerName()
                            for (doc in task.result) {
                                val creator = doc.getString("creator") ?: ""
                                val gegnerStr = doc.getString("gegner") ?: ""
                                val invitations = doc.get("invitations") as? Map<String, String> ?: emptyMap()
                                val myInvitationStatus = invitations[myName] ?: invitations[myEmail]

                                // Ein Duell gehört nur unter "Verfügbare Duelle", wenn ich der Ersteller bin
                                // ODER wenn ich eingeladen wurde UND die Einladung ACCEPTED habe.
                                if (creator == myEmail || myInvitationStatus == "ACCEPTED") {
                                    list.add(
                                        Duell(
                                            id = doc.id,
                                            name = doc.getString("name") ?: "Duell",
                                            spotsAnzahl = doc.getLong("spotsAnzahl")?.toInt() ?: 1,
                                            zeitLimitMinuten = doc.getLong("zeitLimitMinuten")?.toInt() ?: 15,
                                            spot1Lat = doc.getDouble("spot1Lat") ?: 0.0,
                                            spot1Lng = doc.getDouble("spot1Lng") ?: 0.0,
                                            spot2Lat = doc.getDouble("spot2Lat") ?: 0.0,
                                            spot2Lng = doc.getDouble("spot2Lng") ?: 0.0,
                                            spot3Lat = doc.getDouble("spot3Lat") ?: 0.0,
                                            spot3Lng = doc.getDouble("spot3Lng") ?: 0.0,
                                            spot4Lat = doc.getDouble("spot4Lat") ?: 0.0,
                                            spot4Lng = doc.getDouble("spot4Lng") ?: 0.0,
                                            spot5Lat = doc.getDouble("spot5Lat") ?: 0.0,
                                            spot5Lng = doc.getDouble("spot5Lng") ?: 0.0,
                                            gegner = gegnerStr
                                        )
                                    )
                                }
                            }
                            continuation.resume(list)
                        } else {
                            continuation.resume(emptyList())
                        }
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Alle gespeicherten Duelle aus der Room-DB laden und als Domain-Objekte zurückgeben
    suspend fun holeDuelle(): List<Duell> = withContext(Dispatchers.IO) {
        val serverDuelle = ladeDuelleVomServer()
        for (duell in serverDuelle) {
            dao.insertDuell(
                DuellEntity(
                    id = duell.id,
                    name = duell.name,
                    spotsAnzahl = duell.spotsAnzahl,
                    zeitLimitMinuten = duell.zeitLimitMinuten,
                    spot1Lat = duell.spot1Lat, spot1Lng = duell.spot1Lng,
                    spot2Lat = duell.spot2Lat, spot2Lng = duell.spot2Lng,
                    spot3Lat = duell.spot3Lat, spot3Lng = duell.spot3Lng,
                    spot4Lat = duell.spot4Lat, spot4Lng = duell.spot4Lng,
                    spot5Lat = duell.spot5Lat, spot5Lng = duell.spot5Lng,
                    gegner = duell.gegner
                )
            )
        }
        if (networkMonitor.isOnline.value) {
            serverDuelle
        } else {
            val myName = ladeSpielerName()
            val myEmail = getAccountKey()
            dao.getAlleDuelle().map { it.toDuell() }.filter { duell ->
                duell.gegner.isEmpty() || duell.gegner.contains(myName) || duell.gegner.contains(myEmail)
            }
        }
    }

    // Duell als neue Zeile in die Room-DB und Firestore schreiben
    suspend fun speichereDuell(duell: Duell) = withContext(Dispatchers.IO) {
        dao.insertDuell(
            DuellEntity(
                id = duell.id,
                name = duell.name,
                spotsAnzahl = duell.spotsAnzahl,
                zeitLimitMinuten = duell.zeitLimitMinuten,
                spot1Lat = duell.spot1Lat, spot1Lng = duell.spot1Lng,
                spot2Lat = duell.spot2Lat, spot2Lng = duell.spot2Lng,
                spot3Lat = duell.spot3Lat, spot3Lng = duell.spot3Lng,
                spot4Lat = duell.spot4Lat, spot4Lng = duell.spot4Lng,
                spot5Lat = duell.spot5Lat, spot5Lng = duell.spot5Lng,
                gegner = duell.gegner
            )
        )
        if (networkMonitor.isOnline.value) {
            val duelMap = mapOf(
                "id" to duell.id,
                "name" to duell.name,
                "spotsAnzahl" to duell.spotsAnzahl,
                "zeitLimitMinuten" to duell.zeitLimitMinuten,
                "spot1Lat" to duell.spot1Lat, "spot1Lng" to duell.spot1Lng,
                "spot2Lat" to duell.spot2Lat, "spot2Lng" to duell.spot2Lng,
                "spot3Lat" to duell.spot3Lat, "spot3Lng" to duell.spot3Lng,
                "spot4Lat" to duell.spot4Lat, "spot4Lng" to duell.spot4Lng,
                "spot5Lat" to duell.spot5Lat, "spot5Lng" to duell.spot5Lng,
                "gegner" to duell.gegner,
                "creator" to getAccountKey(),
                "invitations" to buildInvitationsMap(duell.gegner)
            )
            try {
                firestore.collection("duels").document(duell.id).set(duelMap)
            } catch (e: Exception) {
                e.printStackTrace()
                scheduleOfflineSync() // Falls der Upload fehlschlägt, Offline-Sync triggern
            }
        } else {
            scheduleOfflineSync() // Offline -> Sync für später einplanen
        }
    }

    // Duell anhand seiner ID aus der Room-DB und Firestore löschen
    suspend fun loescheDuell(duell: Duell) = withContext(Dispatchers.IO) {
        dao.deleteDuellById(duell.id)
        if (networkMonitor.isOnline.value) {
            try {
                firestore.collection("duels").document(duell.id).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ==========================================================================
    // Benutzer
    // ==========================================================================

    // Benutzer anhand der E-Mail laden (z.B. für Login-Validierung)
    suspend fun holeBenutzer(email: String): BenutzerEntity? = withContext(Dispatchers.IO) {
        dao.getBenutzerByEmail(email)
    }

    // Neuen Benutzer in der Room-DB anlegen (bei Registrierung)
    suspend fun speichereBenutzer(benutzer: BenutzerEntity) = withContext(Dispatchers.IO) {
        dao.insertBenutzer(benutzer)
    }

    // Prüft ob ein Benutzername bereits vergeben ist (für Eindeutigkeits-Validierung bei Registrierung)
    suspend fun existiertBenutzerMitName(name: String): Boolean = withContext(Dispatchers.IO) {
        dao.getBenutzerByName(name) != null
    }

    // Sucht Benutzernamen die den eingegebenen Begriff enthalten (für Autocomplete bei Gegner-/Freundessuche)
    suspend fun sucheBenutzerNamen(query: String): List<String> = withContext(Dispatchers.IO) {
        val lokaleNamen = dao.sucheBenutzerNamen(query).toMutableList()
        if (networkMonitor.isOnline.value) {
            try {
                val onlineNamen = suspendCancellableCoroutine<List<String>> { continuation ->
                    firestore.collection("users")
                        .orderBy("name")
                        .startAt(query)
                        .endAt(query + "\uf8ff")
                        .limit(10)
                        .get()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val list = task.result.mapNotNull { it.getString("name") }
                                continuation.resume(list)
                            } else {
                                continuation.resume(emptyList())
                            }
                        }
                }
                for (name in onlineNamen) {
                    if (!lokaleNamen.contains(name)) lokaleNamen.add(name)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        lokaleNamen
    }

    // Konto vollständig löschen: Benutzer-Zeile + alle Freundschafts-Einträge lokal und in Firestore entfernen
    suspend fun loescheKonto(email: String) = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val myName = ladeSpielerName()

        dao.deleteBenutzerByEmail(cleanEmail)
        dao.deleteFreundeByEmail(cleanEmail)

        if (networkMonitor.isOnline.value) {
            try {
                // 1. User-Dokument synchron löschen
                suspendCancellableCoroutine<Unit> { cont ->
                    firestore.collection("users").document(cleanEmail).delete()
                        .addOnCompleteListener { cont.resume(Unit) }
                }

                // 2. Anfragen in Firestore synchron löschen
                val requestsToDelete = suspendCancellableCoroutine<List<com.google.firebase.firestore.DocumentReference>> { cont ->
                    firestore.collection("friend_requests").get()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful && task.result != null) {
                                val refs = task.result.documents.mapNotNull { doc ->
                                    val sEmail = (doc.getString("senderEmail") ?: "").lowercase()
                                    val sName = doc.getString("senderName") ?: ""
                                    val rEmail = (doc.getString("receiverEmail") ?: "").lowercase()
                                    val rName = doc.getString("receiverName") ?: ""

                                    if (sEmail == cleanEmail || rEmail == cleanEmail ||
                                        sName.equals(myName, ignoreCase = true) || rName.equals(myName, ignoreCase = true)) {
                                        doc.reference
                                    } else null
                                }
                                cont.resume(refs)
                            } else {
                                cont.resume(emptyList())
                            }
                        }
                }
                for (ref in requestsToDelete) {
                    suspendCancellableCoroutine<Unit> { cont ->
                        ref.delete().addOnCompleteListener { cont.resume(Unit) }
                    }
                }

                // 3. Freundschaften in Firestore synchron löschen
                val friendsToDelete = suspendCancellableCoroutine<List<com.google.firebase.firestore.DocumentReference>> { cont ->
                    firestore.collection("friends").get()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful && task.result != null) {
                                val refs = task.result.documents.mapNotNull { doc ->
                                    val oEmail = (doc.getString("ownerEmail") ?: "").lowercase()
                                    val fEmail = (doc.getString("friendEmail") ?: "").lowercase()
                                    if (oEmail == cleanEmail || fEmail == cleanEmail) {
                                        doc.reference
                                    } else null
                                }
                                cont.resume(refs)
                            } else {
                                cont.resume(emptyList())
                            }
                        }
                }
                for (ref in friendsToDelete) {
                    suspendCancellableCoroutine<Unit> { cont ->
                        ref.delete().addOnCompleteListener { cont.resume(Unit) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ==========================================================================
    // Freunde
    // ==========================================================================

    // Alle Freunde des Spielers laden und deren Anzeigenamen zurückgeben
    // Freundschaften sind in der DB beidseitig gespeichert (ownerEmail ↔ friendEmail)
    suspend fun holeFreunde(ownerEmail: String): List<String> = withContext(Dispatchers.IO) {
        val cleanOwnerEmail = ownerEmail.trim().lowercase()
        if (networkMonitor.isOnline.value) {
            try {
                val serverFriendEmails = suspendCancellableCoroutine<List<String>> { continuation ->
                    firestore.collection("friends")
                        .whereEqualTo("ownerEmail", cleanOwnerEmail)
                        .get()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val list = task.result.mapNotNull { it.getString("friendEmail")?.lowercase() }
                                continuation.resume(list)
                            } else {
                                continuation.resume(emptyList())
                            }
                        }
                }
                // Neu hinzugekommene Freunde lokal speichern
                for (fEmail in serverFriendEmails) {
                    if (fEmail.isNotBlank()) {
                        dao.insertFreund(FreundEntity(cleanOwnerEmail, fEmail, "ACCEPTED"))
                    }
                }
                // Gelöschte Freunde (von Firestore entfernt) auch lokal entfernen.
                // Das stellt sicher, dass wenn Kaido Sami löscht, Sami beim nächsten Laden
                // Kaido ebenfalls aus seiner lokalen Freundesliste verliert.
                val localFriends = dao.getFreundeByOwner(cleanOwnerEmail)
                for (local in localFriends) {
                    if (local.status == "ACCEPTED" && local.friendEmail !in serverFriendEmails) {
                        dao.deleteFreund(local)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        dao.getFreundeByOwner(cleanOwnerEmail).mapNotNull { friend ->
            val localUser = dao.getBenutzerByEmail(friend.friendEmail)
            if (localUser != null) {
                localUser.name
            } else if (networkMonitor.isOnline.value) {
                try {
                    val fireName = suspendCancellableCoroutine<String?> { continuation ->
                        firestore.collection("users").document(friend.friendEmail).get()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful && task.result != null) {
                                    continuation.resume(task.result.getString("name"))
                                } else {
                                    continuation.resume(null)
                                }
                            }
                    }
                    if (fireName != null) {
                        dao.insertBenutzer(BenutzerEntity(friend.friendEmail, fireName, ""))
                        fireName
                    } else {
                        friend.friendEmail.substringBefore("@")
                    }
                } catch (e: Exception) {
                    friend.friendEmail.substringBefore("@")
                }
            } else {
                friend.friendEmail.substringBefore("@")
            }
        }.distinct()
    }

    // Freundschaftsanfrage senden
    suspend fun fuegeFreundHinzu(ownerEmail: String, friendName: String): String =
        withContext(Dispatchers.IO) {
            val cleanOwnerEmail = ownerEmail.trim().lowercase()
            val cleanFriendName = friendName.trim()
            val cleanFriendNameLower = cleanFriendName.lowercase()

            var friendUser = dao.getBenutzerByName(cleanFriendName)
            if (friendUser == null && networkMonitor.isOnline.value) {
                try {
                    val onlineUser = suspendCancellableCoroutine<BenutzerEntity?> { continuation ->
                        firestore.collection("users")
                            .whereEqualTo("name_lowercase", cleanFriendNameLower)
                            .limit(1)
                            .get()
                            .addOnSuccessListener { r ->
                                if (!r.isEmpty) {
                                    val doc = r.documents[0]
                                    val email = doc.getString("email") ?: ""
                                    val name = doc.getString("name") ?: ""
                                    continuation.resume(BenutzerEntity(email, name, ""))
                                } else {
                                    firestore.collection("users")
                                        .whereEqualTo("name", cleanFriendName)
                                        .limit(1)
                                        .get()
                                        .addOnCompleteListener { t2 ->
                                            if (t2.isSuccessful && !t2.result.isEmpty) {
                                                val doc = t2.result.documents[0]
                                                val email = doc.getString("email") ?: ""
                                                val name = doc.getString("name") ?: ""
                                                continuation.resume(BenutzerEntity(email, name, ""))
                                            } else {
                                                continuation.resume(null)
                                            }
                                        }
                                }
                            }
                            .addOnFailureListener { continuation.resume(null) }
                    }
                    if (onlineUser != null) {
                        dao.insertBenutzer(onlineUser)
                        friendUser = onlineUser
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (friendUser == null) return@withContext "USER_NOT_FOUND"
            val friendEmail = friendUser.email.trim().lowercase()
            val friendActualName = friendUser.name
            if (cleanOwnerEmail == friendEmail) return@withContext "SELF_REQUEST"

            val myName = ladeSpielerName()
            val docId = if (cleanOwnerEmail < friendEmail) "${cleanOwnerEmail}_${friendEmail}" else "${friendEmail}_${cleanOwnerEmail}"

            // 1. Lokale DB auf existierende Verbindung prüfen
            val lokaleFreundschaft = dao.getFreundschaft(cleanOwnerEmail, friendEmail)
            if (lokaleFreundschaft != null) {
                when (lokaleFreundschaft.status) {
                    "ACCEPTED" -> return@withContext "ALREADY_FRIENDS"
                    "SENT_PENDING" -> {
                        // Ich habe bereits eine Anfrage gesendet — keine doppelte Anfrage erlaubt
                        return@withContext "ALREADY_SENT"
                    }
                    "RECEIVED_PENDING" -> {
                        // Der andere hat mich bereits angefragt → automatisch annehmen
                        antworteAufFreundesanfrage(cleanOwnerEmail, friendActualName, akzeptiert = true)
                        return@withContext "SUCCESS"
                    }
                }
            }

            // 2. Firestore auf existierende Verbindung prüfen (falls online)
            if (networkMonitor.isOnline.value) {
                try {
                    val existingDoc = suspendCancellableCoroutine<com.google.firebase.firestore.DocumentSnapshot?> { continuation ->
                        firestore.collection("friend_requests").document(docId).get()
                            .addOnCompleteListener { t ->
                                if (t.isSuccessful && t.result != null && t.result.exists()) {
                                    continuation.resume(t.result)
                                } else {
                                    continuation.resume(null)
                                }
                            }
                    }
                    if (existingDoc != null) {
                        val status = existingDoc.getString("status") ?: ""
                        val senderEmail = (existingDoc.getString("senderEmail") ?: "").lowercase()
                        when {
                            status == "ACCEPTED" -> {
                                // Bereits befreundet — lokal synchronisieren
                                dao.insertFreund(FreundEntity(ownerEmail = cleanOwnerEmail, friendEmail = friendEmail, status = "ACCEPTED"))
                                dao.insertFreund(FreundEntity(ownerEmail = friendEmail, friendEmail = cleanOwnerEmail, status = "ACCEPTED"))
                                return@withContext "ALREADY_FRIENDS"
                            }
                            status == "PENDING" && senderEmail == cleanOwnerEmail -> {
                                // Ich habe bereits eine Anfrage gesendet — keine parallele Anfrage erlaubt
                                dao.insertFreund(FreundEntity(ownerEmail = cleanOwnerEmail, friendEmail = friendEmail, status = "SENT_PENDING"))
                                return@withContext "ALREADY_SENT"
                            }
                            status == "PENDING" && senderEmail != cleanOwnerEmail -> {
                                // Der andere Nutzer hat mich bereits angefragt → automatisch annehmen
                                antworteAufFreundesanfrage(cleanOwnerEmail, friendActualName, akzeptiert = true)
                                return@withContext "SUCCESS"
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Lokalen Eintrag als PENDING speichern
            dao.insertFreund(FreundEntity(ownerEmail = cleanOwnerEmail, friendEmail = friendEmail, status = "SENT_PENDING"))

            if (networkMonitor.isOnline.value) {
                try {
                    val requestMap = mapOf(
                        "senderEmail" to cleanOwnerEmail,
                        "senderName" to myName,
                        "receiverEmail" to friendEmail,
                        "receiverName" to friendActualName,
                        "status" to "PENDING",
                        "timestamp" to System.currentTimeMillis()
                    )
                    suspendCancellableCoroutine<Boolean> { continuation ->
                        firestore.collection("friend_requests")
                            .document(docId)
                            .set(requestMap)
                            .addOnSuccessListener { continuation.resume(true) }
                            .addOnFailureListener { e ->
                                e.printStackTrace()
                                continuation.resume(false)
                            }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            "SUCCESS"
        }

    // Löscht veraltete Hängengebliebene ausstehende Anfragen im lokalen Cache
    // Bereinigt beim App-Start nur die gesendeten, noch ausstehenden Anfragen im lokalen Cache.
    // RECEIVED_PENDING Einträge werden NICHT gelöscht, damit die Freundesliste korrekt bleibt.
    suspend fun bereinigeAusstehendeAnfragen(ownerEmail: String) = withContext(Dispatchers.IO) {
        val cleanEmail = ownerEmail.trim().lowercase()
        dao.deleteSentPendingByOwner(cleanEmail)
    }

    // Holt ausstehende Freundschaftsanfragen für den angemeldeten Benutzer
    suspend fun holeAusstehendeFreundesanfragen(ownerEmail: String): List<String> = withContext(Dispatchers.IO) {
        val cleanOwnerEmail = ownerEmail.trim().lowercase()

        if (!networkMonitor.isOnline.value) {
            // Offline: Bereits akzeptierte Anfragen rausfiltern
            return@withContext dao.getPendingRequestsByOwner(cleanOwnerEmail)
                .filter { pending ->
                    val accepted = dao.getFreundschaft(cleanOwnerEmail, pending.friendEmail)
                    accepted?.status != "ACCEPTED"
                }
                .mapNotNull { pending ->
                    dao.getBenutzerByEmail(pending.friendEmail)?.name ?: pending.friendEmail
                }
        }
        try {
            val pendingSenders = suspendCancellableCoroutine<List<Pair<String, String>>> { continuation ->
                // Query NUR per receiverEmail (einfacher Index, kein Composite Index nötig).
                // Status-Filter (PENDING) wird client-seitig durchgeführt.
                firestore.collection("friend_requests")
                    .whereEqualTo("receiverEmail", cleanOwnerEmail)
                    .get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val list = task.result.mapNotNull { doc ->
                                val status = doc.getString("status") ?: ""
                                val sEmail = (doc.getString("senderEmail") ?: "").lowercase()
                                val sName = doc.getString("senderName") ?: ""

                                // Nur echte offene Anfragen (status=PENDING), nicht von mir selbst
                                val isPending = status == "PENDING"
                                val isNotFromMe = sEmail != cleanOwnerEmail && sName.isNotBlank()

                                if (isPending && isNotFromMe) sEmail to sName else null
                            }
                            continuation.resume(list)
                        } else {
                            continuation.resume(emptyList())
                        }
                    }
            }

            for ((senderEmail, senderName) in pendingSenders) {
                dao.insertBenutzer(BenutzerEntity(senderEmail, senderName, ""))
                dao.insertFreund(FreundEntity(ownerEmail = cleanOwnerEmail, friendEmail = senderEmail, status = "RECEIVED_PENDING"))
            }

            // Bereits bestehende Freundschaften sauber aus der RECEIVED_PENDING-Liste rausfiltern
            pendingSenders
                .filter { (sEmail, _) ->
                    dao.getFreundschaft(cleanOwnerEmail, sEmail)?.status != "ACCEPTED"
                }
                .map { it.second }.distinct()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Holt ausstehende GESENDETE Freundschaftsanfragen für den angemeldeten Benutzer
    suspend fun holeGesendeteFreundesanfragen(ownerEmail: String): List<String> = withContext(Dispatchers.IO) {
        val cleanOwnerEmail = ownerEmail.trim().lowercase()
        val myName = ladeSpielerName()

        if (!networkMonitor.isOnline.value) return@withContext emptyList()
        try {
            suspendCancellableCoroutine<List<String>> { continuation ->
                firestore.collection("friend_requests")
                    .whereEqualTo("status", "PENDING")
                    .get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val list = task.result.mapNotNull { doc ->
                                val sEmail = (doc.getString("senderEmail") ?: "").lowercase()
                                val sName = doc.getString("senderName") ?: ""
                                val rName = doc.getString("receiverName") ?: ""

                                val isFromMe = (sEmail == cleanOwnerEmail || sName.equals(myName, ignoreCase = true))
                                if (isFromMe && rName.isNotBlank()) rName else null
                            }
                            continuation.resume(list.distinct())
                        } else {
                            continuation.resume(emptyList())
                        }
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Gesendete Freundschaftsanfrage zurückziehen / stornieren
    suspend fun zieheFreundesanfrageZurueck(ownerEmail: String, targetName: String) = withContext(Dispatchers.IO) {
        val cleanOwnerEmail = ownerEmail.trim().lowercase()
        var targetUser = dao.getBenutzerByName(targetName)
        var targetEmail = targetUser?.email?.trim()?.lowercase() ?: ""

        if (targetEmail.isBlank() && networkMonitor.isOnline.value) {
            try {
                val foundEmail = suspendCancellableCoroutine<String?> { continuation ->
                    firestore.collection("users")
                        .whereEqualTo("name_lowercase", targetName.trim().lowercase())
                        .limit(1)
                        .get()
                        .addOnSuccessListener { r ->
                            if (!r.isEmpty) continuation.resume(r.documents[0].getString("email"))
                            else continuation.resume(null)
                        }
                        .addOnFailureListener { continuation.resume(null) }
                }
                if (foundEmail != null) targetEmail = foundEmail.trim().lowercase()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (targetEmail.isNotBlank()) {
            val docId = if (cleanOwnerEmail < targetEmail) "${cleanOwnerEmail}_${targetEmail}" else "${targetEmail}_${cleanOwnerEmail}"
            dao.deleteFreund(FreundEntity(ownerEmail = cleanOwnerEmail, friendEmail = targetEmail, status = "SENT_PENDING"))

            if (networkMonitor.isOnline.value) {
                try {
                    firestore.collection("friend_requests").document(docId).delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Setzt alle hängengebliebenen ausstehenden Freundschaftsanfragen in Firestore und DB zurück
    suspend fun resetAlleFreundschaftsanfragen(ownerEmail: String) = withContext(Dispatchers.IO) {
        val cleanOwnerEmail = ownerEmail.trim().lowercase()
        val myName = ladeSpielerName()

        dao.deletePendingFreundeByOwner(cleanOwnerEmail)

        if (networkMonitor.isOnline.value) {
            try {
                val refsToDelete = suspendCancellableCoroutine<List<com.google.firebase.firestore.DocumentReference>> { cont ->
                    firestore.collection("friend_requests").get()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful && task.result != null) {
                                val list = task.result.documents.mapNotNull { doc ->
                                    val status = doc.getString("status") ?: ""
                                    if (status == "PENDING") {
                                        val sEmail = (doc.getString("senderEmail") ?: "").lowercase()
                                        val sName = doc.getString("senderName") ?: ""
                                        val rEmail = (doc.getString("receiverEmail") ?: "").lowercase()
                                        val rName = doc.getString("receiverName") ?: ""

                                        if (sEmail == cleanOwnerEmail || rEmail == cleanOwnerEmail ||
                                            sName.equals(myName, ignoreCase = true) || rName.equals(myName, ignoreCase = true)) {
                                            doc.reference
                                        } else null
                                    } else null
                                }
                                cont.resume(list)
                            } else {
                                cont.resume(emptyList())
                            }
                        }
                }
                for (ref in refsToDelete) {
                    suspendCancellableCoroutine<Unit> { cont ->
                        ref.delete().addOnCompleteListener { cont.resume(Unit) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Antwortet auf eine Freundschaftsanfrage
    suspend fun antworteAufFreundesanfrage(ownerEmail: String, senderName: String, akzeptiert: Boolean) = withContext(Dispatchers.IO) {
        val cleanOwnerEmail = ownerEmail.trim().lowercase()
        var senderUser = dao.getBenutzerByName(senderName)
        var senderEmail = senderUser?.email?.trim()?.lowercase() ?: ""

        if (senderEmail.isBlank() && networkMonitor.isOnline.value) {
            try {
                val foundEmail = suspendCancellableCoroutine<String?> { continuation ->
                    firestore.collection("users")
                        .whereEqualTo("name_lowercase", senderName.trim().lowercase())
                        .limit(1)
                        .get()
                        .addOnSuccessListener { r ->
                            if (!r.isEmpty) continuation.resume(r.documents[0].getString("email"))
                            else continuation.resume(null)
                        }
                        .addOnFailureListener { continuation.resume(null) }
                }
                if (foundEmail != null) {
                    senderEmail = foundEmail.trim().lowercase()
                    dao.insertBenutzer(BenutzerEntity(senderEmail, senderName, ""))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (senderEmail.isBlank()) return@withContext
        val docId = if (cleanOwnerEmail < senderEmail) "${cleanOwnerEmail}_${senderEmail}" else "${senderEmail}_${cleanOwnerEmail}"

        if (akzeptiert) {
            // Lokal: RECEIVED_PENDING entfernen und als ACCEPTED eintragen (beidseitig)
            dao.deleteFreund(FreundEntity(ownerEmail = cleanOwnerEmail, friendEmail = senderEmail, status = "RECEIVED_PENDING"))
            dao.insertFreund(FreundEntity(ownerEmail = cleanOwnerEmail, friendEmail = senderEmail, status = "ACCEPTED"))
            dao.insertFreund(FreundEntity(ownerEmail = senderEmail, friendEmail = cleanOwnerEmail, status = "ACCEPTED"))

            if (networkMonitor.isOnline.value) {
                try {
                    // Firestore: friend_requests Dokument LÖSCHEN (nicht updaten auf ACCEPTED).
                    // Das verhindert, dass es bei der nächsten holeAusstehendeFreundesanfragen-Abfrage
                    // noch als offene Anfrage erscheint (da wir per status=PENDING filtern).
                    suspendCancellableCoroutine<Unit> { cont ->
                        firestore.collection("friend_requests").document(docId).delete()
                            .addOnCompleteListener { cont.resume(Unit) }
                    }

                    val map1 = mapOf("ownerEmail" to cleanOwnerEmail, "friendEmail" to senderEmail)
                    val map2 = mapOf("ownerEmail" to senderEmail, "friendEmail" to cleanOwnerEmail)
                    firestore.collection("friends").document("${cleanOwnerEmail}_$senderEmail").set(map1)
                    firestore.collection("friends").document("${senderEmail}_$cleanOwnerEmail").set(map2)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            // Ablehnen: RECEIVED_PENDING lokal entfernen und Firestore-Dokument löschen
            dao.deleteFreund(FreundEntity(ownerEmail = cleanOwnerEmail, friendEmail = senderEmail, status = "RECEIVED_PENDING"))

            if (networkMonitor.isOnline.value) {
                try {
                    firestore.collection("friend_requests").document(docId).delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Freundschaft beidseitig löschen (lokal + Firestore)
    // Wenn Kaido Sami löscht, verliert auch Sami Kaido aus seiner Freundesliste beim nächsten Laden.
    suspend fun loescheFreund(ownerEmail: String, friendName: String) = withContext(Dispatchers.IO) {
        val cleanOwnerEmail = ownerEmail.trim().lowercase()
        val friendUser = dao.getBenutzerByName(friendName) ?: run {
            // Kein lokaler Eintrag — trotzdem versuchen via Firestore aufzulösen
            return@withContext
        }
        val friendEmail = friendUser.email.trim().lowercase()

        // Lokal beidseitig entfernen (alle Status)
        dao.deleteFreund(FreundEntity(ownerEmail = cleanOwnerEmail, friendEmail = friendEmail, status = "ACCEPTED"))
        dao.deleteFreund(FreundEntity(ownerEmail = friendEmail, friendEmail = cleanOwnerEmail, status = "ACCEPTED"))

        if (networkMonitor.isOnline.value) {
            val docId = if (cleanOwnerEmail < friendEmail) "${cleanOwnerEmail}_${friendEmail}" else "${friendEmail}_${cleanOwnerEmail}"
            try {
                // friends-Collection (beidseitig) löschen
                firestore.collection("friends").document("${cleanOwnerEmail}_$friendEmail").delete()
                firestore.collection("friends").document("${friendEmail}_$cleanOwnerEmail").delete()
                // friend_requests-Dokument ebenfalls löschen (verhindert Neuanzeige als Anfrage)
                firestore.collection("friend_requests").document(docId).delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Antwortet auf eine Duell-Einladung
    suspend fun antworteAufDuellEinladung(duelId: String, akzeptiert: Boolean) = withContext(Dispatchers.IO) {
        if (!networkMonitor.isOnline.value) return@withContext
        val myName = ladeSpielerName()
        val myEmail = getAccountKey()
        val statusValue = if (akzeptiert) "ACCEPTED" else "DECLINED"
        try {
            val docRef = firestore.collection("duels").document(duelId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val invitations = snapshot.get("invitations") as? Map<String, String> ?: emptyMap()
                val updatedInvitations = invitations.toMutableMap()
                // Sowohl Name als auch E-Mail als Schlüssel berücksichtigen
                for (key in invitations.keys) {
                    if (key.equals(myName, ignoreCase = true) || key.equals(myEmail, ignoreCase = true)) {
                        updatedInvitations[key] = statusValue
                    }
                }
                updatedInvitations[myName] = statusValue
                transaction.update(docRef, "invitations", updatedInvitations)
            }.addOnFailureListener { it.printStackTrace() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Setzt das Signal in Firestore, dass das Duell gestartet wurde
    fun starteSpielInFirestore(duelId: String) {
        if (!networkMonitor.isOnline.value) return
        try {
            firestore.collection("duels").document(duelId)
                .update("started", true)
                .addOnFailureListener { it.printStackTrace() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Beobachtet in Echtzeit, ob der Ersteller das Duell gestartet hat
    fun beobachteSpielstart(duelId: String, onStart: () -> Unit): com.google.firebase.firestore.ListenerRegistration? {
        if (!networkMonitor.isOnline.value) return null
        return try {
            firestore.collection("duels").document(duelId)
                .addSnapshotListener { snapshot, error ->
                    if (snapshot != null && snapshot.exists()) {
                        val isStarted = snapshot.getBoolean("started") ?: false
                        if (isStarted) {
                            onStart()
                        }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Schreibt Live-Koordinaten, Score und Aufgabe-Status in die Multiplayer-Session
    fun updateLiveSession(duelId: String, playerEmail: String, lat: Double, lng: Double, spotsCaptured: Int, giveUp: Boolean = false) {
        if (!networkMonitor.isOnline.value) return
        val playerSession = mapOf(
            "lat" to lat,
            "lng" to lng,
            "spotsCaptured" to spotsCaptured,
            "giveUp" to giveUp,
            "timestamp" to System.currentTimeMillis()
        )
        try {
            firestore.collection("duel_sessions").document(duelId)
                .set(mapOf(playerEmail to playerSession), com.google.firebase.firestore.SetOptions.merge())
                .addOnFailureListener { it.printStackTrace() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Löscht das Dokument der Live-Session nach Spielende
    fun loescheLiveSession(duelId: String) {
        if (!networkMonitor.isOnline.value) return
        try {
            firestore.collection("duel_sessions").document(duelId).delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Übersetzt technische Firebase-Fehlermeldungen in benutzerfreundliche deutsche Texte
    fun translateAuthError(exception: Throwable?): String {
        val msg = exception?.message ?: ""
        return when {
            // Präzise Fehlercodes aus lokalem Fallback oder Firebase-Exceptions
            msg.contains("WRONG_PASSWORD", ignoreCase = true) ||
            msg.contains("wrong-password", ignoreCase = true) ||
            msg.contains("password is invalid", ignoreCase = true) -> {
                "Das eingegebene Passwort ist falsch!"
            }

            msg.contains("USER_NOT_FOUND", ignoreCase = true) ||
            msg.contains("no user record", ignoreCase = true) ||
            msg.contains("user-not-found", ignoreCase = true) -> {
                "Diese E-Mail-Adresse ist noch nicht registriert!"
            }

            msg.contains("USER_ALREADY_EXISTS", ignoreCase = true) ||
            msg.contains("email already in use", ignoreCase = true) ||
            msg.contains("email-already-in-use", ignoreCase = true) -> {
                "Diese E-Mail-Adresse ist bereits registriert!"
            }

            msg.contains("invalid-email", ignoreCase = true) ||
            msg.contains("email address is badly formatted", ignoreCase = true) -> {
                "Das E-Mail-Format ist ungültig (z.B. @ fehlt)!"
            }

            msg.contains("network-request-failed", ignoreCase = true) ||
            msg.contains("network error", ignoreCase = true) ||
            msg.contains("ssl", ignoreCase = true) ||
            msg.contains("i/o error", ignoreCase = true) ||
            msg.contains("connection reset", ignoreCase = true) ||
            msg.contains("socket", ignoreCase = true) -> {
                "Netzwerkfehler! Bitte überprüfe deine Internetverbindung."
            }

            msg.contains("API key not valid", ignoreCase = true) ||
            msg.contains("Please pass a valid API key", ignoreCase = true) -> {
                // Wenn Firebase-API ungültig ist, aber wir keinen lokalen User in Room haben
                "Diese E-Mail-Adresse ist noch nicht registriert!"
            }

            else -> {
                // Generischer, aber verständlicher Text
                "Ungültige E-Mail-Adresse oder Passwort falsch!"
            }
        }
    }

    // Beobachtet in Echtzeit, ob gesendete Freundschaftsanfragen akzeptiert wurden
    fun starteBeobachtungAngenommeneAnfragen(ownerEmail: String, onAccepted: (friendName: String) -> Unit): com.google.firebase.firestore.ListenerRegistration? {
        if (!networkMonitor.isOnline.value) return null
        return try {
            firestore.collection("friend_requests")
                .whereEqualTo("senderEmail", ownerEmail)
                .whereEqualTo("status", "ACCEPTED")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        error.printStackTrace()
                        return@addSnapshotListener
                    }
                    if (snapshots != null && !snapshots.isEmpty) {
                        for (doc in snapshots.documentChanges) {
                            if (doc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED ||
                                doc.type == com.google.firebase.firestore.DocumentChange.Type.MODIFIED) {
                                val receiverEmail = doc.document.getString("receiverEmail") ?: ""
                                CoroutineScope(Dispatchers.IO).launch {
                                    val friendUser = holeBenutzer(receiverEmail)
                                    val friendName = friendUser?.name ?: receiverEmail
                                    
                                    // Lokal auf ACCEPTED aktualisieren
                                    dao.insertFreund(FreundEntity(ownerEmail = ownerEmail, friendEmail = receiverEmail, status = "ACCEPTED"))
                                    dao.insertFreund(FreundEntity(ownerEmail = receiverEmail, friendEmail = ownerEmail, status = "ACCEPTED"))
                                    
                                    // Callback aufrufen für Toast
                                    withContext(Dispatchers.Main) {
                                        onAccepted(friendName)
                                    }
                                    
                                    // Dokument aus friend_requests löschen, da verarbeitet
                                    try {
                                        firestore.collection("friend_requests").document(doc.document.id).delete()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Beobachtet in Echtzeit, ob dieser Nutzer neue Freundschaftsanfragen erhält
    // Startet den Echtzeit-Firestore-Listener für eingehende Freundschaftsanfragen.
    // Filtert direkt per receiverEmail in Firestore (effizient, keine Client-seitige Schleife nötig).
    // WICHTIG: Beim ersten Snapshot-Start sendet Firestore alle bestehenden Docs als ADDED —
    //          diese werden ignoriert (hasPendingWrites=false + fromCache=false = serverseitig vorhandene Altdaten).
    //          Nur echte neue Dokumente (hasPendingWrites=true) oder MODIFIED-Events lösen den Dialog aus.
    fun starteBeobachtungEingehendeAnfragen(ownerEmail: String, onRequestReceived: (senderName: String) -> Unit): com.google.firebase.firestore.ListenerRegistration? {
        val cleanOwnerEmail = ownerEmail.trim().lowercase()
        if (cleanOwnerEmail.isBlank()) return null
        if (!networkMonitor.isOnline.value) return null

        var erstesSnapshot = true  // Beim ersten Feuern des Listeners alle Altdaten ignorieren

        return try {
            // Query NUR per receiverEmail (kein Composite Index in Firestore nötig).
            // status-Filter (PENDING) wird client-seitig geprüft.
            firestore.collection("friend_requests")
                .whereEqualTo("receiverEmail", cleanOwnerEmail)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        error.printStackTrace()
                        return@addSnapshotListener
                    }

                    // Beim allerersten Snapshot kommen alle bestehenden Docs als ADDED —
                    // das führt zu falschen Dialog-Popups. Wir überspringen diesen ersten Durchlauf.
                    if (erstesSnapshot) {
                        erstesSnapshot = false
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        for (doc in snapshots.documentChanges) {
                            // Nur neu hinzugefügte (ADDED) Dokumente verarbeiten
                            if (doc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                val d = doc.document
                                val status = d.getString("status") ?: ""
                                val sName = d.getString("senderName") ?: ""
                                val sEmail = (d.getString("senderEmail") ?: "").lowercase()
                                val myName = ladeSpielerName()

                                // Nur offene Anfragen (PENDING), nicht meine eigenen
                                val isPending = status == "PENDING"
                                val isNotFromMe = (sEmail != cleanOwnerEmail && !sName.equals(myName, ignoreCase = true))

                                if (isPending && isNotFromMe && sName.isNotBlank()) {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        onRequestReceived(sName)
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ==========================================================================
    // Mapper (privat)
    // ==========================================================================

    // Konvertiert eine Room-Entity (DuellEntity) in ein Domain-Objekt (Duell)
    // Trennung: DB-Schicht kennt nur Entities, ViewModel/UI nur Domain-Objekte
    private fun DuellEntity.toDuell() = Duell(
        id = id,
        name = name,
        spotsAnzahl = spotsAnzahl,
        zeitLimitMinuten = zeitLimitMinuten,
        spot1Lat = spot1Lat, spot1Lng = spot1Lng,
        spot2Lat = spot2Lat, spot2Lng = spot2Lng,
        spot3Lat = spot3Lat, spot3Lng = spot3Lng,
        spot4Lat = spot4Lat, spot4Lng = spot4Lng,
        spot5Lat = spot5Lat, spot5Lng = spot5Lng,
        gegner = gegner
    )
}
