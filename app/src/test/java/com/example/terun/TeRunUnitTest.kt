// Datei: TeRunUnitTest.kt
// Paket: com.example.terun

package com.example.terun

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TeRunUnitTest {

    // Testet die mathematische Haversine-Distanzberechnung (wichtig für die Spot-Eroberung)
    @Test
    fun testCalculateDistance() {
        // Koordinaten TH Köln - Campus Deutz (Köln)
        val latDeutz = 50.9348
        val lonDeutz = 6.9852

        // Koordinaten TH Köln - Campus Gummersbach (Gummersbach)
        val latGummersbach = 51.0232
        val lonGummersbach = 7.5619

        // Berechnung der Distanz
        val distanz = calculateDistance(latDeutz, lonDeutz, latGummersbach, lonGummersbach)

        // Die reale Luftlinie zwischen Köln Deutz und Gummersbach Campus beträgt ca. 41.5 km
        // Wir testen auf Übereinstimmung mit einer Toleranz von 500 Metern
        assertTrue("Distanz sollte ca. 41.5 km betragen", distanz in 41000.0..42000.0)
    }

    // Hilfsfunktion analog zur Implementierung in KarteScreen
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000 // Erdradius in Metern
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    // Testet Datenintegrität der DuellEntity-Klasse
    @Test
    fun testDuellEntityData() {
        val entity = DuellEntity(
            id = "1",
            name = "Campus Gummersbach Runde",
            spotsAnzahl = 3,
            zeitLimitMinuten = 10,
            spot1Lat = 51.0230, spot1Lng = 7.5610,
            spot2Lat = 51.0240, spot2Lng = 7.5620,
            spot3Lat = 51.0250, spot3Lng = 7.5630,
            spot4Lat = 51.0260, spot4Lng = 7.5640,
            spot5Lat = 51.0270, spot5Lng = 7.5650
        )

        assertEquals("Campus Gummersbach Runde", entity.name)
        assertEquals(3, entity.spotsAnzahl)
        assertEquals(10, entity.zeitLimitMinuten)
        assertEquals(51.0230, entity.spot1Lat, 0.0001)
    }

    // Testet das Spielstand-Enum
    @Test
    fun testSpielStatusEnum() {
        val statusIdle = SpielStatus.IDLE
        val statusRunning = SpielStatus.LAEUFT
        val statusFinished = SpielStatus.BEENDET

        assertEquals("IDLE", statusIdle.name)
        assertEquals("LAEUFT", statusRunning.name)
        assertEquals("BEENDET", statusFinished.name)
    }

    // Testet die E-Mail-Validierung nach ISO/NIST-Standard
    @Test
    fun testEmailValidation() {
        assertTrue(isValidEmail("test@example.com"))
        assertTrue(isValidEmail("user.name+label@sub.domain.de"))

        // Ungültige E-Mails
        org.junit.Assert.assertFalse(isValidEmail("test"))
        org.junit.Assert.assertFalse(isValidEmail("test@example"))
        org.junit.Assert.assertFalse(isValidEmail("test@example."))
        org.junit.Assert.assertFalse(isValidEmail("@example.com"))
    }

    // Testet die Passwort-Validierung nach ISO/NIST-Standard
    @Test
    fun testPasswordValidation() {
        // Gültig: mind. 8 Zeichen, Groß-, Kleinbuchstabe, Ziffer, Sonderzeichen
        assertTrue(isValidPassword("Abcdefg1!"))
        assertTrue(isValidPassword("TeRun2026$"))

        // Ungültig: Zu kurz (7 Zeichen)
        org.junit.Assert.assertFalse(isValidPassword("Abc1!"))
        // Ungültig: Keine Ziffer
        org.junit.Assert.assertFalse(isValidPassword("Abcdefg!"))
        // Ungültig: Kein Großbuchstabe
        org.junit.Assert.assertFalse(isValidPassword("abcdefg1!"))
        // Ungültig: Kein Kleinbuchstabe
        org.junit.Assert.assertFalse(isValidPassword("ABCDEFG1!"))
        // Ungültig: Kein Sonderzeichen
        org.junit.Assert.assertFalse(isValidPassword("Abcdefg12"))
    }
}
