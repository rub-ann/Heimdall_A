package de.tomcory.heimdall.evaluator

import android.content.Context
import android.content.pm.PackageInfo
import de.tomcory.heimdall.evaluator.modules.Module
import de.tomcory.heimdall.evaluator.modules.ModuleFactory
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.database.entity.Report
import timber.log.Timber

object Evaluator {
    var modules: List<Module>
    val moduleFactory: ModuleFactory = ModuleFactory

    init {
        Timber.d("Evaluator init")
        this.modules = moduleFactory.registeredModules
    }

    suspend fun evaluateApp(packageName: String, context: Context): Report? {

        val app = HeimdallDatabase.instance?.appDao?.getAppByName(packageName)
        if (app == null){
            Timber.d("Evaluation of ${packageName} failed, because Database Entry not found")
            return null
        }
        Timber.d("Evaluating score of ${app.packageName}")
        val scores = mutableListOf<SubReport>()
        var n = this.modules.size

        for (module in this.modules) {

            val result = module.calculateOrLoad(app, context)
            if (result.isFailure) {
                Timber.log(
                    3,
                    result.exceptionOrNull(),
                    "Module $module encountered Error while evaluating $app"
                )
                n--
                continue
            }
            result.getOrNull()?.let {
                scores.add(it)
                Timber.d("module $module subscore: ${it.score}")
            }

        }
        val totalScore = scores.fold(0.0){sum, s -> sum + s.score * s.weight} / n
        Timber.d("evaluation complete for ${app.packageName}: $totalScore}")
        return createReport(app.packageName, totalScore, scores)
    }


    private suspend fun createReport(packageName:String, totalScore: Double, subScores: List<SubReport>): Report {
        val report = Report(packageName = packageName, timestamp = System.currentTimeMillis(), mainScore = totalScore)
        Timber.d("$report")
        HeimdallDatabase.instance?.reportDao?.insertReport(report)
        return report
    }

}