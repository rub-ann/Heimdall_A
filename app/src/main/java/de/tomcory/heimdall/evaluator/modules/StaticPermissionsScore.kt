package de.tomcory.heimdall.evaluator.modules

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.tomcory.heimdall.evaluator.SubScore
import de.tomcory.heimdall.persistence.database.entity.App
import de.tomcory.heimdall.ui.apps.DonutChart
import timber.log.Timber

class StaticPermissionsScore: Module() {
    override val name: String = "StaticPermissionScore"

    override suspend fun calculateOrLoad(
        app: App,
        context: Context,
        forceRecalculate: Boolean
    ): Result<SubScore> {
        // TODO implement lazy loading from Database
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

        return Result.success(SubScore(this.name, score))
    }

    @Composable
    override fun buildUICard(app: App, context: Context): () -> Unit {
        val uiFactory = @Composable fun() {
            PermissionUICard(app = app, pm = context.packageManager)
        }
        return uiFactory
    }

    override fun exportJSON(): String {
        TODO("Not yet implemented")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionUICard(app: App, pm: PackageManager)
{

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

    DonutChart(
        values = listOf(countDangerous.toFloat(), countNormal.toFloat(), countSignature.toFloat()),
        legend = listOf("Dangerous", "Normal", "Signature"),
        size = 200.dp,
        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary)
    )
}

