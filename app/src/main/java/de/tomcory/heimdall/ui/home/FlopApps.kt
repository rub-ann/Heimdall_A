package de.tomcory.heimdall.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.tomcory.heimdall.persistence.database.dao.AppWithReports
import de.tomcory.heimdall.ui.apps.AppListItem
import de.tomcory.heimdall.ui.apps.appDetailScreen.NewAppDetailScreen

@Composable
fun FlopApps(apps: List<AppWithReports>) {

    val title = remember { "Flop Apps" }
    val infoText = remember {
        "These are the worst and most privacy intrusive apps on your device. Maybe you want to reconsider using them."
    }
    TopSegmentBar(title, infoText)

    Column() {
        for (app in apps) {
            var showAppDetailDialog by remember { mutableStateOf(false) }
            AppListItem(
                appWithReports = app,
                modifier = Modifier.clickable {
                    showAppDetailDialog = true
                }
            )
            if (showAppDetailDialog) {
                Dialog(
                    onDismissRequest = { showAppDetailDialog = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    NewAppDetailScreen(
                        appWithReports = app,
                        onDismissRequest = { showAppDetailDialog = false })
                }
            }
        }
    }

}
