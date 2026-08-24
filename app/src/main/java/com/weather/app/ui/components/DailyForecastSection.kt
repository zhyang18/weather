package com.weather.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.weather.app.model.DailyForecast

/**
 * 7日天气预报列表卡片组件
 *
 * 以结构化列表展示未来 7 天的每日天气现象、温差走势与风力概况。
 *
 * @param dailyList 每日天气预报列表 [DailyForecast]
 * @param modifier 外部修饰符
 */
@Composable
fun DailyForecastSection(
    dailyList: List<DailyForecast>,
    modifier: Modifier = Modifier
) {
    if (dailyList.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(16.dp)
    ) {
        // 卡片标题
        Text(
            text = "7天天气预报",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 计算这几天的全局最高温与最低温以绘制温差比例条
        val globalMin = dailyList.minOfOrNull { it.minTemperature } ?: 0.0
        val globalMax = dailyList.maxOfOrNull { it.maxTemperature } ?: 40.0
        val tempSpan = (globalMax - globalMin).coerceAtLeast(1.0)

        // 渲染每一天的预报行
        dailyList.forEachIndexed { index, forecast ->
            DailyForecastRow(
                forecast = forecast,
                globalMin = globalMin,
                tempSpan = tempSpan
            )
            if (index < dailyList.size - 1) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/**
 * 单日预报行视图
 *
 * @param forecast 单日预报数据项 [DailyForecast]
 * @param globalMin 全局最低气温基准
 * @param tempSpan 全局温差跨度
 */
@Composable
private fun DailyForecastRow(
    forecast: DailyForecast,
    globalMin: Double,
    tempSpan: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 星期与天气
        Row(
            modifier = Modifier.width(130.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = forecast.dayOfWeek,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(42.dp)
            )
            Text(
                text = WeatherIcons.getWeatherEmoji(forecast.dayWeatherText),
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = forecast.dayWeatherText,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp
            )
        }

        // 最低温
        Text(
            text = "${forecast.minTemperature.toInt()}°",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            modifier = Modifier.width(30.dp)
        )

        // 温差渐变条
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.2f))
        ) {
            val startRatio = ((forecast.minTemperature - globalMin) / tempSpan).toFloat().coerceIn(0f, 1f)
            val endRatio = ((forecast.maxTemperature - globalMin) / tempSpan).toFloat().coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (startRatio * 100).dp, end = ((1f - endRatio) * 100).coerceAtLeast(0f).dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFFFB74D))
            )
        }

        // 最高温
        Text(
            text = "${forecast.maxTemperature.toInt()}°",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(30.dp)
        )
    }
}
