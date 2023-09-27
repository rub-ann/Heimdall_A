package de.tomcory.heimdall.ui.home

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutExpo
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@Composable
fun DeviceOverview(
    viewModel: DeviceOverviewViewModel = viewModel(),
    context: Context = LocalContext.current,
    userScanApps: () -> Unit = {viewModel.scanApps(context)},
    showNoReportApps: Boolean = false
) {
    viewModel.updateApps()
    val uiState by viewModel.uiState.collectAsState()
    val animateFloat = remember { Animatable(0f) }

    val title = "Device Privacy Overview"
    val infoText = "This is an overview over the apps installed on your device. They are grouped into 'unacceptable', 'questionable' and 'acceptable' in regards to their privacy impact."

    LaunchedEffect(animateFloat) {
        animateFloat.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2000, easing = EaseInOutExpo)
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp, 0.dp)
    ) {

        AnimatedVisibility(visible = uiState.loadingApps, enter = fadeIn(), exit = fadeOut()) {
                CircularProgressIndicator()
                Text(text = "Loading apps...")
        }

        AnimatedVisibility(visible = !uiState.loadingApps, enter = fadeIn(), exit = fadeOut()) {

            var appSets = viewModel.getAppSetSizes(showNoReportApps)
            val defaultColors = getDefaultColorsFromTheme(showNoReportApps)
            appSets = appSets.mapIndexed { index, item ->
                if (item.color == Color.Unspecified) item.color = defaultColors[index]
                item
            }
            LazyColumn {
                item { Spacer(modifier = Modifier.height(30.dp)) }

                item {
                    TopSegmentBar(title, infoText)
                }
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                }
                item {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        AllAppsChart(appSets = appSets, totalScore = viewModel.getTotalScore())
                    }
                }
                item { Spacer(modifier = Modifier.height(50.dp)) }
                if (appSets.sumOf { it.size } > 0) {
                    item {
                        FlopApps(apps = viewModel.getFlopApps(context))
                    }
                } else {
                    item {
                        Text(text = "No App Info found. Consider scanning")
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

// TODO load thresholds from preferenceStore somehow
data class DeviceOverviewUIState(
    val apps: List<AppWithReports> = listOf(
        AppWithReports(
            App(
                "com.test.package",
                "TestPackage",
                "0.0.1",
                1
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

    var loadingApps: Boolean = true,
)


@Composable
fun getDefaultColorsFromTheme(showNoReportApps: Boolean): List<Color> {
    var colors = listOf(
        noReportScoreColor,
        unacceptableScoreColor,
        questionableScoreColor,
        acceptableScoreColor
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