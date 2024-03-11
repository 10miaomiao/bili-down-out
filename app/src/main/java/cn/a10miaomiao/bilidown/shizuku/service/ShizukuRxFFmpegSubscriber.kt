package cn.a10miaomiao.bilidown.shizuku.service

import android.annotation.SuppressLint
import android.os.Build
import cn.a10miaomiao.bilidown.callback.ProgressCallback
import cn.a10miaomiao.bilidown.common.CommandUtil
import cn.a10miaomiao.bilidown.entity.VideoOutInfo
import io.microshow.rxffmpeg.RxFFmpegSubscriber
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission


class ShizukuRxFFmpegSubscriber(
    val videoOutInfo: VideoOutInfo,
    val callback: ProgressCallback?
) : RxFFmpegSubscriber() {
    override fun onError(message: String?) {
        callback?.onError(videoOutInfo, message)
    }

    override fun onFinish() {
        callback?.onFinish(videoOutInfo)
        val outFile = File(videoOutInfo.outFilePath)
        val ffTxtFile = File(outFile.parent, ".ff.txt")
        if (ffTxtFile.exists()) {
            ffTxtFile.delete()
        }
        if (outFile.exists()) {
            // 设置读写权限
            changeFolderPermission(outFile)
        }
    }

    override fun onProgress(progress: Int, progressTime: Long) {
        callback?.onProgress(videoOutInfo, progress, progressTime)
    }

    override fun onCancel() {
        callback?.onCancel(videoOutInfo)
    }

    private fun changeFolderPermission(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val perms: MutableSet<PosixFilePermission> = HashSet()
                perms.add(PosixFilePermission.OWNER_READ)
                perms.add(PosixFilePermission.OWNER_WRITE)
                perms.add(PosixFilePermission.OWNER_EXECUTE)
                perms.add(PosixFilePermission.GROUP_READ)
                perms.add(PosixFilePermission.GROUP_WRITE)
                perms.add(PosixFilePermission.GROUP_EXECUTE)
                val path = Paths.get(file.absolutePath)
                Files.setPosixFilePermissions(path, perms)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            chmodFile(file)
        }
    }

    private fun chmodFile(destFile: File) {
        try {
            val command = "chmod 777 ${CommandUtil.filePath(destFile)}"
            val runtime = Runtime.getRuntime()
            runtime.exec(command)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}