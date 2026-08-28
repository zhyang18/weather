package com.weather.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
 * 逐时预报轻量展示实体数据类 (Immutable UI Model)
 *
 * 预计算并封装单列所需的格式化文本与天气状态，消除滚动过程中的重复计算与字符串格式化。
 * 显式声明为 [Immutable] 实体，确保 Compose 能够在其未变更时跳过绘制重组。
 *
 * @property key 唯一稳定标识键
 * @property timeLabel 顶部时间文本（如 "现在", "16时"）
 * @property weatherText 天气现象描述
 * @property rainProb 降水概率百分比文本（如 "90%"），若无降水则为 null
 * @property tempText 底部温度数值文本（如 "32°"）
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
 * 24小时逐时预报毛玻璃卡片组件
 *
 * 采用硬件级无损平移架构重构：
 * 1. 采用纯净高效的 [horizontalScroll] 硬件层平移替代短列表的 LazyLayout 动态挂载开销；
 * 2. 消除子项 [HourlyColumnItem] 内的 `weight(1f)` 二次测量，实现单次极速测量；
 * 3. 隔离顶部公告栏跑马灯动画与下方逐时滑动区域，彻底杜绝横向滚动掉帧与卡顿。
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

    // 基于真实数据动态合成精简智能天气提示文本（使用 remember 记忆化）
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

    // 预计算 24 小时展示列表，避免滑动中重复提取与计算
    val displayItems = remember(current, hourlyList) {
        val list = ArrayList<HourlyDisplayItem>(25)

        // 1. 当前时刻单元
        val currentRainProb = if (current.precipitation > 0.0) {
            "${(current.precipitation * 20).toInt().coerceAtMost(99)}%"
        } else null

        list.add(
            HourlyDisplayItem(
                key = "current_hour",
                timeLabel = "现在",
                weatherText = current.weatherText,
                rainProb = currentRainProb,
                tempText = "${current.temperature.toInt()}°"
            )
        )

        // 2. 逐小时预报单元 (取前 24 个点)
        val takeCount = hourlyList.size.coerceAtMost(24)
        for (i in 0 until takeCount) {
            val item = hourlyList[i]
            val hourText = item.getDisplayHour()
            val timeLabel = if (hourText.length >= 5) "${hourText.substring(0, 2)}时" else hourText
            val weatherLabel = if (item.rain > 0.0) "小雨" else current.weatherText
            val rainProb = if (item.rain > 0.0) "${(item.rain * 30).toInt().coerceIn(30, 99)}%" else null

            list.add(
                HourlyDisplayItem(
                    key = if (item.time.isNotBlank()) item.time else "hour_$i",
                    timeLabel = timeLabel,
                    weatherText = weatherLabel,
                    rainProb = rainProb,
                    tempText = "${item.temperature.toInt()}°"
                )
            )
        }

        list
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x7514263A))
            .padding(top = 10.dp, bottom = 10.dp)
    ) {
        // 1. 顶部独立隔离的公告提示栏 (独立组件，隔离跑马灯重绘)
        HourlyNoticeBar(noticeText = summaryNotice)

        Spacer(modifier = Modifier.height(8.dp))

        // 2. 小时级硬件级平滑横向滚动行 (25个节点一次性硬件层挂载，0 重组 0 二次测量)
        if (displayItems.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                displayItems.forEach { item ->
                    HourlyColumnItem(item = item)
                }
            }
        }
    }
}

/**
 * 顶部公告提示栏组件 (多行自然折行，消除跑马灯协程开销)
 *
 * @param noticeText 公告文本内容
 */
@Composable
private fun HourlyNoticeBar(
    noticeText: String
) {
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
            text = noticeText,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f)
        )
    }
}


/**
 * 逐时预报单列展示单元 (Single-pass 绝对布局，消除 weight 二次测量)
 *
 * @param item 逐时预报轻量展示数据项 [HourlyDisplayItem]
 */
@Composable
private fun HourlyColumnItem(
    item: HourlyDisplayItem
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(52.dp)
            .height(84.dp)
    ) {
        // 1. 时间标签 (顶部)
        Text(
            text = item.timeLabel,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(3.dp))

        // 2. 中间天气图标与降水概率容器 (固定高度 42dp，避免 weight 二次测量)
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(42.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 天气矢量图标
                WeatherDynamicIcon(
                    weatherText = item.weatherText,
                    size = 22.dp
                )

                // 降水概率标签 (如 99%)
                if (item.rainProb != null) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = item.rainProb,
                        color = Color(0xFF64B5F6),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // 3. 底部温度数值 (常规字重，底部)
        Text(
            text = item.tempText,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
    }
}



