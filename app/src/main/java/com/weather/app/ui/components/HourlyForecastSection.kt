package com.weather.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.model.HourlyForecast

/**
 * 24小时逐小时实况走势卡片组件
 *
 * 以横向平滑滚动视图呈现过去与近期的气温变化、湿度与降雨指标。
 *
 * @param hourlyList 逐小时天气数据列表 [HourlyForecast]
 * @param modifier 外部修饰符
 */
@Composable
fun HourlyForecastSection(
    hourlyList: List<HourlyForecast>,
    modifier: Modifier = Modifier
) {
    if (hourlyList.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(vertical = 12.dp)
    ) {
        // 卡片标题
        Text(
            text = "24小时逐时观测走势",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 横向滚动小时气温列表
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(hourlyList) { item ->
                HourlyItemView(item = item)
            }
        }
    }
}

/**
 * 单个时间点的观测单元视图
 *
 * @param item 逐小时观测数据项 [HourlyForecast]
 */
@Composable
private fun HourlyItemView(item: HourlyForecast) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(52.dp)
    ) {
        Text(
            text = item.getDisplayHour(),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "${item.temperature.toInt()}°",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "${item.humidity.toInt()}%",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp
        )

        if (item.rain > 0.0) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${item.rain}mm",
                color = Color(0xFF64B5F6),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
