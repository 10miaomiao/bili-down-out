package cn.a10miaomiao.bilidown.common

typealias AndroidLog = android.util.Log

object MiaoLog {

    private val currentLevel: Int = AndroidLog.INFO
    private fun String.simpleName() = substring(lastIndexOf('.') + 1, indexOf("$"))

    fun info(msg: () -> String) {
        AndroidLog.i("MiaoLog:" + msg::class.java.name.simpleName(), msg())
    }

    fun debug(msg: () -> String) {
        AndroidLog.d("MiaoLog:" + msg::class.java.name.simpleName(), msg())
    }

    fun error(msg: () -> String) {
        AndroidLog.e("MiaoLog:" + msg::class.java.name.simpleName(), msg())
    }

}