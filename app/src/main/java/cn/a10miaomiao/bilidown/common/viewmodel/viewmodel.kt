package cn.a10miaomiao.bilidown.common.viewmodel

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

fun <T : ViewModel> newViewModelFactory(initializer: (() -> T)): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        override fun <R : ViewModel> create(modelClass: Class<R>): R {
            return initializer.invoke() as R
        }
    }
}

@Suppress("MissingJvmstatic")
@Composable
inline fun <reified VM : ViewModel> viewModel(
    noinline initializer: (() -> VM)
): VM {
    return androidx.lifecycle.viewmodel.compose.viewModel<VM>(
        factory = newViewModelFactory<VM>(initializer)
    )
}