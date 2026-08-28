package com.weather.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.model.WeatherAlert

import androidx.compose.ui.graphics.graphicsLayer

/**
 * 官方气象灾害预警卡片组件
 *
 * 严格对齐设计稿：顶部预警图标 ⚠️ 与预警标题（如“高温预警”、“雷雨大风预警”），
 * 中间为气象台发布的官方预警正文详情，底部注明发布机构与具体发布时刻。
 * 遵循全局字重规范，文字使用常规字重（不加粗）。
 *
 * @param alert 气象预警数据实体 [WeatherAlert]
 * @param modifier 外部修饰符
 */
@Composable
fun WeatherAlertCard(
    alert: WeatherAlert,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .graphicsLayer {
                clip = true
                shape = RoundedCornerShape(20.dp)
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x7514263A))
            .padding(18.dp)
    ) {
        // 顶部预警标题栏 (⚠️ 图标 + 预警名称，常规字重)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "气象预警",
                tint = Color(0xFFFFD54F),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = alert.title,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 预警详细正文
        Text(
            text = alert.content,
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 13.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 底部发布来源与发布时间
        Text(
            text = "${alert.publisher} ${alert.publishTime}",
            color = Color.White.copy(alpha = 0.60f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

