package de.tomcory.heimdall.ui.apps

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.database.dao.AppWithReports
import de.tomcory.heimdall.persistence.database.entity.App
import de.tomcory.heimdall.persistence.database.entity.Report
import de.tomcory.heimdall.scanner.code.ScanManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class AppsScreenViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AppsScreenUIState())
    val uiState: StateFlow<AppsScreenUIState> = _uiState.asStateFlow()
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    init {
        updateApps()
    }

    fun updateApps() {
        viewModelScope.launch { fetchApps() }
    }

    private suspend fun fetchApps() = withContext(ioDispatcher) {
        Timber.d("loading apps for apps screen from DB")
        val apps =
            HeimdallDatabase.instance?.appDao?.getInstalledUserAppWithReports() ?: listOf()
        _uiState.update { AppsScreenUIState(apps, loadingApps = false) }
        Timber.d("finished loading ${apps.size} apps from DB")
    }

    // Icons fetched externally because Context from UI is needed.
    // Context should never be statically stored in viewModel to avoid memory leaks
    suspend fun loadIcons(context: Context) = withContext(ioDispatcher) {
        Timber.d("loading apps for apps screen from DB")
        var apps = uiState.value.apps
        apps = apps.map {
            if (it.app.icon == null && it.app.isInstalled) {
                it.app.icon = ScanManager.getAppIcon(context, it.app.packageName)
            }
            it
        }
        _uiState.update { AppsScreenUIState(apps, loadingApps = false) }
        Timber.d("finished loading ${apps.size} apps from DB")
    }
}

data class AppsScreenUIState(
    val apps: List<AppWithReports> = listOf(
        AppWithReports(
            App(
                "com.test.package",
                "TestPackage",
                "0.0.1",
                1
            ),
            listOf(Report(appPackageName = "com.test.package", timestamp = 1234, mainScore = 0.76))
        )
    ),
    var loadingApps: Boolean = true,
)