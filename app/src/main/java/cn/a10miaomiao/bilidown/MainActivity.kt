package cn.a10miaomiao.bilidown

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.documentfile.provider.DocumentFile
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

    override fun onResume() {
        super.onResume()
    }

    fun test() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        intent.data = Uri.parse("package:$packageName")
        startActivityForResult(intent, Request_Code)
    }

    private fun createShortcutFilesActivity() {
        val supported = ShortcutManagerCompat.isRequestPinShortcutSupported(this)
        if (!supported) {
            Toast.makeText(this, "Can not create shortcut!", Toast.LENGTH_LONG).show()
            return
        }

        val component = ComponentName(
            "com.google.android.documentsui",
            "com.android.documentsui.files.FilesActivity"
        )
        val shortcutIntent = Intent()
        shortcutIntent.component = component
        shortcutIntent.action = "android.intent.action.MAIN"
        shortcutIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        try {
            val activityInfo = packageManager.getActivityInfo(component, PackageManager.MATCH_ALL)
        } catch (e: Exception) {
            Toast.makeText(this, "FilesActivity not exists.", Toast.LENGTH_LONG).show()
            return
        }

        val shortcut = ShortcutInfoCompat.Builder(this, "id1")
            .setShortLabel("FilesActivity")
            .setLongLabel("Open FilesActivity")
            .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
            .setIntent(shortcutIntent)
            .build()

        ShortcutManagerCompat.requestPinShortcut(this, shortcut, null)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
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
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, flags)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Log.d(TAG, uri.toString())
            }
        }
    }

}