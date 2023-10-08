package de.tomcory.heimdall.ui.home

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.tomcory.heimdall.persistence.database.dao.AppWithReports
import de.tomcory.heimdall.persistence.database.entity.App
import de.tomcory.heimdall.persistence.database.entity.Report
import de.tomcory.heimdall.ui.chart.AllAppsChart
import de.tomcory.heimdall.ui.theme.acceptableScoreColor
import de.tomcory.heimdall.ui.theme.noReportScoreColor
import de.tomcory.heimdall.ui.theme.questionableScoreColor
import de.tomcory.heimdall.ui.theme.unacceptableScoreColor
import kotlinx.coroutines.flow.MutableStateFlow

/**
 *
 */
@Composable
fun DeviceOverview(
    viewModel: DeviceOverviewViewModel = viewModel(),
    context: Context = LocalContext.current,
    userScanApps: () -> Unit = { viewModel.scanApps(context) },
    showNoReportApps: Boolean = false
): DeviceOverviewViewModel {
    val uiState by viewModel.uiState.collectAsState()

    val title = "Device Privacy Overview"
    val infoText =
        "This is an overview over the apps installed on your device. They are grouped into 'unacceptable', 'questionable' and 'acceptable' in regards to their privacy impact."

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp, 0.dp)
    ) {

        AnimatedVisibility(visible = uiState.loadingApps, enter = fadeIn(), exit = fadeOut()) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Text(text = "Loading apps...")
            }
        }

        AnimatedVisibility(visible = !uiState.loadingApps, enter = fadeIn(), exit = fadeOut()) {

            var appSets = viewModel.getAppSetSizes(showNoReportApps)
            val defaultColors = getDefaultColorsFromTheme(showNoReportApps)
            appSets = appSets.mapIndexed { index, item ->
                if (item.color == Color.Unspecified) item.color = defaultColors[index]
                item
            }
            LazyColumn(
                Modifier.paddingFromBaseline(top = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    TopSegmentBar(title, infoText)
                }
                item {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        AllAppsChart(appSets = appSets, totalScore = viewModel.getTotalScore())
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
                if (appSets.sumOf { it.size } > 0) {
                    item {
                        FlopApps(apps = viewModel.getFlopApps(context))
                    }
                } else {
                    item {
                        Text(text = "No App Info found. Consider scanning")
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
                item {
                    Button(
                        onClick = { userScanApps() },
                        enabled = !uiState.scanningApps
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Scan Device Icon")
                        Text(text = "Scan Device")
                    }
                }
                item {
                    AnimatedVisibility(visible = uiState.scanningApps) {
                        val progress by uiState.scanAppsProgressFlow.collectAsState()
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.size(width = 200.dp, height = 8.dp),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
                //}
                item { Spacer(modifier = Modifier.height(70.dp)) }
            }
        }
    }
    return viewModel
}

// TODO load thresholds from preferenceStore somehow
data class DeviceOverviewUIState(
    val apps: List<AppWithReports> = listOf(
        AppWithReports(
            App(
                "com.test.package", "TestPackage", "0.0.1", 1
            ),
            listOf(Report(appPackageName = "com.test.package", timestamp = 1234, mainScore = 0.76))
        )
    ),
    val appsNoReport: List<AppWithReports> = apps.filter { it.getLatestReport() != null },
    val appsUnacceptable: List<AppWithReports> = apps.filter {
        (it.getLatestReport()?.mainScore ?: -1.0) in 0.0..0.49
    },
    val appsQuestionable: List<AppWithReports> = apps.filter {
        (it.getLatestReport()?.mainScore ?: -1.0) in 0.5..0.74
    },
    val appsAcceptable: List<AppWithReports> = apps.filter {
        (it.getLatestReport()?.mainScore ?: -1.0) >= 0.75
    },

    val colors: List<Color> = listOf(),
    var scanningApps: Boolean = false,
    val scanAppsProgressFlow: MutableStateFlow<Float> = MutableStateFlow(0f),
    var loadingApps: Boolean = true,
)


@Composable
fun getDefaultColorsFromTheme(showNoReportApps: Boolean): List<Color> {
    var colors = listOf(
        noReportScoreColor, unacceptableScoreColor, questionableScoreColor, acceptableScoreColor
    )
    if (!showNoReportApps) colors = colors.drop(1)
    return colors
}

@Composable
fun HelpTextBox(infoText: String) {
    Text(
        text = infoText, style = MaterialTheme.typography.bodySmall.merge(
            TextStyle(fontStyle = FontStyle.Italic)
        )
    )
    Spacer(modifier = Modifier.height(5.dp))
}

@Preview
@Composable
fun DeviceOverviewPreview() {
    DeviceOverview()
}