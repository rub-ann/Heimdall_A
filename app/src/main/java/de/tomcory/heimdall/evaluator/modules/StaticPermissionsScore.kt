package de.tomcory.heimdall.evaluator.modules

import de.tomcory.heimdall.evaluator.SubScore

object StaticPermissionsScore: Module() {
    override val name: String = "StaticPermissionScore";

    override fun calculate(): Result<SubScore> {
        val score:Double = 0.5;
        return Result.success(SubScore(this.name, this.defaultWeight, score))
    }
}