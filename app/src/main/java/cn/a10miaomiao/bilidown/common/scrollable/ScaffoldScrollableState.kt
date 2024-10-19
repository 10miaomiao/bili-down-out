package cn.a10miaomiao.bilidown.common.scrollable

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

@Stable
class ScaffoldScrollableState {

    private val _showBottomBar = mutableStateOf(true)
    val showBottomBar get() = _showBottomBar.value

    fun slideDown() {
        _showBottomBar.value = false
    }

    fun slideUp() {
        _showBottomBar.value = true
    }
}

class ScaffoldNestedScrollConnection(
    val state: ScaffoldScrollableState,
) : NestedScrollConnection {
    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        if (available.y > 0) {
            state.slideUp()
        } else if (available.y < 0) {
            state.slideDown()
        }
        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        return Offset.Zero
    }

}