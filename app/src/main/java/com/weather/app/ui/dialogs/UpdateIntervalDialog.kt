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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 自动更新间隔项数据结构
 *
 * @property minutes 间隔分钟数（0 为无/关闭）
 * @property label 展示在界面上的文字描述（如“无”、“30 分钟”、“1 小时”）
 * @property description 模式的详细功能说明
 */
data class UpdateIntervalOption(
    val minutes: Int,
    val label: String,
    val description: String
)

/**
 * 更新间隔设置底部面板组件
 *
 * 采用与城市管理页面一致的 80% 半透明磨砂深灰蓝底色，
 * 整体高度严格限制在屏幕 60% 以内，精简行高并支持滚动浏览。
 *
 * @param currentIntervalMinutes 当前生效的更新间隔分钟数（0 为无，30、60、120、360、720、1440）
 * @param onSelectInterval 用户点击选择新间隔时的回调函数
 * @param onDismiss 关闭底部面板时的回调函数
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateIntervalDialog(
    currentIntervalMinutes: Int,
    onSelectInterval: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.60f

    val options = listOf(
        UpdateIntervalOption(0, "无", "完全手动刷新，不消耗任何后台电量"),
        UpdateIntervalOption(30, "30 分钟", "高频更新，适合密切关注天气变化"),
        UpdateIntervalOption(60, "1 小时", "平衡模式，兼顾实时性与省电"),
        UpdateIntervalOption(120, "2 小时", "推荐模式，省电与实况均衡"),
        UpdateIntervalOption(360, "6 小时", "低频模式，适合平稳天气"),
        UpdateIntervalOption(720, "12 小时", "半天更新一次，超长省电"),
        UpdateIntervalOption(1440, "24 小时", "每天自动更新一次")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xF2182230), // 95% 磨砂深灰蓝底色
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
                .heightIn(max = maxSheetHeight)
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 2.dp)
        ) {
            // 1. 弹窗头部标题与副标题
            Text(
                text = "更新间隔",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "设置后台自动静默刷新频率，系统将按设定间隔智能唤醒",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. 紧凑精简的间隔选项列表（支持滑动）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState)
            ) {
                options.forEach { option ->
                    val isSelected = option.minutes == currentIntervalMinutes

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0x402563EB) else Color(0x18FFFFFF),
                        border = BorderStroke(
                            0.6.dp,
                            if (isSelected) Color(0xFF60A5FA).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.08f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.5.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onSelectInterval(option.minutes)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 7.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = option.label,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                        color = Color.White
                                    )

                                    if (option.minutes == 120) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF2563EB))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "推荐",
                                                color = Color.White,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(1.dp))

                                Text(
                                    text = option.description,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.60f)
                                )
                            }

                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2563EB)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "当前选中",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
