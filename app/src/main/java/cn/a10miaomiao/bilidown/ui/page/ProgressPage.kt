package cn.a10miaomiao.bilidown.ui.page

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import cn.a10miaomiao.bilidown.common.UrlUtil
import cn.a10miaomiao.bilidown.common.molecule.collectAction
import cn.a10miaomiao.bilidown.common.molecule.rememberPresenter
import cn.a10miaomiao.bilidown.db.dao.OutRecord
import cn.a10miaomiao.bilidown.service.BiliDownService
import cn.a10miaomiao.bilidown.ui.components.OutFolderDialog
import cn.a10miaomiao.bilidown.ui.components.RecordItem
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File


data class ProgressPageState(
    val status: BiliDownService.Status,
    val recordList: List<OutRecord>,
) {
}



sealed class ProgressPageAction {
    object GetTaskList : ProgressPageAction()
    object OpenOutFolder : ProgressPageAction()
    class OpenVideo(
        val record: OutRecord
    ) : ProgressPageAction()
    class DeleteOutRecord(
        val record: OutRecord
    ) : ProgressPageAction()
}

@Composable
fun ProgressPagePresenter(
    context: Context,
    action: Flow<ProgressPageAction>,
): ProgressPageState {
    var status by remember {
        mutableStateOf<BiliDownService.Status>(BiliDownService.Status.InIdle)
    }
    val recordList = remember {
        mutableStateListOf<OutRecord>()
    }
    LaunchedEffect(context) {
        BiliDownService.status.collect {
            status = it
        }
    }
    action.collectAction {
        when (it) {
            is ProgressPageAction.OpenOutFolder -> {
            }
            is ProgressPageAction.GetTaskList -> {
                val biliDownService = BiliDownService.getService(context)
                recordList.clear()
                recordList.addAll(biliDownService.getTaskList())
            }
            is ProgressPageAction.OpenVideo -> {
                val videoFile = File(it.record.outFilePath)
                if (videoFile.exists()) {
                    val intent = Intent(Intent.ACTION_VIEW)
                    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                        FileProvider.getUriForFile(
                            context,
                            "cn.a10miaomiao.bilidown.fileprovider",
                            videoFile
                        )
                    } else {
                        Uri.fromFile(videoFile)
                    }
                    intent.setDataAndType(uri, "video/*")
                    context.startActivity(intent)
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "视频文件不存在", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
            is ProgressPageAction.DeleteOutRecord -> {
                val biliDownService = BiliDownService.getService(context)
                biliDownService.delTask(it.record)
                recordList.clear()
                recordList.addAll(biliDownService.getTaskList())
            }
        }
    }
    return ProgressPageState(
        status,
        recordList,
    )
}

@Composable
fun TaskProgress(
    status: BiliDownService.Status,
) {
    if (status is BiliDownService.Status.InIdle) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = cn.a10miaomiao.bilidown.R.drawable.ic_movie_pay_area_limit),
                contentDescription = "空空如也",
                modifier = Modifier.size(150.dp, 150.dp)
            )
            Text(
                modifier = Modifier.padding(vertical = 8.dp),
                text = "没有正在进行的任务",
            )
        }
    } else {
        Box(
            modifier = Modifier.padding(5.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Column() {
                    Row(
                        modifier = Modifier
                            .clickable(onClick = { })
                            .padding(10.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = UrlUtil.autoHttps(status.cover) + "@672w_378h_1c_",
                            contentDescription = status.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 120.dp, height = 80.dp)
                                .clip(RoundedCornerShape(5.dp))
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .padding(horizontal = 10.dp),
                        ) {
                            Text(
                                text = status.name,
                                maxLines = 2,
                                modifier = Modifier.weight(1f),
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (status is BiliDownService.Status.InProgress) {
                                Text(
                                    text = "导出中：${(status.progress * 100).toInt()}%",
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.outline,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            } else if (status is BiliDownService.Status.Copying) {
                                Text(
                                    text = "导出复制中",
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.outline,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            } else if (status is BiliDownService.Status.CopyingToTemp) {
                                Text(
                                    text = "正在复制文件到临时目录",
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.outline,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            } else if (status is BiliDownService.Status.Error) {
                                Text(
                                    text = "错误：" + status.message,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.error,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    LinearProgressIndicator(
                        progress = status.progress,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressPage(
    navController: NavHostController,
) {
    val context = LocalContext.current
    val (state, channel) = rememberPresenter {
        ProgressPagePresenter(context, it)
    }
    LaunchedEffect(
        channel,
        state.status is BiliDownService.Status.InIdle,
    ) {
        channel.send(ProgressPageAction.GetTaskList)
    }

    var showOutFolderDialog by remember {
        mutableStateOf(false)
    }
    OutFolderDialog(
        showOutFolderDialog = showOutFolderDialog,
        onDismiss = {
            showOutFolderDialog = false
        },
    )

    LazyColumn() {
        item("TaskProgress-head") {
            Text(
                text = "正在进行",
                modifier = Modifier.padding(10.dp)
            )
        }
        item("TaskProgress") {
            TaskProgress(status = state.status)
        }
        item("RecordList-head") {
            Text(
                text = "导出记录",
                modifier = Modifier.padding(10.dp)
            )
        }
        items(state.recordList, { it.id!! }) {
            RecordItem(
                title = it.title,
                cover = it.cover,
                status = it.status,
                onClick = {
                    channel.trySend(
                        ProgressPageAction.OpenVideo(it)
                    )
                },
                onDeleteClick = {
                    channel.trySend(
                        ProgressPageAction.DeleteOutRecord(it)
                    )
                }
            )
        }

        item("RecordList-foot") {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TextButton(
                    onClick = { showOutFolderDialog = true }
                ) {
                    Text(text = "导出文件夹在哪？")
                }
            }
        }
        
    }
}