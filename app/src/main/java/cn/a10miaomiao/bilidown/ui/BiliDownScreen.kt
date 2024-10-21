package cn.a10miaomiao.bilidown.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BiliDownScreen(
    val route: String,
    val name: String,
    val icon: ImageVector = Icons.Filled.Favorite,
) {
    object List : BiliDownScreen("list", "列表", Icons.Filled.Home)
    object More : BiliDownScreen("more", "设置", Icons.Filled.Settings)
    object Progress : BiliDownScreen("progress", "进度", Icons.Filled.DateRange)
    object OutList: BiliDownScreen("out_list", "已导出", Icons.Filled.CheckCircle)
    object Detail : BiliDownScreen("detail", "详情")
    object AddApp : BiliDownScreen("add_app", "添加APP信息")
    object About : BiliDownScreen("about", "关于")

    companion object {
        private val routeToNameMap = mapOf(
            "list" to "哔哩缓存导出",
            "progress" to "当前进度",
            "out_list" to "导出记录",
            "more" to "设置",
            "add_app" to "添加APP",
            "about" to "关于",
            "detail" to "哔哩缓存详情",
        )

        fun getRouteName(route: String?): String {
            val key = route?.split("?")?.get(0) ?: ""
            return routeToNameMap[key] ?: "BiliDownOut"
        }
    }
}