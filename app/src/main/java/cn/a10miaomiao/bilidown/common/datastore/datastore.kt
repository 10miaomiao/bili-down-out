package cn.a10miaomiao.bilidown.common.datastore

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATA_STORE_KEY = "BiliDown_DataStore"

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(DATA_STORE_KEY)

@Composable
fun <T> rememberDataStorePreferencesFlow(
    context: Context,
    key: Preferences.Key<T>
) : Flow<T?> {
    return remember {
        context.dataStore.data.map {
            it[key]
        }
    }
}

@Composable
fun <T> rememberDataStorePreferencesFlow(
    context: Context,
    key: Preferences.Key<T>,
    initial: T,
) : Flow<T> {
    return remember {
        context.dataStore.data.map {
            it[key] ?: initial
        }
    }
}