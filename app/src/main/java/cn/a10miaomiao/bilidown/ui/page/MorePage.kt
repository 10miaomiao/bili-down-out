package cn.a10miaomiao.bilidown.ui.page

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@Composable
fun MorePage(
    navController: NavHostController,
) {
    val context = LocalContext.current
    /*
    inverseOnSurface: Color,
    error: Color,
    onError: Color,
    errorContainer: Color,
    onErrorContainer: Color,
    outline: Color,
     */
    val colorList = listOf<Pair<String, Color>>(
        Pair("primary",  MaterialTheme.colorScheme.primary),
        Pair("onPrimary",  MaterialTheme.colorScheme.onPrimary),
        Pair("primaryContainer",  MaterialTheme.colorScheme.primaryContainer),
        Pair("onPrimaryContainer",  MaterialTheme.colorScheme.onPrimaryContainer),
        Pair("inversePrimary",  MaterialTheme.colorScheme.inversePrimary),
        Pair("secondary",  MaterialTheme.colorScheme.secondary),
        Pair("onSecondary",  MaterialTheme.colorScheme.onSecondary),
        Pair("secondaryContainer",  MaterialTheme.colorScheme.secondaryContainer),
        Pair("onSecondaryContainer",  MaterialTheme.colorScheme.onSecondaryContainer),
        Pair("tertiary",  MaterialTheme.colorScheme.tertiary),
        Pair("onTertiary",  MaterialTheme.colorScheme.onTertiary),
        Pair("tertiaryContainer",  MaterialTheme.colorScheme.tertiaryContainer),
        Pair("onTertiaryContainer",  MaterialTheme.colorScheme.onTertiaryContainer),
        Pair("background",  MaterialTheme.colorScheme.background),
        Pair("onBackground",  MaterialTheme.colorScheme.onBackground),
        Pair("surface",  MaterialTheme.colorScheme.surface),
        Pair("onSurface",  MaterialTheme.colorScheme.onSurface),
        Pair("surfaceVariant",  MaterialTheme.colorScheme.surfaceVariant),
        Pair("onSurfaceVariant",  MaterialTheme.colorScheme.onSurfaceVariant),
        Pair("inverseSurface",  MaterialTheme.colorScheme.inverseSurface),
        Pair("inverseOnSurface",  MaterialTheme.colorScheme.inverseOnSurface),
        Pair("error",  MaterialTheme.colorScheme.error),
        Pair("onError",  MaterialTheme.colorScheme.onError),
        Pair("errorContainer",  MaterialTheme.colorScheme.errorContainer),
        Pair("onErrorContainer",  MaterialTheme.colorScheme.onErrorContainer),
        Pair("outline",  MaterialTheme.colorScheme.outline),
    )
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(100.dp))
        Text(
            text = "哔哩缓存导出",
            fontSize = 36.sp,
        )
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "BiliDownOut",
        )
        Text(
            text = "v0.2 alpha",
        )
        Spacer(modifier = Modifier.height(10.dp))
        TextButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://10miaomiao.cn")
                context.startActivity(intent)
            },
        ) {
            Text(
                text = "by 10miaomiao.cn",

            )
        }
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "还未完善，尽情期待",
        )
        Text(
            text = "૮₍ ˃ ⤙ ˂ ₎ა",
        )
//        colorList.forEach {
//            Box(modifier = Modifier
//                .size(200.dp, 50.dp)
//                .background(it.second)) {
//                Text(text = it.first)
//            }
//        }
//        Text(text = "更多列表")
//        Text(text = "更多列表")
//        Text(text = "更多列表")
//        Button(onClick = { /*TODO*/ }) {
//            Text(text = "测试跳转1")
//        }
//        Button(onClick = { /*TODO*/ }) {
//            Text(text = "测试跳转2")
//        }
    }
}