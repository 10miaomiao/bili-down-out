package cn.a10miaomiao.bilidown

import android.app.Application
import cn.a10miaomiao.bilidown.state.AppState

class BiliDownApp: Application() {

    val state = AppState()

    override fun onCreate() {
        super.onCreate()
        state.init(this)
    }

}