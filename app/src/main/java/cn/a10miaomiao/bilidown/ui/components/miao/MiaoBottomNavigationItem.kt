package cn.a10miaomiao.bilidown.ui.components.miao

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun RowScope.MiaoBottomNavigationItem(
//    icon: @Composable () -> Unit,
//    label: @Composable () -> Unit,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxHeight()
            .weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.clickable(onClick = onClick),
            shape = RoundedCornerShape(8.dp),
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Transparent
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(12.dp, 8.dp)
            ) {
                Icon(
                    icon,
                    modifier = Modifier.size(20.dp),
                    contentDescription = label,
                    tint = if (selected) {
                        Color.White
                    } else {
                        Color.LightGray
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) {
                        Color.White
                    } else {
                        Color.Gray
                    }
                )
            }
        }
    }
}