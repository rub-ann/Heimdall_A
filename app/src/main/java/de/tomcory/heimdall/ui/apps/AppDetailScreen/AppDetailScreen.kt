package de.tomcory.heimdall.ui.apps.AppDetailScreen

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import de.tomcory.heimdall.evaluator.Evaluator
import de.tomcory.heimdall.evaluator.modules.Module
import de.tomcory.heimdall.persistence.database.dao.AppWithReport
import de.tomcory.heimdall.persistence.database.entity.App
import de.tomcory.heimdall.persistence.database.entity.Report
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAppDetailScreen(
    appWithReport: AppWithReport,
    onDismissRequest: () -> Unit,
    factory: AppDetailViewModelFactory = AppDetailViewModelFactory(appWithReport),
    appDetailViewModel: AppDetailViewModel = viewModel(factory = factory),
    context: Context = LocalContext.current,
    userRescanApp: () -> Unit = {appDetailViewModel.rescanApp(context)},
    userUninstallApp : ()-> Unit = {appDetailViewModel.uninstallApp(context)}
) {

    val appDetailUiState by appDetailViewModel.uiState.collectAsState()
    appDetailViewModel.updateApp(appWithReport)

    var dropdownExpanded by remember { mutableStateOf(false) }

    Timber.d("Showing Details of $appWithReport")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(0.dp, 0.dp, 12.dp, 0.dp),
                title = {
                    ListItem(headlineContent = { Text(text = appDetailUiState.packageLabel) },
                        supportingContent = {
                            Text(
                                text = appDetailUiState.packageName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingContent = {
                            Image(
                                painter = rememberDrawablePainter(drawable = appDetailUiState.packageIcon),
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
                            onClick = { userRescanApp() }
                        )
                        DropdownMenuItem(
                            text = { Text("Uninstall") },
                            onClick = {
                                userUninstallApp()
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
        floatingActionButton = { RescanFloatingActionButton(userRescanApp) },

    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(8.dp, 0.dp)
        ){
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                ScoreCard(report = appDetailUiState.report)
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
            items(appDetailUiState.modules){module ->
                module.BuildUICard(report = appWithReport.report)
                Spacer(modifier = Modifier.height(9.dp))
            }
        }
    }
}



data class AppDetailScreeUIState(
    var appWithReport:AppWithReport = AppWithReport(App("com.test.package", "TestPackage", "0.0.1", 1), Report("com.test.package", 1234, 0.76)),
    val app:App = appWithReport.app,

    var report: Report? = appWithReport.report,

    val packageLabel:String = app.label,
    val packageName:String = app.packageName,
    val packageIcon: Drawable? = app.icon,

    val modules: List<Module> = Evaluator.modules,
    var dropdownExpanded: MutableState<Boolean> = mutableStateOf(false)
)


@Preview
@Composable
fun NewAppDetailScreenPreview() {
    val app = App(
        packageName = "test.package.com",
        label = "TestApp",
        versionName = "v0.1",
        versionCode = 0
    )
    val report = Report(mainScore = 0.76, timestamp = 1234, packageName = "com.test.package")
    NewAppDetailScreen(appWithReport = AppWithReport(app, report), onDismissRequest = { })
}

