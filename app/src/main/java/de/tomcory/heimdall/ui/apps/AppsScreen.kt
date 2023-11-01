package de.tomcory.heimdall.ui.apps

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import de.tomcory.heimdall.R
import de.tomcory.heimdall.persistence.database.dao.AppWithReports
import de.tomcory.heimdall.persistence.database.entity.App
import de.tomcory.heimdall.persistence.database.entity.Report
import de.tomcory.heimdall.ui.apps.appDetailScreen.NewAppDetailScreen

@Composable
fun AppInfoList(paddingValues: PaddingValues, apps: List<AppWithReports>) {
    if (apps.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                text = "No apps found in Database. Consider rescanning",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.labelMedium
            )
        }
    } else {
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(apps) {
                var showAppDetailDialog by remember { mutableStateOf(false) }
                AppListItem(
                    appWithReports = it,
                    modifier = Modifier.clickable {
                        showAppDetailDialog = true
                    }
                )

                if (showAppDetailDialog) {
                    Dialog(
                        onDismissRequest = { showAppDetailDialog = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        NewAppDetailScreen(
                            appWithReports = it,
                            onDismissRequest = { showAppDetailDialog = false })
                    }
                }
            }
        }
    }
}

@Composable
fun AppListItem(appWithReports: AppWithReports, modifier: Modifier) {
    val app = remember {
        appWithReports.app
    }
    ListItem(
        headlineContent = {
            if (!app.isInstalled) {
                StrikethroughText(text = app.label)
            } else {
                Text(text = app.label)
            }

        },
        supportingContent = {
            if(!app.isInstalled) {
                StrikethroughText(text = app.packageName)
            } else {
                Text(text = app.packageName)
            }
        },
        leadingContent = @Composable {
            val painter = if (app.icon == null) {
                painterResource(R.drawable.robot)
            } else {
                rememberDrawablePainter(drawable = app.icon)
            }

            val colorFilter = remember {
                if (!app.isInstalled) {
                    ColorFilter.colorMatrix(ColorMatrix().apply {
                        setToSaturation(0f) // setting saturation to 0 will convert image to grayscale
                    })
                } else {
                    null
                }
            }

            Image(painter = painter, contentDescription = "App icon", modifier = Modifier.size(40.dp), colorFilter = colorFilter)
        },
        trailingContent = {
            val score: Double? = remember { appWithReports.getLatestReport()?.mainScore }
            score?.let {
                SmallScoreIndicator(score = score)
            }

        },
        modifier = modifier
    )
}

@Composable
fun StrikethroughText(text: String, modifier: Modifier = Modifier) {
    val annotatedString = remember {
        buildAnnotatedString {
            withStyle(style = SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                append(text)
            }
        }
    }

    Text(text = annotatedString, modifier = modifier)
}

@Composable
fun AppsScreen(
    viewModel: AppsScreenViewModel = viewModel(),
    context: Context = LocalContext.current,
) {

    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(key1 = 0) {
        viewModel.loadIcons(context)
    }
    Scaffold(
        topBar = {
            AppsTopBar()
        }
    ) {
        AnimatedVisibility(visible = uiState.loadingApps, enter = fadeIn(), exit = fadeOut()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                CircularProgressIndicator()
                Text(text = "Loading apps...")
            }
        }

        AnimatedVisibility(visible = !uiState.loadingApps, enter = fadeIn(), exit = fadeOut()) {
            AppInfoList(it, apps = uiState.apps)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsTopBar() {
    var searchActive by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        SearchBar(
            placeholder = { Text(text = "Search apps") },
            query = "",
            onQueryChange = {},
            onSearch = {},
            active = searchActive,
            onActiveChange = { searchActive = it },
            leadingIcon = {
                if (searchActive) {
                    IconButton(onClick = { searchActive = false }) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = ""
                        )
                    }
                } else {
                    IconButton(onClick = { searchActive = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = ""
                        )
                    }
                }
            },
            trailingIcon = {
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = ""
                    )
                }
            },
        ) {}
    }
}

@Preview(showBackground = true)
@Composable
fun AppsScreenPreview() {
    AppsScreen()
}

@Preview
@Composable
fun AppListItemPreview() {
    val app = App("test.package.com", "TestApp", "0.1", 1)
    val reports =
        listOf(Report(appPackageName = "test.package.com", timestamp = 1234, mainScore = 0.76))
    val appWithReports = AppWithReports(app, reports)
    AppListItem(
        appWithReports,
        Modifier
            .height(60.dp)
            .fillMaxWidth()
    )
}


data class AppInfo(
    val packageName: String,
    val label: String,
    var icon: Drawable? = null,
    var flags: Int = 0
)