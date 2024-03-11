package cn.a10miaomiao.bilidown.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VideoOutInfo(
    val entryDirPath: String,
    val outFilePath: String,
    val name: String,
    val cover: String,
) : Parcelable
