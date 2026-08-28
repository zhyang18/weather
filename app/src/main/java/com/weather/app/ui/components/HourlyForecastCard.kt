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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
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
 * 逐时预报轻量展示实体数据类
 *
 * @property key 唯一标识键
 * @property timeLabel 顶部时间文本（如 "现在", "16时"）
 * @property weatherText 天气现象描述
 * @property rainProb 降水概率文本（如 "90%"），若无降水则为 null
 * @property tempText 底部温度数值（如 "32°"）
 */
@Immutable
private data class HourlyDisplayItem(
    val key: String,
    val timeLabel: String,
    val weatherText: String,
    val rainProb: String?,
    val tempText: String
)

/**
 * 24小时逐时预报卡片组件 (极简原生实现)
 *
 * 采用原生标准 LazyRow 构建，结构极致扁平，零冗余图层，保障主页面上下滑动满帧流畅。
 *
 * @param weatherData 聚合天气数据模型 [WeatherData]
 * @param modifier 外部修饰符
 */
@Composable
fun HourlyForecastCard(
    weatherData: WeatherData,
    modifier: Modifier = Modifier
) {
    val current = weatherData.current
    val hourlyList = weatherData.hourlyForecasts

    // 顶部天气提示文案
    val summaryNotice = remember(current.weatherText, current.feelsLike, current.temperature, hourlyList, weatherData.dailyForecasts) {
        val firstRainHour = hourlyList.take(6).firstOrNull { it.rain > 0.0 }
        if (firstRainHour != null) {
            val hourDisplay = firstRainHour.getDisplayHour()
            "当前${current.weatherText}，预计 $hourDisplay 有降水（${firstRainHour.rain}mm）"
        } else {
            val futureRainDay = weatherData.dailyForecasts.drop(1).take(4).firstOrNull {
                it.dayWeatherText.contains("雨") || it.nightWeatherText.contains("雨")
            }
            if (futureRainDay != null) {
                "当前${current.weatherText}，预计${futureRainDay.dayOfWeek}有降雨"
            } else {
                val feels = current.feelsLike?.toInt() ?: current.temperature.toInt()
                "当前${current.weatherText}，体感 ${feels}°，适宜出行"
            }
        }
    }

    // 预计算逐时展示项
    val displayItems = remember(current, hourlyList) {
        val list = ArrayList<HourlyDisplayItem>(25)

        // 1. 当前时刻
        val currentRainProb = if (current.precipitation > 0.0) {
            "${(current.precipitation * 20).toInt().coerceAtMost(99)}%"
        } else null

        list.add(
            HourlyDisplayItem(
                key = "now",
                timeLabel = "现在",
                weatherText = current.weatherText,
                rainProb = currentRainProb,
                tempText = "${current.temperature.toInt()}°"
            )
        )

        // 2. 逐小时预报 (前 24 个点)
        val takeCount = hourlyList.size.coerceAtMost(24)
        for (i in 0 until takeCount) {
            val item = hourlyList[i]
            val hourText = item.getDisplayHour()
            val timeLabel = if (hourText.length >= 5) "${hourText.substring(0, 2)}时" else hourText
            val weatherLabel = if (item.rain > 0.0) "小雨" else current.weatherText
            val rainProb = if (item.rain > 0.0) "${(item.rain * 30).toInt().coerceIn(30, 99)}%" else null

            list.add(
                HourlyDisplayItem(
                    key = if (item.time.isNotBlank()) item.time else "h_$i",
                    timeLabel = timeLabel,
                    weatherText = weatherLabel,
                    rainProb = rainProb,
                    tempText = "${item.temperature.toInt()}°"
                )
            )
        }
        list
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x7514263A))
            .padding(vertical = 12.dp)
    ) {
        // 1. 顶部公告栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "提醒",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .size(15.dp)
                    .padding(top = 1.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = summaryNotice,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 16.sp,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. 原生极简 LazyRow
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = displayItems,
                key = { it.key }
            ) { item ->
                HourlyColumnItem(item = item)
            }
        }
    }
}

/**
 * 逐时预报单列展示单元 (扁平极简单层布局)
 *
 * @param item 逐时预报数据项 [HourlyDisplayItem]
 */
@Composable
private fun HourlyColumnItem(
    item: HourlyDisplayItem
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(48.dp)
    ) {
        // 时间
        Text(
            text = item.timeLabel,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 天气图标
        WeatherDynamicIcon(
            weatherText = item.weatherText,
            size = 22.dp
        )

        // 降水概率 (如果有)
        if (item.rainProb != null) {
            Text(
                text = item.rainProb,
                color = Color(0xFF64B5F6),
                fontSize = 9.sp,
                fontWeight = FontWeight.Normal
            )
        } else {
            Spacer(modifier = Modifier.height(13.dp))
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 温度
        Text(
            text = item.tempText,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
    }
}




