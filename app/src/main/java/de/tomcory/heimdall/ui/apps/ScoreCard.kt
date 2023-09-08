package de.tomcory.heimdall.ui.apps

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.tomcory.heimdall.persistence.database.entity.Report

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreCard(report: Report) {

    ElevatedCard(
        onClick = { /*TODO*/ },
        modifier = Modifier
            .padding(8.dp, 0.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp, 12.dp)
        ) {

            Text(
                text = "Score",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (report != null) {
                Text(text = "${report.mainScore * 100}", style = MaterialTheme.typography.titleLarge)
                Text(text = "/ 100", style = MaterialTheme.typography.labelSmall)
            } else {
                Text(text = "No Score found in Database. Consider re-scanning")
            }
        }
    }
}

