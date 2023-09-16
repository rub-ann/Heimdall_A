package de.tomcory.heimdall.evaluator.modules

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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

abstract class Module {

    abstract val name: String

    val defaultWeight:Double = 1.0

    abstract suspend fun calculateOrLoad(app: App, context: Context, forceRecalculate:Boolean = false): Result<ModuleResult>

    // TODO work with SubReports
    @Composable
    abstract fun BuildUICard(report: Report?)

    @Composable
    fun UICard(
        title:String,
        infoText: String,
        content: @Composable() () -> Unit
    ){
        var showInfoText: Boolean by remember { mutableStateOf(false) }
        OutlinedCard(
            // modifier = Modifier.padding(10.dp, 10.dp)
        ) {
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
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    IconButton(
                        onClick = { showInfoText = !showInfoText },
                        enabled = true,
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Icon(Icons.Outlined.Info, "infoTextButton")
                    }
                }
                AnimatedVisibility(visible = showInfoText) {
                    Text(text = infoText, style= MaterialTheme.typography.labelMedium.merge(
                        TextStyle(fontStyle = FontStyle.Italic)
                    ))
                    Spacer(modifier = Modifier.height(5.dp))
                }
                content()
            }
        }
    }

    abstract fun exportJSON() : String


    override fun toString(): String {
        return this.name
    }

}


@Preview
@Composable
fun UICardPreview(){
    TrackerScore().UICard(
        title = "TestCard",
        infoText = "This Module examines a particular part of the app. A lower score means is it not particularly privacy conscious."
    ){
        Spacer(modifier = Modifier
            .height(100.dp)
            .fillMaxSize())
    }
}

