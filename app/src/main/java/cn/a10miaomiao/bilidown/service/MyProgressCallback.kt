package cn.a10miaomiao.bilidown.service

import android.util.Log
import cn.a10miaomiao.bilidown.callback.ProgressCallback
import cn.a10miaomiao.bilidown.common.MiaoLog
import cn.a10miaomiao.bilidown.entity.VideoOutInfo

class MyProgressCallback(
    val service: BiliDownService,
) : ProgressCallback.Stub() {
    override fun onStart(info: VideoOutInfo?) {
        MiaoLog.debug { "onStart(${info})" }
        BiliDownService.status.value = BiliDownService.Status.InProgress(
            name = info?.name ?: "unknown name",
            entryDirPath = info?.name ?: "unknown path",
            cover = info?.cover ?: "",
            progress = 0f
        )
    }

    override fun onFinish(info: VideoOutInfo?) {
        MiaoLog.debug { "onFinish(${info})" }
        BiliDownService.status.value = BiliDownService.Status.InIdle
        service.tryAddTask(
            entryDirPath = info?.entryDirPath ?: "unknown path",
            outFilePath = info?.outFilePath ?: "unknown path",
            title = info?.name ?: "unknown name",
            cover = info?.cover ?: "",
        )
    }

    override fun onCancel(info: VideoOutInfo?) {
        MiaoLog.debug { "onCancel(${info})" }
        BiliDownService.status.value = BiliDownService.Status.InIdle
    }

    override fun onProgress(info: VideoOutInfo?, progress: Int, progressTime: Long) {
        MiaoLog.debug { "onProgress(_, $progress, $progressTime)" }
        val _status = BiliDownService.status.value
        if (_status is BiliDownService.Status.InProgress) {
            BiliDownService.status.value = _status.copy(
                progress = progress.toFloat() / 100f
            )
        }
    }

    override fun onError(info: VideoOutInfo?, message: String?) {
        MiaoLog.debug { "onError(${info}, $message)" }
        BiliDownService.status.value = BiliDownService.Status.Error(
            BiliDownService.status.value,
            message ?: "unknown error",
        )
    }
}