package de.tomcory.heimdall.persistence.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Report(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(index = true)
        val reportId: Long = 0,
        val appPackageName: String,

        @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
        val timestamp: Long,
        val mainScore: Double
) {
        override fun toString(): String {
                return """
                        Score Evaluation Report of $reportId:
                                package: $appPackageName
                                timestamp: $timestamp
                                Score: $mainScore
                """.trimIndent()
        }
}