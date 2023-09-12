package cn.a10miaomiao.bilidown.ui.page

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import cn.a10miaomiao.bilidown.common.BiliDownFile
import cn.a10miaomiao.bilidown.common.BiliDownOutFile
import cn.a10miaomiao.bilidown.common.file.MiaoDocumentFile
import cn.a10miaomiao.bilidown.common.file.MiaoJavaFile
import cn.a10miaomiao.bilidown.common.molecule.collectAction
import cn.a10miaomiao.bilidown.common.molecule.rememberPresenter
import cn.a10miaomiao.bilidown.entity.DownloadInfo
import cn.a10miaomiao.bilidown.entity.DownloadItemInfo
import cn.a10miaomiao.bilidown.entity.DownloadType
import cn.a10miaomiao.bilidown.service.BiliDownService
import cn.a10miaomiao.bilidown.ui.BiliDownScreen
import cn.a10miaomiao.bilidown.ui.components.DownloadDetailItem
import cn.a10miaomiao.bilidown.ui.components.DownloadListItem
import cn.a10miaomiao.bilidown.ui.components.FileNameInputDialog
import kotlinx.coroutines.flow.Flow
import java.io.File


data class DownloadDetailPageState(
    val detailInfo: DownloadInfo?,
)

sealed class DownloadDetailPageAction {
    class Export(
        val entryDirPath: String,
        val outFile: BiliDownOutFile,
    ): DownloadDetailPageAction()
}

@Composable
fun DownloadDetailPagePresenter(
    context: Context,
    packageName: String,
    dirPath: String,
    navController: NavHostController,
    action: Flow<DownloadDetailPageAction>,
): DownloadDetailPageState {
    var detailInfo by remember {
        mutableStateOf<DownloadInfo?>(null)
    }
    var path by remember {
        mutableStateOf("")
    }
    LaunchedEffect(packageName, dirPath) {
        val biliDownFile = BiliDownFile(context, packageName)
//        val list = mutableListOf<DownloadInfo>()
        val dirFile = if (dirPath.startsWith("content://")) {
            MiaoDocumentFile(
                context,
                DocumentFile.fromTreeUri(
                    context,
                    Uri.parse(dirPath)
                )!!
            )
        } else {
            MiaoJavaFile(File(dirPath))
        }
        val list = biliDownFile.readDownloadDirectory(dirFile)
        val items = mutableListOf<DownloadItemInfo>()
        var isCompleted = true
        list.forEach {
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
                itemTitle = ep.index + ep.index_title
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
            items.add(item)
            if (!item.is_completed) {
                isCompleted = false
            }
        }
        if (list.isEmpty()) {
            detailInfo = null
        } else {
            val biliEntry = list[0].entry
            val item = items[0]
            detailInfo = DownloadInfo(
                dir_path = list[0].pageDirPath,
                media_type = biliEntry.media_type,
                has_dash_audio = biliEntry.has_dash_audio,
                is_completed = isCompleted,
                total_bytes = biliEntry.total_bytes,
                downloaded_bytes = biliEntry.downloaded_bytes,
                title = biliEntry.title,
                cover = biliEntry.cover,
                cid = item.cid,
                id = item.id,
                type = item.type,
                items = items
            )
        }
    }
    action.collectAction {
        when (it) {
            is DownloadDetailPageAction.Export -> {
                val biliDownService = BiliDownService.getService(context)
                val isSuccess = biliDownService.exportBiliVideo(
                    it.entryDirPath,
                    it.outFile.file,
                )
                if (isSuccess) {
                    navController.navigate(BiliDownScreen.Progress.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }

        }
    }
    return DownloadDetailPageState(
        detailInfo,
    )
}

@Composable
fun DownloadDetailPage(
    navController: NavHostController,
    packageName: String,
    dirPath: String,
) {
    val context = LocalContext.current
    val (state, channel) = rememberPresenter(listOf(packageName, dirPath)) {
        DownloadDetailPagePresenter(context, packageName, dirPath, navController, it)
    }
    var selectedItem by remember {
        mutableStateOf<DownloadItemInfo?>(null)
    }

    FileNameInputDialog(
        showInputDialog = selectedItem != null,
        fileName = selectedItem?.title ?: "",
        onDismiss = {
            selectedItem = null
        },
        onConfirm = { outFile ->
            selectedItem?.let { item ->
                channel.trySend(DownloadDetailPageAction.Export(
                    entryDirPath = item.dir_path,
                    outFile = outFile,
                ))
            }
        }
    )

    LazyColumn {
        val detailInfo = state.detailInfo
        if (detailInfo == null) {

        } else {
            item {
                DownloadListItem(
                    item = detailInfo,
                    onClick = { },
                )
            }
            items(detailInfo.items, { it.cid }) {
                DownloadDetailItem(
                    item = it,
                    onClick = {
                    },
                    onStartClick = {
                    },
                    onPauseClick = {
                    },
                    onExportClick = {
                        selectedItem = it
                    }
                )
            }
        }
    }
}