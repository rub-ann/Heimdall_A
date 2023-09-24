package de.tomcory.heimdall.ui.chart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.tomcory.heimdall.R
import de.tomcory.heimdall.ui.home.HelpTextBox
import de.tomcory.heimdall.ui.theme.BlueGradientBrush
import de.tomcory.heimdall.ui.theme.GrayScaleGradientBrush
import de.tomcory.heimdall.ui.theme.acceptableScoreColor
import de.tomcory.heimdall.ui.theme.questionableScoreColor
import de.tomcory.heimdall.ui.theme.unacceptableScoreColor
import timber.log.Timber

@Composable
fun AllAppsChart(
    thickness: Dp = 12.dp,
    size: Dp = 240.dp,
    bottomGap: Float = 60f,
    appSets: List<ChartData>,
    totalScore: Float = -1f,
    maxScore: Float = 100f
) {

    val total = appSets.sumOf { it.size }
    val arcRange = 360f - bottomGap

    // Convert each value to angle
    val sweepAngles: List<Float> = appSets.map {
        arcRange * it.size / total
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,

        ) {
        Timber.d("AllAppsChart debug: total: $total, arc: $arcRange, totalScore: $totalScore\n$appSets\n ")
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.ic_heimdall_round),
                contentDescription = "Heimdall App Icon",
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(
                    color = Color.DarkGray,
                    blendMode = BlendMode.Multiply
                ),
                modifier = Modifier
                    .size(size = 150.dp)
                    .clip(CircleShape)
                    .clickable { /*userScanApps() */ })
            Canvas(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(size)
            ) {
                if (total > 0) {
                    val startAngle = 90f + bottomGap / 2
                    val arcRadius = size.toPx() - thickness.toPx()

                    var currentAngle = startAngle
                    for (setIndex in appSets.indices) {
                        // val brush = BlueGradientBrush
                        drawArc(
                            color = appSets[setIndex].color,
                            startAngle = currentAngle,
                            sweepAngle = sweepAngles[setIndex],
                            useCenter = false,
                            style = Stroke(width = thickness.toPx(), cap = StrokeCap.Round),
                            size = Size(arcRadius, arcRadius),
                            topLeft = Offset(
                                x = (size.toPx() - arcRadius) / 2,
                                y = (size.toPx() - arcRadius) / 2
                            ),
                            //blendMode = BlendMode.Multiply,
                            alpha = 1f,
                        )
                        currentAngle += sweepAngles[setIndex]
                    }

                    // underlaying arc for colorblending
                    drawArc(
                        brush = GrayScaleGradientBrush,
                        startAngle = startAngle,
                        sweepAngle = arcRange,
                        useCenter = false,
                        style = Stroke(width = thickness.toPx(), cap = StrokeCap.Round),
                        size = Size(arcRadius, arcRadius),
                        topLeft = Offset(
                            x = (size.toPx() - arcRadius) / 2,
                            y = (size.toPx() - arcRadius) / 2
                        ),
                        blendMode = BlendMode.Softlight,
                        alpha = 1f
                    )
                } else {
                    val arcRadius = size.toPx() - thickness.toPx()
                    val brush = BlueGradientBrush
                    drawArc(
                        brush = brush,
                        startAngle = 90f + bottomGap / 2,
                        sweepAngle = (360f - bottomGap), // * animateFloat.value,
                        useCenter = false,
                        style = Stroke(width = thickness.toPx(), cap = StrokeCap.Round),
                        size = Size(arcRadius, arcRadius),
                        topLeft = Offset(
                            x = (size.toPx() - arcRadius) / 2,
                            y = (size.toPx() - arcRadius) / 2
                        )
                    )
                }
            }
            if (totalScore > -1f) {
                Box {
                    Text(
                        text = totalScore.toInt().toString(),
                        style = MaterialTheme.typography.displayLarge.merge(
                            TextStyle(brush = GrayScaleGradientBrush)
                        ),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                ) {
                    Text(
                        text = "/ ${maxScore.toInt()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Gray
                    )
                }
            }
        }
        if (totalScore > -1f) {
            Spacer(modifier = Modifier.height(30.dp))
            Column(
                Modifier.padding(10.dp, 0.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                Text(text = "of your apps are")
                appSets.forEach {
                    var showInfoText by remember { mutableStateOf(false) }
                    ChartLegendItem(data = it, onTrigger = fun() { showInfoText = !showInfoText })
                    AnimatedVisibility(visible = showInfoText) {
                        HelpTextBox(infoText = it.infoText)
                    }
                }
            }
        }
    }
}

data class ChartData(
    val label: String,
    val size: Int,
    var color: Color,
    val infoText: String = "InfoText missing"
)

@Composable
fun ChartLegendItem(data: ChartData, onTrigger: () -> Unit) {
    ListItem(
        leadingContent = {
            Text(
                color = data.color,
                text = data.size.toString(),
                style = MaterialTheme.typography.displaySmall.merge(
                    TextStyle(color = data.color)
                ),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        },
        /*overlineContent = {
            Text(text = "of your apps are considered", style = MaterialTheme.typography.labelLarge)
                          },*/
        headlineContent = {
            Text(
                text = data.label,
                style = MaterialTheme.typography.headlineSmall.merge(TextStyle(fontStyle = FontStyle.Italic))
            )
        },
        trailingContent = {
            IconButton(
                onClick = { onTrigger() },
                enabled = true,
                modifier = Modifier.padding(0.dp)
            ) {
                Icon(Icons.Outlined.Info, "infoTextButton")
            }
        }
    )
}

@Preview
@Composable
fun AllAppsChartPreview() {
    val sizes = listOf(12, 4, 100)
    val labels = listOf("unacceptable", "questionable", "acceptable")
    val colors = listOf(unacceptableScoreColor, questionableScoreColor, acceptableScoreColor)
    val sets = labels.mapIndexed { index, item ->
        ChartData(
            item,
            size = sizes[index],
            color = colors[index]
        )
    }
    AllAppsChart(appSets = sets, totalScore = 76f)
}