package cn.a10miaomiao.bilidown.service

import android.util.Log
import io.microshow.rxffmpeg.RxFFmpegSubscriber
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

class MyRxFFmpegSubscriber(
    private val status: MutableStateFlow<BiliDownService.Status>,
    private val tempPath: String,
) : RxFFmpegSubscriber() {
    private val TAG = "MyRxFFmpegSubscriber"

    override fun onFinish() {
        status.value = BiliDownService.Status.InIdle
        val tempVideoFile = File(tempPath,"video.m4s")
        if (tempVideoFile.exists()) {
            tempVideoFile.delete()
        }
        val tempAudioFile = File(tempPath,"audio.m4s")
        if (tempAudioFile.exists()) {
            tempAudioFile.delete()
        }
        Log.d(TAG, "onFinish")
    }

    override fun onProgress(progress: Int, progressTime: Long) {
        val _status = status.value
        if (_status is BiliDownService.Status.InProgress) {
            status.value = _status.copy(
                progress = progress.toFloat() / 100f
            )
        }
        Log.d(TAG, "onProgress$progress $progressTime")
    }

    override fun onCancel() {
        status.value = BiliDownService.Status.InIdle
    }

    override fun onError(message: String) {
        status.value = BiliDownService.Status.Error(
            status.value,
            message,
        )
    }
}