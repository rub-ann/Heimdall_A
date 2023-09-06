package de.tomcory.heimdall.evaluator

data class SubScore(
    val module:String,
    val weight: Double,
    val score: Double,
    val respected: Boolean = true
)