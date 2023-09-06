package de.tomcory.heimdall.evaluator

import de.tomcory.heimdall.evaluator.modules.Module
import de.tomcory.heimdall.evaluator.modules.StaticPermissions
import de.tomcory.heimdall.persistence.database.entity.App
import de.tomcory.heimdall.persistence.database.entity.Report
import timber.log.Timber

object EvaluatorService {
    lateinit var modules: List<Module>

    init {
        TODO("implement automatic module discovery")
        this.modules = listOf(StaticPermissions)
    }

    fun onCreate() {
        Timber.d("Evaluator onCreate")
    }

    fun evaluateApp(app: App): Pair<Double, MutableList<SubScore>> {
        var scores = mutableListOf<SubScore>()
        var n = this.modules.size
        for (module in this.modules) {
            val result = module.calculate()
            if (result.isFailure) {
                Timber.log(
                    3,
                    result.exceptionOrNull(),
                    "Module $module encountered Error while evaluating $app"
                )
                n--
                continue
            }
            result.getOrNull()?.let { scores.add(it) }
        }
        val totalScore = scores.fold(0.0){sum, s -> sum + s.score * s.weight}
        return Pair(totalScore, scores)
    }


    fun createReport(app: App): Report {
        val (totalScore, subScores) = this.evaluateApp(app);

        return Report(packageName = app.packageName, )
    }
}