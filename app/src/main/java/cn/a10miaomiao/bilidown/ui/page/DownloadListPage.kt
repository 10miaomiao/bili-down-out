package cn.a10miaomiao.bilidown.ui.page

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import cn.a10miaomiao.bilidown.R
import cn.a10miaomiao.bilidown.common.BiliDownFile
import cn.a10miaomiao.bilidown.common.BiliDownUtils
import cn.a10miaomiao.bilidown.common.datastore.DataStoreKeys
import cn.a10miaomiao.bilidown.common.datastore.rememberDataStorePreferencesFlow
import cn.a10miaomiao.bilidown.common.lifecycle.LaunchedLifecycleObserver
import cn.a10miaomiao.bilidown.common.localStoragePermission
import cn.a10miaomiao.bilidown.common.molecule.collectAction
import cn.a10miaomiao.bilidown.common.molecule.rememberPresenter
import cn.a10miaomiao.bilidown.entity.DownloadInfo
import cn.a10miaomiao.bilidown.entity.DownloadItemInfo
import cn.a10miaomiao.bilidown.entity.DownloadType
import cn.a10miaomiao.bilidown.ui.BiliDownScreen
import cn.a10miaomiao.bilidown.ui.components.DownloadListItem
import cn.a10miaomiao.bilidown.ui.components.PermissionDialog
import kotlinx.coroutines.flow.Flow


data class DownloadListPageState(
    val list: List<DownloadInfo>,
    val path: String,
    val canRead: Boolean,
    val loading: Boolean,
)

sealed class DownloadListPageAction {
    class GetList(
        val packageName: String,

    ) : DownloadListPageAction()
}

@Composable
fun DownloadListPagePresenter(
    context: Context,

    action: Flow<DownloadListPageAction>,
): DownloadListPageState {
    val list = remember {
        mutableStateListOf<DownloadInfo>()
    }
    var path by remember {
        mutableStateOf("")
    }
    var canRead by remember {
        mutableStateOf(true)
    }
    var loading by remember {
        mutableStateOf(true)
    }

    action.collectAction {
        when (it) {
            is DownloadListPageAction.GetList -> {
                val biliDownFile = BiliDownFile(context, it.packageName)
                canRead = biliDownFile.canRead()
                if (!canRead) {
                    return@collectAction
                }
                loading = true
                list.clear()
                biliDownFile.readDownloadList().forEach {
                    val biliEntry = it.entry
                    var indexTitle = ""
                    var itemTitle = ""
                    var id = 0L
                    var cid = 0L
                    var epid = 0L
                    var type = DownloadType.VIDEO
                    val page = biliEntry.page_data
                    if (page != null) {
                        id = biliEntry.avid!!
                        indexTitle = page.download_title
                        cid = page.cid
                        type = DownloadType.VIDEO
                        itemTitle = page.part
                    }
                    val ep = biliEntry.ep
                    val source = biliEntry.source
                    if (ep != null && source != null) {
                        id = biliEntry.season_id!!.toLong()
                        indexTitle = ep.index_title
                        epid = ep.episode_id
                        cid = source.cid
                        type = DownloadType.BANGUMI
                        itemTitle = if (ep.index_title.isNotBlank()) {
                            ep.index_title
                        } else {
                            ep.index
                        }
                    }
                    val item = DownloadItemInfo(
                        dir_path = it.entryDirPath,
                        media_type = biliEntry.media_type,
                        has_dash_audio = biliEntry.has_dash_audio,
                        is_completed = biliEntry.is_completed,
                        total_bytes = biliEntry.total_bytes,
                        downloaded_bytes = biliEntry.downloaded_bytes,
                        title = itemTitle,
                        cover = biliEntry.cover,
                        id = id,
                        type = type,
                        cid = cid,
                        epid = epid,
                        index_title = indexTitle,
                    )
                    val last = list.lastOrNull()
                    if (last != null
                        && last.type == item.type
                        && last.id == item.id) {
                        if (last.is_completed && !item.is_completed) {
                            last.is_completed = false
                        }
                        last.items.add(item)
                    } else {
                        list.add(
                            DownloadInfo(
                                dir_path = it.pageDirPath,
                                media_type = biliEntry.media_type,
                                has_dash_audio = biliEntry.has_dash_audio,
                                is_completed = biliEntry.is_completed,
                                total_bytes = biliEntry.total_bytes,
                                downloaded_bytes = biliEntry.downloaded_bytes,
                                title = biliEntry.title,
                                cover = biliEntry.cover,
                                cid = cid,
                                id = id,
                                type = type,
                                items = mutableListOf(item)
                            )
                        )
                    }
                }
                loading = false
            }
        }
    }
    return DownloadListPageState(
        list,
        path,
        canRead,
        loading,
    )
}

@Composable
fun DownloadListPage(
    navController: NavHostController,
    packageName: String,
) {
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    val storagePermission = localStoragePermission()
    val permissionState = storagePermission.collectState()

    val (state, channel) = rememberPresenter(listOf(packageName, permissionState)) {
        DownloadListPagePresenter(context, it)
    }

    LaunchedEffect(packageName, permissionState.isGranted, permissionState.isExternalStorage) {
        if (permissionState.isGranted
            && permissionState.isExternalStorage
            && state.list.isEmpty())
        {
            channel.send(DownloadListPageAction.GetList(
                packageName = packageName,
            ))
        }
    }

    LaunchedLifecycleObserver(
        onResume = {
            if (state.list.isEmpty()) {
                channel.trySend(DownloadListPageAction.GetList(
                    packageName = packageName,
                ))
            }
        }
    )

    fun resultCallBack() {
        if (!permissionState.isGranted || !permissionState.isExternalStorage) {
            showPermissionDialog = true
        }
    }

    PermissionDialog(
        showPermissionDialog = showPermissionDialog,
        isGranted = permissionState.isGranted,
        onDismiss = { showPermissionDialog = false }
    )
    if (!permissionState.isGranted || !permissionState.isExternalStorage) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (permissionState.isGranted) {
                Text(text = "请授予所有文件的存储权限")
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        storagePermission.requestPermissions(::resultCallBack)
                    }
                ) {
                    Text(text = "授予所有文件的权限")
                }
            } else {
                Text(text = "请授予存储权限")
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        storagePermission.requestPermissions(::resultCallBack)
                    }
                ) {
                    Text(text = "授予权限")
                }
            }
        }
    } else if (!state.canRead) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "请授予文件夹权限")
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    val biliDownFile = BiliDownFile(context, packageName)
                    biliDownFile.startFor(2)
                }
            ) {
                Text(text = "授予权限")
            }
        }
    } else if (!state.loading && state.list.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_movie_pay_area_limit),
                contentDescription = "空空如也",
                modifier = Modifier.size(200.dp, 200.dp)
            )
            Text(
                modifier = Modifier.padding(vertical = 8.dp),
                text = "空空如也",
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.list, { it.id }) {
                DownloadListItem(
                    item = it,
                    onClick = {
                        val dirPath = Uri.encode(it.dir_path)
                        navController.navigate(
                            BiliDownScreen.Detail.route + "?packageName=${packageName}&dirPath=${dirPath}"
                        )
                    }
                )
            }
        }
    }
}