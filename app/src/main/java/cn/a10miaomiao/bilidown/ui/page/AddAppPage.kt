package cn.a10miaomiao.bilidown.ui.page

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavHostController
import cn.a10miaomiao.bilidown.R
import cn.a10miaomiao.bilidown.common.BiliDownUtils
import cn.a10miaomiao.bilidown.common.ConstantUtil
import cn.a10miaomiao.bilidown.common.datastore.DataStoreKeys
import cn.a10miaomiao.bilidown.common.datastore.dataStore
import cn.a10miaomiao.bilidown.common.datastore.rememberDataStorePreferencesFlow
import cn.a10miaomiao.bilidown.common.localStoragePermission
import cn.a10miaomiao.bilidown.common.molecule.collectAction
import cn.a10miaomiao.bilidown.common.molecule.rememberPresenter
import cn.a10miaomiao.bilidown.common.permission.StoragePermission
import cn.a10miaomiao.bilidown.entity.BiliAppInfo
import cn.a10miaomiao.bilidown.ui.components.PermissionDialog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map


data class AddAppPageState(
    val appList: List<BiliAppInfo>,
    val selectedAppPackageNameSet: Set<String>,
) {
}

sealed class AddAppPageAction {
    class CheckedChange(
        val packageName: String,
    ) : AddAppPageAction()
}


@Composable
fun AddAppPagePresenter(
    action: Flow<AddAppPageAction>,
    context: Context
): AddAppPageState {
    val selectedAppPackageNameSet by rememberDataStorePreferencesFlow(
        context = context,
        key = DataStoreKeys.appPackageNameSet,
        initial = emptySet(),
    ).collectAsState(emptySet())
    var appList by remember {
        mutableStateOf(BiliDownUtils.biliAppList)
    }
    LaunchedEffect(context) {
        val packageManager = context.packageManager
        appList = appList.map { item ->
            val packageInfo = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(
                        item.packageName,
                        PackageManager.PackageInfoFlags.of(0)
                    )
                } else {
                    packageManager.getPackageInfo(item.packageName, 0)
                }
            } catch (e: Exception) {
                null
            }
            item.copy(
                isInstall = packageInfo != null,
            )
        }
    }
    action.collectAction { action ->
        when (action) {
            is AddAppPageAction.CheckedChange -> {
                val packageName = action.packageName
                val appInfo = appList.find { packageName == it.packageName } ?: return@collectAction
                if (!appInfo.isInstall) {
                    return@collectAction
                }
                if (selectedAppPackageNameSet.contains(packageName)) {
                    context.dataStore.edit {
                        // 移除
                        it[DataStoreKeys.appPackageNameSet] = selectedAppPackageNameSet.filter { name ->
                            packageName != name
                        }.toSet()
                    }
                } else {
                    // 添加
                    context.dataStore.edit {
                        // 移除
                        it[DataStoreKeys.appPackageNameSet] = setOf(
                            *selectedAppPackageNameSet.toTypedArray(),
                            packageName,
                        )
                    }
                }
//                appList.add("列表")
            }
        }
        Unit
    }
    return AddAppPageState(
        appList = appList,
        selectedAppPackageNameSet = selectedAppPackageNameSet,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppPage(
    navController: NavHostController,
) {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    val storagePermission = localStoragePermission()
    val permissionState = storagePermission.collectState()
    val (state, channel) = rememberPresenter {
        AddAppPagePresenter(
            it,
            context,
        )
    }

    fun resultCallBack() {
        if (!permissionState.isGranted || !permissionState.isExternalStorage) {
            showPermissionDialog = true
        }
    }

    fun checkedChange(packageName: String) {
        if (permissionState.isGranted && permissionState.isExternalStorage) {
            channel.trySend(AddAppPageAction.CheckedChange(
                packageName = packageName,
            ))
        } else {
            storagePermission.requestPermissions(::resultCallBack)
        }
    }

    PermissionDialog(
        showPermissionDialog = showPermissionDialog,
        isGranted = permissionState.isGranted,
        onDismiss = { showPermissionDialog = false }
    )
    LazyColumn {
        item {

        }
        items(state.appList, { it.packageName }) { item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        checkedChange(item.packageName)
                    },
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            modifier = Modifier
                                .size(60.dp, 60.dp)
                                .padding(end = 6.dp),
                            painter = painterResource(item.icon),
                            contentDescription = item.name,
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                        ) {
                            Row(
                                modifier = Modifier.padding(bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                if (!item.isInstall) {
                                    Text(
                                        text = "未安装",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.Red,
                                        modifier = Modifier.padding(start = 4.dp),
                                    )
                                }
                            }
                            Text(
                                text = item.packageName,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Checkbox(
                            checked = state.selectedAppPackageNameSet.contains(item.packageName),
                            enabled = item.isInstall,
                            onCheckedChange = {
                                checkedChange(item.packageName)
                            },
                        )
                    }
                }
            }
        }
    }

}