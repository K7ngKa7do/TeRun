package com.example.terun

import androidx.room.Entity

/**
 * =====================================================================
 * FreundEntity – Datenbankzeile für eine Freundschaftsverbindung
 * =====================================================================
 *
 * VORLESUNG 34 – Entities (Room):
 * Diese Entity speichert Freundschaftsverbindungen zwischen zwei Spielern.
 * Eine Freundschaft wird beidseitig gespeichert:
 * - Spieler A → Spieler B (A sieht B in seiner Freundesliste)
 * - Spieler B → Spieler A (B sieht A in seiner Freundesliste)
 *
 * Zusammengesetzter Primärschlüssel (Composite Primary Key):
 * - Statt einer einzelnen ID werden ZWEI Felder gemeinsam als Primärschlüssel verwendet.
 * - Das verhindert, dass dieselbe Freundschaft doppelt eingetragen wird.
 * - Beispiel: ownerEmail="a@b.de", friendEmail="c@d.de" → einmalige Kombination
 *
 * Status-Werte:
 * - "ACCEPTED"         → bestätigte Freundschaft
 * - "SENT_PENDING"     → Anfrage wurde gesendet, noch nicht bestätigt
 * - "RECEIVED_PENDING" → Anfrage wurde empfangen, noch nicht beantwortet
 */
@Entity(
    tableName = "freunde",
    primaryKeys = ["ownerEmail", "friendEmail"] // Zusammengesetzter Primärschlüssel (VL 34)
)
data class FreundEntity(

    val ownerEmail: String,  // E-Mail des Spielers, dem dieser Freundschaftseintrag gehört

    val friendEmail: String, // E-Mail des befreundeten Spielers

    val status: String = "ACCEPTED" // Aktueller Status der Freundschaft (s. oben)
)
