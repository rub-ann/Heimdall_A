package de.tomcory.heimdall.evaluator

import android.content.Context
import de.tomcory.heimdall.evaluator.modules.Module
import de.tomcory.heimdall.evaluator.modules.ModuleFactory
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.database.entity.Report
import de.tomcory.heimdall.persistence.database.entity.SubReport
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import org.json.JSONObject
import timber.log.Timber

/**
 * Evaluator Service responsible for all app evaluation, scoring, creating and storing reports and module specific UI elements.
 * Abstraction interface between modules and application.
 * Should always be called via [Evaluator.instance].
 *
 * @constructor should not be manually constructed, use [Evaluator.instance] instead.
 */
class Evaluator {
    /**
     * List of [Module]s known to the evaluator. Only modules included here are considered in evaluation.
     * Should not be set manually ore altered, only set trough [ModuleFactory.registeredModules].
     */
    private var modules: List<Module>


    /**
     * [ModuleFactory] for instantiating modules
     */
    private val moduleFactory: ModuleFactory = ModuleFactory

    init {
        Timber.d("Evaluator init")
        this.modules = moduleFactory.registeredModules
    }

    /**
     * Returns a [List] of registered [Module] instances know to the Evaluator
     */
    fun getModules(): List<Module> {
        return this.modules
    }

    /**
     * Main action of [Evaluator].
     * Evaluates an app by [packageName] trough calling all registered [modules] to evaluate the app.
     * Resulting scores are collected and calculates average as main Score.
     * Creates and stores a [Report] with this score and additional metadata in the database.
     *
     * Currently, only weights set in Modules are respected.
     *
     * Suspends for fetching app info from database.
     * [context] may be used for computation, e.g. calling package manager.
     *
     * Returns the created [Report] and associated [SubReport]s
     *
     * @see Report
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
        // creating Report and storing database
        return createReport(app.packageName, totalScore, results)
    }

    /**
     * Creates a [Report] for the app containing [packageName] and [totalScore] and transforms [ModuleResult] to [SubReport], adding metadata like current time as timestamp.
     * If successful, storing both in database
     *
     * Returns  Pair of the created Report and transformed SubReports
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

    // TODO export to file
    /**
     * Export [report] to json, fetching corresponding [SubReport]s and issue parsing by each module.
     * Currently only exporting JSON to log.
     *
     * @see kotlinx.serialization.json
     * @return report with supports as json encoded string
     */
    fun exportReportToJson(report: Report?): String {
        // return empty JSON if report is null
        if (report == null) return JSONObject.NULL.toString()

        // fetch corresponding subreports from db and issue encoding for each module
        val subReports: List<JsonElement> = modules.map {
            val subReport =
                HeimdallDatabase.instance?.subReportDao?.getSubReportsByPackageNameAndModule(
                    report.appPackageName,
                    it.name
                )
            // issue encoding of sub-report in module
            // needed because only thies module knows, what data it put in additionalDetails
            it.exportToJsonObject(subReport)
        }
        // create JSON array from list
        val subReportList = JsonArray(subReports)
        // report to JSONObject
        var serializedReport: JsonObject = Json.encodeToJsonElement(report) as JsonObject
        // appending subreports list to JSON Report
        serializedReport = JsonObject(serializedReport.plus(Pair("subReports", subReportList)))
        // logging
        Timber.d("exported report $report\nto JSON:\n$serializedReport")
        // return JSON report as String
        return serializedReport.toString()
    }

    companion object {
        // to assert singleton, this class is instantiated once and then referred to by this property
        var instance: Evaluator = Evaluator()
            private set

        // not sure if tis is necessary; copied from HeimdallDatabase but this installation is less complex
        @JvmStatic
        fun init(): Boolean {
            instance = Evaluator()
            return true
        }
    }

}