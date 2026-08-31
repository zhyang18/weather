package com.weather.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.weather.app.model.CityInfo
import com.weather.app.util.LunarAstroCalculator
import com.weather.app.util.SolarAstroCalculator
import com.weather.app.util.SolarDayDetail
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 摄影级 3D 日出日落与天体运行全屏沉浸式详情交互页面
 *
 * 严格按照月相详情页的视觉风格、交互架构与质量规范构建：
 * 1. 沉浸式深空星光天穹背景与全景太阳天球运行轨迹主舞台；
 * 2. 交互式前后 30 天连续时间轴，支持滑动或点选任意日期实时观察日出日落演变；
 * 3. 关键太阳运行四节点卡片（日出、正午中天、日落、次日日出）；
 * 4. 2x3 结构化太阳物理与天文坐标指标网格（严格消除任何文字截断）；
 * 5. 晨昏蒙影（Twilight）精细三阶段卡片（民用、航海、天文时段与照度科普）；
 * 6. 摄影黄金时刻（Golden Hour）与蓝调时刻（Blue Hour）时刻表与光影指南；
 * 7. 太阳与四季节气天文科普卡片。
 *
 * @param visible 是否展示该全屏页面
 * @param city 当前聚焦城市信息对象 [CityInfo]
 * @param onBackClick 点击返回或系统返回触发回调
 * @param modifier 外部修饰符 [Modifier]
 */
@Composable
fun SunriseSunsetDetailScreen(
    visible: Boolean,
    city: CityInfo?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = visible) {
        onBackClick()
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        SunriseSunsetDetailContent(
            city = city,
            onBackClick = onBackClick
        )
    }
}

/**
 * 日出日落详情核心内容与多维天文学组件滚动视图
 *
 * @param city 城市信息对象 [CityInfo]
 * @param onBackClick 返回事件回调
 * @param modifier 外部修饰符
 */
@Composable
private fun SunriseSunsetDetailContent(
    city: CityInfo?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // 当前选中的相对偏移天数（0 表示今天，负数过去，正数未来）
    var selectedDayOffset by remember { mutableIntStateOf(0) }

    // 系统时钟状态：进入前台 (ON_RESUME) 即时校准，前台运行期间每分钟自增更新
    var currentSystemTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentSystemTimeMillis = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60 * 1000L)
            currentSystemTimeMillis = System.currentTimeMillis()
        }
    }

    // 计算选中日期的日历对象
    val activeCalendar = remember(selectedDayOffset, currentSystemTimeMillis / 60000L) {
        val cal = Calendar.getInstance().apply { timeInMillis = currentSystemTimeMillis }
        cal.add(Calendar.DAY_OF_YEAR, selectedDayOffset)
        cal
    }

    // 计算选中日期的太阳天文详细指标
    val solarDetail = remember(city?.getCacheKey(), selectedDayOffset, currentSystemTimeMillis / 60000L) {
        SolarAstroCalculator.calculateSolarDayDetail(city, activeCalendar)
    }

    // 计算选中日期的农历信息
    val (lunarYearGZ, lunarDateStr) = remember(selectedDayOffset, currentSystemTimeMillis / 60000L) {
        LunarAstroCalculator.convertSolarToLunar(
            activeCalendar.get(Calendar.YEAR),
            activeCalendar.get(Calendar.MONTH) + 1,
            activeCalendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    // 生成 30 天连续日出日落序列（过去 3 天 + 未来 27 天）
    val solarSequence = remember(city?.getCacheKey(), currentSystemTimeMillis / 86400000L) {
        val center = Calendar.getInstance().apply { timeInMillis = currentSystemTimeMillis }
        SolarAstroCalculator.generate30DaysSequence(city, center, pastDays = 3, futureDays = 27)
    }

    val cityName = city?.name ?: "北京"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF060A12),
                        Color(0xFF0D1524),
                        Color(0xFF131F33),
                        Color(0xFF090E18)
                    )
                )
            )
    ) {
        // 背景星空粒子装饰
        StarlightBackgroundCanvas(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 1. 顶部沉浸式导航栏（对齐月相详情页）
            SolarDetailTopBar(
                cityName = cityName,
                isToday = selectedDayOffset == 0,
                onBackClick = onBackClick,
                onResetTodayClick = { selectedDayOffset = 0 }
            )

            // 2. 可滚动主内容区
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 2.1 摄影级 3D 拟真日出日落主舞台与日期切换
                HeroInteractiveSolarStage(
                    solarDetail = solarDetail,
                    lunarYearGanZhi = lunarYearGZ,
                    lunarDateStr = lunarDateStr,
                    isToday = selectedDayOffset == 0,
                    onPrevDayClick = { selectedDayOffset -= 1 },
                    onNextDayClick = { selectedDayOffset += 1 }
                )

                // 2.2 关键太阳运行四节点卡片
                MajorSolarEventsSection(detail = solarDetail)

                // 2.3 30 天日照周期变化轮播时间轴
                SolarCycleCarouselSection(
                    sequence = solarSequence,
                    selectedOffset = selectedDayOffset,
                    onSelectDayOffset = { offset -> selectedDayOffset = offset }
                )

                // 2.4 太阳天文详细物理指标网格 (2x3 矩阵，彻底消除截断)
                SolarAstrometricsGrid(detail = solarDetail)

                // 2.5 晨昏蒙影（Twilight）精细指南卡片
                TwilightGuideCard(detail = solarDetail)

                // 2.6 摄影绝美光影时刻（Golden & Blue Hours）指南卡片
                PhotographyGuideCard(detail = solarDetail)

                // 2.7 太阳与四季节气天文科普卡片
                SolarScienceGuideCard()

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

/**
 * 顶部沉浸式导航栏（完全对齐月相详情页规范）
 *
 * @param cityName 城市名称
 * @param isToday 当前选中的是否为今天
 * @param onBackClick 返回按钮点击回调
 * @param onResetTodayClick 重置回今天按钮点击回调
 */
@Composable
private fun SolarDetailTopBar(
    cityName: String,
    isToday: Boolean,
    onBackClick: () -> Unit,
    onResetTodayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // 左侧圆形返回按钮（绝对靠左）
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.15f),
            modifier = Modifier
                .size(38.dp)
                .align(Alignment.CenterStart)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 居中标题（始终锁定在屏幕物理水平绝对中心，不随两侧元素宽度变化而偏移）
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                text = "日出日落与天象",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = cityName,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // 右侧“今天”快速重置按钮（绝对靠右）
        if (!isToday) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF2A5288).copy(alpha = 0.70f),
                modifier = Modifier
                    .height(32.dp)
                    .align(Alignment.CenterEnd)
                    .clickable(onClick = onResetTodayClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = "回到今天",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "今天",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * 摄影级 3D 拟真交互太阳主舞台（完全对齐月相详情页 HeroInteractiveMoonStage 规范）
 *
 * @param solarDetail 太阳天文详细数据 [SolarDayDetail]
 * @param lunarYearGanZhi 农历干支年
 * @param lunarDateStr 农历月日
 * @param isToday 是否为今天
 * @param onPrevDayClick 切换至前一天点击回调
 * @param onNextDayClick 切换至后一天点击回调
 */
@Composable
private fun HeroInteractiveSolarStage(
    solarDetail: SolarDayDetail,
    lunarYearGanZhi: String,
    lunarDateStr: String,
    isToday: Boolean,
    onPrevDayClick: () -> Unit,
    onNextDayClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x6014263D))
            .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            .padding(vertical = 18.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 日期前后切换控制条
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrevDayClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "前一天",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${solarDetail.dateMonthDayStr} ${solarDetail.dayOfWeekStr}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$lunarYearGanZhi · $lunarDateStr",
                    color = Color(0xFFFFD54F).copy(alpha = 0.90f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            IconButton(
                onClick = onNextDayClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "后一天",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 天球太阳拱弧主 Canvas
        SolarSkyArcCanvas(
            detail = solarDetail,
            isToday = isToday,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 主标题：白昼时长或日落倒计时
        val mainTitleText = if (isToday) {
            if (solarDetail.isSunAboveHorizon) "日落 ${solarDetail.sunsetTimeStr}" else "明日日出 ${solarDetail.sunriseTimeStr}"
        } else {
            "白昼 ${solarDetail.daylightDurationStr}"
        }
        Text(
            text = mainTitleText,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 状态与光照标签胶囊
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusDesc = if (isToday) {
                if (solarDetail.isSunAboveHorizon) {
                    "白昼中 · 距日落 ${formatRemainingMinutes(solarDetail.sunsetMinutes - getNowMinutes())}"
                } else {
                    "夜幕中 · 距日出 ${formatRemainingMinutes(getNextSunriseRemaining(solarDetail.sunriseMinutes, solarDetail.sunsetMinutes))}"
                }
            } else {
                solarDetail.daylightDifferenceDesc
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.12f)
            ) {
                Text(
                    text = statusDesc,
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "高度角 ${solarDetail.currentElevationDeg}°",
                    color = Color(0xFFFFD54F).copy(alpha = 0.90f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * 关键太阳运行四节点卡片容器（日出、正午中天、日落、次日日出）
 *
 * @param detail 太阳天文数据 [SolarDayDetail]
 */
@Composable
private fun MajorSolarEventsSection(detail: SolarDayDetail) {
    val items = listOf(
        SolarEventItemData("日出", detail.sunriseTimeStr, "${detail.sunriseAzimuthDeg}°", detail.sunriseAzimuthDirectionStr),
        SolarEventItemData("正午中天", detail.solarNoonTimeStr, "${detail.maxElevationDeg}°", "最高天顶"),
        SolarEventItemData("日落", detail.sunsetTimeStr, "${detail.sunsetAzimuthDeg}°", detail.sunsetAzimuthDirectionStr),
        SolarEventItemData("白昼时长", detail.daylightDurationStr.replace("小时", "h").replace("分", "m"), "夜长 ${detail.nightDurationStr.replace("小时", "h").replace("分", "m")}", "昼夜更替")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x6014263D))
            .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = "关键太阳时刻",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "今日关键天体时刻",
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = item.timeStr,
                        color = Color(0xFFFFD54F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.subDesc,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/** 太阳时刻条目数据结构 */
private data class SolarEventItemData(
    val title: String,
    val timeStr: String,
    val angleStr: String,
    val subDesc: String
)

/**
 * 30 天日照周期变化水平滚动轮播组件（完全对齐 MoonCycleCarouselSection 规范）
 *
 * @param sequence 连续 30 天日照数据列表 [List] of [SolarDayDetail]
 * @param selectedOffset 当前选中的偏移天数
 * @param onSelectDayOffset 点击选择日期偏移回调
 */
@Composable
private fun SolarCycleCarouselSection(
    sequence: List<SolarDayDetail>,
    selectedOffset: Int,
    onSelectDayOffset: (Int) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x6014263D))
            .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(top = 16.dp, bottom = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "日照变化",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "30 天日照与白昼变化",
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "点击可切换上方日期",
                color = Color.White.copy(alpha = 0.50f),
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sequence.forEachIndexed { index, item ->
                val itemOffset = index - 3
                val isSelected = itemOffset == selectedOffset
                val isToday = itemOffset == 0

                Column(
                    modifier = Modifier
                        .width(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) Color(0xFF2E5A8E).copy(alpha = 0.90f)
                            else if (isToday) Color.White.copy(alpha = 0.15f)
                            else Color.White.copy(alpha = 0.05f)
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) Color(0xFF90CAF9) else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onSelectDayOffset(itemOffset) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isToday) "今天" else item.dayOfWeekStr,
                        color = if (isSelected || isToday) Color.White else Color.White.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal
                    )

                    Text(
                        text = item.dateMonthDayStr.substringAfter("月"),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = if (isSelected) Color(0xFFFFD54F) else Color(0xFFFFB300),
                        modifier = Modifier.size(18.dp)
                    )

                    Text(
                        text = item.sunriseTimeStr,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp
                    )

                    Text(
                        text = item.sunsetTimeStr,
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 9.5.sp
                    )
                }
            }
        }
    }
}

/**
 * 太阳天文详细物理指标网格 (2x3 矩阵，完全对齐 LunarAstrometricsGrid 规范)
 *
 * @param detail 太阳天文学数据 [SolarDayDetail]
 */
@Composable
private fun SolarAstrometricsGrid(detail: SolarDayDetail) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 第一行：日出时刻 & 日落时刻
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SolarAstrometricCard(
                icon = Icons.Default.WbSunny,
                title = "日出时刻",
                primaryValue = detail.sunriseTimeStr,
                secondaryValue = "方位 ${detail.sunriseAzimuthDirectionStr} (${detail.sunriseAzimuthDeg}°)",
                modifier = Modifier.weight(1f)
            )

            SolarAstrometricCard(
                icon = Icons.Default.WbTwilight,
                title = "日落时刻",
                primaryValue = detail.sunsetTimeStr,
                secondaryValue = "方位 ${detail.sunsetAzimuthDirectionStr} (${detail.sunsetAzimuthDeg}°)",
                modifier = Modifier.weight(1f)
            )
        }

        // 第二行：正午中天 & 白昼总时长
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SolarAstrometricCard(
                icon = Icons.Default.Explore,
                title = "正午中天最高点",
                primaryValue = detail.solarNoonTimeStr,
                secondaryValue = "最大高度角 ${detail.maxElevationDeg}°",
                modifier = Modifier.weight(1f)
            )

            SolarAstrometricCard(
                icon = Icons.Default.Schedule,
                title = "白昼总时长",
                primaryValue = detail.daylightDurationStr,
                secondaryValue = detail.daylightDifferenceDesc,
                modifier = Modifier.weight(1f)
            )
        }

        // 第三行：太阳赤纬角 & 日地距离
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SolarAstrometricCard(
                icon = Icons.Default.Public,
                title = "太阳赤纬角",
                primaryValue = if (detail.declinationDeg >= 0) "+${detail.declinationDeg}°" else "${detail.declinationDeg}°",
                secondaryValue = if (detail.declinationDeg >= 0) "直射北半球" else "直射南半球",
                modifier = Modifier.weight(1f)
            )

            SolarAstrometricCard(
                icon = Icons.Default.Speed,
                title = "日地距离",
                primaryValue = "${detail.earthSunDistanceAu} AU",
                secondaryValue = "${detail.earthSunDistanceKm} 万公里",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 单个太阳天文指标毛玻璃小卡片（完全对齐 AstrometricCard 规范，绝无裁切）
 *
 * @param icon 指标图标 [ImageVector]
 * @param title 指标标题
 * @param primaryValue 核心指标数值
 * @param secondaryValue 辅助说明文本
 * @param modifier 外部修饰符
 */
@Composable
private fun SolarAstrometricCard(
    icon: ImageVector,
    title: String,
    primaryValue: String,
    secondaryValue: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(108.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x6014263D))
            .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }

        Column {
            Text(
                text = primaryValue,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = secondaryValue,
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

/**
 * 晨昏蒙影（Twilight）精细三阶段指南卡片（结构化排版，彻底解决文字换行挤压与截断）
 *
 * @param detail 太阳天文学指标模型 [SolarDayDetail]
 */
@Composable
private fun TwilightGuideCard(detail: SolarDayDetail) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x6014263D))
            .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.WbTwilight,
                contentDescription = null,
                tint = Color(0xFF60A5FA),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "晨昏蒙影（Twilight）三阶段",
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3 个阶段结构化卡片
        TwilightStageBlock(
            name = "民用晨昏蒙影 (Civil)",
            angleTag = "-0° ~ -6°",
            dawnRange = "${detail.civilTwilight.dawnStartStr} ~ ${detail.civilTwilight.dawnEndStr}",
            duskRange = "${detail.civilTwilight.duskStartStr} ~ ${detail.civilTwilight.duskEndStr}",
            desc = "天空明亮，无需人工照明即可进行日常户外活动与阅读。"
        )

        Spacer(modifier = Modifier.height(8.dp))

        TwilightStageBlock(
            name = "航海晨昏蒙影 (Nautical)",
            angleTag = "-6° ~ -12°",
            dawnRange = "${detail.nauticalTwilight.dawnStartStr} ~ ${detail.nauticalTwilight.dawnEndStr}",
            duskRange = "${detail.nauticalTwilight.duskStartStr} ~ ${detail.nauticalTwilight.duskEndStr}",
            desc = "海天分界线依然可辨，明亮恒星清晰显现，水手可借星定位。"
        )

        Spacer(modifier = Modifier.height(8.dp))

        TwilightStageBlock(
            name = "天文晨昏蒙影 (Astronomical)",
            angleTag = "-12° ~ -18°",
            dawnRange = "${detail.astronomicalTwilight.dawnStartStr} ~ ${detail.astronomicalTwilight.dawnEndStr}",
            duskRange = "${detail.astronomicalTwilight.duskStartStr} ~ ${detail.astronomicalTwilight.duskEndStr}",
            desc = "天光彻底融入深邃暗夜，暗弱星体全数显现，深空观测极佳。"
        )
    }
}

/** 单个晨昏蒙影阶段结构化条目 */
@Composable
private fun TwilightStageBlock(
    name: String,
    angleTag: String,
    dawnRange: String,
    duskRange: String,
    desc: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E3A8A).copy(alpha = 0.6f)
                ) {
                    Text(
                        text = angleTag,
                        color = Color(0xFF93C5FD),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "晨光：$dawnRange",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.5.sp
                )
                Text(
                    text = "昏影：$duskRange",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.5.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = desc,
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}

/**
 * 摄影绝美光影时刻指南卡片（完全解决卡片紧凑与裁切问题）
 *
 * @param detail 太阳天文数据 [SolarDayDetail]
 */
@Composable
private fun PhotographyGuideCard(detail: SolarDayDetail) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x6014263D))
            .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = Color(0xFFFFB74D),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "摄影绝美光影时刻（Photography Hours）",
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 黄金时刻
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x353E2B1F))
                    .border(0.8.dp, Color(0xFFFFB74D).copy(alpha = 0.40f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "黄金时刻 (Golden)",
                        color = Color(0xFFFFB74D),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "晨间：${detail.goldenHourMorning}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "傍晚：${detail.goldenHourEvening}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "太阳低角度斜射，暖金柔光，人像与风光影调极佳",
                    color = Color.White.copy(alpha = 0.60f),
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp
                )
            }

            // 蓝调时刻
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x351E293B))
                    .border(0.8.dp, Color(0xFF60A5FA).copy(alpha = 0.40f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "蓝调时刻 (Blue)",
                        color = Color(0xFF60A5FA),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "晨间：${detail.blueHourMorning}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "傍晚：${detail.blueHourEvening}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "天空呈现深邃纯净冷蓝，城市夜景冷暖对比绝美",
                    color = Color.White.copy(alpha = 0.60f),
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

/**
 * 太阳与四季节气天文科普卡片（安全留白充足，彻底消除圆角截断）
 */
@Composable
private fun SolarScienceGuideCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x6014263D))
            .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF64B5F6),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "太阳与二分二至天文节气",
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ScienceFactItem(title = "春分 (3月20日前后)", desc = "太阳直射赤道，全球昼夜等长各 12 小时。")
            ScienceFactItem(title = "夏至 (6月21日前后)", desc = "太阳直射北回归线 (+23.44°)，北半球迎来全年最长白昼与最短黑夜。")
            ScienceFactItem(title = "秋分 (9月22日前后)", desc = "太阳再次直射赤道，昼夜再度全球平分。")
            ScienceFactItem(title = "冬至 (12月21日前后)", desc = "太阳直射南回归线 (-23.44°)，北半球迎来全年最短白昼与最长黑夜。")
        }
    }
}

/** 单条科学事实组件 */
@Composable
private fun ScienceFactItem(title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            color = Color(0xFFFFD54F),
            fontSize = 12.sp,
            modifier = Modifier.padding(end = 6.dp)
        )
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = desc,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

/**
 * 绘制天球拱弧与拟真发光太阳的 Canvas
 *
 * @param detail 太阳天文数据 [SolarDayDetail]
 * @param isToday 是否为今天
 * @param modifier 外部修饰符 [Modifier]
 */
@Composable
private fun SolarSkyArcCanvas(
    detail: SolarDayDetail,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val arcPath = remember { Path() }
    val passedPath = remember { Path() }
    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f) }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val startX = 16.dp.toPx()
        val endX = w - 16.dp.toPx()
        val horizonY = h - 16.dp.toPx()
        val arcHeight = h * 0.76f

        // 1. 绘制地平线渐变线
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.40f),
                    Color.White.copy(alpha = 0.40f),
                    Color.Transparent
                ),
                startX = startX - 8.dp.toPx(),
                endX = endX + 8.dp.toPx()
            ),
            start = Offset(startX - 8.dp.toPx(), horizonY),
            end = Offset(endX + 8.dp.toPx(), horizonY),
            strokeWidth = 1.5f,
            cap = StrokeCap.Round
        )

        // 2. 绘制完整白昼拱弧轨迹虚线
        arcPath.reset()
        arcPath.moveTo(startX, horizonY)
        arcPath.cubicTo(
            startX + (endX - startX) * 0.22f, horizonY - arcHeight * 1.10f,
            startX + (endX - startX) * 0.78f, horizonY - arcHeight * 1.10f,
            endX, horizonY
        )

        drawPath(
            path = arcPath,
            color = Color.White.copy(alpha = 0.28f),
            style = Stroke(
                width = 2.0f,
                cap = StrokeCap.Round,
                pathEffect = dashEffect
            )
        )

        // 3. 计算太阳渲染位置与已过轨迹光幕
        val progress = if (isToday) detail.dayProgress.coerceIn(0f, 1f) else 0.5f
        val sunX = startX + progress * (endX - startX)
        val sunY = horizonY - sin(progress * PI.toFloat()) * (arcHeight * 0.88f)

        if (isToday && detail.isSunAboveHorizon && progress > 0f) {
            passedPath.reset()
            passedPath.moveTo(startX, horizonY)
            val stepCount = (progress * 36).toInt().coerceAtLeast(2)
            for (i in 1..stepCount) {
                val t = (i.toFloat() / 36f).coerceAtMost(progress)
                val px = startX + t * (endX - startX)
                val py = horizonY - sin(t * PI.toFloat()) * (arcHeight * 0.88f)
                passedPath.lineTo(px, py)
            }
            passedPath.lineTo(sunX, horizonY)
            passedPath.close()

            drawPath(
                path = passedPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFB300).copy(alpha = 0.30f),
                        Color(0xFFFF8F00).copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    startY = sunY,
                    endY = horizonY
                )
            )
        }

        // 4. 地平线日出/日落两端点高亮锚点（日出晨金白点，日落浓郁晚霞赤橙）
        drawCircle(color = Color(0xFFFFCA28).copy(alpha = 0.6f), radius = 4.5.dp.toPx(), center = Offset(startX, horizonY))
        drawCircle(color = Color.White, radius = 2.2.dp.toPx(), center = Offset(startX, horizonY))

        drawCircle(color = Color(0xFFFF5722).copy(alpha = 0.65f), radius = 4.5.dp.toPx(), center = Offset(endX, horizonY))
        drawCircle(color = Color.White, radius = 2.2.dp.toPx(), center = Offset(endX, horizonY))

        // 5. 渲染发光太阳实体
        val renderCenter = if (isToday) {
            if (detail.isSunAboveHorizon) Offset(sunX, sunY) else Offset(startX, horizonY + 6.dp.toPx())
        } else {
            Offset(sunX, sunY)
        }

        drawDetailPhotorealisticSun(
            center = renderCenter,
            isNight = isToday && !detail.isSunAboveHorizon
        )
    }
}

/**
 * 绘制详情页高精度发光太阳
 *
 * @param center 太阳中心坐标 [Offset]
 * @param isNight 是否为夜间
 */
private fun DrawScope.drawDetailPhotorealisticSun(
    center: Offset,
    isNight: Boolean
) {
    if (isNight) {
        drawCircle(
            color = Color(0xFF64B5F6).copy(alpha = 0.35f),
            radius = 10.dp.toPx(),
            center = center
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.70f),
            radius = 3.5.dp.toPx(),
            center = center
        )
        return
    }

    val baseRadius = 10.dp.toPx()

    // 1. 最外层柔散天光日晕 (3.8x)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFB300).copy(alpha = 0.40f),
                Color(0xFFFF8F00).copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = center,
            radius = baseRadius * 3.8f
        ),
        radius = baseRadius * 3.8f,
        center = center
    )

    // 2. 紧贴日盘等离子日冕辉光 (1.9x)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF9C4),
                Color(0xFFFFCA28).copy(alpha = 0.75f),
                Color.Transparent
            ),
            center = center,
            radius = baseRadius * 1.9f
        ),
        radius = baseRadius * 1.9f,
        center = center
    )

    // 3. 衍射星芒
    for (i in 0 until 4) {
        val angleRad = (i * 45f) * (PI.toFloat() / 180f)
        val len = baseRadius * 2.2f
        val p1 = Offset(center.x + cos(angleRad) * len, center.y + sin(angleRad) * len)
        val p2 = Offset(center.x - cos(angleRad) * len, center.y - sin(angleRad) * len)
        drawLine(
            color = Color.White.copy(alpha = 0.60f),
            start = p1,
            end = p2,
            strokeWidth = 1.3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // 4. 实景日盘实体
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White,
                Color(0xFFFFF59D),
                Color(0xFFFFA000)
            ),
            center = center,
            radius = baseRadius
        ),
        radius = baseRadius,
        center = center
    )

    // 5. 极炽光核
    drawCircle(
        color = Color.White,
        radius = baseRadius * 0.45f,
        center = center
    )
}

/**
 * 绘制背景星空粒子装饰
 *
 * @param modifier 外部修饰符 [Modifier]
 */
@Composable
private fun StarlightBackgroundCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val starPoints = listOf(
            Triple(0.12f, 0.08f, 1.2f),
            Triple(0.85f, 0.12f, 1.0f),
            Triple(0.25f, 0.22f, 1.5f),
            Triple(0.70f, 0.28f, 0.8f),
            Triple(0.18f, 0.45f, 1.0f),
            Triple(0.90f, 0.52f, 1.3f),
            Triple(0.35f, 0.68f, 0.9f),
            Triple(0.80f, 0.78f, 1.4f),
            Triple(0.15f, 0.88f, 1.1f),
            Triple(0.65f, 0.92f, 0.8f)
        )

        starPoints.forEach { (nx, ny, r) ->
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = r.dp.toPx(),
                center = Offset(size.width * nx, size.height * ny)
            )
        }
    }
}

/**
 * 格式化剩余分钟数为 "X小时Y分"
 *
 * @param minutes 剩余分钟
 * @return 格式化字符串
 */
private fun formatRemainingMinutes(minutes: Int): String {
    val m = minutes.coerceAtLeast(0)
    val h = m / 60
    val min = m % 60
    return if (h > 0) "${h}小时${min}分" else "${min}分钟"
}

/**
 * 获取当前系统时间对应当天的分钟数
 *
 * @return 0~1439 分钟数
 */
private fun getNowMinutes(): Int {
    val cal = Calendar.getInstance()
    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
}

/**
 * 计算距离下一个日出的剩余分钟数
 *
 * @param sunriseMinutes 今日日出分钟
 * @param sunsetMinutes 今日日落分钟
 * @return 剩余分钟数
 */
private fun getNextSunriseRemaining(sunriseMinutes: Int, sunsetMinutes: Int): Int {
    val now = getNowMinutes()
    return if (now >= sunsetMinutes) {
        sunriseMinutes + 1440 - now
    } else {
        sunriseMinutes - now
    }.coerceAtLeast(0)
}
