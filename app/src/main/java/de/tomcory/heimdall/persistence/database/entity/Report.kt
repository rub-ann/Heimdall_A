package de.tomcory.heimdall.persistence.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Report constructor(
        val packageName:String,
        val timestamp: Long,
        //val subScores: List<SubScore>, TODO()
        val mainScore: Double
) {
        @PrimaryKey (autoGenerate = true)
        @ColumnInfo(index = true)
        var reportId: Int = 0

        override fun toString(): String {
                return """
                        Score Evaluation Report of $packageName:
                                ID: $reportId    timestamp: $timestamp
                                Score: $mainScore
                                SubScores: // not yet stored part of DB because difficult to store in DB
                """.trimIndent()
        }
}
