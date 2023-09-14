package de.tomcory.heimdall.evaluator.modules

import android.content.Context
import android.graphics.ColorSpace
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
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import de.tomcory.heimdall.evaluator.SubScore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.tomcory.heimdall.persistence.database.entity.App
import kotlinx.serialization.json.Json

abstract class Module {

    abstract val name: String

    val defaultWeight:Double = 1.0

    abstract suspend fun calculateOrLoad(app: App, context: Context, forceRecalculate:Boolean = false): Result<SubScore>

    // TODO work with SubScores
    @Composable
    abstract fun buildUICard(app: App, context: Context): () -> Unit

    @Composable
    protected fun UICard(
        title:String,
        content: @Composable () -> Unit,
        infoText: String
    ){ModuleUICard(title, content, infoText)}

    abstract fun exportJSON() : String


    override fun toString(): String {
        return this.name
    }

}

@Composable
fun ModuleUICard(
    title:String,
    content: @Composable () -> Unit,
    infoText: String
){
    var showInfoText: Boolean by remember { mutableStateOf(false) }
    Card(
        // modifier = Modifier.padding(10.dp, 10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp, 0.dp),
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
                Text(text = infoText)
            }
            content()
        }
    }
}

@Preview
@Composable
fun UICardPreview(){
    ModuleUICard(
        title = "TestCard",
        content = { Spacer(modifier = Modifier.height(100.dp).fillMaxSize()) },
        infoText = "This Module examines a particular part of the app. A lower score means is it not particularly privacy conscious."
    )
}

