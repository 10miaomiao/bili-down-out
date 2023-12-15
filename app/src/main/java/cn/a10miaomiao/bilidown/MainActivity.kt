package cn.a10miaomiao.bilidown

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalView
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import cn.a10miaomiao.bilidown.common.LocalStoragePermission
import cn.a10miaomiao.bilidown.common.permission.StoragePermission
import cn.a10miaomiao.bilidown.service.BiliDownService
import cn.a10miaomiao.bilidown.ui.BiliDownApp
import io.microshow.rxffmpeg.RxFFmpegInvoke
import kotlinx.coroutines.launch
import moe.tlaster.precompose.lifecycle.PreComposeActivity
import moe.tlaster.precompose.lifecycle.setContent


class MainActivity : PreComposeActivity() {

    private val TAG = "MainActivity"
    private val Request_Code = 1234
    private lateinit var storagePermission: StoragePermission
    private lateinit var biliDownService: BiliDownService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        RxFFmpegInvoke.getInstance().setDebug(true);
        storagePermission = StoragePermission(this)
        setContent {
            CompositionLocalProvider(
                LocalStoragePermission provides storagePermission,
            ) {
                BiliDownApp()
            }
        }
        lifecycleScope.launch {
            biliDownService = BiliDownService.getService(this@MainActivity)
        }
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
        Log.d(TAG, "onActivityResult" + requestCode)
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
                Log.d(TAG, uri.toString())
            }
        }
    }

}