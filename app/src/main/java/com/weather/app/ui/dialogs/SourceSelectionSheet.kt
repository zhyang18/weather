package com.weather.app.ui.dialogs

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
import com.weather.app.model.WeatherSourceInfo

/**
 * 手动选择天气数据源底部面板组件
 *
 * 采用与城市管理页面一致的 80% 半透明磨砂深灰蓝底色，
 * 展示应用支持的所有天气数据源提供商列表，允许用户实时手动切换生效的天气源。
 *
 * @param availableSources 系统支持的所有天气数据源元数据列表 [WeatherSourceInfo]
 * @param currentSourceId 当前正在生效的天气数据源唯一标识符
 * @param onSelectSource 用户选择目标天气源时的回调函数
 * @param onDismiss 关闭底部面板时的回调函数
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSelectionSheet(
    availableSources: List<WeatherSourceInfo>,
    currentSourceId: String,
    onSelectSource: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            // 弹窗头部标题
            Text(
                text = "选择天气数据源",
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "系统采用多源架构设计，可按需切换不同气象服务提供商",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 数据源列表
            availableSources.forEach { source ->
                val isSelected = source.id == currentSourceId

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
                        .clickable(enabled = source.isAvailable) {
                            onSelectSource(source.id)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = source.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = if (source.isAvailable) Color.White else Color.White.copy(alpha = 0.4f)
                                )

                                if (source.isDefault) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF2563EB))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "官方默认",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                } else if (!source.isAvailable) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Gray.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "扩展中",
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = source.description,
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
