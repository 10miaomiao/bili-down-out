package cn.a10miaomiao.bilidown

import android.app.Application
import cn.a10miaomiao.bilidown.db.AppDatabase
import cn.a10miaomiao.bilidown.state.AppState

class BiliDownApp: Application() {

    val state = AppState()

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        AppCrashHandler.getInstance(this)
        state.init(this)
        database = AppDatabase.initialize(this)
    }

}