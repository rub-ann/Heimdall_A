package de.tomcory.heimdall.ui.apps.appDetailScreen

import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.database.entity.Tracker

@Deprecated(
    "UI card creation has been moved into the modules",
    ReplaceWith(
        "Module.buildUICard()",
        imports = ["de.tomcory.heimdall.evaluator.modules.Module.buildUICard()"]
    )
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryCard(pkgInfo: PackageInfo) {

    val context = LocalContext.current
    var trackers = listOf<Tracker>()
    var loadingTrackers by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = null, block = {
        trackers = HeimdallDatabase.instance?.appXTrackerDao
            ?.getAppWithTrackers(pkgInfo.packageName)?.trackers ?: trackers

        loadingTrackers = false
    })

    ElevatedCard(
        onClick = { /*TODO*/ },
        modifier = Modifier
            .padding(8.dp, 0.dp)
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp, 12.dp)
        ) {

            Text(
                text = "Tracker Libraries",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                                    startActivity(context, browserIntent, null)
                                }
                            )
                        }
                    } else {
                        Text(text = "0 tracker libraries found")
                    }
                }
            }
        }
    }
}