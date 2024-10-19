package cn.a10miaomiao.bilidown.state

sealed class TaskStatus {
    open val entryDirPath: String = ""
    open val name: String = ""
    open val cover: String = ""
    open val progress = 0f

    object InIdle : TaskStatus()

    data class CopyingToTemp(
        override val entryDirPath: String,
        override val name: String,
        override val cover: String,
        override val progress: Float,
//            val videoFile: MiaoDocumentFile,
//            val audioFile: MiaoDocumentFile,
    ) : TaskStatus()

    data class Copying(
        override val entryDirPath: String,
        override val name: String,
        override val cover: String,
        override val progress: Float,
    ) : TaskStatus()

    data class InProgress(
        override val entryDirPath: String,
        override val name: String,
        override val cover: String,
        override val progress: Float,
    ) : TaskStatus()

    data class Error(
        override val entryDirPath: String,
        override val name: String,
        override val cover: String,
        override val progress: Float,
        val message: String,
    ) : TaskStatus() {
        constructor(
            status: TaskStatus,
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