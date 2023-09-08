package de.tomcory.heimdall.persistence.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.ExperimentalTime

@Entity
data class Report @OptIn(ExperimentalTime::class) constructor(
        val packageName:String,
        val timestamp: Long,
        //val subScores: List<SubScore>, TODO()
        val mainScore: Double
) {
        @PrimaryKey (autoGenerate = true)
        @ColumnInfo(index = true)
        var reportId: Int = 0
}
