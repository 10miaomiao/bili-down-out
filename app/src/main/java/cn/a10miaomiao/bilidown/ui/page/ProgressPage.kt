package cn.a10miaomiao.bilidown.ui.page

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavHostController
import cn.a10miaomiao.bilidown.common.BiliDownOutFile
import cn.a10miaomiao.bilidown.common.UrlUtil
import cn.a10miaomiao.bilidown.common.molecule.collectAction
import cn.a10miaomiao.bilidown.common.molecule.rememberPresenter
import cn.a10miaomiao.bilidown.service.BiliDownService
import cn.a10miaomiao.bilidown.ui.components.OutFolderDialog
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.Flow


data class ProgressPageState(
    val status: BiliDownService.Status,
) {
}



sealed class ProgressPageAction {
    object OpenOutFolder : ProgressPageAction()
}

@Composable
fun ProgressPagePresenter(
    context: Context,
    action: Flow<ProgressPageAction>,
): ProgressPageState {
    var status by remember {
        mutableStateOf<BiliDownService.Status>(BiliDownService.Status.InIdle)
    }
    LaunchedEffect(context) {
        val biliDownService = BiliDownService.getService(context)
        biliDownService.status.collect {
            status = it
        }
    }
    action.collectAction {
        when (it) {
            is ProgressPageAction.OpenOutFolder -> {
            }
        }
    }
    return ProgressPageState(
        status
    )
}

@Composable
fun ProgressPage(
    navController: NavHostController,
) {
    val context = LocalContext.current
    val (state, channel) = rememberPresenter {
        ProgressPagePresenter(context, it)
    }
    var showOutFolderDialog by remember {
        mutableStateOf(false)
    }
    val status = state.status
    OutFolderDialog(
        showOutFolderDialog = showOutFolderDialog,
        onDismiss = {
            showOutFolderDialog = false
        },
    )
    if (status is BiliDownService.Status.InIdle) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = cn.a10miaomiao.bilidown.R.drawable.ic_movie_pay_area_limit),
                contentDescription = "空空如也",
                modifier = Modifier.size(200.dp, 200.dp)
            )
            Text(
                modifier = Modifier.padding(vertical = 8.dp),
                text = "没有正在进行的任务",
            )
            TextButton(
                onClick = { showOutFolderDialog = true }
            ) {
                Text(text = "导出文件夹在哪？")
            }
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