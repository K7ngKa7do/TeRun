package com.example.terun

enum class SpielStatus { IDLE, LAEUFT, BEENDET }

data class Duell(
    val id: String,
    val name: String,
    val spotsAnzahl: Int,
    val zeitLimitMinuten: Int,
    val spot1Lat: Double = 50.9355, val spot1Lng: Double = 6.9860,
    val spot2Lat: Double = 50.9340, val spot2Lng: Double = 6.9840,
    val spot3Lat: Double = 50.9360, val spot3Lng: Double = 6.9845,
    val spot4Lat: Double = 50.9350, val spot4Lng: Double = 6.9850,
    val spot5Lat: Double = 50.9365, val spot5Lng: Double = 6.9835,
    val gegner: String = ""
)

data class Ergebnis(val name: String, val punkte: Int)
