package cn.a10miaomiao.bilidown.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cn.a10miaomiao.bilidown.ui.components.miao.MiaoBottomNavigation
import cn.a10miaomiao.bilidown.ui.components.miao.MiaoBottomNavigationItem
import cn.a10miaomiao.bilidown.ui.page.*
import cn.a10miaomiao.bilidown.ui.theme.BiliDownTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiliDownApp(

) {
    BiliDownTheme {
        val navController = rememberNavController()
        val bottomNavList = remember {
            listOf(
                BiliDownScreen.List,
                BiliDownScreen.Progress,
                BiliDownScreen.More,
            )
        }
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val showBottomBar = currentDestination?.hierarchy?.any { i ->
            bottomNavList.indexOfFirst { j -> i.route == j.route } != -1
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val screenTitle = BiliDownScreen.getRouteName(currentDestination?.route)
                        Crossfade(
                            label = screenTitle,
                            targetState = screenTitle,
                        ) {
                            Text(it)
                        }
                    },
                    navigationIcon = {
                        AnimatedVisibility (showBottomBar == false) {
                            IconButton(
                                onClick = {
                                    navController.popBackStack()
                                }
                            ) {
                                Icon(Icons.Filled.ArrowBack, null)
                            }
                        }
                    },
                    actions = {
                        val isHome = currentDestination?.hierarchy?.any { i ->
                            i.route == BiliDownScreen.List.route
                        }
                        if (isHome == true) {
                            IconButton(
                                onClick = {
                                    navController.navigate(BiliDownScreen.AddApp.route)
                                }
                            ) {
                                Icon(Icons.Filled.Add, null)
                            }
                        }
                    }
                )
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar == true,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                    ),
                ){
//                    MiaoBottomNavigation(
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        bottomNavList.forEach { screen ->
//                            MiaoBottomNavigationItem(
//                                icon = screen.icon,
//                                label = screen.name,
//                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
//                                onClick = {
//                                    navController.navigate(screen.route) {
//                                        popUpTo(navController.graph.findStartDestination().id) {
//                                            saveState = true
//                                        }
//                                        launchSingleTop = true
//                                        restoreState = true
//                                    }
//                                },
//                            )
//                        }
//                    }
                    NavigationBar {
                        bottomNavList.forEach { screen ->
                            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                            val label = screen.name
                            NavigationBarItem(
                                selected = isSelected,
//                                alwaysShowLabel = labelVisibility == HomePageBottomBarLabelVisibility.ALWAYS_VISIBLE,
                                icon = {
//                                    Crossfade(
//                                        label = "home-bottom-bar",
//                                        targetState = isSelected,
//                                    ) {
                                    Icon(
                                        screen.icon,
                                        label,
                                    )
//                                    }
                                },
                                label = {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false,
                                    )
                                },
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController,
                startDestination = BiliDownScreen.List.route,
                Modifier.padding(innerPadding),
            ) {
                composable(BiliDownScreen.List.route) { ListPage(navController) }
                composable(BiliDownScreen.More.route) { MorePage(navController) }
                composable(BiliDownScreen.Progress.route) { ProgressPage(navController) }
                composable(
                    BiliDownScreen.Detail.route + "?packageName={packageName}&dirPath={dirPath}",
                    arguments = listOf(
                        navArgument("packageName") { type = NavType.StringType },
                        navArgument("dirPath") { type = NavType.StringType }
                    )
                ) {
                    val packageName = it.arguments?.getString("packageName") ?: ""
                    val dirPath = it.arguments?.getString("dirPath") ?: ""
                    DownloadDetailPage(navController, packageName, dirPath)
                }

                composable(BiliDownScreen.AddApp.route) { AddAppPage(navController) }
                composable(BiliDownScreen.About.route) { AboutPage(navController) }
            }
        }
    }
}