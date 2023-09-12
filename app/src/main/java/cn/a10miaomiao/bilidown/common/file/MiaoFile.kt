package cn.a10miaomiao.bilidown.common.file

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.File

interface MiaoFile {
    val path: String
    val isDirectory: Boolean
    fun exists(): Boolean
    fun listFiles(): List<MiaoFile>
    fun canRead(): Boolean
    fun readText(): String
}
