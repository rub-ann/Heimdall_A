package de.tomcory.heimdall.persistence.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.ComparableTimeMark
import kotlin.time.ExperimentalTime

data class SubScore(
        val module:String,
        val weight: Double,
        val score: Double,
        val respected: Boolean = true
)
@Entity
data class Report @OptIn(ExperimentalTime::class) constructor(
        @PrimaryKey
        @ColumnInfo(index = true)
        val reportId:String,
        val packageName:String,
        val timestamp: ComparableTimeMark,
        val subScores: Map<String, Result<SubScore>>,
        val mainScore: Double
)
