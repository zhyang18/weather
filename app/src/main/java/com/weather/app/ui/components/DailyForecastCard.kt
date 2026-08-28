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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.model.DailyForecast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.hypot

import androidx.compose.runtime.Immutable

/**
 * 近日天气趋势图单日轻量展示数据实体类 (Immutable UI Model)
 *
 * 预计算并封装单日图表所需的格式化文案与天气指标，彻底消除横向滚动时的重复计算。
 * 显式声明为 [Immutable] 实体，确保 Compose 编译器能够对其智能跳过不必要的父级重组。
 *
 * @property weekLabel 星期显示文案（如 "昨天", "今天", "周四"）
 * @property dateText 短日期文本（如 "8月23日"）
 * @property weatherText 天气现象描述文本
 * @property rainPercentage 降水概率百分比文本（如 "80%"），若无降雨则为 null
 * @property maxTempText 最高温显示文本（如 "34°"）
 * @property minTempText 最低温显示文本（如 "25°"）
 * @property isYesterday 是否为昨天历史数据项
 */
@Immutable
private data class DailyChartDisplayItem(
    val weekLabel: String,
    val dateText: String,
    val weatherText: String,
    val rainPercentage: String?,
    val maxTempText: String,
    val minTempText: String,
    val isYesterday: Boolean
)

/**
 * 近日天气趋势图全量状态数据类
 *
 * 封装趋势图所需的展示实体列表、全局温度极值范围与坐标运算原语数组。
 * 显式声明为 [Immutable] 实体，确保 Compose 能够在其未变更时跳过绘制重组。
 *
 * @property items 单日展示实体列表
 * @property allMax 全局最高温基准
 * @property allMin 全局最低温基准
 * @property range 温差跨度范围
 * @property maxTemps 原生最高温浮点数组 (FloatArray)
 * @property minTemps 原生最低温浮点数组 (FloatArray)
 */
@Immutable
private data class DailyChartUiState(
    val items: List<DailyChartDisplayItem>,
    val allMax: Float,
    val allMin: Float,
    val range: Float,
    val maxTemps: FloatArray,
    val minTemps: FloatArray
)


/**
 * 构建包含“昨天”历史天气的完整近日预报列表
 *
 * @param dailyList 原始预报列表
 * @return 包含昨天在内的完整预报列表
 */
private fun buildFullDailyList(dailyList: List<DailyForecast>): List<DailyForecast> {
    if (dailyList.isEmpty()) return emptyList()
    if (dailyList.first().dayOfWeek == "昨天") return dailyList

    val first = dailyList.first()
    val yesterdayCal = Calendar.getInstance().apply {
        try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
            val parsedDate = format.parse(first.date)
            if (parsedDate != null) {
                time = parsedDate
            }
        } catch (e: Exception) {
            time = Date()
        }
        add(Calendar.DAY_OF_YEAR, -1)
    }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    val yesterdayDate = dateFormat.format(yesterdayCal.time)

    val yesterdayForecast = DailyForecast(
        date = yesterdayDate,
        dayOfWeek = "昨天",
        dayWeatherText = first.dayWeatherText,
        nightWeatherText = first.nightWeatherText,
        maxTemperature = (first.maxTemperature - 0.5).coerceAtLeast(first.minTemperature),
        minTemperature = first.minTemperature - 0.5,
        precipitation = 0.0
    )
    return listOf(yesterdayForecast) + dailyList
}

/**
 * 近日天气预报毛玻璃卡片组件
 *
 * 支持“趋势折线图表”与“逐日温差列表”双视图无缝切换：
 * 1. 趋势折线图模式（Chart Mode）：呈现包含“昨天”在内的全量天气趋势，昨日与今日之间虚线连接，今日往后实线加粗；
 * 2. 列表模式（List Mode）：呈现包含“昨天”的温差指示条列表；
 * 3. 记住用户切换状态并在本地持久化保存。
 *
 * @param dailyList 每日预报列表 [DailyForecast]
 * @param isChartMode 当前是否为折线趋势图表模式
 * @param onChartModeChange 切换图表/列表模式时的回调
 * @param modifier 外部修饰符
 */
@Composable
fun DailyForecastCard(
    dailyList: List<DailyForecast>,
    isChartMode: Boolean,
    onChartModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (dailyList.isEmpty()) return

    // 组合包含“昨天”的完整预报列表
    val fullDailyList = remember(dailyList) {
        buildFullDailyList(dailyList)
    }

    val globalMin = remember(fullDailyList) { fullDailyList.minOfOrNull { it.minTemperature } ?: 15.0 }
    val globalMax = remember(fullDailyList) { fullDailyList.maxOfOrNull { it.maxTemperature } ?: 35.0 }
    val tempSpan = remember(globalMin, globalMax) { (globalMax - globalMin).coerceAtLeast(1.0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .graphicsLayer {
                // 开启独立硬件渲染图层缓存
                clip = true
                shape = RoundedCornerShape(20.dp)
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x7514263A))
            .padding(top = 16.dp, bottom = 16.dp)
    ) {
        // 头部栏：📅 近日天气 与 列表/趋势图表切换按钮（保持 16dp 水平内边距）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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
                        .clickable { onChartModeChange(false) }
                        .padding(horizontal = 2.dp, vertical = 4.dp),
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
                        .clickable { onChartModeChange(true) }
                        .padding(horizontal = 2.dp, vertical = 4.dp),
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
            // 模式 1：趋势折线图表视图（水平内边距为 0，支持全宽无缝平滑横滑）
            DailyForecastChartView(
                dailyList = fullDailyList
            )
        } else {
            // 模式 2：列表视图（保留 16dp 水平内边距）
            DailyForecastListView(
                dailyList = fullDailyList,
                globalMin = globalMin,
                tempSpan = tempSpan,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

/**
 * 近日天气趋势折线图表视图组件（水平内边距为 0、加粗 2 倍至 9.6f 的饱满线条、昨日与今天虚线连接、今日往后实线）
 *
 * 采用 [remember] 预计算全量 UI 数据与 Float 原生数组，消除横向滚动时的任何重复计算与重绘抖动。
 *
 * @param dailyList 包含昨天在内的逐日预报数据项列表 [DailyForecast]
 */
@Composable
private fun DailyForecastChartView(
    dailyList: List<DailyForecast>
) {
    val scrollState = rememberScrollState()

    // 预计算图表所需的全量状态数据
    val chartState = remember(dailyList) {
        val count = dailyList.size
        val maxArray = FloatArray(count)
        val minArray = FloatArray(count)
        var maxVal = Float.NEGATIVE_INFINITY
        var minVal = Float.POSITIVE_INFINITY

        val displayItems = ArrayList<DailyChartDisplayItem>(count)

        for (i in 0 until count) {
            val f = dailyList[i]
            val maxT = f.maxTemperature.toFloat()
            val minT = f.minTemperature.toFloat()
            maxArray[i] = maxT
            minArray[i] = minT
            if (maxT > maxVal) maxVal = maxT
            if (minT < minVal) minVal = minT

            val isYesterday = i == 0
            val rainPercentage = if (f.dayWeatherText.contains("雨") || f.dayWeatherText.contains("雷")) {
                "${(f.precipitation * 20).toInt().coerceIn(60, 95)}%"
            } else null

            val weekLabel = when (i) {
                0 -> "昨天"
                1 -> "今天"
                2 -> "明天"
                3 -> "后天"
                else -> f.dayOfWeek
            }

            displayItems.add(
                DailyChartDisplayItem(
                    weekLabel = weekLabel,
                    dateText = f.getShortDateText(),
                    weatherText = f.dayWeatherText,
                    rainPercentage = rainPercentage,
                    maxTempText = "${f.maxTemperature.toInt()}°",
                    minTempText = "${f.minTemperature.toInt()}°",
                    isYesterday = isYesterday
                )
            )
        }

        val allMax = if (maxVal == Float.NEGATIVE_INFINITY) 35f else maxVal
        val allMin = if (minVal == Float.POSITIVE_INFINITY) 15f else minVal
        val range = (allMax - allMin).coerceAtLeast(2f)

        DailyChartUiState(
            items = displayItems,
            allMax = allMax,
            allMin = allMin,
            range = range,
            maxTemps = maxArray,
            minTemps = minArray
        )
    }

    val itemWidth = 58.dp
    val totalWidth = itemWidth * chartState.items.size
    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(9.0f, 9.0f), 0f) }

    // 横向可平滑滑动容器（水平内边距为 0）
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
                chartState.items.forEach { item ->
                    DailyChartTopColumnItem(item = item, itemWidth = itemWidth)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. 中间金黄/天蓝双温走势图
            Box(
                modifier = Modifier
                    .width(totalWidth)
                    .height(72.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val count = chartState.items.size
                    if (count < 2) return@Canvas

                    val stepX = w / count
                    val maxTemps = chartState.maxTemps
                    val minTemps = chartState.minTemps
                    val allMin = chartState.allMin
                    val range = chartState.range

                    val strokeWidthPx = 9.6f
                    val circleRadius = 13.3f
                    val circleGap = 6.0f
                    val totalOffset = circleRadius + circleGap

                    val highColor = Color(0xFFF9BF33)
                    val lowColor = Color(0xFF38BDF8)
                    val historyAlpha = 0.42f

                    // ================= 1. 最高温走势（金黄色线段 + 实心圆点） =================
                    for (i in 0 until count - 1) {
                        val cx1 = stepX * i + stepX / 2f
                        val highNorm1 = (maxTemps[i] - allMin) / range
                        val hy1 = h * (0.42f - highNorm1 * 0.24f)

                        val cx2 = stepX * (i + 1) + stepX / 2f
                        val highNorm2 = (maxTemps[i + 1] - allMin) / range
                        val hy2 = h * (0.42f - highNorm2 * 0.24f)

                        val isYesterdaySegment = (i == 0)
                        val segColor = if (isYesterdaySegment) highColor.copy(alpha = historyAlpha) else highColor

                        val dx = cx2 - cx1
                        val dy = hy2 - hy1
                        val dist = hypot(dx, dy)
                        if (dist > totalOffset * 2f) {
                            val ux = dx / dist
                            val uy = dy / dist
                            val start = Offset(cx1 + ux * totalOffset, hy1 + uy * totalOffset)
                            val end = Offset(cx2 - ux * totalOffset, hy2 - uy * totalOffset)
                            drawLine(
                                color = segColor,
                                start = start,
                                end = end,
                                strokeWidth = strokeWidthPx,
                                cap = StrokeCap.Round,
                                pathEffect = if (isYesterdaySegment) dashEffect else null
                            )
                        }
                    }

                    for (i in 0 until count) {
                        val cx = stepX * i + stepX / 2f
                        val highNorm = (maxTemps[i] - allMin) / range
                        val hy = h * (0.42f - highNorm * 0.24f)
                        val ptColor = if (i == 0) highColor.copy(alpha = historyAlpha) else highColor
                        drawCircle(
                            color = ptColor,
                            radius = circleRadius,
                            center = Offset(cx, hy)
                        )
                    }

                    // ================= 2. 最低温走势（天蓝色线段 + 实心圆点） =================
                    for (i in 0 until count - 1) {
                        val cx1 = stepX * i + stepX / 2f
                        val lowNorm1 = (minTemps[i] - allMin) / range
                        val ly1 = h * (0.82f - lowNorm1 * 0.24f)

                        val cx2 = stepX * (i + 1) + stepX / 2f
                        val lowNorm2 = (minTemps[i + 1] - allMin) / range
                        val ly2 = h * (0.82f - lowNorm2 * 0.24f)

                        val isYesterdaySegment = (i == 0)
                        val segColor = if (isYesterdaySegment) lowColor.copy(alpha = historyAlpha) else lowColor

                        val dx = cx2 - cx1
                        val dy = ly2 - ly1
                        val dist = hypot(dx, dy)
                        if (dist > totalOffset * 2f) {
                            val ux = dx / dist
                            val uy = dy / dist
                            val start = Offset(cx1 + ux * totalOffset, ly1 + uy * totalOffset)
                            val end = Offset(cx2 - ux * totalOffset, ly2 - uy * totalOffset)
                            drawLine(
                                color = segColor,
                                start = start,
                                end = end,
                                strokeWidth = strokeWidthPx,
                                cap = StrokeCap.Round,
                                pathEffect = if (isYesterdaySegment) dashEffect else null
                            )
                        }
                    }

                    for (i in 0 until count) {
                        val cx = stepX * i + stepX / 2f
                        val lowNorm = (minTemps[i] - allMin) / range
                        val ly = h * (0.82f - lowNorm * 0.24f)
                        val ptColor = if (i == 0) lowColor.copy(alpha = historyAlpha) else lowColor
                        drawCircle(
                            color = ptColor,
                            radius = circleRadius,
                            center = Offset(cx, ly)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. 底部最低温度数值行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                chartState.items.forEach { item ->
                    DailyChartBottomColumnItem(item = item, itemWidth = itemWidth)
                }
            }
        }
    }
}

/**
 * 趋势图单日顶部信息列组件 (Immutable UI Item)
 *
 * 显式声明为独立 Composable，支持 Compose 编译器进行细粒度跳过重组（Skippable），消除滚动时的全局重组。
 *
 * @param item 单日展示数据项 [DailyChartDisplayItem]
 * @param itemWidth 单列宽度
 */
@Composable
private fun DailyChartTopColumnItem(
    item: DailyChartDisplayItem,
    itemWidth: androidx.compose.ui.unit.Dp
) {
    val isYesterday = item.isYesterday
    val itemAlpha = if (isYesterday) 0.55f else 1.0f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(itemWidth)
    ) {
        // 星期
        Text(
            text = item.weekLabel,
            color = Color.White.copy(alpha = itemAlpha),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 日期
        Text(
            text = item.dateText,
            color = Color.White.copy(alpha = if (isYesterday) 0.40f else 0.65f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 天气现象名称
        Text(
            text = item.weatherText,
            color = Color.White.copy(alpha = if (isYesterday) 0.50f else 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 天气矢量图标容器
        Box(
            modifier = Modifier
                .width(itemWidth)
                .height(42.dp),
            contentAlignment = Alignment.Center
        ) {
            WeatherDynamicIcon(
                weatherText = item.weatherText,
                size = 24.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer { alpha = itemAlpha }
            )

            if (item.rainPercentage != null && !isYesterday) {
                Text(
                    text = item.rainPercentage,
                    color = Color(0xFF64B5F6),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 最高温度数值
        Text(
            text = item.maxTempText,
            color = Color.White.copy(alpha = if (isYesterday) 0.55f else 1.0f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

/**
 * 趋势图单日底部最低温度列组件 (Immutable UI Item)
 *
 * @param item 单日展示数据项 [DailyChartDisplayItem]
 * @param itemWidth 单列宽度
 */
@Composable
private fun DailyChartBottomColumnItem(
    item: DailyChartDisplayItem,
    itemWidth: androidx.compose.ui.unit.Dp
) {
    val isYesterday = item.isYesterday
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(itemWidth)
    ) {
        Text(
            text = item.minTempText,
            color = Color.White.copy(alpha = if (isYesterday) 0.55f else 0.90f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal
        )
    }
}


/**
 * 包含“昨天”在内的近日天气温差条列表视图组件
 *
 * @param dailyList 每日预报列表 [DailyForecast]
 * @param globalMin 全局最低气温
 * @param tempSpan 全局温差跨度
 * @param modifier 外部修饰符
 */
@Composable
private fun DailyForecastListView(
    dailyList: List<DailyForecast>,
    globalMin: Double,
    tempSpan: Double,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        dailyList.forEachIndexed { index, forecast ->
            val isYesterday = index == 0
            val isToday = index == 1

            DailyForecastRow(
                forecast = forecast,
                displayDayLabel = when (index) {
                    0 -> "昨天"
                    1 -> "今天"
                    2 -> "明天"
                    3 -> "后天"
                    else -> forecast.dayOfWeek
                },
                globalMin = globalMin,
                tempSpan = tempSpan,
                showDot = isToday,
                isHistorical = isYesterday
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
 * @param displayDayLabel 星期显示文案（如 "昨天", "今天", "周四"）
 * @param globalMin 全局最低气温基准
 * @param tempSpan 全局温差跨度
 * @param showDot 是否在温差条上高亮当前气温实况点
 * @param isHistorical 是否为历史过去日期（用于微调半透明度）
 */
@Composable
private fun DailyForecastRow(
    forecast: DailyForecast,
    displayDayLabel: String,
    globalMin: Double,
    tempSpan: Double,
    showDot: Boolean,
    isHistorical: Boolean = false
) {
    val rowAlpha = if (isHistorical) 0.60f else 1.0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = rowAlpha },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 星期与天气名称 (如 昨天  多云 / 周四  小雨)
        Row(
            modifier = Modifier.width(100.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayDayLabel,
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

        // 天气矢量高保真图标 (含降水概率)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            WeatherDynamicIcon(
                weatherText = forecast.dayWeatherText,
                size = 20.dp
            )
            if (!isHistorical && (forecast.dayWeatherText.contains("雨") || forecast.dayWeatherText.contains("雷"))) {
                Text(
                    text = "${(forecast.precipitation * 20).toInt().coerceIn(60, 95)}%",
                    color = Color(0xFF64B5F6),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        // 最低温数值 (如 25°)
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

        // 最高温数值 (如 34°，常规字重)
        Text(
            text = "${forecast.maxTemperature.toInt()}°",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.width(32.dp)
        )
    }
}



