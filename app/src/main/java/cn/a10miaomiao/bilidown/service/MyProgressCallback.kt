package cn.a10miaomiao.bilidown.service

import android.util.Log
import cn.a10miaomiao.bilidown.callback.ProgressCallback
import cn.a10miaomiao.bilidown.common.MiaoLog
import cn.a10miaomiao.bilidown.entity.VideoOutInfo
import cn.a10miaomiao.bilidown.state.AppState
import cn.a10miaomiao.bilidown.state.TaskStatus

class MyProgressCallback(
    val service: BiliDownService,
    val appState: AppState,
) : ProgressCallback.Stub() {
    override fun onStart(info: VideoOutInfo?) {
        MiaoLog.debug { "onStart(${info})" }
        appState.putTaskStatus(
            TaskStatus.InProgress(
                name = info?.name ?: "unknown name",
                entryDirPath = info?.entryDirPath ?: "unknown path",
                cover = info?.cover ?: "",
                progress = 0f
            )
        )
    }

    override fun onFinish(info: VideoOutInfo?) {
        MiaoLog.debug { "onFinish(${info})" }
        appState.putTaskStatus(TaskStatus.InIdle)
        service.tryAddOutRecord(
            entryDirPath = info?.entryDirPath ?: "unknown path",
            outFilePath = info?.outFilePath ?: "unknown path",
            title = info?.name ?: "unknown name",
            cover = info?.cover ?: "",
        )
    }

    override fun onCancel(info: VideoOutInfo?) {
        MiaoLog.debug { "onCancel(${info})" }
        appState.putTaskStatus(TaskStatus.InIdle)
    }

    override fun onProgress(info: VideoOutInfo?, progress: Int, progressTime: Long) {
        MiaoLog.debug { "onProgress(_, $progress, $progressTime)" }
        val taskStatus = appState.taskStatus.value
        if (taskStatus is TaskStatus.InProgress) {
            appState.putTaskStatus(
                taskStatus.copy(
                    progress = progress.toFloat() / 100f
                )
            )
        }
    }

    override fun onError(info: VideoOutInfo?, message: String?) {
        MiaoLog.debug { "onError(${info}, $message)" }
        appState.putTaskStatus(TaskStatus.Error(
            appState.taskStatus.value,
            message ?: "unknown error",
        ))
    }
}