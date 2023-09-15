package de.tomcory.heimdall.ui.apps.AppDetailScreen

import androidx.compose.animation.scaleIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun RescanFloatingActionButton() {
    ExtendedFloatingActionButton(
        text = { Text(text = "Rescan App") },
        icon = { Icon(Icons.Filled.Refresh, contentDescription = null)},
        onClick = { scaleIn() },
    )
}