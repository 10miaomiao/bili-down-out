package cn.a10miaomiao.bilidown.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppState {

    private val _taskStatus = MutableStateFlow<TaskStatus>(TaskStatus.InIdle)
    val taskStatus: StateFlow<TaskStatus> = _taskStatus

    fun putTaskStatus(taskStatus: TaskStatus) {
        _taskStatus.value = taskStatus
    }

}