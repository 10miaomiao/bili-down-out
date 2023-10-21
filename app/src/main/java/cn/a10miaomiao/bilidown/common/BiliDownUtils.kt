package cn.a10miaomiao.bilidown.common

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import cn.a10miaomiao.bilidown.R
import cn.a10miaomiao.bilidown.entity.BiliAppInfo
import java.io.File


object BiliDownUtils {

    val biliAppList = listOf(
        BiliAppInfo(
            "哔哩哔哩",
            "tv.danmaku.bili",
            icon = R.drawable.ic_bilibili
        ),
        BiliAppInfo(
            "哔哩哔哩(概念版)",
            "com.bilibili.app.blue",
            icon = R.drawable.ic_bilibili_blue
        ),
        BiliAppInfo(
            "哔哩哔哩(谷歌版)",
            "com.bilibili.app.in",
            icon = R.drawable.ic_bilibili
        ),
        BiliAppInfo(
            "bilimiao",
            "com.a10miaomiao.bilimiao",
            icon = R.drawable.ic_bilimiao
        ),
        BiliAppInfo(
            "bilimiao-dev",
            "cn.a10miaomiao.bilimiao.dev",
            icon = R.drawable.ic_bilimiao
        ),
    )

    fun checkSelfPermission(
        context: Context,
        packageName: String,
    ): Boolean {
        if (Build.VERSION.SDK_INT < 23) {  // 5.0
            return true
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) { // 6.0-10.0
            val f = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
            return f == PackageManager.PERMISSION_GRANTED
        }
        return false
    }
}