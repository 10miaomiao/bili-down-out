package cn.a10miaomiao.bilidown.state

import android.content.Context
import cn.a10miaomiao.bilidown.shizuku.permission.ShizukuPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppState {

    private val _taskStatus = MutableStateFlow<TaskStatus>(TaskStatus.InIdle)
    val taskStatus: StateFlow<TaskStatus> = _taskStatus

    private val _shizukuState = MutableStateFlow(
        ShizukuPermission.ShizukuPermissionState()
    )
    val shizukuState: StateFlow<ShizukuPermission.ShizukuPermissionState> = _shizukuState

    fun init(context: Context) {

    }

    fun putTaskStatus(taskStatus: TaskStatus) {
        _taskStatus.value = taskStatus
    }

    fun putShizukuState(state: ShizukuPermission.ShizukuPermissionState) {
        _shizukuState.value = state
    }

}