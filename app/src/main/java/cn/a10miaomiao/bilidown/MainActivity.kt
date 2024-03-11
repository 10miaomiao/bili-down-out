package cn.a10miaomiao.bilidown

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import cn.a10miaomiao.bilidown.common.LocalStoragePermission
import cn.a10miaomiao.bilidown.common.MiaoLog
import cn.a10miaomiao.bilidown.common.permission.StoragePermission
import cn.a10miaomiao.bilidown.service.BiliDownService
import cn.a10miaomiao.bilidown.shizuku.service.UserService
import cn.a10miaomiao.bilidown.ui.BiliDownApp
import cn.a10miaomiao.bilidown.shizuku.IUserService
import cn.a10miaomiao.bilidown.shizuku.LocalShizukuPermission
import cn.a10miaomiao.bilidown.shizuku.permission.ShizukuPermission
import io.microshow.rxffmpeg.RxFFmpegInvoke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import moe.tlaster.precompose.lifecycle.PreComposeActivity
import moe.tlaster.precompose.lifecycle.setContent
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.UserServiceArgs


class MainActivity : PreComposeActivity(), Shizuku.OnRequestPermissionResultListener {
    companion object {
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 23
    }

    private val TAG = "MainActivity"
    private val Request_Code = 1234
    private lateinit var storagePermission: StoragePermission
    private lateinit var shizukuPermission: ShizukuPermission
    private lateinit var biliDownService: BiliDownService
    private var userService: IUserService? = null

    private val textFlow = MutableStateFlow<String>("2333333")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        RxFFmpegInvoke.getInstance().setDebug(true);
        storagePermission = StoragePermission(this)
        shizukuPermission = ShizukuPermission(this)
        setContent {
            CompositionLocalProvider(
                LocalStoragePermission provides storagePermission,
                LocalShizukuPermission provides shizukuPermission,
            ) {
                BiliDownApp()
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

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {

    }

    private val userServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder?) {
            val res = StringBuilder()
            res.append("onServiceConnected: ").append(componentName.className).append('\n')
            if (binder != null && binder.pingBinder()) {
                val service = IUserService.Stub.asInterface(binder)
                try {
                    res.append(service.doSomething())
                } catch (e: RemoteException) {
                    e.printStackTrace()
                    res.append(Log.getStackTraceString(e))
                }
                userService = service
            } else {
                res.append("invalid binder for ").append(componentName).append(" received")
            }
            textFlow.value = res.toString().trim { it <= ' ' }
            MiaoLog.debug { res.toString().trim { it <= ' ' } }
//            binding.text3.setText(res.toString().trim { it <= ' ' })
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            MiaoLog.debug {
                """
                onServiceDisconnected:
                ${componentName.className}
                """.trimIndent()
            }
            textFlow.value =  """
                onServiceDisconnected:
                ${componentName.className}
                """.trimIndent()
//            binding.text3.setText(
//                """
//                onServiceDisconnected:
//                ${componentName.className}
//                """.trimIndent()
//            )
        }
    }

    private val userServiceArgs = UserServiceArgs(
        ComponentName(
            BuildConfig.APPLICATION_ID,
            UserService::class.java.name
        )
    ).apply {
        daemon(false)
        processNameSuffix("service")
        debuggable(BuildConfig.DEBUG)
        version(BuildConfig.VERSION_CODE)
    }

    private fun bindUserService() {
        val res = java.lang.StringBuilder()
        try {
            if (Shizuku.getVersion() < 10) {
                res.append("requires Shizuku API 10")
            } else {
                Shizuku.bindUserService(userServiceArgs, userServiceConnection)
            }
        } catch (tr: Throwable) {
            tr.printStackTrace()
            res.append(tr.toString())
        }
        textFlow.value = res.toString().trim { it <= ' ' }
        MiaoLog.debug { res.toString().trim { it <= ' ' } }
//        binding.text3.setText(res.toString().trim { it <= ' ' })
    }

    private fun unbindUserService() {
        val res = java.lang.StringBuilder()
        try {
            if (Shizuku.getVersion() < 10) {
                res.append("requires Shizuku API 10")
            } else {
                Shizuku.unbindUserService(userServiceArgs, userServiceConnection, true)
            }
        } catch (tr: Throwable) {
            tr.printStackTrace()
            res.append(tr.toString())
        }
        textFlow.value = res.toString().trim { it <= ' ' }
        MiaoLog.debug { res.toString().trim { it <= ' ' } }
//        binding.text3.setText(res.toString().trim { it <= ' ' })
    }
}