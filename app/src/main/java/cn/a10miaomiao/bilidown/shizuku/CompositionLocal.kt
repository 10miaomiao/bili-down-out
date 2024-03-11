package cn.a10miaomiao.bilidown.shizuku

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import cn.a10miaomiao.bilidown.shizuku.permission.ShizukuPermission

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}

internal val LocalShizukuPermission: ProvidableCompositionLocal<ShizukuPermission> = staticCompositionLocalOf {
    noLocalProvidedFor("LocalContext")
}

@Composable
fun localShizukuPermission() = LocalShizukuPermission.current