package cn.a10miaomiao.bilidown.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import cn.a10miaomiao.bilidown.common.BiliDownOutFile

@Composable
fun OutFolderDialog(
    showOutFolderDialog: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    if (showOutFolderDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = "导出文件夹") },
            text = {
                Column() {
                    Text(text = "视频输出文件夹为：${BiliDownOutFile.getOutFolderPath()}")
                    Text(text = "打开方式：[文件夹管理器]->内部储存空间->Download->${BiliDownOutFile.DIR_NAME}")
                    Text(text = "或 [文件夹管理器]->下载->${BiliDownOutFile.DIR_NAME}")
                }
            },
            confirmButton = {
                Row() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                        TextButton(
                            onClick = {
                                val uri = BiliDownOutFile.getOutFolderUri()
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                intent.setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
                                context.startActivity(intent)
                                onDismiss()
                            },
                        ) {
                            Text("尝试打开")
                        }
                    }
                    TextButton(
                        onClick = {
                            val path = BiliDownOutFile.getOutFolderPath()
                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboardManager.setPrimaryClip(ClipData.newPlainText("", path))
                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2){
                                Toast.makeText(context, "已复制路径到剪切板", Toast.LENGTH_SHORT).show()
                            }
                            onDismiss()
                        },
                    ) {
                        Text("复制路径")
                    }
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