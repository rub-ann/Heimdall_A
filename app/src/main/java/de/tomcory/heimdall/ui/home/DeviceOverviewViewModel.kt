package de.tomcory.heimdall.ui.home

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.ViewModelFactoryDsl
import com.patrykandpatrick.vico.core.extension.sumByFloat
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.database.dao.AppWithReports
import de.tomcory.heimdall.scanner.code.ScanManager
import de.tomcory.heimdall.ui.chart.ChartData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber


@ViewModelFactoryDsl
class DeviceOverviewViewModel() : ViewModel() {

    private val _uiState: MutableStateFlow<DeviceOverviewUIState> =
        MutableStateFlow(DeviceOverviewUIState())
    val uiState: StateFlow<DeviceOverviewUIState> = _uiState.asStateFlow()
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    init {
        updateApps()
    }

    fun updateApps() {
        viewModelScope.launch { fetchApps() }
    }

    private suspend fun fetchApps() = withContext(ioDispatcher) {
        Timber.d("loading apps for home screen from DB")
        val apps =
            HeimdallDatabase.instance?.appDao?.getInstalledUserAppWithReports() ?: listOf()
        _uiState.update { DeviceOverviewUIState(apps, loadingApps = false) }
        Timber.d("finished loading ${apps.size} apps for home screen from DB")
    }

    fun getTotalScore(): Float {
        val apps = listOf(
            uiState.value.appsUnacceptable,
            _uiState.value.appsQuestionable,
            _uiState.value.appsAcceptable
        ).flatten()
        if (apps.isEmpty()) return -1f
        val n = apps.size
        val totalScore = apps.sumByFloat { it.getLatestReport()?.mainScore?.toFloat() ?: 0f }

        return totalScore / n * 100
    }

    fun getAppSetSizes(showNoReport: Boolean = false): List<ChartData> {
        val label = listOf("no evaluation found", "unacceptable", "questionable", "acceptable")
        var sets = listOf(
            uiState.value.appsNoReport,
            uiState.value.appsUnacceptable,
            uiState.value.appsQuestionable,
            uiState.value.appsAcceptable
        ).mapIndexed { index, item ->
            ChartData(
                label[index],
                color = uiState.value.colors.getOrElse(index) { Color.Unspecified },
                size = item.size
            )
        }
        if (!showNoReport) {
            sets = sets.drop(1)
        }
        return sets
    }


    fun scanApps(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            _uiState.update {
                it.copy(scanningApps = true)
            }
            Timber.d("scanning all apps")
            ScanManager.create(context).scanAllApps(context, uiState.value.scanAppsProgressFlow)
            fetchApps()
        }
    }

    fun getFlopApps(context: Context, nFlopApps: Int = 5): List<AppWithReports> {
        var n = nFlopApps
        if (uiState.value.apps.size < n) {
            n = uiState.value.apps.size
        }
        var apps = uiState.value.apps.sortedBy { it.getLatestReport()?.mainScore }.subList(0, n)
        apps = apps.map {
            if (it.app.icon == null && it.app.isInstalled) {
                it.app.icon = ScanManager.getAppIcon(context, it.app.packageName)
            }
            it
        }
        Timber.d("loaded $n flop apps: $apps")
        return apps

    }
}