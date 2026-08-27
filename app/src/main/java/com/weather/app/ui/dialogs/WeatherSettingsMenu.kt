package com.weather.app.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * 严格对齐设计要求的右上角设置弹出菜单组件
 *
 * 1. 紧凑自适应超小宽度（106dp ~ 126dp，紧密贴合文字，无多余横向冗余）；
 * 2. 与右上角设置按钮保持舒适的纵向间距；
 * 3. 支持从上到下顺滑垂直展开与滑下的进出场动效；
 * 4. 85% 半透明磨砂深灰卡片与圆润圆角容器（16.dp）。
 *
 * @param expanded 菜单是否展开可见
 * @param onDismissRequest 关闭菜单回调
 * @param onSelectSourceClick 点击“天气数据源”回调
 * @param onIntervalClick 点击“更新间隔”回调
 * @param onLocationSettingsClick 点击“定位设置”回调
 * @param onPrivacyClick 点击“隐私与免责声明”回调
 */
@Composable
fun WeatherSettingsMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onSelectSourceClick: () -> Unit,
    onIntervalClick: () -> Unit = {},
    onLocationSettingsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {}
) {
    if (!expanded) return

    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(x = 0, y = 135), // 与顶部设置按钮拉开舒适纵向间距
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(
            focusable = true,
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        )
    ) {
        var isVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            isVisible = true
        }

        // 从上到下垂直展开/滑下的流畅进出场动效
        AnimatedVisibility(
            visible = isVisible,
            enter = expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + slideInVertically(
                initialOffsetY = { -it / 3 },
                animationSpec = tween(180)
            ) + fadeIn(animationSpec = tween(180)),
            exit = shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = tween(130)
            ) + slideOutVertically(
                targetOffsetY = { -it / 3 },
                animationSpec = tween(130)
            ) + fadeOut(animationSpec = tween(130))
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xF2182230), // 95% 磨砂深灰蓝背景
                shadowElevation = 14.dp,
                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier
                    .wrapContentWidth()
                    .widthIn(min = 110.dp, max = 132.dp)
            ) {
                Column(
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(vertical = 3.dp)
                ) {
                    val menuItems = listOf(
                        "更新间隔" to { onIntervalClick() },
                        "天气数据源" to { onSelectSourceClick() },
                        "定位设置" to { onLocationSettingsClick() },
                        "隐私与免责" to { onPrivacyClick() }
                    )

                    menuItems.forEachIndexed { index, (title, action) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismissRequest()
                                    action()
                                }
                                .padding(horizontal = 13.dp, vertical = 9.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1
                            )
                        }

                        if (index < menuItems.size - 1) {
                            Divider(
                                color = Color.White.copy(alpha = 0.10f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
