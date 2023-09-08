package de.tomcory.heimdall.evaluator.modules

import de.tomcory.heimdall.evaluator.SubScore
import timber.log.Timber

abstract class Module constructor(){

    open abstract val name: String;

    open val defaultWeight:Double = 1.0;

    abstract fun calculate(): Result<SubScore>;

    override fun toString(): String {
        return this.name
    }
}