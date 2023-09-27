package de.tomcory.heimdall.evaluator.modules

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import de.tomcory.heimdall.evaluator.ModuleResult
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.database.entity.App
import de.tomcory.heimdall.persistence.database.entity.Report
import de.tomcory.heimdall.persistence.database.entity.SubReport
import de.tomcory.heimdall.persistence.database.entity.Tracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import timber.log.Timber

class TrackerScore : Module() {
    override val name: String = "TrackerScore"
    val label = "Tracker Libraries"

    override suspend fun calculateOrLoad(
        app: App,
        context: Context,
        forceRecalculate: Boolean
    ): Result<ModuleResult> {
        // TODO implement lazy loading from Database
        val trackers = HeimdallDatabase.instance?.appXTrackerDao
            ?.getAppWithTrackers(app.packageName)?.trackers ?: listOf()

        val score = maxOf(1f - trackers.size * 0.2f, 0f)
        val additionalDetails: String = Json.encodeToString(trackers)
        return Result.success(ModuleResult(this.name, score, additionalDetails = additionalDetails))
    }

    @Composable
    override fun BuildUICard(report: Report?) {
        super.UICard(
            title = this.label,
            infoText = "This modules scans for Libraries in the apps code that are know to relate to tracking and lists them."
        ) {
            val context = LocalContext.current
            LibraryUICardContent(report = report, context)
        }
    }

    private fun loadSubReportFromDB(report: Report): SubReport? {
        return HeimdallDatabase.instance?.subReportDao?.getSubReportsByPackageNameAndModule(
            report.appPackageName,
            name
        )
    }

    private fun decode(info: String): List<Tracker> {
        Timber.d("trying to decode: $info")
        return try {
            Json.decodeFromString(info)
        } catch (e: Exception) {
            Timber.w(e, "Failed to decode in module: ${this.name}")
            listOf<Tracker>()
        }
    }

    private fun loadAndDecode(report: Report?): List<Tracker> {
        var trackers = listOf<Tracker>()
        var subReport: SubReport? = null
        report?.let {
            subReport = loadSubReportFromDB(report = report)
        }

        subReport?.let {
            trackers = decode(it.additionalDetails)
        }
        return trackers
    }

    @Composable
    fun LibraryUICardContent(report: Report?, context: Context) {
        var trackers: List<Tracker> by remember { mutableStateOf(listOf()) }
        var loadingTrackers by remember { mutableStateOf(true) }

        LaunchedEffect(key1 = 2, block = {
            this.launch(Dispatchers.IO) {
                trackers = loadAndDecode(report)
                loadingTrackers = false
            }
        })

        AnimatedVisibility(visible = loadingTrackers, enter = fadeIn(), exit = fadeOut()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        AnimatedVisibility(
            visible = !loadingTrackers,
            enter = slideInVertically(),
            exit = slideOutVertically()
        ) {
            Column {
                if (trackers.isNotEmpty()) {
                    for (tracker in trackers) {
                        ListItem(
                            headlineContent = { Text(text = tracker.name) },
                            supportingContent = { Text(text = tracker.web) },
                            modifier = Modifier.clickable(tracker.web.isNotEmpty()) {
                                // open the tracker's URL in the browser
                                val browserIntent =
                                    Intent(Intent.ACTION_VIEW, Uri.parse(tracker.web))
                                ContextCompat.startActivity(context, browserIntent, null)
                            }
                        )
                    }
                } else {
                    Text(text = "0 tracker libraries found")
                }
            }
        }
    }

    override fun exportToJsonObject(subReport: SubReport?): JsonObject {
        if (subReport == null) return buildJsonObject {
            put(name, "No Info found")
        }
        val trackers =
            Json.encodeToJsonElement(Json.decodeFromString<List<Tracker>>(subReport.additionalDetails)).jsonArray
        var serializedJsonObject: JsonObject = Json.encodeToJsonElement(subReport).jsonObject
        serializedJsonObject = JsonObject(serializedJsonObject.minus("additionalDetails"))
        val additionalPair = Pair("trackers", trackers)
        return JsonObject(serializedJsonObject.plus(additionalPair))
    }

    override fun exportToJson(subReport: SubReport?): String {
        return exportToJsonObject(subReport).toString()
    }
}