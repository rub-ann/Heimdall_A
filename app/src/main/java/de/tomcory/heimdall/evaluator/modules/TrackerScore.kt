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
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

class TrackerScore: Module() {
    override val name: String = "TrackerScore"
    val label = "Tracker Libraries"

    override suspend fun calculateOrLoad(app: App, context: Context, forceRecalculate: Boolean): Result<ModuleResult> {
        // TODO implement lazy loading from Database
        val trackers = HeimdallDatabase.instance?.appXTrackerDao
            ?.getAppWithTrackers(app.packageName)?.trackers ?: listOf()

        val score = maxOf(1f - trackers.size * 0.2f, 0f)
        val additionalDetails:String = Json.encodeToString(trackers)
        return Result.success(ModuleResult(this.name, score, additionalDetails = additionalDetails))
    }

    @Composable
    override fun BuildUICard(report: Report?) {
        super.UICard(
            title = this.label,
            infoText = "This modules scans for Libraries in the apps code that are know to relate to tracking and lists them."
        ){
            val context = LocalContext.current
            LibraryUICardContent(report = report, context)
        }
    }

    private suspend fun loadAndDecode(report: Report?): List<Tracker> {
        var trackers = listOf<Tracker>()
        var subReport: SubReport? = null
        report?.let {
            subReport = HeimdallDatabase.instance?.subReportDao
                ?.getSubReportsByPackageNameAndModule(report.packageName, name)
        }

        subReport?.let {
            try {
                trackers =
                    Json.decodeFromString(it.additionalDetails)
            } catch (e: Exception) {
                Timber.w(e, "Failed to decode in module: ${it.additionalDetails}")
            }
            Timber.d("loaded and decoded from DB: $trackers")
        }
        return trackers
    }

    @Composable
    fun LibraryUICardContent(report: Report?, context: Context) {
        var trackers: List<Tracker> by remember { mutableStateOf(listOf())}
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

        AnimatedVisibility(visible = !loadingTrackers, enter = slideInVertically(), exit = slideOutVertically()) {
            Column {
                if (trackers.isNotEmpty()) {
                    for (tracker in trackers) {
                        ListItem(
                            headlineContent = { Text(text = tracker.name) },
                            supportingContent = { Text(text = tracker.web) },
                            modifier = Modifier.clickable(tracker.web.isNotEmpty()) {
                                // open the tracker's URL in the browser
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(tracker.web))
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

    override fun exportJSON(): String {
        TODO("Not yet implemented")
    }
}


@Serializable
data class TrackerList(val trackerList: List<Tracker> = listOf())