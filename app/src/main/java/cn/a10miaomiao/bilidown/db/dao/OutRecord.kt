package cn.a10miaomiao.bilidown.db.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "out_record")
data class OutRecord (
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    @ColumnInfo(name = "input_path") val entryDirPath: String,
    @ColumnInfo(name = "out_file_path") val outFilePath: String,
    @ColumnInfo val title: String,
    @ColumnInfo val cover: String,
    @ColumnInfo val status: Int,
    @ColumnInfo val type: Int,
    @ColumnInfo(name = "create_time") val createTime: Long,
    @ColumnInfo(name = "update_time") val updateTime: Long,
) {
    companion object {
        const val STATUS_WAIT = 0
        const val STATUS_SUCCESS = 1
        const val STATUS_FAIL = 2
        const val STATUS_IN_PROGRESS = 10
    }
}