package de.tomcory.heimdall.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.ViewModelFactoryDsl
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.database.dao.AppWithReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber


@ViewModelFactoryDsl
class DeviceOverviewViewModel() : ViewModel() {

    private val _uiState : MutableStateFlow<DeviceOverviewUIState> = MutableStateFlow(DeviceOverviewUIState())
    val uiState: StateFlow<DeviceOverviewUIState> = _uiState.asStateFlow()
    init {
        CoroutineScope(Dispatchers.IO).launch {
            val apps = HeimdallDatabase.instance?.appDao?.getInstalledUserAppWithReports() ?: listOf()
            _uiState.update { DeviceOverviewUIState(apps, loadingApps = false) }
        }
    }

    fun loadPreferences(){

    }

    fun scanApps(context: Context) {
        TODO("Not yet implemented")
        CoroutineScope(Dispatchers.IO).launch {
            Timber.d("scanning all apps")
            //ScanManager(context).scanAllApps()

        }
    }

    fun getFlopApps(n: Int = 5): List<AppWithReport> {
        return uiState.value.apps.sortedByDescending { it.report?.mainScore}.subList(0, 5)
    }

}