package de.tomcory.heimdall.evaluator.modules

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.tomcory.heimdall.evaluator.ModuleResult
import de.tomcory.heimdall.persistence.database.entity.App
import de.tomcory.heimdall.persistence.database.entity.Report
import de.tomcory.heimdall.persistence.database.entity.SubReport
import kotlinx.serialization.json.JsonObject

/**
 * Abstract class as template for all modules describing an evaluation metric.
 * All Modules should inherit from this and implement the mandatory properties and function.
 *
 * @constructor Create empty Module
 */
abstract class Module {

    /**
     * Name describing this metric. Used for
     */
    abstract val name: String

    /**
     * Weight factor used in [Evaluator] score computation
     */
    val weight: Double = 1.0

    /**
     * Main function to compute score for an app in regards to the privacy metric this module implements.
     * Dynamic modules can use context of app data to generate a result every call.
     * Static modules can decide to load a possibly existing score from the database if the metric relies on non-changing parameters to reduce load.
     *
     * @param app Database entry containing app metadata
     * @param context Context to enable computation resources like package manager
     * @param forceRecalculate Indicates to static modules that must recompute
     * @return [Result] containing a [ModuleResult] if successful
     */
    abstract suspend fun calculateOrLoad(
        app: App,
        context: Context,
        forceRecalculate: Boolean = false
    ): Result<ModuleResult>

    // TODO make an evaluator function that loads sub-reports from db so that this is only called
    //  from evaluator and takes a a sub-report as argument
    /**
     * Called by UI and should return a generated `@Composable` [UICard]
     *
     * @param report [Report] the UICard should describe
     */
    @Composable
    abstract fun BuildUICard(report: Report?)

    /**
     * Standard format `@Composable` the UI expects to display additional metric details.
     *
     * @param title Heading title of the UI Card. Should be understandable by users
     * @param infoText Help text to be displayed if the user wishes more information about this metric
     * @param content `@Composable` UI content of the card
     * @receiver
     */
    @Composable
    fun UICard(
        title: String,
        infoText: String,
        content: @Composable () -> Unit
    ) {
        // indicated if the help text should be displayed or not
        var showInfoText: Boolean by remember { mutableStateOf(false) }
        OutlinedCard {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp, 0.dp, 10.dp, 10.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Card Title
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    // Button to toggle explanatory help text
                    IconButton(
                        onClick = { showInfoText = !showInfoText },
                        enabled = true,
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Icon(Icons.Outlined.Info, "infoTextButton")
                    }
                }
                // Help text explaining the additional scoring details of the module
                AnimatedVisibility(visible = showInfoText) {
                    Text(
                        text = infoText, style = MaterialTheme.typography.labelMedium.merge(
                            TextStyle(fontStyle = FontStyle.Italic)
                        )
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                }
                // render Card content
                content()
            }
        }
    }

    /**
     * Returns [name] for display and logging purposes
     */
    override fun toString(): String {
        return this.name
    }

    /**
     * Transforms [subReport] to [JsonObject], decoding and appending [SubReport.additionalDetails] as class specific property;
     * returning one coherent JsonObjet.
     *
     * Use [exportToJson] for String representation.
     * @see exportToJson
     */
    abstract fun exportToJsonObject(subReport: SubReport?): JsonObject

    /**
     * Similar to [exportToJsonObject], but returning JSON as [String].
     * @see exportToJsonObject
     */
    abstract fun exportToJson(subReport: SubReport?): String
}

/**
 * Debugging Preview of UICard Composable
 */
@Preview
@Composable
fun UICardPreview() {
    TrackerScore().UICard(
        title = "Title",
        infoText = "Info text; should explain what this module does on what data it displays"
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = "Arbitrary content of card")
        Spacer(modifier = Modifier.height(10.dp))
    }
}

