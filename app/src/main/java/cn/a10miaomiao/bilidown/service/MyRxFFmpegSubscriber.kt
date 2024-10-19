package cn.a10miaomiao.bilidown.service

import android.util.Log
import cn.a10miaomiao.bilidown.common.MiaoLog
import cn.a10miaomiao.bilidown.state.AppState
import cn.a10miaomiao.bilidown.state.TaskStatus
import io.microshow.rxffmpeg.RxFFmpegSubscriber
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

open class MyRxFFmpegSubscriber(
    private val appState: AppState,
//    private val tempPath: String,
) : RxFFmpegSubscriber() {
    private val TAG = "MyRxFFmpegSubscriber"

    override fun onFinish() {
        appState.putTaskStatus(TaskStatus.InIdle)
    }

    override fun onProgress(progress: Int, progressTime: Long) {
        val taskStatus = appState.taskStatus.value
        if (taskStatus is TaskStatus.InProgress) {
            appState.putTaskStatus(
                taskStatus.copy(
                    progress = progress.toFloat() / 100f
                )
            )
        }
        Log.d(TAG, "onProgress$progress $progressTime")
    }

    override fun onCancel() {
        appState.putTaskStatus(TaskStatus.InIdle)
    }

    override fun onError(message: String) {
        MiaoLog.info { message }
        appState.putTaskStatus(TaskStatus.Error(
            appState.taskStatus.value,
            message
        ))
    }
}