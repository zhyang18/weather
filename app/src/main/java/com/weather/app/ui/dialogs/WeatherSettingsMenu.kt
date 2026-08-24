package com.weather.app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 严格对齐设计要求的右上角设置弹出菜单组件
 *
 * 采用 80% 半透明磨砂质感深灰卡片与大圆角容器（24.dp），仅保留“更新间隔”、“天气数据源”与“设置”三大核心菜单项。
 *
 * @param expanded 菜单是否展开可见
 * @param onDismissRequest 关闭菜单回调
 * @param onSelectSourceClick 点击“天气数据源”回调
 * @param onIntervalClick 点击“更新间隔”回调
 * @param onLocationSettingsClick 点击“定位设置”回调
 */
@Composable
fun WeatherSettingsMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onSelectSourceClick: () -> Unit,
    onIntervalClick: () -> Unit = {},
    onLocationSettingsClick: () -> Unit = {}
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .width(170.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xCC262C33)) // 80% 透明度深灰磨砂背景
            .padding(vertical = 4.dp)
    ) {
        val menuItems = listOf(
            "更新间隔" to { onIntervalClick() },
            "天气数据源" to { onSelectSourceClick() },
            "定位设置" to { onLocationSettingsClick() }
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            menuItems.forEachIndexed { index, (title, action) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismissRequest()
                            action()
                        }
                        .padding(horizontal = 20.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                if (index < menuItems.size - 1) {
                    Divider(
                        color = Color.White.copy(alpha = 0.12f),
                        thickness = 0.6.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}
