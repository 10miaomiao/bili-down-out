package cn.a10miaomiao.bilidown.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.room.Room
import cn.a10miaomiao.bilidown.common.file.MiaoDocumentFile
import cn.a10miaomiao.bilidown.db.AppDatabase
import cn.a10miaomiao.bilidown.db.dao.OutRecord
import cn.a10miaomiao.bilidown.entity.BiliDownloadEntryInfo
import io.microshow.rxffmpeg.RxFFmpegCommandList
import io.microshow.rxffmpeg.RxFFmpegInvoke
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        val status = MutableStateFlow<Status>(Status.InIdle)

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

    private lateinit var appDatabase: AppDatabase

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    override fun onCreate() {
        super.onCreate()
        appDatabase = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "bili-down-out"
        ).build()
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

    suspend fun exportBiliVideo(
        entryDirPath: String,
        outFile: File,
    ) : Boolean{

        if (status.value != Status.InIdle && status.value !is Status.Error) {
            toast("有视频正在导出中，请稍后再试")
            return false
        }

        val t = appDatabase.outRecordDao().findByPath(entryDirPath)
        if (t != null) {
            toast("此视频已导出")
            return false
        }

        if(entryDirPath.startsWith("content:")) {
            return copyAndExportBiliVideo(entryDirPath, outFile)
        }
        val entryDirFile = File(entryDirPath)
        val entryJsonFile = File(entryDirPath, "entry.json")
        val json = Json { ignoreUnknownKeys = true }
        val entry = json.decodeFromString<BiliDownloadEntryInfo>(entryJsonFile.readText())

        val videoDirPath = entryDirPath + "/" + entry.type_tag
        val videoDir = File(videoDirPath)
        if (!videoDir.exists() || !videoDir.isDirectory) {
            val videoFile = entryDirFile.listFiles().find {
                it.isFile && it.name.startsWith(entry.type_tag)
                        && it.name.endsWith(".mp4")
            }
            if (videoFile == null) {
                toast("找不到视频文件夹：${entry.type_tag}")
                return false
            }
            // 直接复制mp4文件
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
//        val videoIndexJsonFile = File(videoDirPath, "index.json")
//        if (!videoIndexJsonFile.exists()) {
//            toast("缓存文件丢失，请重新缓存")
//            return false
//        }
//        val videoIndexJson = videoIndexJsonFile.readText()
        if (entry.media_type == 1) {
            // 多段blv(flv)视频
            val blvFiles = mutableListOf<File>()
            var blvIndex = 0
            while (true) {
                val file = File(videoDir, "/${blvIndex}.blv")
                if (file.exists()) {
                    blvFiles.add(file)
                    blvIndex++
                } else {
                    break
                }
            }
            if (blvFiles.isEmpty()) {
                toast("找不到视频文件：0.blv")
                return false
            }
            status.value = Status.InProgress(
                name = entry.name,
                entryDirPath = entryDirPath,
                cover = entry.cover,
                progress = 0f
            )
            mergerVideos(
                blvFiles,
                outFile
            )
            return true
        } else {
            val videoFile = File(videoDir, "video.m4s")
            val audioFile = File(videoDir, "audio.m4s")
            if (!videoFile.exists()) {
                toast("找不到视频文件")
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
    suspend fun copyAndExportBiliVideo(
        entryDirPath: String,
        outFile: File,
    ) : Boolean{
        val entryDirFile = DocumentFile.fromTreeUri(this, Uri.parse(entryDirPath))!!
        val entryJsonFile = MiaoDocumentFile(this, entryDirFile, "/entry.json")
        val json = Json { ignoreUnknownKeys = true }
        val entry = json.decodeFromString<BiliDownloadEntryInfo>(entryJsonFile.readText())
        val videoDir = MiaoDocumentFile(this, entryDirFile, "/${entry.type_tag}")

        if (!videoDir.exists() || !videoDir.isDirectory) {
            val videoFile = entryDirFile.listFiles().find {
                it.isFile && it.name?.startsWith(entry.type_tag) == true
                        && it.name?.endsWith(".mp4") == true
            }
            if (videoFile == null) {
                toast("找不到视频文件夹：${entry.type_tag}")
                return false
            }
            // 直接复制mp4文件
            status.value = Status.Copying(
                name = entry.name,
                entryDirPath = entryDirPath,
                cover = entry.cover,
                progress = 0f,
            )
            launch {
                copyFile(MiaoDocumentFile(this@BiliDownService, videoFile), outFile)
            }
            return true
        }
//        val videoIndexJsonFile = File(videoDirPath, "index.json")
//        if (!videoIndexJsonFile.exists()) {
//            toast("缓存文件丢失，请重新缓存")
//            return false
//        }
//        val videoIndexJson = videoIndexJsonFile.readText()
        if (entry.media_type == 1) {
            // 多段blv(flv)视频
            val blvFiles = mutableListOf<MiaoDocumentFile>()
            var blvIndex = 0
            while (true) {
                val file = MiaoDocumentFile(this, videoDir.documentFile, "/${blvIndex}.blv")
                if (file.exists()) {
                    blvFiles.add(file)
                    blvIndex++
                } else {
                    break
                }
            }
            if (blvFiles.isEmpty()) {
                toast("找不到视频文件：0.blv")
                return false
            }
            status.value = Status.CopyingToTemp(
                name = entry.name,
                entryDirPath = entryDirPath,
                cover = entry.cover,
                progress = 0f,
            )
            launch {
                try {
                    val tempFiles = blvFiles.map {
                        val tempF = File(getTempPath(), it.name)
                        it.copyToTemp(tempF)
                        tempF
                    }
                    status.value = Status.InProgress(
                        name = entry.name,
                        entryDirPath = entryDirPath,
                        cover = entry.cover,
                        progress = 0f
                    )
                    mergerVideos(
                        tempFiles,
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
        } else {
            // 音视频分离视频
            val videoFile = MiaoDocumentFile(this, videoDir.documentFile, "/video.m4s")
            val audioFile = MiaoDocumentFile(this, videoDir.documentFile, "/audio.m4s")
            if (!videoFile.exists()) {
                toast("找不到视频文件：video.m4s")
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
                    copyFile(videoFile, outFile)
                }
                return false
            }
            status.value = Status.CopyingToTemp(
                name = entry.name,
                entryDirPath = entryDirPath,
                cover = entry.cover,
                progress = 0f,
//                audioFile = audioFile,
//                videoFile = videoFile,
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

        val currentStatus = status.value
        addTask(
            currentStatus.entryDirPath,
            outFile.path,
            outFile.name,
            currentStatus.cover,
        )
        status.value = Status.InIdle
    }

    private suspend fun copyFile(
        inputFile: MiaoDocumentFile,
        outFile: File
    ) {
        inputFile.copyToTemp(outFile)
        val currentStatus = status.value
        addTask(
            currentStatus.entryDirPath,
            outFile.path,
            outFile.name,
            currentStatus.cover,
        )
        status.value = Status.InIdle
    }

    private fun mergerVideoAndAudio(
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
        val myRxFFmpegSubscriber = object : MyRxFFmpegSubscriber(
//            getTempPath()
        ) {
            override fun onFinish() {
                val currentStatus = status.value
                launch {
                    addTask(
                        currentStatus.entryDirPath,
                        outFile.path,
                        outFile.name,
                        currentStatus.cover,
                    )
                }
                val tempPath = getTempPath()
                File(tempPath).deleteRecursively()
                super.onFinish()
            }
        }
        RxFFmpegInvoke.getInstance()
            .runCommandRxJava(commands)
            .subscribe(myRxFFmpegSubscriber)
    }

    private fun  mergerVideos(
        videoFiles: List<File>,
        outFile: File,
    ) {
        val ffTxtFile = File(getTempPath(),"ff.txt")
        val ffTxtContent = videoFiles.map {
            file -> "file '${file.path}'"
        }.joinToString("\n")
        ffTxtFile.writeText(ffTxtContent)

        val commands = RxFFmpegCommandList().apply {
            append("-f")
            append("concat")
            append("-safe")
            append("0")
            append("-i")
            append(ffTxtFile.path)
            append("-c")
            append("copy")
            append(outFile.path)
        }.build()
        //开始执行FFmpeg命令
        val myRxFFmpegSubscriber = object : MyRxFFmpegSubscriber() {
            override fun onFinish() {
                val currentStatus = status.value
                launch {
                    addTask(
                        currentStatus.entryDirPath,
                        outFile.path,
                        outFile.name,
                        currentStatus.cover,
                    )
                }
                val tempPath = getTempPath()
                File(tempPath).deleteRecursively()
                super.onFinish()
            }
        }
        RxFFmpegInvoke.getInstance()
            .runCommandRxJava(commands)
            .subscribe(myRxFFmpegSubscriber)
    }

    private suspend fun addTask(
        entryDirPath: String,
        outFilePath: String,
        title: String,
        cover: String,
    ) {
        val currentTime = System.currentTimeMillis()
        val task = OutRecord(
            entryDirPath = entryDirPath,
            outFilePath = outFilePath,
            title = title,
            cover = cover,
            status = 1,
            type = 1,
            createTime = currentTime,
            updateTime = currentTime,
        )
        appDatabase.outRecordDao().insertAll(task)
    }

    suspend fun getTaskList(): List<OutRecord> {
        return appDatabase.outRecordDao().getAll()
    }

    suspend fun delTask(
        task: OutRecord,
        isDeleteFile: Boolean,
    ) {
        if (isDeleteFile) {
            val outFile = File(task.outFilePath)
            if (outFile.exists()) {
                outFile.delete()
            }
            appDatabase.outRecordDao().delete(task)
            withContext(Dispatchers.Main) {
                toast("已删除记录及文件${task.title}")
            }
        } else {
            appDatabase.outRecordDao().delete(task)
            withContext(Dispatchers.Main) {
                toast("已删除记录${task.title}")
            }
        }
    }

    private suspend fun toast(message: String) {
        val duration = if (message.length > 10) {
            Toast.LENGTH_LONG
        } else {
            Toast.LENGTH_SHORT
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(this@BiliDownService, message, duration)
                .show()
        }
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
//            val videoFile: MiaoDocumentFile,
//            val audioFile: MiaoDocumentFile,
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