package de.tomcory.heimdall.evaluator

import android.content.Context
import de.tomcory.heimdall.evaluator.modules.Module
import de.tomcory.heimdall.evaluator.modules.ModuleFactory
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.database.entity.Report
import de.tomcory.heimdall.persistence.database.entity.SubReport
import timber.log.Timber

object Evaluator {
    var modules: List<Module>
    val moduleFactory: ModuleFactory = ModuleFactory

    init {
        Timber.d("Evaluator init")
        this.modules = moduleFactory.registeredModules
    }

    suspend fun evaluateApp(packageName: String, context: Context): Pair<Report, List<SubReport>>? {

        val app = HeimdallDatabase.instance?.appDao?.getAppByName(packageName)
        if (app == null){
            Timber.d("Evaluation of $packageName failed, because Database Entry not found")
            return null
        }
        Timber.d("Evaluating score of $packageName")
        val results = mutableListOf<ModuleResult>()
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
            } else {
                result.getOrNull()?.let {
                    results.add(it)
                    Timber.d("module $module result: ${it.score}")
                }
            }

        }
        // TODO implement weights
        val totalScore = results.fold(0.0){sum, s -> sum + s.score} / n
        Timber.d("evaluation complete for ${app.packageName}: $totalScore}")
        return createReport(app.packageName, totalScore, results)
    }


    private suspend fun createReport(packageName:String, totalScore: Double, moduleResults: MutableList<ModuleResult>): Pair<Report, List<SubReport>> {
        val report = Report(packageName = packageName, timestamp = System.currentTimeMillis(), mainScore = totalScore)
        val subReports = moduleResults.map(){result ->
            SubReport(result, packageName)
        }
        Timber.d("writing Report and SubReports to Database")
        HeimdallDatabase.instance?.reportDao?.insertReport(report)
        HeimdallDatabase.instance?.subReportDao?.insertSubReport(subReports)
        return Pair(report, subReports)
    }

}