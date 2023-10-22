package de.tomcory.heimdall.ui.apps

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.tomcory.heimdall.ui.theme.acceptableScoreColor
import de.tomcory.heimdall.ui.theme.questionableScoreColor
import de.tomcory.heimdall.ui.theme.unacceptableScoreColor

// private val colors = mapOf("red" to Color(0xFF914406), "yellow" to Color(0xFF5e5006), "green" to Color(0xFF437a5a))
private val colors = mapOf(
    "red" to unacceptableScoreColor,
    "yellow" to questionableScoreColor,
    "green" to acceptableScoreColor
)


@Composable
fun SmallScoreIndicator(score:Double, size: Dp = 50.dp) {
    var backgroundColor = if (score > .75) colors["green"]!!
    else if (score > .50) colors["yellow"]!!
    else colors["red"]!!

    Box(modifier = Modifier
        .size(size)
    ) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .align(Alignment.Center)) {
            // val brush = Brush.radialGradient()
            drawRoundRect(
                color = backgroundColor,
                cornerRadius = CornerRadius(10f, 10f)
                )
        }
        Text(
            text = "${(score * 100).toInt()}",
            style = MaterialTheme.typography.headlineSmall.merge(
                TextStyle(color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
            ),
            modifier = Modifier.align(Alignment.Center)
        )
    }

}

@Preview
@Composable
fun SmallScoreIndicatorPreviewAcceptable() {
    SmallScoreIndicator(score = .90)
}
@Preview
@Composable
fun SmallScoreIndicatorPreviewQustionable() {
    SmallScoreIndicator(score = .70)
}

@Preview
@Composable
fun SmallScoreIndicatorPreviewUnacceptable() {
    SmallScoreIndicator(score = .23)
}