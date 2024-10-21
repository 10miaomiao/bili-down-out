package cn.a10miaomiao.bilidown.ui.page

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import cn.a10miaomiao.bilidown.BiliDownApp
import cn.a10miaomiao.bilidown.common.BiliDownOutFile
import cn.a10miaomiao.bilidown.common.UrlUtil
import cn.a10miaomiao.bilidown.common.molecule.collectAction
import cn.a10miaomiao.bilidown.common.molecule.rememberPresenter
import cn.a10miaomiao.bilidown.db.dao.OutRecord
import cn.a10miaomiao.bilidown.service.BiliDownService
import cn.a10miaomiao.bilidown.state.TaskStatus
import cn.a10miaomiao.bilidown.ui.components.OutFolderDialog
import cn.a10miaomiao.bilidown.ui.components.RecordItem
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File


data class ProgressPageState(
    val status: TaskStatus,
    val recordList: List<OutRecord>,
) {
}


sealed class ProgressPageAction {
    object GetTaskList : ProgressPageAction()
    object OpenOutFolder : ProgressPageAction()
    class StartTask(
        val record: OutRecord
    ) : ProgressPageAction()

    class RemoveTask(
        val record: OutRecord,
    ) : ProgressPageAction()
}

@Composable
fun ProgressPagePresenter(
    context: Context,
    action: Flow<ProgressPageAction>,
): ProgressPageState {
    val appState = remember(context) {
        (context.applicationContext as BiliDownApp).state
    }
    val taskStatus by appState.taskStatus.collectAsState()

    var recordList by remember {
        mutableStateOf(emptyList<OutRecord>())
    }

    action.collectAction {
        when (it) {
            is ProgressPageAction.OpenOutFolder -> {
            }

            is ProgressPageAction.GetTaskList -> {
                val biliDownService = BiliDownService.getService(context)
                recordList = biliDownService.getRecordList(OutRecord.STATUS_WAIT)
                withContext(Dispatchers.IO) {
                    recordList = recordList.map { record ->
                        if (record.status == 1) {
                            val exists = File(record.outFilePath).exists()
                            record.copy(
                                status = if (exists) 1 else -1,
                            )
                        } else {
                            record
                        }
                    }
                }
            }

            is ProgressPageAction.StartTask -> {
                val biliDownService = BiliDownService.getService(context)
                biliDownService.startTask(it.record)
            }
            is ProgressPageAction.RemoveTask -> {
                val biliDownService = BiliDownService.getService(context)
                biliDownService.delTask(it.record, false)
                recordList = biliDownService.getRecordList(OutRecord.STATUS_WAIT)
            }
        }
    }
    return ProgressPageState(
        taskStatus,
        recordList,
    )
}

@Composable
fun TaskProgress(
    status: TaskStatus,
) {
    if (status is TaskStatus.InIdle) {
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
                            if (status is TaskStatus.InProgress) {
                                Text(
                                    text = "导出中：${(status.progress * 100).toInt()}%",
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.outline,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            } else if (status is TaskStatus.Copying) {
                                Text(
                                    text = "导出复制中",
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.outline,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            } else if (status is TaskStatus.CopyingToTemp) {
                                Text(
                                    text = "正在复制文件到临时目录",
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.outline,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            } else if (status is TaskStatus.Error) {
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
internal fun ReconfirmRemoveDialog(
    channel: Channel<ProgressPageAction>,
    action: ProgressPageAction.RemoveTask?,
    onDismiss: () -> Unit,
) {
    if (action != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = "确认移除该条任务？")
            },
            text = {
                Text("移除：" + action.record.title)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        channel.trySend(action)
                        onDismiss()
                    },
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                ) {
                    Text("取消")
                }
            }
        )
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
        state.status,
    ) {
        channel.send(ProgressPageAction.GetTaskList)
    }

    var reconfirmRemoveDialogAction by remember {
        mutableStateOf<ProgressPageAction.RemoveTask?>(null)
    }
    ReconfirmRemoveDialog(
        channel = channel,
        action = reconfirmRemoveDialogAction,
        onDismiss = {
            reconfirmRemoveDialogAction = null
        },
    )

    LazyColumn(
        contentPadding = PaddingValues(bottom = 80.dp),
    ) {
        item("TaskProgress-head") {
            Text(
                text = "正在进行",
                modifier = Modifier.padding(10.dp)
            )
        }
        item("TaskProgress") {
            TaskProgress(status = state.status)
        }
        if (state.recordList.isNotEmpty()) {
            item("RecordList-head") {
                Text(
                    text = "导出队列",
                    modifier = Modifier.padding(10.dp)
                )
            }

            items(state.recordList, { it.id!! }) { item ->
                RecordItem(
                    title = item.title,
                    cover = item.cover,
                    status = item.status,
                    onClick = {
                        channel.trySend(
                            ProgressPageAction.StartTask(item)
                        )
                    },
                    onDeleteClick = {
                        reconfirmRemoveDialogAction = ProgressPageAction.RemoveTask(
                            record = item
                        )
                    }
                )
            }
        }

    }
}