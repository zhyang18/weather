package com.weather.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
 * 定位设置全屏界面组件
 *
 * 100% 精准对齐设计图：纯黑深色背景、左上角“< 定位设置”标题栏、圆角设置卡片及单选 Radio 切换。
 *
 * @param visible 是否展开显示
 * @param currentMode 当前生效的定位展示模式 [LocationDisplayMode]
 * @param onModeSelected 选中某一定位展示模式时的回调
 * @param onBackClick 点击顶部返回按钮或物理返回键时的回调
 */
@Composable
fun LocationSettingsScreen(
    visible: Boolean,
    currentMode: LocationDisplayMode,
    onModeSelected: (LocationDisplayMode) -> Unit,
    onBackClick: () -> Unit
) {
    // 拦截物理返回键与侧滑手势
    BackHandler(enabled = visible) {
        onBackClick()
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it })
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212)) // 深色背景
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // 顶部导航栏：< 定位设置
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBackIosNew,
                            contentDescription = "返回",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "定位设置",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 定位展示模式单选设置卡片 (圆角 16dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF262628))
                ) {
                    // 选项 1：展示附近地标/乡镇/街道
                    LocationSettingOptionRow(
                        title = LocationDisplayMode.LANDMARK.title,
                        subtitle = LocationDisplayMode.LANDMARK.example,
                        isSelected = currentMode == LocationDisplayMode.LANDMARK,
                        onClick = { onModeSelected(LocationDisplayMode.LANDMARK) }
                    )

                    // 分割线
                    Divider(
                        color = Color(0x1AFFFFFF),
                        thickness = 0.6.dp,
                        modifier = Modifier.padding(start = 16.dp)
                    )

                    // 选项 2：展示附近区县
                    LocationSettingOptionRow(
                        title = LocationDisplayMode.DISTRICT.title,
                        subtitle = LocationDisplayMode.DISTRICT.example,
                        isSelected = currentMode == LocationDisplayMode.DISTRICT,
                        onClick = { onModeSelected(LocationDisplayMode.DISTRICT) }
                    )
                }
            }
        }
    }
}

/**
 * 定位设置单选行条目
 *
 * @param title 标题文案
 * @param subtitle 副标题说明文案（如 "例：xx大厦"）
 * @param isSelected 是否处于选中状态
 * @param onClick 点击事件回调
 */
@Composable
private fun LocationSettingOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 单选框 (选中为蓝底白心，未选中为灰色圆圈)
        CustomRadioButton(isSelected = isSelected)
    }
}

/**
 * 自定义高颜值单选指示圆圈组件（100% 对齐设计图）
 *
 * @param isSelected 是否选中
 */
@Composable
private fun CustomRadioButton(
    isSelected: Boolean
) {
    val activeBlue = Color(0xFF2979FF)

    Box(
        modifier = Modifier
            .size(22.dp)
            .border(
                width = 2.dp,
                color = if (isSelected) activeBlue else Color.White.copy(alpha = 0.40f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(activeBlue)
            )
        }
    }
}
