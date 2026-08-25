package com.weather.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.model.LocationDisplayMode

/**
 * 定位设置底部面板组件
 *
 * 采用与城市管理页面一致的 80% 半透明磨砂深灰蓝底色，
 * 原生支持全屏沉浸式状态栏与底部安全区，提供平滑自底而上的手势交互与视觉体验。
 *
 * @param currentMode 当前生效的定位展示模式 [LocationDisplayMode]
 * @param onModeSelected 选中某一定位展示模式时的回调
 * @param onDismiss 关闭底部面板时的回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSettingsScreen(
    currentMode: LocationDisplayMode,
    onModeSelected: (LocationDisplayMode) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val options = listOf(
        LocationDisplayMode.LANDMARK,
        LocationDisplayMode.DISTRICT
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xE6182230), // 90% 不透明磨砂深灰蓝底色
        scrimColor = Color.Transparent,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color.White.copy(alpha = 0.35f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            // 1. 弹窗头部标题与副标题
            Text(
                text = "定位设置",
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "选择主页定位城市名称的展示级别与颗粒度",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. 模式选项列表
            options.forEach { mode ->
                val isSelected = mode == currentMode

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0x402563EB) else Color(0x18FFFFFF),
                    border = BorderStroke(
                        0.6.dp,
                        if (isSelected) Color(0xFF60A5FA).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onModeSelected(mode)
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.title,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = mode.example,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }

                        if (isSelected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2563EB)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "当前选中",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
