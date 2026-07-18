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
 * SpielRepository — Datenschicht der App.
 * Kapselt den gesamten Datenzugriff: Room-Datenbank (DAO) und SharedPreferences (PreferencesManager).
 * Das ViewModel kennt nur das Repository, nie direkt DAO oder Prefs.
 */
class SpielRepository(private val context: Context) {

    val dao = TeRunDatabase.getDatabase(context).teRunDao() // Datenbankzugriffsobjekt (public für SyncWorker)
    private val prefs = PreferencesManager(context)                 // SharedPreferences-Wrapper
    val networkMonitor = NetworkMonitor(context)            // Verbindungssensor
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    init {
        // Automatisch synchronisieren sobald das Gerät online geht (VL 11: Data Strategies)
        CoroutineScope(Dispatchers.IO).launch {
            networkMonitor.isOnline.collect { online ->
                if (online) {
                    scheduleOfflineSync()
                }
            }
        }

        // Testdaten (User1, User2, User3) automatisch anlegen, falls die DB zurückgesetzt wurde
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (dao.getBenutzerByEmail("user1@TeRun.de") == null) {
                    dao.insertBenutzer(BenutzerEntity("user1@TeRun.de", "user1", "Passwort123."))
                }
                if (dao.getBenutzerByEmail("user2@TeRun.de") == null) {
                    dao.insertBenutzer(BenutzerEntity("user2@TeRun.de", "user2", "Passwort123."))
                }
                if (dao.getBenutzerByEmail("user3@TeRun.de") == null) {
                    dao.insertBenutzer(BenutzerEntity("user3@TeRun.de", "user3", "Passwort123."))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Plant eine Hintergrund-Synchronisation mit WorkManager ein
    fun scheduleOfflineSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<TeRunSyncWorker>()
            .setConstraints(constraints)
            .build()

        try {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "TeRunOfflineSync",
                ExistingWorkPolicy.KEEP,
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

    // Synchronisiert lokale Duelle und Profile mit dem Server (VL 11 / Data Strategies)
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
    // Account
    // ==========================================================================

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    // Firebase-Anmeldung mit Fallback auf lokale SQLite-Datenbank (Room)
    suspend fun anmelden(email: String, passwort: String): Result<String> {
        val result = try {
            suspendCancellableCoroutine<Result<String>> { continuation ->
                auth.signInWithEmailAndPassword(email, passwort)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            continuation.resume(Result.success(auth.currentUser?.email ?: email))
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
            val lokalerUser = holeBenutzer(email)
            if (lokalerUser != null && lokalerUser.passwort == passwort) {
                Result.success(email)
            } else {
                result // Originalen Firebase-Fehler zurückgeben, wenn auch lokal nicht vorhanden
            }
        } else {
            result
        }
    }

    // Firebase-Registrierung mit lokaler Spiegelung & Fallback
    suspend fun registrieren(email: String, name: String, passwort: String): Result<String> {
        val result = try {
            suspendCancellableCoroutine<Result<String>> { continuation ->
                auth.createUserWithEmailAndPassword(email, passwort)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            continuation.resume(Result.success(auth.currentUser?.email ?: email))
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
            val existing = holeBenutzer(email)
            if (existing != null) {
                Result.failure(Exception("Benutzer existiert bereits lokal!"))
            } else {
                val lokalerUser = BenutzerEntity(email, name, passwort)
                speichereBenutzer(lokalerUser)
                Result.success(email)
            }
        } else {
            if (result.isSuccess) {
                speichereBenutzer(BenutzerEntity(email, name, passwort))
                speichereProfilFirestore(email, name)
            }
            result
        }
    }

    // Profildaten in Firestore synchronisieren (Name, Distanz, Duelle)
    fun speichereProfilFirestore(email: String, name: String) {
        if (!networkMonitor.isOnline.value) return
        val userMap = mapOf(
            "email" to email,
            "name" to name,
            "gesamtDistanz" to ladeGesamtDistanz(),
            "absolvierteDuelleCount" to ladeAbsolvierteDuelleCount()
        )
        try {
            firestore.collection("users").document(email).set(userMap)
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
    // Profil
    // ==========================================================================

    // Anzeigenamen des aktuell eingeloggten Spielers laden
    // Fallback: E-Mail-Präfix (z.B. "max" aus "max@mail.de") wenn kein Name gesetzt
    fun ladeSpielerName(): String {
        val key = prefs.getAccountKey()
        return if (key.isBlank()) "Spieler" else prefs.getDisplayName(key, key.substringBefore("@"))
    }

    // Anzeigenamen in SharedPreferences und Room-DB aktualisieren
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
    // Duelle
    // ==========================================================================

    // Alle Duelle vom Server abrufen
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
                                val gegnerStr = doc.getString("gegner") ?: ""
                                val gegnerList = gegnerStr.split(",").map { it.trim() }
                                if (gegnerList.contains(myName) || gegnerList.contains(myEmail) || doc.getString("creator") == myEmail) {
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
        dao.getAlleDuelle().map { it.toDuell() } // Entity → Domain-Objekt
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

    // Konto vollständig löschen: Benutzer-Zeile + alle Freundschafts-Einträge entfernen
    suspend fun loescheKonto(email: String) = withContext(Dispatchers.IO) {
        dao.deleteBenutzerByEmail(email)
        dao.deleteFreundeByEmail(email)
    }

    // ==========================================================================
    // Freunde
    // ==========================================================================

    // Alle Freunde des Spielers laden und deren Anzeigenamen zurückgeben
    // Freundschaften sind in der DB beidseitig gespeichert (ownerEmail ↔ friendEmail)
    suspend fun holeFreunde(ownerEmail: String): List<String> = withContext(Dispatchers.IO) {
        if (networkMonitor.isOnline.value) {
            try {
                val serverFriendEmails = suspendCancellableCoroutine<List<String>> { continuation ->
                    firestore.collection("friends")
                        .whereEqualTo("ownerEmail", ownerEmail)
                        .get()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val list = task.result.mapNotNull { it.getString("friendEmail") }
                                continuation.resume(list)
                            } else {
                                continuation.resume(emptyList())
                            }
                        }
                }
                for (fEmail in serverFriendEmails) {
                    if (fEmail.isNotBlank()) {
                        dao.insertFreund(FreundEntity(ownerEmail, fEmail))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        dao.getFreundeByOwner(ownerEmail).mapNotNull { friend ->
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
        }
    }

    // Freundschaftsanfrage senden
    suspend fun fuegeFreundHinzu(ownerEmail: String, friendName: String): Boolean =
        withContext(Dispatchers.IO) {
            var friendUser = dao.getBenutzerByName(friendName)
            if (friendUser == null && networkMonitor.isOnline.value) {
                try {
                    val onlineUser = suspendCancellableCoroutine<BenutzerEntity?> { continuation ->
                        firestore.collection("users")
                            .whereEqualTo("name", friendName)
                            .limit(1)
                            .get()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful && !task.result.isEmpty) {
                                    val doc = task.result.documents[0]
                                    val email = doc.getString("email") ?: ""
                                    val name = doc.getString("name") ?: ""
                                    continuation.resume(BenutzerEntity(email, name, ""))
                                } else {
                                    continuation.resume(null)
                                }
                            }
                    }
                    if (onlineUser != null) {
                        dao.insertBenutzer(onlineUser)
                        friendUser = onlineUser
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Smart-Cast-sicheren Zugriff auf friendUser erzwingen
            val resolvedFriend = friendUser ?: return@withContext false
            val friendEmail = resolvedFriend.email
            if (ownerEmail == friendEmail) return@withContext false

            // Lokalen Eintrag als PENDING speichern
            dao.insertFreund(FreundEntity(ownerEmail = ownerEmail, friendEmail = friendEmail, status = "PENDING"))

            if (networkMonitor.isOnline.value) {
                try {
                    val requestMap = mapOf(
                        "senderEmail" to ownerEmail,
                        "senderName" to ladeSpielerName(),
                        "receiverEmail" to friendEmail,
                        "status" to "PENDING"
                    )
                    // Auf Firestore-Write warten, damit die Anfrage sicher ankommt
                    suspendCancellableCoroutine<Boolean> { continuation ->
                        firestore.collection("friend_requests")
                            .document("${ownerEmail}_$friendEmail")
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
            true
        }

    // Holt ausstehende Freundschaftsanfragen für den angemeldeten Benutzer
    suspend fun holeAusstehendeFreundesanfragen(ownerEmail: String): List<String> = withContext(Dispatchers.IO) {
        if (!networkMonitor.isOnline.value) {
            return@withContext dao.getPendingRequestsByOwner(ownerEmail).mapNotNull { pending ->
                dao.getBenutzerByEmail(pending.friendEmail)?.name ?: pending.friendEmail
            }
        }
        try {
            val pendingSenders = suspendCancellableCoroutine<List<Pair<String, String>>> { continuation ->
                firestore.collection("friend_requests")
                    .whereEqualTo("receiverEmail", ownerEmail)
                    .whereEqualTo("status", "PENDING")
                    .get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val list = task.result.mapNotNull { doc ->
                                val email = doc.getString("senderEmail") ?: ""
                                val name = doc.getString("senderName") ?: ""
                                if (email.isNotBlank() && name.isNotBlank()) email to name else null
                            }
                            continuation.resume(list)
                        } else {
                            continuation.resume(emptyList())
                        }
                    }
            }

            for ((senderEmail, senderName) in pendingSenders) {
                dao.insertBenutzer(BenutzerEntity(senderEmail, senderName, ""))
                dao.insertFreund(FreundEntity(ownerEmail = ownerEmail, friendEmail = senderEmail, status = "PENDING"))
            }

            pendingSenders.map { it.second }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Antwortet auf eine Freundschaftsanfrage
    suspend fun antworteAufFreundesanfrage(ownerEmail: String, senderName: String, akzeptiert: Boolean) = withContext(Dispatchers.IO) {
        val senderUser = dao.getBenutzerByName(senderName) ?: return@withContext
        val senderEmail = senderUser.email
        val documentId = "${senderEmail}_$ownerEmail"

        if (akzeptiert) {
            dao.insertFreund(FreundEntity(ownerEmail = ownerEmail, friendEmail = senderEmail, status = "ACCEPTED"))
            dao.insertFreund(FreundEntity(ownerEmail = senderEmail, friendEmail = ownerEmail, status = "ACCEPTED"))

            if (networkMonitor.isOnline.value) {
                try {
                    firestore.collection("friend_requests").document(documentId).update("status", "ACCEPTED")

                    val map1 = mapOf("ownerEmail" to ownerEmail, "friendEmail" to senderEmail)
                    val map2 = mapOf("ownerEmail" to senderEmail, "friendEmail" to ownerEmail)
                    firestore.collection("friends").document("${ownerEmail}_$senderEmail").set(map1)
                    firestore.collection("friends").document("${senderEmail}_$ownerEmail").set(map2)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            dao.deleteFreund(FreundEntity(ownerEmail = ownerEmail, friendEmail = senderEmail, status = "PENDING"))

            if (networkMonitor.isOnline.value) {
                try {
                    firestore.collection("friend_requests").document(documentId).delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Freundschaft beidseitig löschen
    suspend fun loescheFreund(ownerEmail: String, friendName: String) = withContext(Dispatchers.IO) {
        val friendUser = dao.getBenutzerByName(friendName) ?: return@withContext
        val friendEmail = friendUser.email
        dao.deleteFreund(FreundEntity(ownerEmail = ownerEmail, friendEmail = friendEmail, status = "ACCEPTED"))
        dao.deleteFreund(FreundEntity(ownerEmail = friendEmail, friendEmail = ownerEmail, status = "ACCEPTED"))

        if (networkMonitor.isOnline.value) {
            try {
                firestore.collection("friends").document("${ownerEmail}_$friendEmail").delete()
                firestore.collection("friends").document("${friendEmail}_$ownerEmail").delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Antwortet auf eine Duell-Einladung
    suspend fun antworteAufDuellEinladung(duelId: String, akzeptiert: Boolean) = withContext(Dispatchers.IO) {
        if (!networkMonitor.isOnline.value) return@withContext
        val myName = ladeSpielerName()
        val statusValue = if (akzeptiert) "ACCEPTED" else "DECLINED"
        try {
            val docRef = firestore.collection("duels").document(duelId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val invitations = snapshot.get("invitations") as? Map<String, String> ?: emptyMap()
                val updatedInvitations = invitations.toMutableMap()
                if (updatedInvitations.containsKey(myName)) {
                    updatedInvitations[myName] = statusValue
                }
                transaction.update(docRef, "invitations", updatedInvitations)
            }.addOnFailureListener { it.printStackTrace() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Schreibt Live-Koordinaten und Score in die Multiplayer-Session
    fun updateLiveSession(duelId: String, playerEmail: String, lat: Double, lng: Double, spotsCaptured: Int) {
        if (!networkMonitor.isOnline.value) return
        val playerSession = mapOf(
            "lat" to lat,
            "lng" to lng,
            "spotsCaptured" to spotsCaptured,
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
