package cn.a10miaomiao.bilidown.shizuku.permission

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.datastore.preferences.core.edit
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import cn.a10miaomiao.bilidown.MainActivity
import cn.a10miaomiao.bilidown.common.MiaoLog
import cn.a10miaomiao.bilidown.common.datastore.DataStoreKeys
import cn.a10miaomiao.bilidown.common.datastore.dataStore
import cn.a10miaomiao.bilidown.common.permission.StoragePermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider.MANAGER_APPLICATION_ID

class ShizukuPermission(
    val activity: FragmentActivity,
): Shizuku.OnRequestPermissionResultListener
    , Shizuku.OnBinderReceivedListener
    , Shizuku.OnBinderDeadListener {

    companion object {
        val isAboveN get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        const val SHIZUKU_PERMISSION_REQUEST_CODE = 413
    }

    private var resultCallBack: (() -> Unit)? = null

    private val state = MutableStateFlow(
        ShizukuPermissionState()
    )

    @Composable
    fun collectState(): ShizukuPermissionState {
        return state.collectAsState().value
    }

    init {
        Shizuku.addBinderReceivedListenerSticky(this)
        Shizuku.addBinderDeadListener(this)
        Shizuku.addRequestPermissionResultListener(this)
    }

    fun onDestroy() {
        Shizuku.removeBinderReceivedListener(this)
        Shizuku.removeBinderDeadListener(this)
        Shizuku.removeRequestPermissionResultListener(this)
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        when (requestCode) {
            SHIZUKU_PERMISSION_REQUEST_CODE -> {
                state.value = state.value.copy(
                    isGranted = grantResult == PackageManager.PERMISSION_DENIED
                )
            }
        }
    }

    override fun onBinderReceived() {
        state.value = state.value.copy(
            isRunning = true
        )
    }

    override fun onBinderDead() {
        if (Shizuku.isPreV11()) {
            state.value = state.value.copy(
                isPreV11 = true,
                isRunning = false,
            )
        } else {
            state.value = state.value.copy(
                isRunning = true
            )
        }
    }

    fun requestPermission(): Boolean {
        if (Shizuku.shouldShowRequestPermissionRationale()) {
            return false
        } else {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_DENIED) {
                Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
            }
            return true
        }
    }

    fun checkSelfPermission(): Boolean {
        return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }

    fun syncShizukuState(context: Context) {
        if (!isAboveN) {
            return
        }
        if (isRunning()) {
            val isGranted = checkSelfPermission()
            state.value = ShizukuPermissionState(
                isInstalled = true,
                isPreV11 = Shizuku.isPreV11(),
                isRunning = true,
                isGranted = isGranted,
                isEnabled = isGranted && state.value.isEnabled,
            )
            activity.lifecycleScope.launch {
                activity.dataStore.edit {
                    state.value = state.value.copy(
                        isEnabled = isGranted && (it[DataStoreKeys.enabledShizuku] ?: false),
                    )
                }
            }
        } else {
            state.value = ShizukuPermissionState(
                isInstalled = isInstalled(context),
            )
        }
    }

    fun isInstalled(context: Context): Boolean {
        return runCatching {
            val packageManager = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    MANAGER_APPLICATION_ID,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                packageManager.getPackageInfo(MANAGER_APPLICATION_ID, 0)
            }
        }.isSuccess
    }

    fun isRunning(): Boolean {
        return try {
            Shizuku.getUid() != -1
        } catch (e: Exception) {
            false
        }
    }

    fun setEnabled(enabled: Boolean) {
        state.value = state.value.copy(
            isEnabled = enabled
        )
        activity.lifecycleScope.launch {
            activity.dataStore.edit {
                it[DataStoreKeys.enabledShizuku] = enabled
            }
        }
    }

    data class ShizukuPermissionState(
        val isInstalled: Boolean = false,
        val isPreV11: Boolean = false,
        val isRunning: Boolean = false,
        val isGranted: Boolean = false,
        val isEnabled: Boolean = false,
    ) {
        val statusText: String get() {
            return if (!isInstalled) {
                "Shizuku未安装"
            } else if (isPreV11) {
                "Shizuku版本过低"
            } else if (!isRunning){
                "Shizuku未在运行"
            } else if (!isGranted){
                "Shizuku未授权"
            } else if (isEnabled){
                "已启用"
            } else {
                "未启用"
            }
        }
    }

}