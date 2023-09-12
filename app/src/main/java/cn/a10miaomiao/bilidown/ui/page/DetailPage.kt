package cn.a10miaomiao.bilidown.ui.page

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController


@Composable
fun DetailPage(
    navController: NavHostController,
) {
    Column() {
        Text(text = "详情也")
        Button(onClick = { /*TODO*/ }) {
            Text(text = "测试跳转2")
        }
    }
}