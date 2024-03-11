package cn.a10miaomiao.bilidown.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat.startActivity
import rikka.shizuku.ShizukuProvider.MANAGER_APPLICATION_ID


@Composable
fun ShizukuHelpDialog(
    action: ShizukuHelpDialogAction,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var message = ""
    var confirmButton = "确认"

    when (action) {
        ShizukuHelpDialogAction.None -> Unit
        ShizukuHelpDialogAction.InstallShizuku -> {
            message = "Shizuku未安装，请先安装Shizuku！"
            confirmButton = "去下载"
        }
        ShizukuHelpDialogAction.ShizukuVersionLow -> {
            message = "Shizuku未安装，请先升级Shizuku到最新版本！"
            confirmButton = "去更新"
        }
        ShizukuHelpDialogAction.RunShizuku -> {
            message = "Shizuku未启动，请先启动Shizuku！"
            confirmButton = "去运行"
        }
        ShizukuHelpDialogAction.RequestPermission -> {
            message = "Shizuku授权失败，请手动打开授权管理器进行授权！"
            confirmButton = "去设置"
        }
    }

    fun onConfirm() {
        when (action) {
            ShizukuHelpDialogAction.None -> Unit
            ShizukuHelpDialogAction.InstallShizuku,
            ShizukuHelpDialogAction.ShizukuVersionLow -> {
                try {
                    val uri = Uri.parse("https://shizuku.rikka.app/")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "打开失败：https://shizuku.rikka.app/", Toast.LENGTH_LONG)
                        .show()
                    e.printStackTrace()
                }
            }
            ShizukuHelpDialogAction.RunShizuku,
            ShizukuHelpDialogAction.RequestPermission-> {
                try {
                    val packageManager = context.packageManager
                    val intent = packageManager.getLaunchIntentForPackage(MANAGER_APPLICATION_ID)
                    if (intent == null) {
                        Toast.makeText(context, "未找到Shizuku", Toast.LENGTH_LONG)
                            .show()
                    } else {
                        context.startActivity(intent)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Shizuku启动失败", Toast.LENGTH_LONG)
                        .show()
                    e.printStackTrace()
                }
            }
        }
        onDismiss()
    }

    if (action != ShizukuHelpDialogAction.None) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = "提示") },
            text = { Text(text = message) },
            confirmButton = {
                TextButton(
                    onClick = ::onConfirm,
                ) {
                    Text(confirmButton)
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

enum class ShizukuHelpDialogAction {
    None,
    InstallShizuku,
    ShizukuVersionLow,
    RunShizuku,
    RequestPermission,
}