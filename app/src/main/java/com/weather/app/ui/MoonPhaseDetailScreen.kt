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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.model.CityInfo
import com.weather.app.ui.components.LunarOpenGlRenderer
import com.weather.app.util.LunarAstroCalculator
import com.weather.app.util.LunarDayDetail
import com.weather.app.util.MajorMoonPhase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 摄影级 3D 月相与天体运行全屏沉浸式详情交互页面
 *
 * 具备以下核心天文学与交互能力：
 * 1. 沉浸式深空星光背景与大尺寸 3D 程序化月球主舞台（基于 [LunarOpenGlRenderer] 与物理晨昏线阴影）；
 * 2. 交互式前后 30 天连续时间轴，支持滑动或点选任意日期实时观察月相形态演变；
 * 3. 关键月相四相节点（新月、上弦月、满月、下弦月）精准发生日期与倒计时；
 * 4. 全套天体物理指标：月出月落、过中天时刻、地平高度角与方位角、地月轨道距离、黄道星座归属、观星指数；
 * 5. 30 天连续月相周期轮播条与天文学月相/潮汐科普指南。
 *
 * @param visible 是否展示月相详情页面
 * @param city 当前聚焦城市信息对象 [CityInfo]
 * @param onBackClick 点击返回或系统返回触发回调
 * @param modifier 外部修饰符 [Modifier]
 */
@Composable
fun MoonPhaseDetailScreen(
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
        MoonPhaseDetailContent(
            city = city,
            onBackClick = onBackClick
        )
    }
}

/**
 * 月相详情核心内容与多维天文学组件滚动视图
 *
 * @param city 城市信息对象 [CityInfo]
 * @param onBackClick 返回事件回调
 * @param modifier 外部修饰符
 */
@Composable
private fun MoonPhaseDetailContent(
    city: CityInfo?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // 基准日历（当前系统时钟）
    val todayCalendar = remember { Calendar.getInstance() }

    // 当前选中的相对偏移天数（0 表示今天，负数过去，正数未来）
    var selectedDayOffset by remember { mutableIntStateOf(0) }

    // 计算选中日期的日历对象
    val activeCalendar = remember(selectedDayOffset) {
        (todayCalendar.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, selectedDayOffset)
        }
    }

    // 选中日期的月相完整详细数据
    val activeLunarDetail = remember(city?.getCacheKey(), selectedDayOffset) {
        LunarAstroCalculator.calculateLunarDayDetail(city, activeCalendar)
    }

    // 未来 4 个关键主要月相节点（基于今天推算）
    val majorPhases = remember(city?.getCacheKey()) {
        LunarAstroCalculator.calculateMajorMoonPhases(todayCalendar)
    }

    // 30 天连续月相序列（前 3 天 ~ 后 27 天）
    val monthSequence = remember(city?.getCacheKey()) {
        LunarAstroCalculator.generate30DaysSequence(city, todayCalendar, pastDays = 3, futureDays = 27)
    }

    // 摄影级 512px 超高清三维月球纹理
    val heroMoonBitmap by produceState<ImageBitmap?>(initialValue = LunarOpenGlRenderer.getPrecachedMoon(512)) {
        if (value == null) {
            val bitmap = withContext(Dispatchers.Default) {
                LunarOpenGlRenderer.getOrRenderMoon(512)
            }
            value = bitmap
        }
    }

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
            // 1. 顶部沉浸式导航栏
            MoonDetailTopBar(
                cityName = city?.name ?: "北京",
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
                // 3D 月相主舞台与日期切换
                HeroInteractiveMoonStage(
                    lunarDetail = activeLunarDetail,
                    moonBitmap = heroMoonBitmap,
                    onPrevDayClick = { selectedDayOffset -= 1 },
                    onNextDayClick = { selectedDayOffset += 1 }
                )

                // 朔望四相关键时间节点
                MajorMoonPhasesSection(majorPhases = majorPhases)

                // 30 天月相周期日历轮播
                MoonCycleCarouselSection(
                    sequence = monthSequence,
                    selectedOffset = selectedDayOffset,
                    onSelectDayOffset = { offset -> selectedDayOffset = offset }
                )

                // 月球天文详细指标网格
                LunarAstrometricsGrid(lunarDetail = activeLunarDetail)

                // 暗夜观星与天文摄影指南卡片
                StargazingGuideCard(lunarDetail = activeLunarDetail)

                // 月相天文学科普卡片
                MoonScienceGuideCard()

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

/**
 * 顶部沉浸式导航栏
 *
 * @param cityName 城市名称
 * @param isToday 当前选中的是否为今天
 * @param onBackClick 返回按钮点击回调
 * @param onResetTodayClick 重置回今天按钮点击回调
 */
@Composable
private fun MoonDetailTopBar(
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
        // 左侧返回按钮（绝对靠左）
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
                text = "月相与天象",
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
 * 摄影级 3D 拟真交互月相主舞台
 *
 * @param lunarDetail 当前选中的月相天文详细实体 [LunarDayDetail]
 * @param moonBitmap 月球 OpenGL 高清纹理位图 [ImageBitmap]
 * @param onPrevDayClick 切换至前一天点击回调
 * @param onNextDayClick 切换至后一天点击回调
 */
@Composable
private fun HeroInteractiveMoonStage(
    lunarDetail: LunarDayDetail,
    moonBitmap: ImageBitmap?,
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
                    text = "${lunarDetail.dateMonthDayStr} ${lunarDetail.dayOfWeekStr}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${lunarDetail.lunarYearGanZhi} · ${lunarDetail.lunarDateStr}",
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

        // 3D 真实月球大尺寸渲染区 (210dp)
        Box(
            modifier = Modifier
                .size(210.dp)
                .drawWithCache {
                    val w = size.width
                    val h = size.height
                    val moonCenter = Offset(w / 2f, h / 2f)
                    val moonRadius = (minOf(w, h) / 2f) * 0.92f
                    val currentPhase = lunarDetail.phaseValue

                    val shadowData = buildHeroLunarShadowData(moonCenter, moonRadius, currentPhase)
                    val dstSize = IntSize((moonRadius * 2f).toInt(), (moonRadius * 2f).toInt())
                    val dstOffset = IntOffset((moonCenter.x - moonRadius).toInt(), (moonCenter.y - moonRadius).toInt())
                    val baseDarkColor = Color(0xFF0B121C)
                    val strokeRimColor = Color.White.copy(alpha = 0.15f)
                    val rimStrokeStyle = Stroke(width = 1.0f)

                    onDrawBehind {
                        // 1. 月面环境微弱夜光晕
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0x354B729F),
                                    Color(0x101C314C),
                                    Color.Transparent
                                ),
                                center = moonCenter,
                                radius = moonRadius * 1.35f
                            ),
                            radius = moonRadius * 1.35f,
                            center = moonCenter
                        )

                        // 2. 暗面球体基底
                        drawCircle(
                            color = baseDarkColor,
                            radius = moonRadius,
                            center = moonCenter
                        )

                        // 3. 高清三维程序化月球正面纹理
                        moonBitmap?.let { bitmap ->
                            drawImage(
                                image = bitmap,
                                dstOffset = dstOffset,
                                dstSize = dstSize
                            )
                        }

                        // 4. 天文学晨昏线曲面物理阴影
                        shadowData.render(this)

                        // 5. 月球外圆周柔光描边
                        drawCircle(
                            color = strokeRimColor,
                            radius = moonRadius,
                            center = moonCenter,
                            style = rimStrokeStyle
                        )
                    }
                }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 月相名称大标题
        Text(
            text = lunarDetail.phaseName,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 光照比例与月龄标签
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "光照 ${lunarDetail.illuminationPercent}%",
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "月龄 ${lunarDetail.moonAgeDays} 天",
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * 关键月相四相节点卡片容器（新月、上弦月、满月、下弦月）
 *
 * @param majorPhases 关键四相数据列表 [List] of [MajorMoonPhase]
 */
@Composable
private fun MajorMoonPhasesSection(
    majorPhases: List<MajorMoonPhase>
) {
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
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "关键月相",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "关键月相节点",
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
            majorPhases.forEach { phase ->
                MajorPhaseItem(
                    phase = phase,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 单个主要月相节点紧凑卡片
 *
 * @param phase 关键月相节点数据 [MajorMoonPhase]
 * @param modifier 外部修饰符
 */
@Composable
private fun MajorPhaseItem(
    phase: MajorMoonPhase,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 微型月相图标
        DetailMiniMoonIcon(
            phase = phase.phaseValue,
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = phase.name,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = phase.dateStr.substringBefore(" "),
            color = Color.White.copy(alpha = 0.70f),
            fontSize = 11.sp
        )

        val daysText = when (phase.daysRemaining) {
            0 -> "今天"
            1 -> "明天"
            else -> "${phase.daysRemaining}天后"
        }
        Text(
            text = daysText,
            color = if (phase.daysRemaining == 0) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.50f),
            fontSize = 10.sp
        )
    }
}

/**
 * 30 天月相周期日历水平滚动轮播组件
 *
 * @param sequence 连续 30 天月相详细列表 [List] of [LunarDayDetail]
 * @param selectedOffset 当前选中的偏移天数
 * @param onSelectDayOffset 点击选择日期偏移回调
 */
@Composable
private fun MoonCycleCarouselSection(
    sequence: List<LunarDayDetail>,
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
                imageVector = Icons.Default.Nightlight,
                contentDescription = "月相周期",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "30 天月相周期变化",
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "点击可切换上方月相",
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
                // 计算当前 item 的实际 offset（序列以 pastDays=3 开头，即 index 0 为 offset -3）
                val itemOffset = index - 3
                val isSelected = itemOffset == selectedOffset
                val isToday = itemOffset == 0

                Column(
                    modifier = Modifier
                        .width(54.dp)
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

                    DetailMiniMoonIcon(
                        phase = item.phaseValue,
                        modifier = Modifier.size(20.dp)
                    )

                    Text(
                        text = "${item.illuminationPercent.toInt()}%",
                        color = Color.White.copy(alpha = 0.80f),
                        fontSize = 10.sp
                    )

                    Text(
                        text = item.lunarDateStr.substringAfter("月"),
                        color = Color(0xFFFFD54F).copy(alpha = if (isSelected) 1f else 0.70f),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

/**
 * 月球天文详细物理指标网格 (2x3 矩阵)
 *
 * @param lunarDetail 月相天文指标实体 [LunarDayDetail]
 */
@Composable
private fun LunarAstrometricsGrid(
    lunarDetail: LunarDayDetail
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 第一行：月出时刻 & 月落时刻
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AstrometricCard(
                icon = Icons.Default.WbTwilight,
                title = "月出时刻",
                primaryValue = lunarDetail.moonriseTimeStr,
                secondaryValue = "当地月轮升出地平线",
                modifier = Modifier.weight(1f)
            )

            AstrometricCard(
                icon = Icons.Default.Nightlight,
                title = "月落时刻",
                primaryValue = lunarDetail.moonsetTimeStr,
                secondaryValue = "当地月轮沉入地平线",
                modifier = Modifier.weight(1f)
            )
        }

        // 第二行：过中天时刻 & 地平高度角与方位
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AstrometricCard(
                icon = Icons.Default.Schedule,
                title = "过中天时刻",
                primaryValue = lunarDetail.transitTimeStr,
                secondaryValue = "行经正南方最高点",
                modifier = Modifier.weight(1f)
            )

            AstrometricCard(
                icon = Icons.Default.Explore,
                title = "高度与方位",
                primaryValue = "${lunarDetail.altitudeDeg}°",
                secondaryValue = "${lunarDetail.azimuthDirectionStr} ${lunarDetail.azimuthDeg}°",
                modifier = Modifier.weight(1f)
            )
        }

        // 第三行：地月轨道距离 & 所在黄道星座
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AstrometricCard(
                icon = Icons.Default.Public,
                title = "地月距离",
                primaryValue = "${String.format(Locale.CHINA, "%,d", lunarDetail.earthMoonDistanceKm)} km",
                secondaryValue = lunarDetail.distanceStatusStr,
                modifier = Modifier.weight(1f)
            )

            AstrometricCard(
                icon = Icons.Default.Star,
                title = "黄道星座",
                primaryValue = lunarDetail.zodiacName,
                secondaryValue = "月球运行天区归属",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 单个天文指标毛玻璃小卡片
 *
 * @param icon 指标图标 [ImageVector]
 * @param title 指标标题
 * @param primaryValue 核心指标数值
 * @param secondaryValue 辅助说明文本
 * @param modifier 外部修饰符
 */
@Composable
private fun AstrometricCard(
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
 * 暗夜观星与深空天文摄影指南卡片
 *
 * @param lunarDetail 月相天文指标实体 [LunarDayDetail]
 */
@Composable
private fun StargazingGuideCard(
    lunarDetail: LunarDayDetail
) {
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "观星指数",
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "暗夜观星与天文摄影指南",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 星级展示
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (i in 1..5) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "星级",
                        tint = if (i <= lunarDetail.stargazingQuality) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.20f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = lunarDetail.stargazingDescription,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

/**
 * 天文科普知识条目数据模型
 *
 * @property title 知识点标题（如“朔望月周期”）
 * @property content 知识点详情说明文本
 */
private data class AstronomyKnowledgeItem(
    val title: String,
    val content: String
)

/**
 * 月相与潮汐天文学科普卡片
 *
 * 采用结构化列表与悬挂缩进排版，确保多行换行文字与第一行内容严格垂直左对齐。
 */
@Composable
private fun MoonScienceGuideCard() {
    val knowledgeItems = remember {
        listOf(
            AstronomyKnowledgeItem(
                title = "朔望月周期",
                content = "月球绕地球公转一个完整朔望周期平均为 29.53 天。新月（朔）日月同向，满月（望）日月相望。"
            ),
            AstronomyKnowledgeItem(
                title = "潮汐效应",
                content = "新月与满月时太阳与月球引潮力叠加产生「大潮」；上弦与下弦引力成直角相互削弱产生「小潮」。"
            ),
            AstronomyKnowledgeItem(
                title = "晨昏线观测",
                content = "月球明暗分界的晨昏线由于低角度太阳照射，是使用天文望远镜观测环形山与月海峡谷的最佳区域。"
            )
        )
    }

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
                contentDescription = "天文学知识",
                tint = Color(0xFF90CAF9),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "月相盈亏与天文知识",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            knowledgeItems.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        color = Color.White.copy(alpha = 0.60f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.90f))) {
                                append("${item.title}：")
                            }
                            append(item.content)
                        },
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 沉浸式星空微光背景画布
 *
 * @param modifier 外部修饰符
 */
@Composable
private fun StarlightBackgroundCanvas(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 静态伪随机星芒粒子（固定种子保持稳定，无动态重绘开销）
        val starCoords = listOf(
            Triple(0.12f, 0.08f, 1.2f),
            Triple(0.35f, 0.04f, 0.8f),
            Triple(0.78f, 0.07f, 1.4f),
            Triple(0.90f, 0.15f, 0.9f),
            Triple(0.08f, 0.22f, 0.7f),
            Triple(0.85f, 0.28f, 1.1f),
            Triple(0.24f, 0.38f, 1.3f),
            Triple(0.68f, 0.42f, 0.8f),
            Triple(0.15f, 0.55f, 1.0f),
            Triple(0.92f, 0.62f, 1.2f),
            Triple(0.40f, 0.70f, 0.7f),
            Triple(0.75f, 0.80f, 1.5f),
            Triple(0.18f, 0.88f, 0.9f),
            Triple(0.88f, 0.92f, 1.1f)
        )

        starCoords.forEach { (rx, ry, r) ->
            drawCircle(
                color = Color.White.copy(alpha = 0.40f),
                radius = r.dp.toPx(),
                center = Offset(w * rx, h * ry)
            )
        }
    }
}

/**
 * 详情页高精度微型月相图标
 *
 * @param phase 归一化月相周期值（0.0f ~ 1.0f，0.0 为新月，0.25 为上弦月，0.50 为满月，0.75 为下弦月）
 * @param modifier 外部修饰符
 */
@Composable
private fun DetailMiniMoonIcon(
    phase: Float,
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier = modifier.drawWithCache {
            val r = size.width / 2f
            val c = Offset(r, r)
            val outerRect = Rect(c.x - r, c.y - r, c.x + r, c.y + r)

            val p = (phase % 1f + 1f) % 1f
            val k = cos(2.0 * PI * p).toFloat()

            val brightPath = Path().apply {
                if (p in 0.02f..0.48f) {
                    // 渐盈阶段（上弦、峨眉月、凸月）：右半侧亮
                    val rx = (kotlin.math.abs(k) * r).coerceAtLeast(0.01f)
                    val termRect = Rect(c.x - rx, c.y - r, c.x + rx, c.y + r)
                    arcTo(outerRect, -90f, 180f, false)
                    if (k > 0f) {
                        // 峨眉月（亮面小于半圆）：晨昏线向右鼓起
                        arcTo(termRect, 90f, 180f, false)
                    } else {
                        // 凸月（亮面大于半圆）：晨昏线向左凹入
                        arcTo(termRect, 90f, -180f, false)
                    }
                    close()
                } else if (p in 0.52f..0.98f) {
                    // 渐亏阶段（下弦、亏凸月、残月）：左半侧亮
                    val rx = (kotlin.math.abs(k) * r).coerceAtLeast(0.01f)
                    val termRect = Rect(c.x - rx, c.y - r, c.x + rx, c.y + r)
                    arcTo(outerRect, 90f, 180f, false)
                    if (k > 0f) {
                        // 残月（亮面小于半圆）：晨昏线向左鼓起
                        arcTo(termRect, 270f, 180f, false)
                    } else {
                        // 亏凸月（亮面大于半圆）：晨昏线向右凹入
                        arcTo(termRect, 270f, -180f, false)
                    }
                    close()
                }
            }

            val baseDarkColor = Color.White.copy(alpha = 0.25f)
            val brightFillColor = Color.White.copy(alpha = 0.95f)

            onDrawBehind {
                // 1. 暗面底色（灰色圆盘）
                drawCircle(
                    color = baseDarkColor,
                    radius = r,
                    center = c
                )

                // 2. 满月极值处理（全亮）
                if (p in 0.48f..0.52f) {
                    drawCircle(
                        color = brightFillColor,
                        radius = r,
                        center = c
                    )
                    return@onDrawBehind
                }

                // 3. 新月极值处理（全暗）
                if (p < 0.02f || p > 0.98f) {
                    return@onDrawBehind
                }

                // 4. 其他中间月相
                drawPath(path = brightPath, color = brightFillColor)
            }
        }
    )
}

/**
 * 详情页主舞台阴影图层数据模型
 *
 * @property path 阴影几何路径 [Path]
 * @property color 阴影层绘制颜色 [Color]
 */
private data class HeroShadowLayer(
    val path: Path,
    val color: Color
)

/**
 * 详情页主舞台晨昏线描边图层数据模型
 *
 * @property path 描边几何路径 [Path]
 * @property color 描边颜色 [Color]
 * @property strokeStyle 描边线型样式 [Stroke]
 */
private data class HeroStrokeLayer(
    val path: Path,
    val color: Color,
    val strokeStyle: Stroke
)

/**
 * 详情页主舞台 3D 月相阴影数据结构
 *
 * @property shadowLayers 渐进阴影填充层列表
 * @property strokeLayers 晨昏线柔和描边层列表
 */
private class HeroLunarShadowData(
    val shadowLayers: List<HeroShadowLayer>,
    val strokeLayers: List<HeroStrokeLayer>
) {
    /**
     * 在目标绘制作用域内极速绘制预构建的阴影与过渡层
     *
     * @param drawScope 目标绘制作用域 [DrawScope]
     */
    fun render(drawScope: DrawScope) {
        shadowLayers.forEach { layer ->
            drawScope.drawPath(path = layer.path, color = layer.color)
        }
        strokeLayers.forEach { layer ->
            drawScope.drawPath(path = layer.path, color = layer.color, style = layer.strokeStyle)
        }
    }
}

/**
 * 预构建主舞台 3D 月球晨昏线多层曲面物理阴影与柔焦过渡数据
 *
 * @param moonCenter 月球中心坐标 [Offset]
 * @param moonRadius 月球圆盘半径 (px)
 * @param phase 归一化月相周期值 (0.0f ~ 1.0f)
 * @return 预构建完成的阴影缓存数据 [HeroLunarShadowData]
 */
private fun buildHeroLunarShadowData(
    moonCenter: Offset,
    moonRadius: Float,
    phase: Float
): HeroLunarShadowData {
    val p = (phase % 1f + 1f) % 1f
    val k = cos(2.0 * PI * p).toFloat()
    val darkFraction = ((1f + k) / 2f).coerceIn(0f, 1f)

    if (darkFraction <= 0.025f) {
        return HeroLunarShadowData(emptyList(), emptyList())
    }

    val brightWidthPx = moonRadius * (1f - k).coerceIn(0.01f, 2f)
    val maxFeatherAllowed = (brightWidthPx * 0.38f).coerceAtMost(moonRadius * 0.28f)
    val adaptScale = ((darkFraction - 0.025f) / 0.225f).coerceIn(0f, 1f)
    val featherPx = maxFeatherAllowed * adaptScale

    val layerConfigs = listOf(
        Pair(featherPx * 1.00f, Color(0x0E0B121C)),
        Pair(featherPx * 0.85f, Color(0x180B121C)),
        Pair(featherPx * 0.70f, Color(0x220B121C)),
        Pair(featherPx * 0.55f, Color(0x2A0B121C)),
        Pair(featherPx * 0.40f, Color(0x300B121C)),
        Pair(featherPx * 0.28f, Color(0x320B121C)),
        Pair(featherPx * 0.18f, Color(0x320B121C)),
        Pair(featherPx * 0.10f, Color(0x2F0B121C)),
        Pair(featherPx * 0.05f, Color(0x2A0B121C)),
        Pair(0f,                Color(0x240B121C))
    )

    val shadowLayers = layerConfigs.map { (offset, color) ->
        val scaledColor = if (adaptScale < 1f) color.copy(alpha = color.alpha * adaptScale) else color
        val path = createHeroLunarShadowPath(moonCenter, moonRadius, phase, featherOffset = offset)
        HeroShadowLayer(path = path, color = scaledColor)
    }

    val strokeLayers = mutableListOf<HeroStrokeLayer>()
    if (adaptScale > 0.05f) {
        val maxStroke = (brightWidthPx * 0.26f).coerceAtMost(moonRadius * 0.18f)
        val strokeConfigs = listOf(
            Pair(featherPx * 0.65f, Pair(maxStroke * 1.00f * adaptScale, Color(0x0C0E1724))),
            Pair(featherPx * 0.35f, Pair(maxStroke * 0.60f * adaptScale, Color(0x160E1724))),
            Pair(0f,                Pair(maxStroke * 0.30f * adaptScale, Color(0x1E0E1724)))
        )

        strokeConfigs.forEach { (offset, strokeInfo) ->
            val (strokeWidth, color) = strokeInfo
            if (strokeWidth > 0.5f) {
                val scaledColor = if (adaptScale < 1f) color.copy(alpha = color.alpha * adaptScale) else color
                val arcPath = createHeroTerminatorArcPath(moonCenter, moonRadius, phase, featherOffset = offset)
                strokeLayers.add(
                    HeroStrokeLayer(
                        path = arcPath,
                        color = scaledColor,
                        strokeStyle = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                )
            }
        }
    }

    return HeroLunarShadowData(shadowLayers, strokeLayers)
}

/**
 * 构建主舞台月球暗面阴影路径
 *
 * @param moonCenter 月球中心坐标 [Offset]
 * @param moonRadius 月球圆盘半径 (px)
 * @param phase 归一化月相周期值 (0.0f ~ 1.0f)
 * @param featherOffset 羽化偏移像素 (px)
 * @return 暗面几何路径 [Path]
 */
private fun createHeroLunarShadowPath(
    moonCenter: Offset,
    moonRadius: Float,
    phase: Float,
    featherOffset: Float = 0f
): Path {
    val cx = moonCenter.x
    val cy = moonCenter.y
    val r = moonRadius
    val p = (phase % 1f + 1f) % 1f
    val isWaxing = p < 0.50f
    val k = cos(2.0 * PI * p).toFloat()

    val outerRect = Rect(cx - r, cy - r, cx + r, cy + r)
    val path = Path()

    if (isWaxing) {
        val rawTermX = k * r + featherOffset
        val termX = rawTermX.coerceIn(-r, r)
        val rx = kotlin.math.abs(termX).coerceAtLeast(0.001f)
        val termRect = Rect(cx - rx, cy - r, cx + rx, cy + r)

        path.arcTo(outerRect, 270f, -180f, false)
        if (termX >= 0f) {
            path.arcTo(termRect, 90f, -180f, false)
        } else {
            path.arcTo(termRect, 90f, 180f, false)
        }
        path.close()
    } else {
        val rawTermX = -k * r - featherOffset
        val termX = rawTermX.coerceIn(-r, r)
        val rx = kotlin.math.abs(termX).coerceAtLeast(0.001f)
        val termRect = Rect(cx - rx, cy - r, cx + rx, cy + r)

        path.arcTo(outerRect, -90f, 180f, false)
        if (termX <= 0f) {
            path.arcTo(termRect, 90f, 180f, false)
        } else {
            path.arcTo(termRect, 90f, -180f, false)
        }
        path.close()
    }

    return path
}

/**
 * 构建主舞台月球晨昏线半椭圆曲线路径
 *
 * @param moonCenter 月球中心坐标 [Offset]
 * @param moonRadius 月球圆盘半径 (px)
 * @param phase 归一化月相周期值 (0.0f ~ 1.0f)
 * @param featherOffset 羽化偏移像素 (px)
 * @return 晨昏线曲线路径 [Path]
 */
private fun createHeroTerminatorArcPath(
    moonCenter: Offset,
    moonRadius: Float,
    phase: Float,
    featherOffset: Float = 0f
): Path {
    val cx = moonCenter.x
    val cy = moonCenter.y
    val r = moonRadius
    val p = (phase % 1f + 1f) % 1f
    val isWaxing = p < 0.50f
    val k = cos(2.0 * PI * p).toFloat()

    val path = Path()
    if (isWaxing) {
        val rawTermX = k * r + featherOffset
        val termX = rawTermX.coerceIn(-r, r)
        val rx = kotlin.math.abs(termX).coerceAtLeast(0.001f)
        val termRect = Rect(cx - rx, cy - r, cx + rx, cy + r)
        if (termX >= 0f) {
            path.arcTo(termRect, 90f, -180f, false)
        } else {
            path.arcTo(termRect, 90f, 180f, false)
        }
    } else {
        val rawTermX = -k * r - featherOffset
        val termX = rawTermX.coerceIn(-r, r)
        val rx = kotlin.math.abs(termX).coerceAtLeast(0.001f)
        val termRect = Rect(cx - rx, cy - r, cx + rx, cy + r)
        if (termX <= 0f) {
            path.arcTo(termRect, 90f, 180f, false)
        } else {
            path.arcTo(termRect, 90f, -180f, false)
        }
    }
    return path
}
