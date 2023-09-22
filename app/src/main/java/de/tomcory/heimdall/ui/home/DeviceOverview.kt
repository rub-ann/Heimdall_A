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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.tomcory.heimdall.R
import de.tomcory.heimdall.persistence.database.dao.AppWithReport
import de.tomcory.heimdall.persistence.database.entity.App
import de.tomcory.heimdall.persistence.database.entity.Report
import de.tomcory.heimdall.ui.chart.AllAppsChart

@Composable
fun DeviceOverview(
    viewModel: DeviceOverviewViewModel = viewModel(),
    context: Context = LocalContext.current,
    userScanApps: () -> Unit = {viewModel.scanApps(context)}
) {
    val uiState by viewModel.uiState.collectAsState()
    val animateFloat = remember { Animatable(0f) }

    LaunchedEffect(animateFloat) {
        animateFloat.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = EaseInOutExpo)
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
    ) {
        AnimatedVisibility(visible = uiState.loadingApps, enter = fadeIn(), exit = fadeOut()) {
                CircularProgressIndicator()
                Text(text = "Loading apps...")
        }

        AnimatedVisibility(visible = !uiState.loadingApps, enter = fadeIn(), exit = fadeOut()) {
            Column {

                val appSets = uiState.getAppSetSizes()
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(R.drawable.ic_heimdall_round),
                        contentDescription = "Heimdall App Icon",
                        modifier = Modifier
                            .size(size = 150.dp)
                            .clickable { /*userScanApps() */ })
                    AllAppsChart(appSets = appSets)
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

    val setColors: List<Color> = listOf(Color.LightGray, Color.Red, Color.Yellow, Color.Green),

    var loadingApps: Boolean = true,
    ) {
    fun getAppSetSizes(ignoreNoReport: Boolean = true): List<Pair<Int, Color>> {
        val sets = listOf(appsNoReport, appsUnacceptable, appsQuestionable, appsAcceptable).map { it.size }
        var setAndColors = sets.zip(this.setColors)
        if (ignoreNoReport) {setAndColors = setAndColors.drop(1)}
        return setAndColors
    }
}

@Preview
@Composable
fun DeviceOverviewPreview() {
    DeviceOverview()
}