package cn.a10miaomiao.bilidown.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BiliDownScreen(
    val route: String,
    val name: String,
    val icon: ImageVector = Icons.Filled.Favorite,
) {
    object List : BiliDownScreen("list", "列表", Icons.Filled.List)
    object More : BiliDownScreen("more", "更多", Icons.Filled.MoreVert)
    object Progress : BiliDownScreen("progress", "进度", Icons.Filled.DateRange)
    object Detail : BiliDownScreen("detail", "详情")
    object AddApp : BiliDownScreen("add_app", "添加APP信息")

    companion object {
        private val routeToNameMap = mapOf(
            "list" to "哔哩缓存导出",
            "progress" to "当前进度",
            "more" to "更多",
            "add_app" to "添加APP",
            "detail" to "哔哩缓存详情",
        )

        fun getRouteName(route: String?): String {
            val key = route?.split("?")?.get(0) ?: ""
            return routeToNameMap[key] ?: "BiliDownOut"
        }
    }
}