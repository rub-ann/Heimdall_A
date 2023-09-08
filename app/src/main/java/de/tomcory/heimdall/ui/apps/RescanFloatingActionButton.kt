package de.tomcory.heimdall.ui.apps

import android.graphics.drawable.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescanFloatingActionButton() {
    ExtendedFloatingActionButton(
        text = { Text(text = "Rescan App") },
        icon = { Icon(Icons.Filled.Refresh, contentDescription = null)},
        onClick = { /*TODO*/ })
}