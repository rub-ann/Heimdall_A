package de.tomcory.heimdall.persistence.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import de.tomcory.heimdall.evaluator.ModuleResult

// score has to be between 0.0 and 1.0
// additional data jas to be serialized as String; eg. as JSON
@Entity(primaryKeys = ["packageName","module"])
data class SubReport (
    @ColumnInfo(index = true)
    val packageName: String,
    val module:String,
    val score: Float,
    val weight: Double = 1.0,
    val additionalDetails: String = "",
){
    constructor(moduleResult: ModuleResult, packageName: String, weight: Double = moduleResult.weight): this(
        packageName = packageName,
        module = moduleResult.module,
        score = moduleResult.score,
        weight = weight,
        additionalDetails = moduleResult.additionalDetails){}
}