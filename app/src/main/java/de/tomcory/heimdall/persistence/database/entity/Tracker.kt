package de.tomcory.heimdall.persistence.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class Tracker(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val categories: String,
    val codeSignature: String,
    val networkSignature: String,
    val creationDate: String,
    val web: String
)