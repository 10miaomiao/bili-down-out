package cn.a10miaomiao.bilidown.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.a10miaomiao.bilidown.common.UrlUtil
import cn.a10miaomiao.bilidown.entity.DownloadInfo
import cn.a10miaomiao.bilidown.entity.DownloadType
import coil.compose.AsyncImage

@Composable
fun DownloadListItem(
    item: DownloadInfo,
    onClick: () -> Unit
) {
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
                        .clickable(onClick = onClick)
                        .padding(10.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = UrlUtil.autoHttps(item.cover) + "@672w_378h_1c_",
                        contentDescription = item.title,
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
                            text = item.title,
                            maxLines = 2,
                            modifier = Modifier.weight(1f),
                            overflow = TextOverflow.Ellipsis,
                        )
                        val status = if (item.is_completed) {
                            "已完成下载"
                        } else {
                            "暂停中"
                        }
                        Text(
                            text = "${item.items.size}个视频 • $status",
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.outline,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun DownloadListItemPreview() {
    DownloadListItem(
        DownloadInfo("", 1,
            has_dash_audio = true,
            is_completed = true,
            total_bytes = 0L,
            downloaded_bytes = 0L,
            title = "标题",
            cover = "",
            id = 0L,
            cid = 0L,
            type = DownloadType.VIDEO,
            items = mutableListOf()
        ),
        {}
    )
}