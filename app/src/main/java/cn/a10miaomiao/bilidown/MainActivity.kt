package cn.a10miaomiao.bilidown

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import cn.a10miaomiao.bilidown.common.LocalStoragePermission
import cn.a10miaomiao.bilidown.common.MiaoLog
import cn.a10miaomiao.bilidown.common.permission.StoragePermission
import cn.a10miaomiao.bilidown.service.BiliDownService
import cn.a10miaomiao.bilidown.ui.BiliDownApp
import cn.a10miaomiao.bilidown.shizuku.IUserService
import cn.a10miaomiao.bilidown.shizuku.LocalShizukuPermission
import cn.a10miaomiao.bilidown.shizuku.permission.ShizukuPermission
import io.microshow.rxffmpeg.RxFFmpegInvoke
import kotlinx.coroutines.launch
import moe.tlaster.precompose.PreComposeApp
import rikka.shizuku.Shizuku


class MainActivity : ComponentActivity(), Shizuku.OnRequestPermissionResultListener {

    private lateinit var storagePermission: StoragePermission
    private lateinit var shizukuPermission: ShizukuPermission
    private lateinit var biliDownService: BiliDownService
    private var userService: IUserService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        RxFFmpegInvoke.getInstance().setDebug(true);
        storagePermission = StoragePermission(this)
        shizukuPermission = ShizukuPermission(this)
        setContent {
            PreComposeApp {
                CompositionLocalProvider(
                    LocalStoragePermission provides storagePermission,
                    LocalShizukuPermission provides shizukuPermission,
                ) {
                    BiliDownApp()
                }
            }
        }
        lifecycleScope.launch {
            biliDownService = BiliDownService.getService(this@MainActivity)
        }
//        bindUserService()
    }

    override fun onResume() {
        super.onResume()
        shizukuPermission.syncShizukuState(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        shizukuPermission.onDestroy()

//        unbindUserService()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            storagePermission.requestPermissionsResult()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        MiaoLog.debug { "onActivityResult" + requestCode }
        if (requestCode == 1) {
            storagePermission.requestPermissionsResult()
        }
        var uri = data?.data ?: return
        if (requestCode == 2) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    val flags =
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, flags)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                MiaoLog.debug { uri.toString() }
            }
        }
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {

    }

}