package com.weather.app.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.weather.app.ui.components.getWeatherMenuBackgroundColor

/**
 * 严格对齐设计要求的右上角设置弹出菜单组件
 *
 * 1. 紧凑自适应超小宽度（110dp ~ 132dp，紧密贴合文字，无多余横向冗余）；
 * 2. 优化进出场缩放与淡入动效，彻底消除高度伸缩抖动与闪烁问题；
 * 3. 外层预留安全内边距，避免 Popup 窗口边界截断阴影产生黑色生硬边缘；
 * 4. 背景色动态取当前天气主色调融合深色半透明磨砂质感，结合精致微光边框与柔和阴影。
 *
 * @param expanded 菜单是否展开可见
 * @param weatherText 当前天气现象描述（用于动态驱动沉浸式磨砂背景色）
 * @param onDismissRequest 关闭菜单回调
 * @param onSelectSourceClick 点击“天气数据源”回调
 * @param onCardSettingsClick 点击“卡片显示设置”回调
 * @param onIntervalClick 点击“更新间隔”回调
 * @param onLocationSettingsClick 点击“定位设置”回调
 * @param onBackupRestoreClick 点击“备份与恢复”回调
 * @param onPrivacyClick 点击“隐私与免责声明”回调
 */
@Composable
fun WeatherSettingsMenu(
    expanded: Boolean,
    weatherText: String = "",
    onDismissRequest: () -> Unit,
    onSelectSourceClick: () -> Unit,
    onCardSettingsClick: () -> Unit = {},
    onIntervalClick: () -> Unit = {},
    onLocationSettingsClick: () -> Unit = {},
    onBackupRestoreClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {}
) {
    if (!expanded) return

    val menuBackgroundColor = remember(weatherText) {
        if (weatherText.isNotBlank()) {
            getWeatherMenuBackgroundColor(weatherText)
        } else {
            Color(0xF2182230)
        }
    }

    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(x = 0, y = 115), // 结合外层 padding 与顶部设置按钮保持最佳视觉间距
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

        // 采用右上角为原点的平滑缩放淡入动效，避免动态改变高度产生的高度闪烁与回弹
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(
                animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
            ) + scaleIn(
                initialScale = 0.85f,
                transformOrigin = TransformOrigin(1f, 0f),
                animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)
            ) + scaleOut(
                targetScale = 0.85f,
                transformOrigin = TransformOrigin(1f, 0f),
                animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing)
            )
        ) {
            // 外层添加 padding，为阴影提供自然的淡出扩散空间，防止被 Popup 窗口硬裁切产生黑边
            Box(
                modifier = Modifier.padding(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = menuBackgroundColor, // 动态天气沉浸式磨砂深色背景
                    shadowElevation = 8.dp,
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.16f)),
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
                            "天气数据源" to { onSelectSourceClick() },
                            "更新间隔" to { onIntervalClick() },
                            "定位设置" to { onLocationSettingsClick() },
                            "卡片显示" to { onCardSettingsClick() },
                            "备份与恢复" to { onBackupRestoreClick() },
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
}
