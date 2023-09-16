package de.tomcory.heimdall.evaluator

data class ModuleResult (
    val module:String,
    val score: Float,
    val weight: Double = 1.0,
    val additionalDetails: String = ""
)