package de.tomcory.heimdall.persistence.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import de.tomcory.heimdall.evaluator.ModuleResult

// score has to be between 0.0 and 1.0
// additional data jas to be serialized as String; eg. as JSON
@Entity(primaryKeys = ["reportId", "module"])
data class SubReport(
    @ColumnInfo(index = true)
    val reportId: Long,
    val packageName: String,
    val module: String,
    val score: Float,
    val weight: Double = 1.0,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    val timestamp: Long,
    val additionalDetails: String = "",
) {
    constructor(
        moduleResult: ModuleResult,
        reportId: Long,
        packageName: String,
        weight: Double = moduleResult.weight,
        timestamp: Long
    ) : this(
        reportId = reportId,
        packageName = packageName,
        module = moduleResult.module,
        score = moduleResult.score,
        weight = weight,
        timestamp = timestamp,
        additionalDetails = moduleResult.additionalDetails
    ) {
    }
}