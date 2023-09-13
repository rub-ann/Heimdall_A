package de.tomcory.heimdall.ui.apps

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import de.tomcory.heimdall.evaluator.Evaluator
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.database.entity.App
import de.tomcory.heimdall.persistence.database.entity.Report
import de.tomcory.heimdall.util.OsUtils.uninstallPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAppDetailScreen(
    app: App,
    onDismissRequest: () -> Unit
) {

    val context = LocalContext.current

    var loadingReports by remember { mutableStateOf(true) }
    var reports by remember { mutableStateOf(listOf<Report>()) }

    val packageLabel =
        app.label //pkgInfo?.applicationInfo?.loadLabel(pm).toString() ?:
    val packageName = app.packageName// pkgInfo?.packageName ?: packageName //
    val packageIcon = app.icon //pkgInfo?.applicationInfo?.loadIcon(pm) //

    val modules = Evaluator.modules
    var dropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = null, block = {
        reports = getAppWithReports(app.packageName)
        loadingReports = false
    })

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(0.dp, 0.dp, 12.dp, 0.dp),
                title = {
                    ListItem(headlineContent = { Text(text = packageLabel) },
                        supportingContent = {
                            Text(
                                text = packageName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingContent = {
                            Image(
                                painter = rememberDrawablePainter(drawable = packageIcon),
                                contentDescription = "App icon",
                                modifier = Modifier.size(40.dp)
                            )
                        })
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.padding(0.dp, 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Close dialog"
                        )
                    }
                },
                actions = {
                    IconToggleButton(checked = false, onCheckedChange = { dropdownExpanded = !dropdownExpanded}, content = { Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More AppDetail Options"
                    )})
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {  Text("Rescan") },
                            onClick = { /* Handle refresh! */ }
                        )
                        DropdownMenuItem(
                            text = { Text("Uninstall") },
                            onClick = {
                                uninstallPackage(context, app.packageName)
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Send Feedback") },
                            onClick = { /* Handle send feedback! */ }
                        )
                    }
                }
            )
        },
        floatingActionButton = { RescanFloatingActionButton() },

    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
/*                AnimatedVisibility(visible = loadingReports, enter = fadeIn(), exit = fadeOut()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator()
                        Text(text = "Loading reports..")
                    }
                }*/

                AnimatedVisibility(visible = !loadingReports, enter = fadeIn(), exit = fadeOut()) {
                    ScoreCard(report = reports.lastOrNull())
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(modules){module ->
                module.UICard(app = app, context = context)
                Spacer(modifier = Modifier.height(9.dp))
            }
        }
    }
}

@Composable
fun AppDetailList(){
    
}


suspend fun getAppWithReports(packageName: String): List<Report> =
    withContext(Dispatchers.IO) {
        return@withContext HeimdallDatabase.instance?.reportDao?.getAppWithReports(packageName) ?: listOf()
    }

@Preview
@Composable
fun NewAppDetailScreenPreview() {
    val app = App(
        packageName = "test.package.com",
        label = "TestApp",
        versionName = "v0.1",
        versionCode = 0
    )
    NewAppDetailScreen(app = app, onDismissRequest = { false })
}

