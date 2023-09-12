package cn.a10miaomiao.bilidown.common

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import cn.a10miaomiao.bilidown.common.permission.StoragePermission

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}

internal val LocalStoragePermission: ProvidableCompositionLocal<StoragePermission> = staticCompositionLocalOf {
    noLocalProvidedFor("LocalContext")
}

@Composable
fun localStoragePermission() = LocalStoragePermission.current