package de.tomcory.heimdall.ui.apps

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableContainer
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.database.entity.Report
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAppDetailScreen(
//    viewModel: AppDetailViewModel = hiltViewModel(),
//    navigateToUpdateBookScreen: (bookId: Int) -> Unit,
    packageName: String, packageLabel: String = "NameNotFound", report: Report? = null, onDismissRequest: () -> Unit){
//    val report by viewModel.reports.collectAsState(
//        initial = emptyList()
//    )

    val context = LocalContext.current
    val pm = context.packageManager

    val pkgInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            pm.getPackageInfo(packageName, 4096)
        }

    val packageLabel = pkgInfo?.applicationInfo?.loadLabel(pm).toString() ?: "NameNotFound" //pkgInfo?.applicationInfo?.loadLabel(pm).toString() ?:
    val packageName = pkgInfo?.packageName ?: packageName //
    val packageIcon = pkgInfo?.applicationInfo?.loadIcon(pm) //

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(0.dp, 0.dp, 12.dp, 0.dp),
                title = {ListItem(headlineContent = { Text(text = packageLabel) },
                    supportingContent = { Text(text = packageName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingContent = { Image(painter = rememberDrawablePainter(drawable = packageIcon), contentDescription = "App icon", modifier = Modifier.size(40.dp)) })
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
                }
            )
        },
        floatingActionButton = { RescanFloatingActionButton() }
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                ScoreCard(report = Report("test", 12345, 0.5))
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                PermissionCard(pkgInfo = pkgInfo, pm = pm)
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                LibraryCard(pkgInfo = pkgInfo)
            }
        }
    }
}

@Preview
@Composable
fun NewAppDetailScreenPreview() {
    NewAppDetailScreen(packageName = "test", onDismissRequest = { false })
}

