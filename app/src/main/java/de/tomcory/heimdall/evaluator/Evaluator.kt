package de.tomcory.heimdall.evaluator

import android.content.Context
import de.tomcory.heimdall.evaluator.modules.Module
import de.tomcory.heimdall.evaluator.modules.ModuleFactory
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.database.entity.Report
import de.tomcory.heimdall.persistence.database.entity.SubReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import org.json.JSONObject
import timber.log.Timber

/**
 * Evaluator TODO
 *
 * @constructor Create empty Evaluator
 */
class Evaluator {
    /**
     * List of Modules known to the evaluator. Only modules included here are considered in evaluation.
     * Should not be set manually ore altered, only set trough [ModuleFactory.registeredModules].
     */
    private var modules: List<Module>
    private val moduleFactory: ModuleFactory = ModuleFactory

    /**
     * instantiates an [ModuleFactory] Object and sets [modules] registered modules.
     */
    init {
        Timber.d("Evaluator init")
        this.modules = moduleFactory.registeredModules
    }

    /**
     * @return List of registered [Module] instances
     */
    fun getModules(): List<Module> {
        return this.modules
    }

    /**
     * Main action of [Evaluator].
     * Evaluate an app by package name trough calling all registered [modules] to evaluate the app. Resulting scores are collected and calculates average as main Score.
     * Creates and stores a [Report] with this score and additional metadata in the database.
     * Currently, only weights set in Modules are respected.
     * Suspends for fetching pp info from database.
     *
     * @param packageName
     * @param context
     * @see Report
     * @return Pair of the created Report and  a List of corresponding SubReports of the different modules
     */
    suspend fun evaluateApp(packageName: String, context: Context): Pair<Report, List<SubReport>>? {

        // fetching app info from db
        val app = HeimdallDatabase.instance?.appDao?.getAppByName(packageName)

        // break if no info found
        if (app == null) {
            Timber.d("Evaluation of $packageName failed, because Database Entry not found")
            return null
        }

        Timber.d("Evaluating score of $packageName")

        val results = mutableListOf<ModuleResult>()

        // count of valid module responses. Might be reduced during loop - needed to calculate average
        var validResponses = this.modules.size

        // looping trough and requesting evaluation
        for (module in this.modules) {

            // Result of current module
            val result = module.calculateOrLoad(app, context)

            // case: Result failure - no valid result
            if (result.isFailure) {
                // logging failure
                Timber.w(
                    result.exceptionOrNull(),
                    "Module $module encountered Error while evaluating $app"
                )
                // reducing count for average computation
                validResponses--
                continue
            }
            // case: Result success - valid response
            else {
                // should not be necessary to call safe getOrNull but compiler complained
                result.getOrNull()?.let {

                    // add to result collection
                    results.add(it)

                    // logging
                    Timber.d("module $module result: ${it.score}")
                }
            }
        }
        // compute average score, respecting (default) weights of modules
        val totalScore = results.fold(0.0) { sum, s -> sum + s.score * s.weight } / validResponses
        // logging
        Timber.d("evaluation complete for ${app.packageName}: $totalScore}")
        // creating Report and stroring database
        return createReport(app.packageName, totalScore, results)
    }

    /**
     * Create a [Report] for the app and transforms [ModuleResult] to [SubReport], using current time as timestamp.
     * If successful, storing both in database
     *
     * @param packageName
     * @param totalScore Overall Score computed for app
     * @param moduleResults List of ModuleResults
     * @return  Pair of the created Report and transformed SubReports
     */
    private suspend fun createReport(
        packageName: String,
        totalScore: Double,
        moduleResults: MutableList<ModuleResult>
    ): Pair<Report, List<SubReport>>? {
        // logging
        Timber.d("writing Report and SubReports to Database")

        // creating Report with current time as timestamp
        val report = Report(
            appPackageName = packageName,
            timestamp = System.currentTimeMillis(),
            mainScore = totalScore
        )

        // storing report in database
        HeimdallDatabase.instance?.reportDao?.insertReport(report)?.let { reportId ->
            // if successful, create SubReport for each ModuleResult
            val subReports = moduleResults.map() { result ->
                SubReport(
                    moduleResult = result,
                    reportId = reportId,
                    packageName = packageName,
                    timestamp = System.currentTimeMillis()
                )
            }
            // bulk store in database
            HeimdallDatabase.instance?.subReportDao?.insertSubReport(subReports)
            return Pair(report, subReports)
        }
        // case: failure
        Timber.d("failed to write report for $packageName to database")
        return null
    }

    /**
     * Export report to json, fetching corresponding SubScores and
     *
     * @param report
     * @return
     */
    suspend fun exportReportToJson(report: Report?): String {
        if (report == null) return JSONObject.NULL.toString()
        CoroutineScope(Dispatchers.IO).launch {

        }
        val subReports: List<JsonElement> = modules.map {
            val subReport =
                HeimdallDatabase.instance?.subReportDao?.getSubReportsByPackageNameAndModule(
                    report.appPackageName,
                    it.name
                )
            it.exportToJsonObject(subReport)
        }
        val subReportList = JsonArray(subReports)
        var serializedReport: JsonObject = Json.encodeToJsonElement(report) as JsonObject
        serializedReport = JsonObject(serializedReport.plus(Pair("subReports", subReportList)))
        Timber.d("exported report $report\nto JSON:\n$serializedReport")
        return serializedReport.toString()
    }

    companion object {
        var instance: Evaluator = Evaluator()
            private set

        @JvmStatic
        fun init(): Boolean {
            instance = Evaluator()
            return true
        }
    }

}