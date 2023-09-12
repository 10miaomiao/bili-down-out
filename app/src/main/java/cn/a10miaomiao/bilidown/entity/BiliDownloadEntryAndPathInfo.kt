package cn.a10miaomiao.bilidown.entity

data class BiliDownloadEntryAndPathInfo(
    val pageDirPath: String,
    val entryDirPath: String,
    val entry: BiliDownloadEntryInfo,
)
