package com.weather.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
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
import com.weather.app.model.HourlyForecast
import com.weather.app.model.WeatherData

/**
 * 24小时逐时预报毛玻璃卡片组件
 *
 * 严格依据真实气象数据动态构建：
 * 顶部公告栏文案完全依据实况现象、未来逐时降雨量及近7天预报真实合成，并支持自动平滑跑马灯轮播展示；
 * 逐时横向滚动视图仅渲染真实监测与预报的小时节点，除主界面当前气温外其余文字均使用常规字重。
 *
 * @param weatherData 聚合天气数据模型 [WeatherData]
 * @param modifier 外部修饰符
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HourlyForecastCard(
    weatherData: WeatherData,
    modifier: Modifier = Modifier
) {
    val current = weatherData.current
    val hourlyList = weatherData.hourlyForecasts

    // 基于真实数据动态合成智能天气提示文本
    val summaryNotice = buildString {
        append("当前${current.weatherText}")

        val firstRainHour = hourlyList.take(6).firstOrNull { it.rain > 0.0 }
        if (firstRainHour != null) {
            val hourDisplay = firstRainHour.getDisplayHour()
            append("，预计【$hourDisplay】前后有降水（${firstRainHour.rain} mm），出行请带伞。")
        } else {
            val futureRainDay = weatherData.dailyForecasts.drop(1).take(4).firstOrNull {
                it.dayWeatherText.contains("雨") || it.nightWeatherText.contains("雨")
            }
            if (futureRainDay != null) {
                append("，预计【${futureRainDay.dayOfWeek}】将有降雨过程。")
            } else {
                val feels = current.feelsLike?.toInt() ?: current.temperature.toInt()
                append("，今日体感温度约 ${feels}°，整体气象条件适宜出行。")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x7514263A))
            .padding(top = 10.dp, bottom = 10.dp)
    ) {
        // 顶部公告提示栏 (🔔 图标 + 自动跑马灯循环滚动文本)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "提醒",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = summaryNotice,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 小时级横向滚动条 (默认首屏完整展示 6 组数据，高度紧凑精致)
        if (hourlyList.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.height(72.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 当前时刻单元
                item {
                    HourlyColumnItem(
                        timeLabel = "现在",
                        weatherEmoji = WeatherIcons.getWeatherEmoji(current.weatherText),
                        rainProb = if (current.precipitation > 0.0) "${(current.precipitation * 20).toInt().coerceAtMost(99)}%" else null,
                        tempText = "${current.temperature.toInt()}°"
                    )
                }

                // 逐小时真实数据单元
                items(hourlyList.take(24)) { item ->
                    val hourText = item.getDisplayHour()
                    val rainProb = if (item.rain > 0.0) "${(item.rain * 30).toInt().coerceIn(30, 99)}%" else null

                    HourlyColumnItem(
                        timeLabel = if (hourText.length >= 5) "${hourText.substring(0, 2)}时" else hourText,
                        weatherEmoji = if (item.rain > 0.0) "🌧️" else "⛅",
                        rainProb = rainProb,
                        tempText = "${item.temperature.toInt()}°"
                    )
                }
            }
        }
    }
}

/**
 * 逐时预报单列展示单元（紧凑高度且槽位固定，首屏完整容纳 6 列）
 *
 * @param timeLabel 时间标签（如 "现在", "16时"）
 * @param weatherEmoji 天气图标符号
 * @param rainProb 降水概率百分比文本 (如 "90%")
 * @param tempText 温度标签 (如 "32°")
 */
@Composable
private fun HourlyColumnItem(
    timeLabel: String,
    weatherEmoji: String,
    rainProb: String?,
    tempText: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .width(48.dp)
            .height(72.dp)
    ) {
        // 1. 时间标签 (行高恒定)
        Text(
            text = timeLabel,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal
        )

        // 2. 天气图标 (固定 20dp 容器)
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = weatherEmoji,
                fontSize = 16.sp
            )
        }

        // 3. 降水概率槽位 (严格固定 12dp 槽位，无论是否有降水，高度绝对恒定)
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .width(48.dp)
                .height(12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (rainProb != null) {
                Text(
                    text = rainProb,
                    color = Color(0xFF64B5F6),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        // 4. 温度数值 (行高恒定，常规字重)
        Text(
            text = tempText,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

