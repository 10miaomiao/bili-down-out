package cn.a10miaomiao.bilidown.db

import androidx.room.Database
import androidx.room.RoomDatabase
import cn.a10miaomiao.bilidown.db.dao.OutRecord
import cn.a10miaomiao.bilidown.db.model.OutRecordDao

@Database(
    entities = [
        OutRecord::class,
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun outRecordDao(): OutRecordDao

}