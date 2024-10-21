package cn.a10miaomiao.bilidown.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.a10miaomiao.bilidown.common.UrlUtil
import cn.a10miaomiao.bilidown.entity.DownloadItemInfo
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun DownloadDetailItem(
    item: DownloadItemInfo,
    isOut: Boolean,
    onClick: () -> Unit,
    onStartClick: () -> Unit,
    onPauseClick: (taskId: Long) -> Unit,
    onExportClick: () -> Unit,
) {
    var expandedMoreMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.padding(5.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Column() {
                Row(
                    modifier = Modifier.padding(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = UrlUtil.autoHttps(item.cover) + "@672w_378h_1c_",
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 60.dp, height = 40.dp)
                            .clip(RoundedCornerShape(5.dp))
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 5.dp)
                    ) {
                        Text(
                            text = item.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val status = if (isOut) {
                            "已导出"
                        } else if (item.is_completed) {
                            "已完成下载"
                        } else {
                            "暂停中"
                        }
                        Text(
                            text = status,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Box() {
                        IconButton(
                            onClick = { expandedMoreMenu = true }
                        ) {
                            Icon(Icons.Filled.MoreVert, null)
                        }
                        DropdownMenu(
                            expanded = expandedMoreMenu,
                            onDismissRequest = { expandedMoreMenu = false },
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    expandedMoreMenu = false
                                    onExportClick()
                                },
                                text = {
                                    Text(text = "导出视频")
                                }
                            )
                        }
                    }

                }
            }

        }
    }
}