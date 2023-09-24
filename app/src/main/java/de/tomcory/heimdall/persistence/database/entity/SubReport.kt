package de.tomcory.heimdall.persistence.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import de.tomcory.heimdall.evaluator.ModuleResult
import kotlinx.serialization.Serializable

@Serializable
abstract class SubReportBase {
    abstract val reportId: Long
    abstract val packageName: String
    abstract val module: String
    abstract val score: Float
    abstract val weight: Double
    abstract val timestamp: Long
}

// score has to be between 0.0 and 1.0
// additional data jas to be serialized as String; eg. as JSON
@Entity(primaryKeys = ["reportId", "module"])
data class SubReport(
    @ColumnInfo(index = true)
    override val reportId: Long,
    override val packageName: String,
    override val module: String,
    override val score: Float,
    override val weight: Double = 1.0,
    @ColumnInfo(defaultValue = "CURRENT_TIMESTAMP")
    override val timestamp: Long,
    val additionalDetails: String = "",
) : SubReportBase() {
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
    )
}