package cn.a10miaomiao.bilidown.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cn.a10miaomiao.bilidown.common.scrollable.ScaffoldNestedScrollConnection
import cn.a10miaomiao.bilidown.common.scrollable.ScaffoldScrollableState
import cn.a10miaomiao.bilidown.ui.page.*
import cn.a10miaomiao.bilidown.ui.theme.BiliDownTheme
import cn.a10miaomiao.bilimiao.compose.animation.materialFadeThroughIn
import cn.a10miaomiao.bilimiao.compose.animation.materialFadeThroughOut

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainComposeApp() {
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
        val enableBottomBar = currentDestination?.hierarchy?.any { i ->
            bottomNavList.indexOfFirst { j -> i.route == j.route } != -1
        }
        val scrollableState = remember { ScaffoldScrollableState() }
        val scrollConnection = remember(scrollableState) {
            ScaffoldNestedScrollConnection(scrollableState)
        }
        LaunchedEffect(currentDestination) {
            if (enableBottomBar == true) {
                scrollableState.slideUp()
            }
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
                        AnimatedVisibility (enableBottomBar == false) {
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
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollConnection)
            ) {
                MainNavHost(
                    navController = navController,
                    innerPadding = innerPadding,
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                ) {
                    MainNavigationBar(
                        navController = navController,
                        currentDestination = currentDestination,
                        bottomNavList = bottomNavList,
                        showBottomBar = scrollableState.showBottomBar,
                    )
                }
            }
        }
    }
}

private fun NavGraphBuilder.defaultComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable(
        route,
        enterTransition = { materialFadeThroughIn(initialScale = 0.85f) },
        exitTransition = { materialFadeThroughOut() },
        popEnterTransition = {  materialFadeThroughIn(initialScale = 1.15f) },
        popExitTransition = { materialFadeThroughOut() },
        arguments = arguments,
        deepLinks = deepLinks,
        content = content,
    )
}

@Composable
fun MainNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
) {
    NavHost(
        navController,
        startDestination = BiliDownScreen.List.route,
        Modifier.padding(innerPadding),
    ) {
        defaultComposable(
            BiliDownScreen.List.route,
        ) { ListPage(navController) }
        defaultComposable(BiliDownScreen.More.route) { MorePage(navController) }
        defaultComposable(BiliDownScreen.Progress.route) { ProgressPage(navController) }
        defaultComposable(
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

        defaultComposable(
            BiliDownScreen.AddApp.route,
        ) { AddAppPage(navController) }
        defaultComposable(BiliDownScreen.About.route) { AboutPage(navController) }
    }
}

@Composable
fun MainNavigationBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
    bottomNavList: List<BiliDownScreen>,
    showBottomBar: Boolean,
) {
    AnimatedVisibility(
        visible = showBottomBar,
        enter = slideInVertically(
            initialOffsetY = { it },
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
        ),
    ){
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
}