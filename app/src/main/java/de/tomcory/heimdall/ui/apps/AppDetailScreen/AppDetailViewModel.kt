package de.tomcory.heimdall.ui.apps.AppDetailScreen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.ViewModelFactoryDsl
import de.tomcory.heimdall.evaluator.Evaluator
import de.tomcory.heimdall.persistence.database.dao.AppWithReports
import de.tomcory.heimdall.util.OsUtils.uninstallPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber


class AppDetailViewModelFactory(private val appWithReports: AppWithReports) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AppDetailViewModel(appWithReports) as T
    }
}

@ViewModelFactoryDsl
class AppDetailViewModel(appWithReports: AppWithReports) : ViewModel() {
    private val _uiState = MutableStateFlow(AppDetailScreeUIState(appWithReports))
    val uiState: StateFlow<AppDetailScreeUIState> = _uiState.asStateFlow()


    fun rescanApp(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            Timber.d("Rescanned ${uiState.value.packageName}")
            val report = Evaluator.instance.evaluateApp(uiState.value.packageName, context)?.first
            if (report != null) {
                Timber.d("Updated UI with new report")
                _uiState.value.report = report
            } else {
                Timber.d("No new report")
            }
        }
    }

    fun uninstallApp(context: Context) {
        uninstallPackage(context, uiState.value.packageName)
    }

    fun updateApp(app: AppWithReports) {
        _uiState.value = AppDetailScreeUIState(app)
    }

    fun exportToJson() {
        CoroutineScope(Dispatchers.IO).launch {
            Evaluator.instance.exportReportToJson(uiState.value.report)
            // TODO write to local file
        }
    }

}
