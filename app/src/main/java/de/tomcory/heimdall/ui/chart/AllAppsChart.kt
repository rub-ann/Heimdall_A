package de.tomcory.heimdall.ui.chart

import android.content.res.Resources
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.tomcory.heimdall.ui.theme.*
import timber.log.Timber

@Composable
fun AllAppsChart(
    thickness: Dp = 12.dp,
    size: Dp = 240.dp,
    bottomGap: Float = 60f,
    appSets: List<Pair<Int, Color>>
) {
    val total = appSets.sumOf { it.first }
    val arcRange = 360f - bottomGap

    // Convert each value to angle
    val sweepAngles: List<Float> = appSets.map {
        arcRange * it.first / total
    }


    Timber.d("Drawing Acrs - $appSets - total: $total, sweeps: $sweepAngles")
    Box(contentAlignment = Alignment.Center) {
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
                    Timber.d("drawing arc from $startAngle for ${sweepAngles[setIndex]}")
                    // val brush = BlueGradientBrush
                    drawArc(
                        color = appSets[setIndex].second,
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
                    sweepAngle = (360f - bottomGap / 2), // * animateFloat.value,
                    useCenter = false,
                    style = Stroke(width = thickness.toPx(), cap = StrokeCap.Round),
                    size = Size(arcRadius, arcRadius),
                    topLeft = Offset(
                        x = (size.toPx() - arcRadius) / 2,
                        y = (size.toPx() - arcRadius) / 2
                    ),
                    alpha = 1f,
                    blendMode = BlendMode.Multiply
                )
            }
        }
    }
}

@Preview
@Composable
fun AllAppsChartPreview(){
    val sets = listOf(4, 4, 4)
    val colors = listOf(unacceptableScoreColor, questionableScoreColor, acceptableScoreColor)
    AllAppsChart(appSets = sets.zip(colors))
}