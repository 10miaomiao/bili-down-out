package cn.a10miaomiao.bilidown.service

import android.util.Log
import io.microshow.rxffmpeg.RxFFmpegSubscriber
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

open class MyRxFFmpegSubscriber(
//    private val status: MutableStateFlow<BiliDownService.Status>,
//    private val tempPath: String,
) : RxFFmpegSubscriber() {
    private val TAG = "MyRxFFmpegSubscriber"

    override fun onFinish() {
        BiliDownService.status.value = BiliDownService.Status.InIdle
    }

    override fun onProgress(progress: Int, progressTime: Long) {
        val _status = BiliDownService.status.value
        if (_status is BiliDownService.Status.InProgress) {
            BiliDownService.status.value = _status.copy(
                progress = progress.toFloat() / 100f
            )
        }
        Log.d(TAG, "onProgress$progress $progressTime")
    }

    override fun onCancel() {
        BiliDownService.status.value = BiliDownService.Status.InIdle
    }

    override fun onError(message: String) {
        BiliDownService.status.value = BiliDownService.Status.Error(
            BiliDownService.status.value,
            message,
        )
    }
}