package cn.a10miaomiao.bilidown.entity

import androidx.annotation.DrawableRes

data class BiliAppInfo(
    val name: String,
    val packageName: String,
    @DrawableRes
    val icon: Int,
    val isInstall: Boolean = false,
)
