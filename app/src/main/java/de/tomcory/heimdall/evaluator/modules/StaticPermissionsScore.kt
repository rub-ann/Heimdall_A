package de.tomcory.heimdall.evaluator.modules

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.tomcory.heimdall.evaluator.ModuleResult
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.database.entity.App
import de.tomcory.heimdall.persistence.database.entity.Report
import de.tomcory.heimdall.persistence.database.entity.SubReport
import de.tomcory.heimdall.ui.apps.DonutChart
import de.tomcory.heimdall.ui.theme.altPrimaryColor
import de.tomcory.heimdall.ui.theme.altSecondaryColor
import de.tomcory.heimdall.ui.theme.altTernaryColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.json.JSONObject
import timber.log.Timber

class StaticPermissionsScore : Module() {
    override val name: String = "StaticPermissionScore"
    val label: String = "Permissions"

    override suspend fun calculateOrLoad(
        app: App,
        context: Context,
        forceRecalculate: Boolean
    ): Result<ModuleResult> {


        val pm = context.packageManager
        val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(
                app.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            )
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

        val score =
            maxOf(1f - countDangerous * 0.4f - countSignature * 0.02f - countNormal * 0.01f, 0f)

        val details =
            Json.encodeToString(PermissionCountInfo(countDangerous, countSignature, countNormal))
        return Result.success(ModuleResult(this.name, score, additionalDetails = details))
    }

    @Composable
    override fun BuildUICard(report: Report?) {
        super.UICard(
            title = this.label,
            infoText = "This modules inspects the permissions the app might request at some point. These are categorized into 'Dangerous', 'Signature' and 'Normal'"
        ) {
            UICardContent(report)
        }
    }

    private fun loadSubReportFromDB(report: Report): SubReport? {
        return HeimdallDatabase.instance?.subReportDao?.getSubReportsByPackageNameAndModule(
            report.appPackageName,
            name
        )
    }

    private fun decode(info: String): PermissionCountInfo? {
        Timber.d("trying to decode: $info")

        return try {
            Json.decodeFromString(info)
        } catch (e: Exception) {
            Timber.w(e, "Failed to decode in module: ${this.name}")
            null
        }
    }

    private fun loadAndDecode(report: Report?): PermissionCountInfo? {
        var permissionCountInfo: PermissionCountInfo? = null
        var subReport: SubReport? = null
        report?.let {
            subReport = loadSubReportFromDB(it)
        }
        subReport?.let {
            permissionCountInfo = decode(it.additionalDetails)

        }
        Timber.d("loaded and decoded from DB: $permissionCountInfo")
        return permissionCountInfo
    }

    @Composable
    fun UICardContent(report: Report?) {
        var permissionCountInfo: PermissionCountInfo? by remember { mutableStateOf(null) }
        var loadingPermissions by remember { mutableStateOf(true) }

        LaunchedEffect(key1 = 1) {
            this.launch(Dispatchers.IO) {
                permissionCountInfo = loadAndDecode(report)
                loadingPermissions = false
            }
        }

        AnimatedVisibility(visible = loadingPermissions, enter = fadeIn(), exit = fadeOut()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        AnimatedVisibility(
            visible = !loadingPermissions,
            enter = slideInVertically(),
            exit = slideOutVertically()
        ) {
            Column(
                modifier = Modifier.padding(12.dp, 12.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                permissionCountInfo?.let {
                    DonutChart(
                        values = listOf(
                            it.dangerousPermissionCount.toFloat(),
                            it.signaturePermissionCount.toFloat(),
                            it.normalPermissionCount.toFloat()
                        ),
                        legend = listOf("Dangerous", "Normal", "Signature"),
                        size = 150.dp,
                        colors = listOf(
                            altPrimaryColor,
                            altSecondaryColor,
                            altTernaryColor
                        )
                    )

                }
            }
            if (permissionCountInfo == null) {
                // No Permission info found
                Text(text = "No permission information found.")
            }
        }
    }


    override fun exportToJsonObject(subReport: SubReport?): JsonObject {
        if (subReport == null) return buildJsonObject {
            put(name, JSONObject.NULL as JsonElement)
        }
        val permissionInfo: PermissionCountInfo? = decode(subReport.additionalDetails)
        val permissionInfoJson = Json.encodeToJsonElement(permissionInfo).jsonObject
        var serializedJsonObject: JsonObject = Json.encodeToJsonElement(subReport).jsonObject
        serializedJsonObject = JsonObject(serializedJsonObject.minus("additionalDetails"))
        val additionalPair = Pair("permission info", permissionInfoJson)
        return JsonObject(serializedJsonObject.plus(additionalPair))
    }

    override fun exportToJson(subReport: SubReport?): String {
        return exportToJsonObject(subReport).toString()
    }
}


@Serializable
data class PermissionCountInfo(
    val dangerousPermissionCount: Int,
    val signaturePermissionCount: Int,
    val normalPermissionCount: Int
)
