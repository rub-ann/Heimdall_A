package de.tomcory.heimdall.evaluator.modules

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.tomcory.heimdall.evaluator.SubReport
import de.tomcory.heimdall.persistence.database.dao.AppWithReports
import de.tomcory.heimdall.persistence.database.entity.App
import de.tomcory.heimdall.ui.apps.DonutChart
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

class StaticPermissionsScore: Module() {
    override val name: String = "StaticPermissionScore"
    val label: String = "Permissions"

    override suspend fun calculateOrLoad(
        app: App,
        context: Context,
        forceRecalculate: Boolean
    ): Result<SubReport> {


        val pm = context.packageManager
        val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(app.packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            pm.getPackageInfo(app.packageName, 4096)
        }

        val permProts = pkgInfo.requestedPermissions.map { perm ->
            try {
                pm.getPermissionInfo(perm, PackageManager.GET_META_DATA).protection
            } catch (e: Exception) {
                Timber.w("Unknown permission: %s", perm)
                PermissionInfo.PROTECTION_NORMAL
            }
        }

        val countDangerous = permProts.count { perm -> perm == PermissionInfo.PROTECTION_DANGEROUS }

        val countSignature = permProts.count { perm -> perm == PermissionInfo.PROTECTION_SIGNATURE }

        val countNormal = permProts.count { perm -> perm == PermissionInfo.PROTECTION_NORMAL }

        val score = maxOf(1f - countDangerous *0.1f - countSignature *0.05f - countNormal * 0.01f, 0f)

        val details = Json.encodeToString(PermissionInfo(countDangerous, countSignature, countNormal))
        return Result.success(SubReport(this.name, score, additionalDetails = details))
    }

    @Composable
    override fun BuildUICard(app: AppWithReports) {
        super.UICard(
            title = this.label,
            infoText = "This modules inspects the permissions the app might request at some point. These are categorized into 'Dangerous', 'Signature' and 'Normal'"
        ){
            val context = LocalContext.current
            UICardContent(app, context.packageManager)
        }
    }


    override fun exportJSON(): String {
        TODO("Not yet implemented")
    }
}

@Composable
fun UICardContent(appWithReports: AppWithReports, pm: PackageManager) {
    val app = appWithReports.app
    val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getPackageInfo(
            app.packageName,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
        )
    } else {
        pm.getPackageInfo(app.packageName, 4096)
    }

    val permProts = pkgInfo.requestedPermissions?.map { perm ->
        try {
            pm.getPermissionInfo(perm, PackageManager.GET_META_DATA).protection
        } catch (e: Exception) {
            Timber.w("Unknown permission: %s", perm)
            PermissionInfo.PROTECTION_NORMAL
        }
    }
    
    if (permProts != null) {


        val countDangerous = permProts.count { perm -> perm == PermissionInfo.PROTECTION_DANGEROUS }

        val countSignature = permProts.count { perm -> perm == PermissionInfo.PROTECTION_SIGNATURE }

        val countNormal = permProts.count { perm -> perm == PermissionInfo.PROTECTION_NORMAL }

        Column(
            modifier = Modifier.padding(12.dp, 12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            DonutChart(
                values = listOf(
                    countDangerous.toFloat(),
                    countNormal.toFloat(),
                    countSignature.toFloat()
                ),
                legend = listOf("Dangerous", "Normal", "Signature"),
                size = 150.dp,
                colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.tertiary
                )
            )
        }
    } else{
        // No Permission info found
        Text(text = "No permission information found.")
    }
}

@Serializable
data class PermissionInfo(val dangerousPermissionCount: Int, val signaturePermissionCount: Int, val normalPermissionCount: Int)

