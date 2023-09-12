package cn.a10miaomiao.bilidown.common.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner


private typealias LifecycleEvent = () -> Unit

class ComposeLifecycle(
    private val createEvent: LifecycleEvent? = null,
    private val startEvent: LifecycleEvent? = null,
    private val resumeEvent: LifecycleEvent? = null,
    private val pauseEvent: LifecycleEvent? = null,
    private val stopEvent: LifecycleEvent? = null,
    private val destroyEvent: LifecycleEvent? = null,
) : DefaultLifecycleObserver {

    override fun onCreate(owner: LifecycleOwner) {
        createEvent?.invoke()
    }

    override fun onStart(owner: LifecycleOwner) {
        startEvent?.invoke()
    }

    override fun onResume(owner: LifecycleOwner) {
        resumeEvent?.invoke()
    }

    override fun onPause(owner: LifecycleOwner) {
        pauseEvent?.invoke()
    }

    override fun onStop(owner: LifecycleOwner) {
        stopEvent?.invoke()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        destroyEvent?.invoke()
    }

}

@Composable
fun LaunchedLifecycleObserver(
    onCreate: LifecycleEvent? = null,
    onStart: LifecycleEvent? = null,
    onResume: LifecycleEvent? = null,
    onPause: LifecycleEvent? = null,
    onStop: LifecycleEvent? = null,
    onDestroy: LifecycleEvent? = null,
) {
    val composeLifecycle = remember(
//        onCreate, onStart, onResume, onPause, onStop, onDestroy
    ) {
        ComposeLifecycle(
            onCreate,
            onStart,
            onResume,
            onPause,
            onStop,
            onDestroy
        )
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        lifecycle.addObserver(composeLifecycle)
        onDispose {
            lifecycle.removeObserver(composeLifecycle)
        }
    }
}