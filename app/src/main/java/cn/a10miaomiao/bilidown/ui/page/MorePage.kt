package cn.a10miaomiao.bilidown.ui.page

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cn.a10miaomiao.bilidown.R
import cn.a10miaomiao.bilidown.common.BiliDownUtils
import cn.a10miaomiao.bilidown.common.datastore.DataStoreKeys
import cn.a10miaomiao.bilidown.common.datastore.rememberDataStorePreferencesFlow
import cn.a10miaomiao.bilidown.common.molecule.collectAction
import cn.a10miaomiao.bilidown.common.molecule.rememberPresenter
import cn.a10miaomiao.bilidown.entity.BiliAppInfo
import cn.a10miaomiao.bilidown.shizuku.localShizukuPermission
import cn.a10miaomiao.bilidown.shizuku.permission.ShizukuPermission
import cn.a10miaomiao.bilidown.ui.BiliDownScreen
import cn.a10miaomiao.bilidown.ui.components.SettingItem
import cn.a10miaomiao.bilidown.ui.components.ShizukuHelpDialog
import cn.a10miaomiao.bilidown.ui.components.ShizukuHelpDialogAction
import kotlinx.coroutines.flow.Flow

data class MorePageState(
    val versionName: String,
)

sealed class MorePageAction {
    object About : MorePageAction()
}

@Composable
fun MorePagePresenter(
    context: Context,
    action: Flow<MorePageAction>,
): MorePageState {
    var versionName by remember { mutableStateOf("v-") }
    LaunchedEffect(Unit) {
        val manager = context.packageManager
        val info = manager.getPackageInfo(context.packageName, 0)
        versionName = info.versionName.toString()
    }
    action.collectAction {
        when (it) {
            MorePageAction.About -> {
            }
        }
    }
    return MorePageState(
        versionName,
    )
}

@Composable
fun MorePage(
    navController: NavHostController,
) {
    val context = LocalContext.current
    val shizukuPermission = localShizukuPermission()
    val shizukuPermissionState by shizukuPermission.collectState()
    val (state, channel) = rememberPresenter {
        MorePagePresenter(context, it)
    }
    var dialogAction by remember {
        mutableStateOf(ShizukuHelpDialogAction.None)
    }

    fun changeShizukuEnabled(enabled: Boolean) {
        if (enabled) {
            if (!shizukuPermissionState.isInstalled) {
                dialogAction = ShizukuHelpDialogAction.InstallShizuku
            } else if (shizukuPermissionState.isPreV11) {
                dialogAction = ShizukuHelpDialogAction.ShizukuVersionLow
            } else if (!shizukuPermissionState.isRunning) {
                dialogAction = ShizukuHelpDialogAction.RunShizuku
            } else if (!shizukuPermissionState.isGranted) {
                if (!shizukuPermission.requestPermission()) {
                    dialogAction = ShizukuHelpDialogAction.RequestPermission
                }
            } else {
                shizukuPermission.setEnabled(true)
            }
        } else {
            shizukuPermission.setEnabled(false)
        }
    }

    ShizukuHelpDialog(
        action = dialogAction,
        onDismiss = { dialogAction = ShizukuHelpDialogAction.None }
    )
    val scrollableState = rememberScrollState()
    Column(
        modifier = Modifier.verticalScroll(scrollableState)
            .padding(bottom = 80.dp)
    ) {
        val isAboveN = ShizukuPermission.isAboveN
        SettingItem(
            title = "Shizuku",
            desc = if (isAboveN) {
                shizukuPermissionState.statusText
            } else {
                "需Android7.0+"
            },
            iconPainter = painterResource(R.mipmap.ic_shizuku),
            enabled = isAboveN,
            action = {
                Switch(
                    checked = shizukuPermissionState.isEnabled,
                    onCheckedChange = ::changeShizukuEnabled,
                    enabled = isAboveN,
                )
            }
        ) {
            changeShizukuEnabled(!shizukuPermissionState.isEnabled)
        }

        SettingItem(
            title = "关于",
            desc = "当前版本：${state.versionName}",
            icon = Icons.Outlined.Info,
        ) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://github.com/10miaomiao/bili-down-out")
            context.startActivity(intent)
        }
    }
}