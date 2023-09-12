package cn.a10miaomiao.bilidown.ui.components.miao

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

//@Composable
//fun MiaoTabs(
//
//) {
//    LazyRow(
//        contentPadding = PaddingValues(start = 12.dp, end = 12.dp),
//        horizontalArrangement = Arrangement.spacedBy(36.dp)
//    ) {
//        itemsIndexed(HomeAreaViewModel.CATEGORY) { index, item ->
//            val focusRequester = remember { FocusRequester() }
//            val isFocused = remember { mutableStateOf(false) }
//
//            if (hasFocus() && cb() == index) {
//                SideEffect {
//                    focusRequester.requestFocus()
//                }
//            }
//            Column(
//                horizontalAlignment = Alignment.CenterHorizontally,
//                modifier = Modifier.onFocusChanged {
//                    isFocused.value = it.isFocused
//                    if (isFocused.value) {
//                        change(index)
//                    }
//                    if (it.isFocused) {
//                        println("home tab isFocused")
//                    }
//                }.focusRequester(focusRequester).focusProperties {
//                    if (index == 0) {
//                        left = otherRequester
//                    }
//                }.focusTarget().noRippleClickable {
//                    focusRequester.requestFocus()
//                },
//            ) {
//                Text(
//                    text = item.name.orEmpty(), style = MaterialTheme.typography.h3, color = if (isFocused.value) {
//                        ColorResource.acRed
//                    } else if (cb() == index) {
//                        ColorResource.acRed30
//                    } else {
//                        Color.Black
//                    }
//                )
//                Spacer(modifier = Modifier.height(3.dp))
//                Box(
//                    modifier = Modifier.height(3.dp).width(22.dp).background(
//                        if (isFocused.value) {
//                            ColorResource.acRed
//                        } else if (cb() == index) {
//                            ColorResource.acRed30
//                        } else {
//                            Color.Transparent
//                        }
//                    )
//                )
//            }
//        }
//    }
//}