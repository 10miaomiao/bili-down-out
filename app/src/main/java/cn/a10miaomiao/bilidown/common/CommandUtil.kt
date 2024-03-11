package cn.a10miaomiao.bilidown.common

import java.io.File

object CommandUtil {

    fun filePath(file: File): String {
        val path = file.absolutePath.replace("\"", "\\\"")
        return "\"${path}\""
    }

}