package de.tomcory.heimdall.evaluator.modules

import de.tomcory.heimdall.persistence.database.entity.SubScore
import timber.log.Timber

abstract class Module {

    abstract val name: String;

    open val defaultWeight:Double = 1.0;

    fun calculate(): Result<SubScore> {
        val score:Double = 1.0;
        return Result.success(SubScore(this.name, this.defaultWeight, score))
    }
}