package de.tomcory.heimdall.evaluator.modules

import de.tomcory.heimdall.evaluator.SubScore

object StaticPermissions: Module() {
    override val name: String = "StaticPermissionScore";

    override fun calculate(): Result<SubScore> {
        val score:Double = 0.0;
        return Result.success(SubScore(this.name, this.defaultWeight, score))
    }
}