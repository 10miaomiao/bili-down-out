package cn.a10miaomiao.bilidown.ui.page

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import cn.a10miaomiao.bilidown.common.BiliDownUtils
import cn.a10miaomiao.bilidown.common.datastore.DataStoreKeys
import cn.a10miaomiao.bilidown.common.datastore.rememberDataStorePreferencesFlow
import cn.a10miaomiao.bilidown.common.molecule.collectAction
import cn.a10miaomiao.bilidown.common.molecule.rememberNestedPresenter
import cn.a10miaomiao.bilidown.common.molecule.rememberPresenter
import cn.a10miaomiao.bilidown.entity.BiliAppInfo
import cn.a10miaomiao.bilidown.ui.BiliDownScreen
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.pagerTabIndicatorOffset
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch


data class ListPageState(
    val appList: List<BiliAppInfo>,
)

sealed class ListPageAction {
    object Add : ListPageAction()
}

@Composable
fun ListPagePresenter(
    context: Context,
    action: Flow<ListPageAction>,
): ListPageState {
    var appList by remember { mutableStateOf<List<BiliAppInfo>>(emptyList()) }
    val selectedAppPackageNameSet by rememberDataStorePreferencesFlow(
        context = context,
        key = DataStoreKeys.appPackageNameSet,
        initial = emptySet(),
    ).collectAsState(emptySet())
    LaunchedEffect(selectedAppPackageNameSet) {
        appList = BiliDownUtils.biliAppList.filter {
            selectedAppPackageNameSet.contains(it.packageName)
        }
    }
    action.collectAction {
        when (it) {
            ListPageAction.Add -> {
//                appList.add("列表")
            }
        }
    }
    return ListPageState(
        appList,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalPagerApi::class)
@Composable
fun ListPage(
    navController: NavHostController,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (state, channel) = rememberPresenter {
        ListPagePresenter(context, it)
    }
    if (state.appList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 空布局
                Text(text = "还未添加APP信息")
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        navController.navigate(BiliDownScreen.AddApp.route)
                    }
                ) {
                    Text(text = "添加")
                }
            }
        }
    } else {
        val pagerState = rememberPagerState(pageCount = { state.appList.size })
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                TabRow(
                    modifier = Modifier.weight(1f),
                    backgroundColor = Color.White,
                    contentColor = Color.Black,
                    selectedTabIndex = pagerState.currentPage,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
                        )
                    }
                ) {
                    // Add tabs for all of our pages
                    state.appList.forEachIndexed { index, app ->
                        Tab(
                            text = { Text(text = app.name) },
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                        )
                    }
                }
                Button(
                    onClick = {
                        navController.navigate(BiliDownScreen.AddApp.route)
                    }
                ) {
                    Text(text = "管理")
                }
            }
            HorizontalPager(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                state = pagerState,
            ) { page ->
                DownloadListPage(
                    navController = navController,
                    packageName = state.appList[page].packageName,
                )
            }
        }
    }

}