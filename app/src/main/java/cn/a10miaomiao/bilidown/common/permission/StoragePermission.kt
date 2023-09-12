package cn.a10miaomiao.bilidown.common.permission

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow


class StoragePermission(
    val activity: Activity,
) {

    private var resultCallBack: (() -> Unit)? = null

    private val state = MutableStateFlow(StoragePermissionState(
        isGranted = checkSelfPermission(),
        isExternalStorage = isExternalStorageManager()
    ))

    init {

    }

    @Composable
    fun collectState(): StoragePermissionState {
        return state.collectAsState().value
    }

    fun requestPermissionsResult() {
        resultCallBack?.invoke()
        state.value = StoragePermissionState(
            isGranted = checkSelfPermission(),
            isExternalStorage = isExternalStorageManager()
        )
    }

    fun checkSelfPermission(): Boolean {
        if (Build.VERSION.SDK_INT < 23) {  // 5.0
            return true
        }
        val permission1 = ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val permission2 = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
        return permission1 == PackageManager.PERMISSION_GRANTED && permission2 == PackageManager.PERMISSION_GRANTED
    }

    fun isExternalStorageManager(): Boolean {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {  // 10以上
            return Environment.isExternalStorageManager()
        }
        return true
    }

    fun requestPermissions(
        callback: () -> Unit
    ): Boolean {
        resultCallBack = callback
        if (!checkSelfPermission()) {
            //2、申请权限: 参数二：权限的数组；参数三：请求码
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                ),
                1
            )
            return false //没有权限
        } else if (!isExternalStorageManager()){
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:${activity.packageName}")
            activity.startActivityForResult(intent, 1)
            return false //没有权限
        }
        return true
    }

    data class StoragePermissionState(
        val isGranted: Boolean,
        val isExternalStorage: Boolean,
    )

}