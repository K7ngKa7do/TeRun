// Datei: FreundEntity.kt
// Paket: com.example.terun

package com.example.terun

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "freunde")
data class FreundEntity(
    @PrimaryKey val name: String
)
