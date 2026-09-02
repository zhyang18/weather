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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Reorder
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.weather.app.model.CityInfo
import com.weather.app.model.WeatherData
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.runtime.mutableFloatStateOf

/**
 * 城市列表拖拽排序状态管理类
 *
 * 性能设计说明：
 * - [dragOffset] 使用 [mutableFloatStateOf] 存储实时偏移值，在 graphicsLayer lambda 内读取时
 *   仅触发 RenderNode 重绘（Draw 阶段），不触发 Compose 重组（Composition 阶段）。
 * - [onDragStart] / [onDrag] 均为普通函数（非 suspend），直接同步赋值，
 *   彻底消除"每帧 launch 新协程 → Animatable.snapTo 互斥锁竞争"导致的向上拖动卡顿问题。
 * - 释放回弹通过局部 [Animatable] 实现，每帧回调更新 [_dragOffset] 驱动重绘。
 * - 边界判定与跨项交换逻辑保持不变。
 *
 * @property onMove 发生跨项调换时的回调函数，参数分别为原位置索引和目标位置索引
 */
class DragDropListState(
    private val onMove: (Int, Int) -> Unit
) {
    /**
     * 当前正在被拖拽的卡片唯一标识键，未拖拽时为 null
     */
    var draggingItemKey by mutableStateOf<String?>(null)
        private set

    /**
     * 当前正在被拖拽的卡片在列表中的实时索引，未拖拽时为 null
     */
    var draggingItemIndex by mutableStateOf<Int?>(null)
        private set

    /**
     * 被拖拽项 Y 轴实时偏移的内部状态。
     *
     * 使用 [mutableFloatStateOf] 而非 [Animatable]：
     * - 可在普通函数中直接同步赋值，无需协程，彻底避免多协程竞争互斥锁。
     * - 在 graphicsLayer lambda 内读取时，仅触发重绘（Draw），不触发重组（Composition）。
     */
    private val _dragOffset = mutableFloatStateOf(0f)

    /**
     * 被拖拽项当前 Y 轴像素偏移量，供 graphicsLayer 直接读取
     */
    val dragOffset: Float get() = _dragOffset.floatValue

    /**
     * 单个列表项步长高度（卡片高度 + 间距）像素值
     */
    var itemHeightPx by mutableStateOf(0f)

    /**
     * 判断指定标识键的卡片是否正处于拖拽状态
     *
     * @param key 卡片唯一标识键
     * @return 若当前卡片正处于拖拽中则返回 true，否则返回 false
     */
    fun isDragging(key: String): Boolean = draggingItemKey == key

    /**
     * 拖拽手势开始时的初始化处理，将偏移量立即归零。
     * 普通函数，无需协程，同步执行。
     *
     * @param key 当前被按住拖拽的城市卡片唯一标识键
     * @param index 当前被按住拖拽的城市卡片初始索引
     */
    fun onDragStart(key: String, index: Int) {
        draggingItemKey = key
        draggingItemIndex = index
        _dragOffset.floatValue = 0f
    }

    /**
     * 拖拽手势移动过程中的位移累加与跨项交换判定。
     *
     * 普通函数（非 suspend），直接同步赋值 [_dragOffset]，
     * 彻底避免旧方案中每次 onDrag 事件 launch 新协程、多协程竞争
     * [Animatable.snapTo] 互斥锁导致偏移量计算错乱、向上拖动卡住的问题。
     *
     * @param dragAmount 本次手势或滚动的 Y 轴移动增量像素值
     * @param itemCount 城市列表总数量
     */
    fun onDrag(dragAmount: Float, itemCount: Int) {
        var newOffset = _dragOffset.floatValue + dragAmount
        val stepHeight = if (itemHeightPx > 0f) itemHeightPx else 1f
        val threshold = stepHeight * 0.50f

        // 向上拖拽交换判定 (超过半个卡片高度立即交换并补偿步长)
        while (newOffset < -threshold && draggingItemIndex != null && draggingItemIndex!! > 0) {
            val currentIdx = draggingItemIndex!!
            val targetIndex = currentIdx - 1
            onMove(currentIdx, targetIndex)
            draggingItemIndex = targetIndex
            newOffset += stepHeight
        }

        // 向下拖拽交换判定 (超过半个卡片高度立即交换并补偿步长)
        while (newOffset > threshold && draggingItemIndex != null && draggingItemIndex!! < itemCount - 1) {
            val currentIdx = draggingItemIndex!!
            val targetIndex = currentIdx + 1
            onMove(currentIdx, targetIndex)
            draggingItemIndex = targetIndex
            newOffset -= stepHeight
        }

        // 视觉阻尼限位：超出列表边界时卡片最多偏移 0.85 个步长，手感自然，不强制回弹
        newOffset = newOffset.coerceIn(-stepHeight * 0.85f, stepHeight * 0.85f)

        // 直接同步赋值，仅触发 graphicsLayer 重绘，不触发 Recomposition，无协程开销
        _dragOffset.floatValue = newOffset
    }

    /**
     * 检查拖拽项是否处于列表边缘并执行平滑自动滚动与同步排序
     *
     * @param lazyListState 列表滚动状态实例 [LazyListState]
     * @param itemCount 城市列表总数量
     * @return 若执行了自动滚动则返回 true，否则返回 false
     */
    suspend fun checkAndAutoScroll(
        lazyListState: LazyListState,
        itemCount: Int
    ): Boolean {
        val key = draggingItemKey ?: return false
        val currentIndex = draggingItemIndex ?: return false
        val layoutInfo = lazyListState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        val stepHeight = if (itemHeightPx > 0f) itemHeightPx else 1f
        val viewportHeight = layoutInfo.viewportSize.height.toFloat()
        if (viewportHeight <= 0f) return false

        // 优先根据唯一 key 查找，若边缘重组瞬态未命中则通过 index 回退查找
        val currentVisibleItem = visibleItems.firstOrNull { it.key == key }
            ?: visibleItems.firstOrNull { it.index == currentIndex }

        val currentOffsetValue = _dragOffset.floatValue
        val (draggedTop, draggedBottom) = if (currentVisibleItem != null) {
            val top = currentVisibleItem.offset + currentOffsetValue
            top to (top + currentVisibleItem.size)
        } else {
            val estimatedTop = (currentIndex - (layoutInfo.visibleItemsInfo.firstOrNull()?.index
                ?: 0)) * stepHeight + currentOffsetValue
            estimatedTop to (estimatedTop + stepHeight)
        }

        val scrollThreshold = (stepHeight * 1.15f).coerceAtLeast(115f)
        val maxScrollSpeed = (stepHeight * 0.28f).coerceAtLeast(24f)

        val scrollDelta = when {
            draggedTop < scrollThreshold && (currentIndex > 0 || lazyListState.canScrollBackward) -> {
                if (lazyListState.canScrollBackward) {
                    val fraction =
                        ((scrollThreshold - draggedTop) / scrollThreshold).coerceIn(0.35f, 2.0f)
                    -maxScrollSpeed * fraction
                } else 0f
            }

            draggedBottom > viewportHeight - scrollThreshold && (currentIndex < itemCount - 1 || lazyListState.canScrollForward) -> {
                if (lazyListState.canScrollForward) {
                    val fraction =
                        ((draggedBottom - (viewportHeight - scrollThreshold)) / scrollThreshold).coerceIn(
                            0.35f, 2.0f
                        )
                    maxScrollSpeed * fraction
                } else 0f
            }

            else -> 0f
        }

        if (scrollDelta != 0f) {
            lazyListState.scrollBy(scrollDelta)
            // 列表平滑滚动时，滚动位移无缝驱动跨项排序更新（直接调用，无需协程）
            onDrag(dragAmount = scrollDelta, itemCount = itemCount)
            return true
        }
        return false
    }

    /**
     * 拖拽结束时的平滑弹簧回落归位逻辑。
     *
     * 使用局部 [Animatable] 从当前偏移值开始弹簧动画至 0，
     * 每帧通过 block 回调同步更新 [_dragOffset]，驱动 graphicsLayer 重绘。
     * 动画完成后清除拖拽状态，避免提前清除导致视觉闪烁。
     */
    suspend fun onDragInterrupted() {
        val startValue = _dragOffset.floatValue
        if (startValue != 0f) {
            // 局部 Animatable 做弹簧回弹，每帧同步更新 _dragOffset 驱动重绘
            Animatable(startValue).animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) {
                _dragOffset.floatValue = value
            }
        }
        // 动画完成后清除拖拽状态，避免提前清除导致视觉闪烁
        _dragOffset.floatValue = 0f
        draggingItemKey = null
        draggingItemIndex = null
    }
}


/**
 * 创建并记住城市列表拖拽排序状态实例
 *
 * @param onMove 跨项移动回调
 * @return 记忆的 [DragDropListState] 状态实例
 */
@Composable
fun rememberDragDropListState(onMove: (Int, Int) -> Unit): DragDropListState {
    return remember { DragDropListState(onMove) }
}

/**
 * 城市管理全屏沉浸式界面组件
 *
 * 严格对齐设计要求：
 * 1. 背景色由当前天气主页色动态驱动、全屏沉浸式展示；
 * 2. 顶部提供"排序 / 完成"切换入口，支持直观的城市顺序拖拽排序；
 * 3. 在排序模式下，每张城市卡片右侧展示拖动排序小图标，按住即可上下拖拽实时调整城市位置；
 * 4. 普通模式下向左滑动卡片露出浅珊瑚粉色大圆角方块（深红棕色垃圾桶图标）；
 * 5. 点击该方块切换为深红棕色"✓"对勾图标（代表确定删除状态）；
 * 6. 再次点击"✓"对勾执行删除，并弹出底部"撤销"按钮；
 * 7. 底部提供"+ 添加城市"操作入口。
 *
 * @param visible 是否展开显示
 * @param weatherText 当前主页天气现象文本（用于动态驱动背景色）
 * @param savedCities 已保存的城市列表 [CityInfo]
 * @param weatherCache 各城市天气快照缓存
 * @param onCityClick 点击选中城市时的回调 (切换至该城市并关闭弹窗)
 * @param onDeleteCity 删除指定城市时的回调
 * @param onRestoreCity 撤销删除并恢复城市时的回调
 * @param onMoveCity 调整城市显示顺序时的回调
 * @param onAddCityClick 点击底部"添加城市"按钮时的回调
 * @param onBackClick 点击返回按钮时的回调
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CityManagementFullScreen(
    visible: Boolean,
    weatherText: String,
    savedCities: List<CityInfo>,
    weatherCache: Map<String, WeatherData>,
    onCityClick: (Int) -> Unit,
    onDeleteCity: (CityInfo) -> Unit,
    onRestoreCity: (CityInfo, Int) -> Unit,
    onMoveCity: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onAddCityClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val lazyListState = rememberLazyListState()
    var isReorderMode by remember { mutableStateOf(false) }

    // 排序能力判断（保存城市数量 >= 2 时支持自由拖拽排序）
    val canReorder = savedCities.size >= 2

    // 拖拽排序状态管理
    val density = LocalDensity.current
    val itemHeightWithSpacingPx = with(density) { (86.dp + 12.dp).toPx() }
    val dragDropState = rememberDragDropListState(onMove = onMoveCity).apply {
        itemHeightPx = itemHeightWithSpacingPx
    }

    // 拖拽至列表边缘时自动平滑滚动并同步更新排序
    LaunchedEffect(dragDropState.draggingItemKey) {
        val currentKey = dragDropState.draggingItemKey ?: return@LaunchedEffect
        while (dragDropState.draggingItemKey == currentKey) {
            val scrolled = dragDropState.checkAndAutoScroll(
                lazyListState = lazyListState,
                itemCount = savedCities.size
            )
            if (scrolled) {
                kotlinx.coroutines.delay(16)
            } else {
                kotlinx.coroutines.delay(32)
            }
        }
    }

    // 拦截物理返回键与侧滑返回手势，按下时优先退出排序模式或收回抽屉
    BackHandler(enabled = visible) {
        if (isReorderMode) {
            isReorderMode = false
        } else {
            onBackClick()
        }
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
                .clickable(onClick = {
                    if (isReorderMode) isReorderMode = false else onBackClick()
                })
        )
    }

    // 2. 抽屉式面板主体（从屏幕左侧滑入、向左侧滑出收起，纯正抽屉动效）
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(
                durationMillis = 280,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(
                durationMillis = 260,
                easing = androidx.compose.animation.core.FastOutLinearInEasing
            )
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
                // 顶部导航栏：返回按钮、"管理城市"标题 与 "排序/完成"切换按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.20f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            IconButton(onClick = {
                                if (isReorderMode) isReorderMode = false else onBackClick()
                            }) {
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
                            text = if (isReorderMode) "调整城市顺序" else "管理城市",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    // 仅当可供调整顺序的城市数量 >= 2 时展示排序/完成操作按钮（自动定位城市固定首行不计入可调序项）
                    if (canReorder) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isReorderMode) Color(0xFF64B5F6).copy(alpha = 0.35f) else Color.White.copy(
                                alpha = 0.18f
                            ),
                            modifier = Modifier.clickable {
                                isReorderMode = !isReorderMode
                            }
                        ) {
                            Text(
                                text = if (isReorderMode) "完成" else "排序",
                                color = if (isReorderMode) Color(0xFF90CAF9) else Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 城市卡片列表 (支持排序模式拖动排序与普通模式向左滑动删除，支持边缘自动滚动)
                LazyColumn(
                    state = lazyListState,
                    // 排序模式下禁用用户手势滚动，防止 LazyColumn 向上滚动手势
                    // 与拖拽 handle 的 detectDragGestures 竞争（导致向上拖动卡住）
                    // lazyListState.scrollBy() 的程序化自动滚动不受影响
                    userScrollEnabled = !isReorderMode,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        items = savedCities,
                        key = { _, city -> city.getCacheKey() }
                    ) { index, city ->
                        val weather = weatherCache[city.getCacheKey()]
                            ?: weatherCache[city.code.ifEmpty { city.name }]
                            ?: weatherCache[city.name]
                        val canDelete = savedCities.size > 1 && !city.isAutoLocated
                        val isDragging = dragDropState.isDragging(city.getCacheKey())

                        SwipeableCityCard(
                            city = city,
                            weather = weather,
                            canDelete = canDelete,
                            isReorderMode = isReorderMode,
                            isDragging = isDragging,
                            dragDropState = dragDropState,
                            onDragStart = {
                                dragDropState.onDragStart(city.getCacheKey(), index)
                            },
                            onDrag = { amount ->
                                dragDropState.onDrag(
                                    dragAmount = amount,
                                    itemCount = savedCities.size
                                )
                            },
                            onDragEnd = {
                                coroutineScope.launch {
                                    dragDropState.onDragInterrupted()
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    dragDropState.onDragInterrupted()
                                }
                            },
                            onClick = {
                                if (!isReorderMode) {
                                    onCityClick(index)
                                }
                            },
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
                            },
                            modifier = if (isDragging) {
                                Modifier.zIndex(10f)
                            } else {
                                Modifier
                                    .zIndex(0f)
                                    .animateItemPlacement(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                            }
                        )
                    }
                }

                // 底部居中"+ 添加城市"操作按钮 (排序模式下隐藏，界面更专注)
                if (!isReorderMode) {
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
                } else {
                    Spacer(modifier = Modifier.height(18.dp))
                }
            }

            // 底部"撤销删除"毛玻璃 Snackbar 提示条
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
 * 支持两段式滑动确认删除与右侧手势拖拽排序的城市卡片组件
 *
 * 严格对齐设计与交互要求：
 * 1. 排序模式下：右侧展示拖动排序小图标，按住图标即可上下实时拖拽卡片调整位置；
 * 2. 默认模式下：卡片完全闭合，向左滑动露出浅珊瑚粉色大圆角方块（深红棕色垃圾桶图标）；
 * 3. 首次点击该方块切换为"✓"对勾图标（代表确定删除状态）；
 * 4. 再次点击"✓"对勾执行删除，并弹出底部撤销 Snackbar。
 *
 * 性能重构说明：
 * - 不再接收 `dragOffsetY: Float` 参数（避免每帧传入 Compose State 触发重组）。
 * - 改为接收 [DragDropListState] 引用，在 `graphicsLayer { }` lambda 内直接读取
 *   `dragDropState.dragOffset`，仅触发 RenderNode 重绘，不触发 Recomposition。
 * - 移除原 `animateFloatAsState` 对拖拽偏移的二次弹簧插值，消除跟手延迟感。
 *
 * @param city 城市信息 [CityInfo]
 * @param weather 城市天气数据 [WeatherData]
 * @param canDelete 是否允许删除
 * @param isReorderMode 是否处于城市排序调整模式
 * @param isDragging 是否当前正在被拖拽
 * @param dragDropState 拖拽排序状态实例，用于在 graphicsLayer 内直接读取偏移，避免重组
 * @param onDragStart 拖拽手势开始回调
 * @param onDrag 拖拽手势位移回调
 * @param onDragEnd 拖拽手势正常结束回调
 * @param onDragCancel 拖拽手势取消回调
 * @param onClick 点击查看城市天气回调
 * @param onDelete 确认删除回调
 * @param modifier 外部修饰符
 */
@Composable
private fun SwipeableCityCard(
    city: CityInfo,
    weather: WeatherData?,
    canDelete: Boolean,
    isReorderMode: Boolean = false,
    isDragging: Boolean = false,
    dragDropState: DragDropListState,
    onDragStart: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var revealState by remember { mutableStateOf(0) } // 0: 闭合, 1: 垃圾桶状态, 2: 勾选✓确定删除状态
    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val actionButtonWidth = 84.dp
    val spacing = 8.dp
    val density = LocalDensity.current
    val totalRevealPx = with(density) { (actionButtonWidth + spacing).toPx() }

    // 退出排序模式时自动收回偏移
    LaunchedEffect(isReorderMode) {
        if (isReorderMode && offsetX.value != 0f) {
            offsetX.snapTo(0f)
            revealState = 0
        }
    }

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
        modifier = modifier
            .fillMaxWidth()
            .height(86.dp)
            .graphicsLayer {
                // 核心修复：必须在 else 分支将 translationY 和 scale 严格归零/复位，
                // 否则当 isDragging 变为 false 时，RenderNode 会永久残留上一帧的偏移量，
                // 导致卡片脱离原本槽位悬在下方、在列表中间留下大片空白。
                if (isDragging) {
                    translationY = dragDropState.dragOffset
                    scaleX = 1.02f
                    scaleY = 1.02f
                } else {
                    translationY = 0f
                    scaleX = 1.0f
                    scaleY = 1.0f
                }
            }
    ) {
        // 普通模式下：右侧独立浅珊瑚粉色操作方块
        if (!isReorderMode && canDelete && revealProgress > 0.005f) {
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

        // 上层城市天空卡片
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    if (isReorderMode) IntOffset.Zero else IntOffset(offsetX.value.roundToInt(), 0)
                }
                .pointerInput(canDelete, isReorderMode) {
                    if (!canDelete || isReorderMode) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = {},
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
                                val nextOffset =
                                    (offsetX.value + dragAmount).coerceIn(-totalRevealPx * 1.05f, 0f)
                                offsetX.snapTo(nextOffset)
                            }
                        }
                    )
                }
        ) {
            SavedCitySkyCard(
                city = city,
                weather = weather,
                isReorderMode = isReorderMode,
                isDragging = isDragging,
                onDragStart = onDragStart,
                onDrag = onDrag,
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel,
                onClick = {
                    if (!isReorderMode && (revealState != 0 || offsetX.value < -1f)) {
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
 * @param isReorderMode 是否处于排序模式
 * @param isDragging 是否正在被拖拽
 * @param onDragStart 拖拽手势开始回调
 * @param onDrag 拖拽手势移动回调
 * @param onDragEnd 拖拽手势正常结束回调
 * @param onDragCancel 拖拽手势取消回调
 * @param onClick 点击事件回调
 */
@Composable
private fun SavedCitySkyCard(
    city: CityInfo,
    weather: WeatherData?,
    isReorderMode: Boolean = false,
    isDragging: Boolean = false,
    onDragStart: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onClick: () -> Unit
) {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)

    val tempText = if (weather != null) "${weather.current.temperature.toInt()}°C" else "--°C"
    val condText = weather?.current?.weatherText ?: "多云"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .shadow(
                elevation = if (isDragging) 8.dp else 0.dp,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF2C6EA8).copy(alpha = if (isDragging) 0.95f else 0.85f),
                        Color(0xFF4C8DC4).copy(alpha = if (isDragging) 0.95f else 0.85f),
                        Color(0xFF75AEE0).copy(alpha = if (isDragging) 0.95f else 0.85f)
                    )
                )
            )
            .clickable(enabled = !isReorderMode) { onClick() }
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧：城市名称与定位图标 (配置 weight(1f) 与 padding(end = 12.dp)，超长时自动换行，绝不挤占右侧温度区域)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = city.name,
                    color = Color.White,
                    fontSize = if (city.name.length > 7) 17.sp else 20.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp,
                    modifier = Modifier.weight(1f, fill = false)
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

            // 右侧：气温与现象 + 排序小图标
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = tempText,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = condText,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }

                // 排序模式下展示右侧可拖拽排序小图标 (所有城市均支持拖拽移动调整顺序)
                if (isReorderMode) {
                    Surface(
                        shape = CircleShape,
                        color = if (isDragging) Color(0xFF64B5F6).copy(alpha = 0.45f) else Color.White.copy(
                            alpha = 0.22f
                        ),
                        modifier = Modifier
                            .size(42.dp)
                            .pointerInput(city.getCacheKey()) {
                                // 在 pointerInput 协程内直接调用 suspend 版本的拖拽回调，
                                // 避免通过 Compose State 传递偏移值。
                                detectDragGestures(
                                    onDragStart = { currentOnDragStart() },
                                    onDragEnd = { currentOnDragEnd() },
                                    onDragCancel = { currentOnDragCancel() },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        currentOnDrag(dragAmount.y)
                                    }
                                )
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Reorder,
                                contentDescription = "拖动排序",
                                tint = if (isDragging) Color(0xFFE3F2FD) else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
