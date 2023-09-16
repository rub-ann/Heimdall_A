package de.tomcory.heimdall.evaluator

// score has to be between 0.0 and 1.0
// additional data jas to be serialized as String; eg. as JSON
data class SubReport constructor(
    val module:String,
    val score: Float,
    val weight: Double = 1.0,
    val additionalDetails: String = ""
)