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
import cn.a10miaomiao.bilidown.BiliDownApp
import cn.a10miaomiao.bilidown.common.CommandUtil
import cn.a10miaomiao.bilidown.common.file.MiaoDocumentFile
import cn.a10miaomiao.bilidown.db.AppDatabase
import cn.a10miaomiao.bilidown.db.dao.OutRecord
import cn.a10miaomiao.bilidown.entity.BiliDownloadEntryInfo
import cn.a10miaomiao.bilidown.shizuku.service.UserService
import cn.a10miaomiao.bilidown.shizuku.util.RemoteServiceUtil
import cn.a10miaomiao.bilidown.state.AppState
import cn.a10miaomiao.bilidown.state.TaskStatus
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

//        val instance get() = _instance

        suspend fun getService(context: Context): BiliDownService {
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
    private lateinit var appState: AppState

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val myProgressCallback by lazy {
        MyProgressCallback(this@BiliDownService, appState)
    }

    override fun onCreate() {
        super.onCreate()
        appDatabase = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "bili-down-out"
        ).build()
        appState = (application as BiliDownApp).state
        job = Job()
        launch {
            channel.send(this@BiliDownService)
            appState.taskStatus.collect {
                if (it is TaskStatus.InIdle) {

                    // 空闲状态，进行下一个任务
                }
            }
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
    ): Boolean {
        val taskStatus = appState.taskStatus.value
        val shizukuState = appState.shizukuState.value
        if (taskStatus != TaskStatus.InIdle && taskStatus !is TaskStatus.Error) {
            toast("有视频正在导出中，请稍后再试")
            return false
        }

        val t = appDatabase.outRecordDao().findByPath(entryDirPath)
        if (t != null) {
            toast("此视频已导出")
            return false
        }
        // 使用Shizuku
        if (shizukuState.isEnabled) {
            val shizukuUserService = RemoteServiceUtil.getUserService()
            val errorMessage = shizukuUserService.exportBiliVideo(
                entryDirPath,
                outFile.path,
                myProgressCallback,
            )
            if (errorMessage != null) {
                toast(errorMessage)
                return false
            }
            return true
        }

        // 使用DocumentFile
        if (entryDirPath.startsWith("content:")) {
            return copyAndExportBiliVideo(entryDirPath, outFile)
        }

        // 使用Java File API正常导出
        val entryDirFile = File(entryDirPath)
        val entryJsonFile = File(entryDirPath, "entry.json")
        val json = Json { ignoreUnknownKeys = true }
        val entry = json.decodeFromString<BiliDownloadEntryInfo>(entryJsonFile.readText())

        val videoDirPath = entryDirPath + "/" + entry.videoDirName
        val videoDir = File(videoDirPath)
        if (!videoDir.exists() || !videoDir.isDirectory) {
            val videoFile = entryDirFile.listFiles().find {
                it.isFile && it.name.startsWith(entry.videoDirName)
                        && it.name.endsWith(".mp4")
            }
            if (videoFile == null) {
                toast("找不到视频文件夹：${entry.videoDirName}")
                return false
            }
            // 直接复制mp4文件
            appState.putTaskStatus(
                TaskStatus.Copying(
                    name = entry.name,
                    entryDirPath = entryDirPath,
                    cover = entry.cover,
                    progress = 0f,
                )
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
            appState.putTaskStatus(
                TaskStatus.InProgress(
                    name = entry.name,
                    entryDirPath = entryDirPath,
                    cover = entry.cover,
                    progress = 0f
                )
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
                appState.putTaskStatus(
                    TaskStatus.Copying(
                        name = entry.name,
                        entryDirPath = entryDirPath,
                        cover = entry.cover,
                        progress = 0f,
                    )
                )
                launch {
                    copyFile(videoFile, outFile)
                }
                return true
            }
            appState.putTaskStatus(
                TaskStatus.InProgress(
                    name = entry.name,
                    entryDirPath = entryDirPath,
                    cover = entry.cover,
                    progress = 0f,
                )
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
    ): Boolean {
        val entryDirFile = DocumentFile.fromTreeUri(this, Uri.parse(entryDirPath))!!
        val entryJsonFile = MiaoDocumentFile(this, entryDirFile, "/entry.json")
        val json = Json { ignoreUnknownKeys = true }
        val entry = json.decodeFromString<BiliDownloadEntryInfo>(entryJsonFile.readText())
        val videoDir = MiaoDocumentFile(this, entryDirFile, "/${entry.videoDirName}")

        if (!videoDir.exists() || !videoDir.isDirectory) {
            val videoFile = entryDirFile.listFiles().find {
                it.isFile && it.name?.startsWith(entry.videoDirName) == true
                        && it.name?.endsWith(".mp4") == true
            }
            if (videoFile == null) {
                toast("找不到视频文件夹：${entry.videoDirName}")
                return false
            }
            // 直接复制mp4文件
            appState.putTaskStatus(
                TaskStatus.Copying(
                    name = entry.name,
                    entryDirPath = entryDirPath,
                    cover = entry.cover,
                    progress = 0f,
                )
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
            appState.putTaskStatus(
                TaskStatus.CopyingToTemp(
                    name = entry.name,
                    entryDirPath = entryDirPath,
                    cover = entry.cover,
                    progress = 0f,
                )
            )
            launch {
                try {
                    val tempFiles = blvFiles.map {
                        val tempF = File(getTempPath(), it.name)
                        it.copyToTemp(tempF)
                        tempF
                    }
                    appState.putTaskStatus(
                        TaskStatus.InProgress(
                            name = entry.name,
                            entryDirPath = entryDirPath,
                            cover = entry.cover,
                            progress = 0f
                        )
                    )
                    mergerVideos(
                        tempFiles,
                        outFile
                    )
                } catch (e: Exception) {
                    appState.putTaskStatus(
                        TaskStatus.Error(
                            appState.taskStatus.value,
                            e.message ?: e.toString(),
                        )
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
                appState.putTaskStatus(
                    TaskStatus.Copying(
                        name = entry.name,
                        entryDirPath = entryDirPath,
                        cover = entry.cover,
                        progress = 0f,
                    )
                )
                launch {
                    copyFile(videoFile, outFile)
                }
                return false
            }
            appState.putTaskStatus(
                TaskStatus.CopyingToTemp(
                    name = entry.name,
                    entryDirPath = entryDirPath,
                    cover = entry.cover,
                    progress = 0f,
    //                audioFile = audioFile,
    //                videoFile = videoFile,
                )
            )
            launch {
                try {
                    val tempVideoFile = File(getTempPath(), "video.m4s")
                    val tempAudioFile = File(getTempPath(), "audio.m4s")
                    videoFile.copyToTemp(tempVideoFile)
                    audioFile.copyToTemp(tempAudioFile)
                    appState.putTaskStatus(
                        TaskStatus.InProgress(
                            name = entry.name,
                            entryDirPath = entryDirPath,
                            cover = entry.cover,
                            progress = 0f
                        )
                    )
                    mergerVideoAndAudio(
                        tempVideoFile,
                        tempAudioFile,
                        outFile
                    )
                } catch (e: Exception) {
                    appState.putTaskStatus(
                        TaskStatus.Error(
                            appState.taskStatus.value,
                            e.message ?: e.toString(),
                        )
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

        val currentStatus = appState.taskStatus.value
        addOutRecord(
            currentStatus.entryDirPath,
            outFile.path,
            outFile.name,
            currentStatus.cover,
            status = OutRecord.STATUS_SUCCESS
        )
        appState.putTaskStatus(TaskStatus.InIdle)
    }

    private suspend fun copyFile(
        inputFile: MiaoDocumentFile,
        outFile: File
    ) {
        inputFile.copyToTemp(outFile)
        val currentStatus = appState.taskStatus.value
        addOutRecord(
            currentStatus.entryDirPath,
            outFile.path,
            outFile.name,
            currentStatus.cover,
            status = OutRecord.STATUS_SUCCESS
        )
        appState.putTaskStatus(TaskStatus.InIdle)
    }

    private fun mergerVideoAndAudio(
        videoFile: File,
        audioFile: File,
        outFile: File,
    ) {
        if (!outFile.parentFile!!.exists()) {
            outFile.parentFile!!.mkdir()
        }
        val commands = RxFFmpegCommandList().apply {
            append("-i")
            append(videoFile.absolutePath)
            append("-i")
            append(audioFile.absolutePath)
            append("-c:v")
            append("copy")
            append("-strict")
            append("experimental")
            append(outFile.absolutePath)
        }.build()
        //开始执行FFmpeg命令
        val myRxFFmpegSubscriber = object : MyRxFFmpegSubscriber(
            appState,
//            getTempPath()
        ) {
            override fun onFinish() {
                val currentStatus = appState.taskStatus.value
                launch {
                    addOutRecord(
                        currentStatus.entryDirPath,
                        outFile.path,
                        outFile.name,
                        currentStatus.cover,
                        status = OutRecord.STATUS_SUCCESS
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

    private fun mergerVideos(
        videoFiles: List<File>,
        outFile: File,
    ) {
        if (!outFile.parentFile!!.exists()) {
            outFile.parentFile!!.mkdir()
        }
        val ffTxtFile = File(getTempPath(), ".ff.txt")
        if (ffTxtFile.exists() && ffTxtFile.isDirectory) {
            ffTxtFile.deleteRecursively()
            ffTxtFile.delete()
        }
        val ffTxtContent = videoFiles.map {
                file -> "file ${CommandUtil.filePath(file)}"
        }.joinToString("\n")
        ffTxtFile.writeText(ffTxtContent)
        val commands = RxFFmpegCommandList().apply {
            append("-f")
            append("concat")
            append("-safe")
            append("0")
            append("-i")
            append(ffTxtFile.absolutePath)
            append("-c")
            append("copy")
            append(outFile.absolutePath)
        }.build()
        //开始执行FFmpeg命令
        val myRxFFmpegSubscriber = object : MyRxFFmpegSubscriber(appState) {
            override fun onFinish() {
                val currentStatus = appState.taskStatus.value
                launch {
                    addOutRecord(
                        currentStatus.entryDirPath,
                        outFile.path,
                        outFile.name,
                        currentStatus.cover,
                        status = OutRecord.STATUS_SUCCESS
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

    private suspend fun addOutRecord(
        entryDirPath: String,
        outFilePath: String,
        title: String,
        cover: String,
        status: Int,
    ) {
        val outRecordDao = appDatabase.outRecordDao()
        val record = outRecordDao.findByPath(entryDirPath)
        val currentTime = System.currentTimeMillis()
        if (record == null) {
            val newRecord = OutRecord(
                entryDirPath = entryDirPath,
                outFilePath = outFilePath,
                title = title,
                cover = cover,
                status = status,
                type = 1,
                createTime = currentTime,
                updateTime = currentTime,
            )
            outRecordDao.insertAll(newRecord)
        } else {
            val newRecord = record.copy(
                entryDirPath = entryDirPath,
                outFilePath = outFilePath,
                title = title,
                cover = cover,
                status = status,
                updateTime = currentTime,
            )
            outRecordDao.update(newRecord)
        }
    }

    fun tryAddOutRecord(
        entryDirPath: String,
        outFilePath: String,
        title: String,
        cover: String,
    ) {
        launch {
            addOutRecord(
                entryDirPath, outFilePath, title, cover,
                status = OutRecord.STATUS_SUCCESS
            )
        }
    }

    suspend fun getRecordList(): List<OutRecord> {
        return appDatabase.outRecordDao().getAll()
    }

    suspend fun getRecordList(status: Int): List<OutRecord> {
        return appDatabase.outRecordDao().getAllByStatus(status)
    }

    suspend fun getRecordList(paths: Array<String>): List<OutRecord> {
        return appDatabase.outRecordDao().getAllByEntryDirPaths(paths)
    }

    suspend fun addTask(
        entryDirPath: String,
        outFilePath: String,
        title: String,
        cover: String,
    ) {
        val outRecordDao = appDatabase.outRecordDao()
        val record = outRecordDao.findByPath(entryDirPath)
        val currentTime = System.currentTimeMillis()
        if (record == null) {
            val newRecord = OutRecord(
                entryDirPath = entryDirPath,
                outFilePath = outFilePath,
                title = title,
                cover = cover,
                status = OutRecord.STATUS_WAIT,
                type = 1,
                createTime = currentTime,
                updateTime = currentTime,
            )
            outRecordDao.insertAll(newRecord)
            withContext(Dispatchers.Main) {
                toast("成功创建任务：${title}")
            }
        } else {
            withContext(Dispatchers.Main) {
                if (record.status == OutRecord.STATUS_SUCCESS) {
                    toast("该视频已导出：${record.title}")
                } else {
                    toast("该视频已在队列中：${record.title}")
                }
            }
        }
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

}