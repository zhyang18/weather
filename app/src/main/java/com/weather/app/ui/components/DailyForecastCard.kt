package com.weather.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.model.DailyForecast

/**
 * 近日天气预报毛玻璃卡片组件
 *
 * 支持“趋势折线图表”与“逐日温差列表”双视图无缝切换：
 * 1. 趋势折线图模式（Chart Mode）：呈现 5 天纵向气象指标与金黄/天蓝双温平滑贝塞尔走势图及底部长按钮；
 * 2. 列表模式（List Mode）：呈现 7 天温差指示条列表；
 * 3. 遵循全局字重规范，除主界面当前气温外其余文字均使用常规字重。
 *
 * @param dailyList 每日预报列表 [DailyForecast]
 * @param modifier 外部修饰符
 */
@Composable
fun DailyForecastCard(
    dailyList: List<DailyForecast>,
    modifier: Modifier = Modifier
) {
    if (dailyList.isEmpty()) return

    var isChartMode by remember { mutableStateOf(true) }

    val globalMin = dailyList.minOfOrNull { it.minTemperature } ?: 15.0
    val globalMax = dailyList.maxOfOrNull { it.maxTemperature } ?: 35.0
    val tempSpan = (globalMax - globalMin).coerceAtLeast(1.0)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x7514263A))
            .padding(16.dp)
    ) {
        // 头部栏：📅 近日天气 与 列表/趋势图表切换按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "近日天气",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "近日天气",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // 切换按钮组 (三横线列表 / 折线图表)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 列表视图按钮
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (!isChartMode) Color.White.copy(alpha = 0.20f) else Color.Transparent)
                        .clickable { isChartMode = false }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "列表视图",
                        tint = if (!isChartMode) Color.White else Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(17.dp)
                    )
                }

                // 趋势折线图表按钮
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isChartMode) Color.White.copy(alpha = 0.20f) else Color.Transparent)
                        .clickable { isChartMode = true }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = "趋势图表视图",
                        tint = if (isChartMode) Color.White else Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isChartMode) {
            // 模式 1：趋势折线图表视图 (支持左右横滑，展示全部逐日预报)
            DailyForecastChartView(
                dailyList = dailyList
            )
        } else {
            // 模式 2：列表视图
            DailyForecastListView(
                dailyList = dailyList,
                globalMin = globalMin,
                tempSpan = tempSpan
            )
        }
    }
}

/**
 * 近日天气趋势折线图表视图组件（支持全量预报平滑左右横滑）
 *
 * @param dailyList 逐日预报数据项列表 [DailyForecast]
 */
@Composable
private fun DailyForecastChartView(
    dailyList: List<DailyForecast>
) {
    val scrollState = rememberScrollState()
    val maxTemps = dailyList.map { it.maxTemperature.toFloat() }
    val minTemps = dailyList.map { it.minTemperature.toFloat() }

    val allMax = maxTemps.maxOrNull() ?: 35f
    val allMin = minTemps.minOrNull() ?: 15f
    val range = (allMax - allMin).coerceAtLeast(2f)

    val itemWidth = 58.dp
    val totalWidth = itemWidth * dailyList.size

    // 横向可平滑滑动容器
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
    ) {
        Column(modifier = Modifier.width(totalWidth)) {
            // 1. 顶部各天指标信息行 (星期、日期、天气、图标与降水概率、最高温数值)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                dailyList.forEachIndexed { index, forecast ->
                    val rainPercentage = if (forecast.dayWeatherText.contains("雨") || forecast.dayWeatherText.contains("雷")) {
                        "${(forecast.precipitation * 20).toInt().coerceIn(60, 95)}%"
                    } else null

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(itemWidth)
                    ) {
                        // 星期 (如 今天, 明天, 后天, 周四)
                        val weekLabel = when (index) {
                            0 -> "今天"
                            1 -> "明天"
                            2 -> "后天"
                            else -> forecast.dayOfWeek
                        }
                        Text(
                            text = weekLabel,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // 日期 (如 8月24日)
                        Text(
                            text = forecast.getShortDateText(),
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // 天气现象名称 (如 中雨)
                        Text(
                            text = forecast.dayWeatherText,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 天气图标
                        Text(
                            text = WeatherIcons.getWeatherEmoji(forecast.dayWeatherText),
                            fontSize = 18.sp
                        )

                        // 降水概率 (如 80%)
                        Box(
                            modifier = Modifier.height(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (rainPercentage != null) {
                                Text(
                                    text = rainPercentage,
                                    color = Color(0xFF64B5F6),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 最高温度数值 (如 30°)
                        Text(
                            text = "${forecast.maxTemperature.toInt()}°",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. 中间金黄/天蓝双温连续平滑贝塞尔曲线走势图 (宽度完全与上方各列对齐)
            Box(
                modifier = Modifier
                    .width(totalWidth)
                    .height(68.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val count = dailyList.size
                    if (count < 2) return@Canvas

                    val stepX = w / count
                    val highPoints = mutableListOf<Offset>()
                    val lowPoints = mutableListOf<Offset>()

                    for (i in 0 until count) {
                        val cx = stepX * i + stepX / 2f
                        val highNorm = (maxTemps[i] - allMin) / range
                        val lowNorm = (minTemps[i] - allMin) / range

                        // 最高温在上部 (y: 12% ~ 42%)
                        val hy = h * (0.42f - highNorm * 0.30f)
                        // 最低温在下部 (y: 58% ~ 88%)
                        val ly = h * (0.88f - lowNorm * 0.30f)

                        highPoints.add(Offset(cx, hy))
                        lowPoints.add(Offset(cx, ly))
                    }

                    // 绘制最高温度平滑贝塞尔曲线 (金黄色)
                    val highPath = Path().apply {
                        moveTo(highPoints.first().x, highPoints.first().y)
                        for (i in 0 until highPoints.size - 1) {
                            val p0 = highPoints[i]
                            val p1 = highPoints[i + 1]
                            val controlX = (p0.x + p1.x) / 2f
                            cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                        }
                    }

                    drawPath(
                        path = highPath,
                        color = Color(0xFFFFCA28),
                        style = Stroke(width = 2.4f, cap = StrokeCap.Round)
                    )

                    // 最高温各节点小圆点
                    highPoints.forEach { pt ->
                        drawCircle(color = Color(0xFF263644), radius = 4.2f, center = pt)
                        drawCircle(color = Color(0xFFFFCA28), radius = 3.0f, center = pt)
                    }

                    // 绘制最低温度平滑贝塞尔曲线 (天蓝色)
                    val lowPath = Path().apply {
                        moveTo(lowPoints.first().x, lowPoints.first().y)
                        for (i in 0 until lowPoints.size - 1) {
                            val p0 = lowPoints[i]
                            val p1 = lowPoints[i + 1]
                            val controlX = (p0.x + p1.x) / 2f
                            cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                        }
                    }

                    drawPath(
                        path = lowPath,
                        color = Color(0xFF29B6F6),
                        style = Stroke(width = 2.4f, cap = StrokeCap.Round)
                    )

                    // 最低温各节点小圆点
                    lowPoints.forEach { pt ->
                        drawCircle(color = Color(0xFF263644), radius = 4.2f, center = pt)
                        drawCircle(color = Color(0xFF29B6F6), radius = 3.0f, center = pt)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. 底部最低温度数值行 (如 25°, 26°)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                dailyList.forEach { forecast ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(itemWidth)
                    ) {
                        Text(
                            text = "${forecast.minTemperature.toInt()}°",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

/**
 * 7天近日天气温差条列表视图组件
 *
 * @param dailyList 每日预报列表 [DailyForecast]
 * @param globalMin 全局最低气温
 * @param tempSpan 全局温差跨度
 */
@Composable
private fun DailyForecastListView(
    dailyList: List<DailyForecast>,
    globalMin: Double,
    tempSpan: Double
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        dailyList.forEachIndexed { index, forecast ->
            DailyForecastRow(
                forecast = forecast,
                globalMin = globalMin,
                tempSpan = tempSpan,
                showDot = index == 0 || index == 1
            )
            if (index < dailyList.size - 1) {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

/**
 * 单日预报行组件
 *
 * @param forecast 单日预报数据项 [DailyForecast]
 * @param globalMin 全局最低气温基准
 * @param tempSpan 全局温差跨度
 * @param showDot 是否在温差条上高亮当前气温实况点
 */
@Composable
private fun DailyForecastRow(
    forecast: DailyForecast,
    globalMin: Double,
    tempSpan: Double,
    showDot: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 星期与天气名称 (如 周四  小雨)
        Row(
            modifier = Modifier.width(100.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = forecast.dayOfWeek,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.width(42.dp)
            )
            Text(
                text = forecast.dayWeatherText,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // 天气图标 (含降水概率)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Text(
                text = WeatherIcons.getWeatherEmoji(forecast.dayWeatherText),
                fontSize = 18.sp
            )
            if (forecast.dayWeatherText.contains("雨") || forecast.dayWeatherText.contains("雷")) {
                Text(
                    text = "${(forecast.precipitation * 20).toInt().coerceIn(60, 95)}%",
                    color = Color(0xFF64B5F6),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        // 最低温数值 (如 26°)
        Text(
            text = "${forecast.minTemperature.toInt()}°",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.width(32.dp)
        )

        // 温差指示条 (支持点指示器)
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.CenterStart
        ) {
            val startRatio = ((forecast.minTemperature - globalMin) / tempSpan).toFloat().coerceIn(0f, 0.8f)
            val endRatio = ((forecast.maxTemperature - globalMin) / tempSpan).toFloat().coerceIn(startRatio + 0.2f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxWidth(endRatio)
                    .padding(start = (startRatio * 100).dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFF9A825),
                                Color(0xFFFFB74D)
                            )
                        )
                    )
            )

            // 实况高亮点
            if (showDot) {
                Box(
                    modifier = Modifier
                        .padding(start = ((startRatio + (endRatio - startRatio) * 0.7f) * 100).dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }

        // 最高温数值 (如 32°，常规字重)
        Text(
            text = "${forecast.maxTemperature.toInt()}°",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.width(32.dp)
        )
    }
}

