package de.tomcory.heimdall.persistence.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import de.tomcory.heimdall.evaluator.SubScore
import kotlin.time.ComparableTimeMark
import kotlin.time.ExperimentalTime

@Entity
data class Report @OptIn(ExperimentalTime::class) constructor(
        @PrimaryKey (autoGenerate = true)
        @ColumnInfo(index = true)
        val reportId:String,
        val packageName:String,
        val timestamp: ComparableTimeMark,
        val subScores: Map<String, Result<SubScore>>,
        val mainScore: Double
)
