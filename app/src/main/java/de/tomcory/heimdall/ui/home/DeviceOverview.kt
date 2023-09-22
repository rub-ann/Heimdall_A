package de.tomcory.heimdall.ui.home

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutExpo
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.tomcory.heimdall.R
import de.tomcory.heimdall.persistence.database.dao.AppWithReport
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
    val uiState by viewModel.uiState.collectAsState()
    val animateFloat = remember { Animatable(0f) }

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
            .padding(10.dp, 0.dp)
    ) {
        TopBar()
        Spacer(modifier = Modifier.height(10.dp))
        AnimatedVisibility(visible = uiState.loadingApps, enter = fadeIn(), exit = fadeOut()) {
                CircularProgressIndicator()
                Text(text = "Loading apps...")
        }

        AnimatedVisibility(visible = !uiState.loadingApps, enter = fadeIn(), exit = fadeOut()) {
            Column {

                val appSets = uiState.getAppSetSizes(showNoReportApps)
                val colors = uiState.colors.ifEmpty { getDefaultColorsFromTheme(showNoReportApps) }
                val appSetsWithColors = appSets.zip(colors)
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(R.drawable.ic_heimdall_round),
                        contentDescription = "Heimdall App Icon",
                        modifier = Modifier
                            .size(size = 150.dp)
                            .clickable { /*userScanApps() */ })
                    AllAppsChart(appSets = appSetsWithColors)
                }

                Spacer(modifier = Modifier.size(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Tap to Scan!",
                        style = MaterialTheme.typography.titleSmall.merge(TextStyle(fontStyle = FontStyle.Italic))
                    )
                }
            }
        }
    }
}

// TODO load thresholds from preferenceStore somehow
data class DeviceOverviewUIState(
    val apps:List<AppWithReport> = listOf(AppWithReport(App("com.test.package", "TestPackage", "0.0.1", 1), Report("com.test.package", 1234, 0.76))),
    val appsNoReport: List<AppWithReport> = apps.filter { it.report != null },
    val appsUnacceptable: List<AppWithReport> = apps.filter {
        (it.report?.mainScore ?: -1.0) in 0.0..0.49
    },
    val appsQuestionable: List<AppWithReport> = apps.filter {
        (it.report?.mainScore ?: -1.0) in 0.5..0.74
    },
    val appsAcceptable: List<AppWithReport> = apps.filter {
        (it.report?.mainScore ?: -1.0) >= 0.75
    },

    val colors: List<Color> = listOf(),

    var loadingApps: Boolean = true,
    ) {
    fun getAppSetSizes(showNoReport: Boolean = false): List<Int> {
        var sets = listOf(appsNoReport, appsUnacceptable, appsQuestionable, appsAcceptable).map { it.size }
        if (!showNoReport) {sets = sets.drop(1)}
        return sets
    }


}

@Composable
fun TopBar(){
    var showInfoText: Boolean by remember { mutableStateOf(false) }

    val infoText = "This is an overview over the apps installed on your device. They are grouped into 'unacceptable', 'questionable' and 'acceptable' in regards to their privacy impact."
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Device Overview",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        IconButton(
            onClick = {showInfoText = !showInfoText},
            enabled = true,
            modifier = Modifier.padding(0.dp)
        ) {
            Icon(Icons.Outlined.Info, "infoTextButton")
        }
    }
    AnimatedVisibility(visible = showInfoText) {
        Text(text = infoText, style= MaterialTheme.typography.displaySmall.merge(
            TextStyle(fontStyle = FontStyle.Italic)
        ))
        Spacer(modifier = Modifier.height(5.dp))
    }
}

@Composable
fun getDefaultColorsFromTheme(showNoReportApps: Boolean): List<Color>{
    var colors = listOf(noReportScoreColor, unacceptableScoreColor, questionableScoreColor, acceptableScoreColor)
    if (!showNoReportApps) colors = colors.drop(1)
    return colors
}

@Preview
@Composable
fun DeviceOverviewPreview() {
    DeviceOverview()
}