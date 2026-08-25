package com.weather.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.model.CityInfo
import com.weather.app.model.WeatherData
import com.weather.app.ui.components.WeatherSkyBackground
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 城市管理全屏沉浸式界面组件
 *
 * 严格对齐设计要求：
 * 1. 背景色由当前天气主页色动态驱动、全屏沉浸式展示；
 * 2. 移除卡片表面删除按钮，界面更加清爽干净；
 * 3. 向左滑动卡片露出浅珊瑚粉色大圆角方块（深红棕色垃圾桶图标）；
 * 4. 点击该方块切换为深红棕色“✓”对勾图标（代表确定删除状态）；
 * 5. 再次点击“✓”对勾执行删除，并弹出底部“撤销”按钮；
 * 6. 底部提供“+ 添加城市”操作入口。
 *
 * @param visible 是否展开显示
 * @param weatherText 当前主页天气现象文本（用于动态驱动背景色）
 * @param savedCities 已保存的城市列表 [CityInfo]
 * @param weatherCache 各城市天气快照缓存
 * @param onCityClick 点击选中城市时的回调 (切换至该城市并关闭弹窗)
 * @param onDeleteCity 删除指定城市时的回调
 * @param onRestoreCity 撤销删除并恢复城市时的回调
 * @param onAddCityClick 点击底部“添加城市”按钮时的回调
 * @param onBackClick 点击返回按钮时的回调
 */
@Composable
fun CityManagementFullScreen(
    visible: Boolean,
    weatherText: String,
    savedCities: List<CityInfo>,
    weatherCache: Map<String, WeatherData>,
    onCityClick: (Int) -> Unit,
    onDeleteCity: (CityInfo) -> Unit,
    onRestoreCity: (CityInfo, Int) -> Unit,
    onAddCityClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 拦截物理返回键与侧滑返回手势，按下时自动收回抽屉
    BackHandler(enabled = visible) {
        onBackClick()
    }

    // 1. 半透明暗色背景遮罩层（独立淡入淡出，点击空白处收回抽屉）
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 280)),
        exit = fadeOut(animationSpec = tween(durationMillis = 260))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onBackClick)
        )
    }

    // 2. 抽屉式面板主体（从屏幕左侧滑入、向左侧滑出收起，纯正抽屉动效）
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(durationMillis = 280, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(durationMillis = 260, easing = androidx.compose.animation.core.FastOutLinearInEasing)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC182230), // 80% 半透明沉浸式磨砂底色
                            Color(0xCC202D3F),
                            Color(0xCC182230)
                        )
                    )
                )
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                // 顶部导航栏：返回按钮与“管理城市”标题
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.20f),
                        modifier = Modifier.size(38.dp)
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

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = "管理城市",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 城市卡片列表 (支持向左滑动露出粉色删除块，点击切换为勾选确定删除)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        items = savedCities,
                        key = { index, city -> "${city.code}_${city.name}_${city.isAutoLocated}_$index" }
                    ) { index, city ->
                        val key = city.code.ifEmpty { city.name }
                        val weather = weatherCache[key] ?: weatherCache[city.name]
                        val canDelete = savedCities.size > 1 && !city.isAutoLocated

                        SwipeableCityCard(
                            city = city,
                            weather = weather,
                            canDelete = canDelete,
                            onClick = { onCityClick(index) },
                            onDelete = {
                                val deletedCity = city
                                val deletedIndex = index
                                onDeleteCity(deletedCity)
                                coroutineScope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    val result = snackbarHostState.showSnackbar(
                                        message = "已删除【${deletedCity.name}】",
                                        actionLabel = "撤销",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        onRestoreCity(deletedCity, deletedIndex)
                                    }
                                }
                            }
                        )
                    }
                }

                // 底部居中“+ 添加城市”操作按钮
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onAddCityClick() }
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加城市",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "添加城市",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            // 底部“撤销删除”毛玻璃 Snackbar 提示条
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 88.dp, start = 16.dp, end = 16.dp),
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = Color(0xE620252D),
                        contentColor = Color.White,
                        actionColor = Color(0xFF64B5F6),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            )
        }
    }
}

/**
 * 支持两段式滑动确认删除的城市卡片组件
 *
 * 严格对齐设计与交互要求：
 * 1. 默认状态下卡片完全闭合，删除按钮不可见（不向外渗色/露角）；
 * 2. 仅在向左滑动时平滑显露右侧独立浅珊瑚粉色大圆角方块（深红棕色垃圾桶图标）；
 * 3. 拖拽释放或中断时自动弹性吸附至完全展开或完全关闭，杜绝停留在中间任意位置；
 * 4. 首次点击该方块切换为“✓”对勾图标（代表确定删除状态）；
 * 5. 再次点击“✓”对勾执行删除，并弹出底部撤销 Snackbar；
 * 6. 点击左侧卡片或右滑自动平滑收回。
 *
 * @param city 城市信息 [CityInfo]
 * @param weather 城市天气数据 [WeatherData]
 * @param canDelete 是否允许删除
 * @param onClick 点击查看城市天气回调
 * @param onDelete 确认删除回调
 */
@Composable
private fun SwipeableCityCard(
    city: CityInfo,
    weather: WeatherData?,
    canDelete: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var revealState by remember { mutableStateOf(0) } // 0: 闭合, 1: 垃圾桶状态, 2: 勾选✓确定删除状态
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val actionButtonWidth = 84.dp
    val spacing = 8.dp
    val density = LocalDensity.current
    val totalRevealPx = with(density) { (actionButtonWidth + spacing).toPx() }

    /**
     * 平滑弹性吸附至目标状态（0: 闭合, 1: 展开）
     *
     * @param targetState 目标展开状态 (0 或 1)
     */
    fun settleTo(targetState: Int) {
        revealState = targetState
        coroutineScope.launch {
            val targetPx = if (targetState == 0) 0f else -totalRevealPx
            offsetX.animateTo(
                targetValue = targetPx,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    val currentOffset = offsetX.value
    val revealProgress = (-currentOffset / totalRevealPx).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
    ) {
        // 右侧独立浅珊瑚粉色操作方块：仅在向左滑动或已展开时渲染，随滑动进度平滑淡入和缩放
        if (canDelete && revealProgress > 0.005f) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF6B8AB), // 浅珊瑚粉红
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(actionButtonWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        alpha = (revealProgress / 0.4f).coerceIn(0f, 1f)
                        scaleX = 0.70f + 0.30f * revealProgress
                        scaleY = 0.70f + 0.30f * revealProgress
                    }
                    .clickable {
                        if (revealState == 1) {
                            // 第一次点击：切换为勾选确定删除状态
                            revealState = 2
                        } else if (revealState == 2) {
                            // 第二次点击(勾选状态)：执行删除并平滑收起
                            settleTo(0)
                            onDelete()
                        }
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    AnimatedContent(
                        targetState = (revealState == 2),
                        transitionSpec = {
                            (fadeIn(tween(140)) + scaleIn(tween(140))).togetherWith(
                                fadeOut(tween(140)) + scaleOut(tween(140))
                            )
                        },
                        label = "DeleteConfirmIconTransition"
                    ) { isConfirm ->
                        if (isConfirm) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "确定删除",
                                tint = Color(0xFF6B1D16), // 深红棕色勾选
                                modifier = Modifier.size(34.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = Color(0xFF6B1D16), // 深红棕色垃圾桶
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        // 上层城市天空卡片 (支持流畅手势拖动与弹性吸附平移)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(canDelete) {
                    if (!canDelete) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = {
                            // 开始手势拖拽
                        },
                        onDragEnd = {
                            val shouldOpen = offsetX.value < -totalRevealPx * 0.35f
                            settleTo(if (shouldOpen) 1 else 0)
                        },
                        onDragCancel = {
                            val shouldOpen = offsetX.value < -totalRevealPx * 0.35f
                            settleTo(if (shouldOpen) 1 else 0)
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                val nextOffset = (offsetX.value + dragAmount).coerceIn(-totalRevealPx * 1.05f, 0f)
                                offsetX.snapTo(nextOffset)
                            }
                        }
                    )
                }
        ) {
            SavedCitySkyCard(
                city = city,
                weather = weather,
                onClick = {
                    if (revealState != 0 || offsetX.value < -1f) {
                        settleTo(0)
                    } else {
                        onClick()
                    }
                }
            )
        }
    }
}

/**
 * 具有拟真天空云彩质感的单城市卡片组件
 *
 * @param city 城市信息 [CityInfo]
 * @param weather 城市关联的实时天气数据 [WeatherData]
 * @param onClick 点击事件回调
 */
@Composable
private fun SavedCitySkyCard(
    city: CityInfo,
    weather: WeatherData?,
    onClick: () -> Unit
) {
    val tempText = if (weather != null) "${weather.current.temperature.toInt()}°C" else "--°C"
    val condText = weather?.current?.weatherText ?: "多云"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF2C6EA8).copy(alpha = 0.85f),
                        Color(0xFF4C8DC4).copy(alpha = 0.85f),
                        Color(0xFF75AEE0).copy(alpha = 0.85f)
                    )
                )
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：城市名称与定位图标
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = city.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal
                )

                if (city.isAutoLocated) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "定位城市",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 右侧：气温与现象 (大字号温度与现象说明，干净整洁)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = tempText,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = condText,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
            }
        }
    }
}
