package cn.a10miaomiao.bilidown.shizuku.util

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import cn.a10miaomiao.bilidown.BuildConfig
import cn.a10miaomiao.bilidown.common.MiaoLog
import cn.a10miaomiao.bilidown.service.BiliDownService
import cn.a10miaomiao.bilidown.shizuku.IUserService
import cn.a10miaomiao.bilidown.shizuku.service.UserService
import kotlinx.coroutines.channels.Channel
import rikka.shizuku.Shizuku

object RemoteServiceUtil {

    private val mChannel = Channel<IUserService>()
    private var mUserService: IUserService? = null

    suspend fun getUserService(): IUserService{
        MiaoLog.debug { "getUserService" }
        mUserService?.let { return it }
        bindUserService()
        return mChannel.receive()
    }


    private val userServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder?) {
            MiaoLog.debug { "onServiceConnected" }
            val res = StringBuilder()
            res.append("onServiceConnected: ").append(componentName.className).append('\n')
            if (binder != null && binder.pingBinder()) {
                val service = IUserService.Stub.asInterface(binder)
                try {
                    res.append(service.doSomething())
                } catch (e: RemoteException) {
                    e.printStackTrace()
                    res.append(Log.getStackTraceString(e))
                }
                service.doSomething()
                mUserService = service
                mChannel.trySend(service)
            } else {
                res.append("invalid binder for ").append(componentName).append(" received")
            }
            MiaoLog.debug { res.toString().trim { it <= ' ' } }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            MiaoLog.debug { "onServiceDisconnected" }
            mUserService = null
        }
    }

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(
            BuildConfig.APPLICATION_ID,
            UserService::class.java.name
        )
    )
        .daemon(false)
        .processNameSuffix("service")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private fun bindUserService() {
        MiaoLog.debug { "bindUserService" }
        val res = java.lang.StringBuilder()
        try {
            if (Shizuku.getVersion() < 10) {
                res.append("requires Shizuku API 10")
            } else {
                Shizuku.bindUserService(userServiceArgs, userServiceConnection)
            }
        } catch (tr: Throwable) {
            tr.printStackTrace()
            res.append(tr.toString())
        }
        MiaoLog.debug { res.toString().trim { it <= ' ' } }
    }

    private fun unbindUserService() {
        MiaoLog.debug { "unbindUserService" }
        val res = java.lang.StringBuilder()
        try {
            if (Shizuku.getVersion() < 10) {
                res.append("requires Shizuku API 10")
            } else {
                Shizuku.unbindUserService(userServiceArgs, userServiceConnection, true)
            }
        } catch (tr: Throwable) {
            tr.printStackTrace()
            res.append(tr.toString())
        }
        MiaoLog.debug { res.toString().trim { it <= ' ' } }
    }
}