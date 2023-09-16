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
import de.tomcory.heimdall.evaluator.SubReport
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.database.dao.AppWithReports
import de.tomcory.heimdall.persistence.database.entity.App
import de.tomcory.heimdall.persistence.database.entity.Tracker
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TrackerScore: Module() {
    override val name: String = "TrackerScore"
    val label = "Tracker Libraries"

    override suspend fun calculateOrLoad(app: App, context: Context, forceRecalculate: Boolean): Result<SubReport> {
        // TODO implement lazy loading from Database
        val trackers = HeimdallDatabase.instance?.appXTrackerDao
            ?.getAppWithTrackers(app.packageName)?.trackers ?: listOf()

        val score = maxOf(1f - trackers.size * 0.2f, 0f)
        val additionalDetails:String = Json.encodeToString(trackers)
        return Result.success(SubReport(this.name, score, additionalDetails = additionalDetails))
    }

    @Composable
    override fun BuildUICard(app: AppWithReports) {
        super.UICard(
            title = this.label,
            infoText = "This modules scans for Libraries in the apps code that are know to relate to tracking and lists them."
        ){
            val context = LocalContext.current
            LibraryUICardContent(appWithReports = app, context = context)
        }
    }

    override fun exportJSON(): String {
        TODO("Not yet implemented")
    }
}

@Composable
fun LibraryUICardContent(appWithReports: AppWithReports, context: Context) {
    val app = appWithReports.app
    var trackers = listOf<Tracker>()
    var loadingTrackers by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = null, block = {
        trackers = HeimdallDatabase.instance?.appXTrackerDao
            ?.getAppWithTrackers(app.packageName)?.trackers ?: trackers

        loadingTrackers = false
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

@Serializable
data class TrackerList(val trackerList: List<Tracker> = listOf())