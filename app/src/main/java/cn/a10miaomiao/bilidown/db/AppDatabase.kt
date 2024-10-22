package cn.a10miaomiao.bilidown.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.a10miaomiao.bilidown.db.dao.OutRecord
import cn.a10miaomiao.bilidown.db.model.OutRecordDao

@Database(
    entities = [ OutRecord::class,],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun outRecordDao(): OutRecordDao

    companion object {

        fun initialize(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "bili-down-out"
            ).apply {
                fallbackToDestructiveMigration()
                addMigrations(
                    MIGRATION_1_2
                )
            }.build()
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE out_record ADD COLUMN `message` TEXT")
            }
        }
    }

}