package de.tomcory.heimdall.evaluator

import android.content.pm.PackageInfo
import de.tomcory.heimdall.evaluator.modules.Module
import de.tomcory.heimdall.evaluator.modules.StaticPermissionsScore
import de.tomcory.heimdall.evaluator.modules.TrackerScore
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.database.entity.AppXReport
import de.tomcory.heimdall.persistence.database.entity.Report
import timber.log.Timber

object Evaluator {
    var modules: List<Module>

    init {
        //TODO("implement automatic module discovery")
        Timber.d("Evaluator onCreate")
        this.modules = listOf(
            StaticPermissionsScore,
            TrackerScore)
    }

    suspend fun evaluateApp(app: PackageInfo) {

        Timber.d("Evaluating score of ${app.packageName}")
        val scores = mutableListOf<SubScore>()
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
            result.getOrNull()?.let {
                scores.add(it)
                Timber.d("module $module subscore: ${it.score}")
            }

        }
        val totalScore = scores.fold(0.0){sum, s -> sum + s.score * s.weight} / n
        Timber.d("evaluation complete for ${app.packageName}: $totalScore}")
        createReport(app.packageName, totalScore, scores)
    }


    private suspend fun createReport(packageName:String, totalScore: Double, subScores: List<SubScore>){
        val report = Report(packageName = packageName, timestamp = System.currentTimeMillis(), mainScore = totalScore)
        Timber.d("$report")
        HeimdallDatabase.instance?.reportDao?.insertReport(report)
            ?.let { rowId ->
                HeimdallDatabase.instance?.reportDao?.getReportByRowId(rowId)?.let {
                HeimdallDatabase.instance?.appXReportDao?.insert(AppXReport(packageName, it))
            }
        }

    }
}