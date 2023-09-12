package cn.a10miaomiao.bilidown.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import cn.a10miaomiao.bilidown.ui.page.AddAppPageAction


@Composable
fun PermissionDialog(
    showPermissionDialog: Boolean,
    isGranted: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val toPermissionSettingPage = remember {
        {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = Uri.parse("package:" + context.packageName)
            context.startActivity(intent)
        }
    }

    if (showPermissionDialog && !isGranted) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = "请授予存储权限") },
            text = {
                Text(text = "需要访问Android/data文件夹，读取哔哩哔哩APP缓存文件")
            },
            confirmButton = {
                TextButton(
                    onClick = toPermissionSettingPage,
                ) {
                    Text("去设置")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                ) {
                    Text("取消")
                }
            }
        )
    }
}