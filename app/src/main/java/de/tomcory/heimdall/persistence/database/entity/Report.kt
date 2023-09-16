package de.tomcory.heimdall.persistence.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Report constructor(
        @PrimaryKey
        @ColumnInfo(index = true)
        val packageName:String,

        @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
        val timestamp: Long,
        val mainScore: Double
) {
        override fun toString(): String {
                return """
                        Score Evaluation Report of $packageName:
                                timestamp: $timestamp
                                Score: $mainScore
                """.trimIndent()
        }
}