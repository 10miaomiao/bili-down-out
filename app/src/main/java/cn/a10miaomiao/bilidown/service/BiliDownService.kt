package cn.a10miaomiao.bilidown.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import cn.a10miaomiao.bilidown.common.file.MiaoDocumentFile
import cn.a10miaomiao.bilidown.entity.BiliDownloadEntryInfo
import io.microshow.rxffmpeg.RxFFmpegCommandList
import io.microshow.rxffmpeg.RxFFmpegInvoke
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.coroutines.CoroutineContext

class BiliDownService :
    Service(),
    CoroutineScope {

    companion object {
        private const val TAG = "DownloadService"
        private val channel = Channel<BiliDownService>()
        private var _instance: BiliDownService? = null

        val instance get() = _instance

        suspend fun getService(context: Context): BiliDownService{
            _instance?.let { return it }
            startService(context)
            return channel.receive().also {
                _instance = it
            }
        }

        fun startService(context: Context) {
            val intent = Intent(context, BiliDownService::class.java)
            context.startService(intent)
        }
    }

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    val status = MutableStateFlow<Status>(Status.InIdle)

    override fun onCreate() {
        super.onCreate()
        job = Job()
        launch {
            channel.send(this@BiliDownService)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        _instance = null
    }

    fun exportBiliVideo(
        entryDirPath: String,
        outFile: File,
    ) : Boolean{
        if (status.value != Status.InIdle && status.value !is Status.Error) {
            toast("有视频正在导出中，请稍后再试")
            return false
        }
        if(entryDirPath.startsWith("content:")) {
            return copyAndExportBiliVideo(entryDirPath, outFile)
        }

        val entryJsonFile = File(entryDirPath, "entry.json")
        val json = Json { ignoreUnknownKeys = true }
        val entry = json.decodeFromString<BiliDownloadEntryInfo>(entryJsonFile.readText())

        val videoDirPath = entryDirPath + "/" + entry.type_tag
        val videoDir = File(videoDirPath)
        if (!videoDir.exists() || !videoDir.isDirectory) {
            toast("找不到视频文件夹")
            return false
        }
//        val videoIndexJsonFile = File(videoDirPath, "index.json")
//        if (!videoIndexJsonFile.exists()) {
//            toast("缓存文件丢失，请重新缓存")
//            return false
//        }
//        val videoIndexJson = videoIndexJsonFile.readText()
        if (entry.media_type == 1) {
            toast("暂不支持导出此类视频")
            return false
            // 直接复制改后缀复制
        } else {
            val videoFile = File(videoDir, "video.m4s")
            val audioFile = File(videoDir, "audio.m4s")
            if (!videoFile.exists()) {
                toast("找不到视频")
                return false
            }
            if (!audioFile.exists()) {
                status.value = Status.Copying(
                    name = entry.name,
                    entryDirPath = entryDirPath,
                    cover = entry.cover,
                    progress = 0f,
                )
                launch {
                    copyFile(videoFile, outFile)
                }
                return true
            }
            status.value = Status.InProgress(
                name = entry.name,
                entryDirPath = entryDirPath,
                cover = entry.cover,
                progress = 0f,
            )
            mergerVideoAndAudio(videoFile, audioFile, outFile)
            return true
        }
    }

    /**
     * 复制并导出
     */
    fun copyAndExportBiliVideo(
        entryDirPath: String,
        outFile: File,
    ) : Boolean{
    val entryDirFile = DocumentFile.fromTreeUri(this, Uri.parse(entryDirPath))!!
        val entryJsonFile = MiaoDocumentFile(this, entryDirFile, "/entry.json")
        val json = Json { ignoreUnknownKeys = true }
        val entry = json.decodeFromString<BiliDownloadEntryInfo>(entryJsonFile.readText())
        val videoDir = MiaoDocumentFile(this, entryDirFile, "/${entry.type_tag}")
        if (!videoDir.exists() || !videoDir.isDirectory) {
            toast("找不到视频文件夹")
            return false
        }
//        val videoIndexJsonFile = File(videoDirPath, "index.json")
//        if (!videoIndexJsonFile.exists()) {
//            toast("缓存文件丢失，请重新缓存")
//            return false
//        }
//        val videoIndexJson = videoIndexJsonFile.readText()
        if (entry.media_type == 1) {
            toast("暂不支持导出此类视频")
            return false
            // 直接复制改后缀复制
        } else {
            val videoFile = MiaoDocumentFile(this, videoDir.documentFile, "/video.m4s")
            val audioFile = MiaoDocumentFile(this, videoDir.documentFile, "/audio.m4s")
            if (!videoFile.exists()) {
                toast("找不到视频")
                return false
            }
            if (!audioFile.exists()) {
                // 直接复制
                status.value = Status.Copying(
                    name = entry.name,
                    entryDirPath = entryDirPath,
                    cover = entry.cover,
                    progress = 0f,
                )
                launch {
                    videoFile.copyToTemp(outFile)
                }
                return false
            }
            status.value = Status.CopyingToTemp(
                name = entry.name,
                entryDirPath = entryDirPath,
                cover = entry.cover,
                progress = 0f,
                audioFile = audioFile,
                videoFile = videoFile,
            )
            launch {
                try {
                    val tempVideoFile = File(getTempPath(),"video.m4s")
                    val tempAudioFile = File(getTempPath(),"audio.m4s")
                    videoFile.copyToTemp(tempVideoFile)
                    audioFile.copyToTemp(tempAudioFile)
                    status.value = Status.InProgress(
                        name = entry.name,
                        entryDirPath = entryDirPath,
                        cover = entry.cover,
                        progress = 0f
                    )
                    mergerVideoAndAudio(
                        tempVideoFile,
                        tempAudioFile,
                        outFile
                    )
                } catch (e: Exception) {
                    status.value = Status.Error(
                        status.value,
                        e.message ?: e.toString(),
                    )
                    e.printStackTrace()
                }
            }
            return true
        }
    }

    private suspend fun copyFile(
        inputFile: File,
        outFile: File
    ) {
        val fileInputStream = FileInputStream(inputFile)
        val fileOutputStream = FileOutputStream(outFile)
        val buffer = ByteArray(1024)
        var byteRead: Int
        while (-1 != fileInputStream.read(buffer).also { byteRead = it }) {
            fileOutputStream.write(buffer, 0, byteRead)
        }
        fileInputStream.close()
        fileOutputStream.flush()
        fileOutputStream.close()
    }

    fun mergerVideoAndAudio(
        videoFile: File,
        audioFile: File,
        outFile: File,
    ) {
        if (!outFile.parentFile.exists()){
            outFile.parentFile.mkdir()
        }
        val commands = RxFFmpegCommandList().apply {
            append("-i")
            append(videoFile.path)
            append("-i")
            append(audioFile.path)
            append("-c:v")
            append("copy")
            append("-strict")
            append("experimental")
            append(outFile.path)
        }.build()
        //开始执行FFmpeg命令
        val myRxFFmpegSubscriber = MyRxFFmpegSubscriber(
            status, getTempPath()
        )
        RxFFmpegInvoke.getInstance()
            .runCommandRxJava(commands)
            .subscribe(myRxFFmpegSubscriber)
    }

    private fun toast(message: String) {
        val duration = if (message.length > 10) {
            Toast.LENGTH_LONG
        } else {
            Toast.LENGTH_SHORT
        }
        Toast.makeText(this, message, duration)
            .show()
    }

    private fun getTempPath(): String {
        var file = File(getExternalFilesDir(null), "../temp")
        if (!file.exists()) {
            file.mkdir()
        }
        return file.canonicalPath
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    sealed class Status {
        open val entryDirPath: String = ""
        open val name: String = ""
        open val cover: String = ""
        open val progress = 0f

        object InIdle : Status()

        data class CopyingToTemp(
            override val entryDirPath: String,
            override val name: String,
            override val cover: String,
            override val progress: Float,
            val videoFile: MiaoDocumentFile,
            val audioFile: MiaoDocumentFile,
        ): Status()

        data class Copying(
            override val entryDirPath: String,
            override val name: String,
            override val cover: String,
            override val progress: Float,
        ): Status()

        data class InProgress(
            override val entryDirPath: String,
            override val name: String,
            override val cover: String,
            override val progress: Float,
        ): Status()

        data class Error(
            override val entryDirPath: String,
            override val name: String,
            override val cover: String,
            override val progress: Float,
            val message: String,
        ): Status() {
            constructor(
                status: Status,
                message: String
            ) : this(
                status.entryDirPath,
                status.name,
                status.cover,
                status.progress,
                message
            )
        }
    }

}