package de.tomcory.heimdall.evaluator.modules

import android.content.Context
import android.content.pm.PackageManager
import de.tomcory.heimdall.evaluator.SubScore
import androidx.compose.runtime.Composable
import de.tomcory.heimdall.persistence.database.entity.App

abstract class Module constructor(){

    open abstract val name: String;

    open val defaultWeight:Double = 1.0;

    abstract fun calculate(): Result<SubScore>;

    // TODO work with SubScores
    @Composable
    abstract fun UICard(app: App, context: Context)

    override fun toString(): String {
        return this.name
    }
}