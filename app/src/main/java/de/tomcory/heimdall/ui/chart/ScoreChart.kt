package de.tomcory.heimdall.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Cyan
import androidx.compose.ui.text.TextStyle

@Preview
@Composable
fun ScoreChartPreview() {
    ScoreChart(
        score = 0.0
    )
}

@Composable
fun ScoreChart(
    score: Double,
    colors: List<Color> = listOf(
        MaterialTheme.colorScheme.error,
        Color(0xFF1EB980)
    ),
    max: Double = 100.0,
    size: Dp = 240.dp,
    thickness: Dp = 20.dp,
    backgroundCircleColor: Color = Color.LightGray.copy(alpha = 0.3f),
    bottomGap: Float = 60f
) {

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .size(size)
            ) {

                var arcRadius = size.toPx() - thickness.toPx()
                var startAngle = bottomGap / 2
                val sweepAngle:Float = ((360-bottomGap/2 - startAngle) * (score/100)).toFloat()

                /*
                drawCircle(
                    color = backgroundCircleColor,
                    radius = arcRadius / 2,
                    style = Stroke(width = thickness.toPx(), cap = StrokeCap.Butt)
                )
                   */
                //arcRadius -= gapBetweenCircles.toPx()
                val colorArcOffset = (bottomGap / 360) / 2
                val brush = Brush.sweepGradient(
                    0f + colorArcOffset to colors[0],
                    1f - colorArcOffset to colors[1]
                )
                rotate(90f) {
                    drawArc(
                        brush = brush,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = thickness.toPx(), cap = StrokeCap.Round),
                        size = Size(arcRadius, arcRadius),
                        topLeft = Offset(
                            x = (size.toPx() - arcRadius) / 2,
                            y = (size.toPx() - arcRadius) / 2
                        ),
                        //alpha = 0f
                    )
                }

            }
            Box {
                val textGradientColors = listOf(Cyan, Color(0xFF0066FF), Color(0xFFdd21d1) /*...*/)
                //brush = Brush.linearGradient(colors = textGradientColors)
                //style = MaterialTheme.typography.displayLarge
                Text(text = score.toInt().toString(), style = MaterialTheme.typography.displayLarge.merge(
                    TextStyle(brush = Brush.linearGradient(colors = textGradientColors))
                ), fontWeight = FontWeight.SemiBold)
            }
            Box(modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(vertical = 12.dp)) {
                Text(text = "/ ${max.toInt()}", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
            }
        }

        //Spacer(modifier = Modifier.height(4.dp + (0.5f * thickness.value).dp))

        /*
        Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
            for (i in values.indices) {
                DisplayLegend(value = values[values.size - 1 - i], color = colors[values.size - 1 - i], legend = legend[values.size - 1 - i])
            }
        }
         */
    }
}