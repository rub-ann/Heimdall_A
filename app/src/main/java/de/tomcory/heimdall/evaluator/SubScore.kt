package de.tomcory.heimdall.evaluator

// score has to be between 0.0 and 1.0
data class SubScore constructor(
    val module:String,
    val score: Float,
    val weight: Double = 1.0,
    val respected: Boolean = true
)