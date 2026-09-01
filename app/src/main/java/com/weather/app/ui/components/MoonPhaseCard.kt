package com.weather.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.weather.app.model.CityInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.cos

/**
 * 首页 3D 拟真月相小卡片组件
 *
 * 1. 采用与气象指标宫格一致的 152.dp 标准高度与深灰蓝毛玻璃质感基底；
 * 2. 顶部微型月相图标 + “月相”标题 + 点击跳转详情箭头；
 * 3. 核心区域展示：
 *    - 左侧：大字月相名称（如“渐盈凸月”）、照明度百分比、底部“下次满月”日期提示；
 *    - 右侧：嵌入 3D 摄影级月球动态渲染球体（基于 OpenGL ES 纹理与晨昏线物理曲面阴影）；
 * 4. 支持点击整张卡片跳转全屏 3D 月相详情页面。
 *
 * @param city 当前城市信息对象 [CityInfo]
 * @param modifier 外部修饰符 [Modifier]
 * @param onClick 点击月相卡片跳转全屏详情回调函数
 */
@Composable
fun MoonPhaseRealCard(
    city: CityInfo,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    // 系统时钟状态：仅在进入前台 (ON_RESUME) 或每隔 15 分钟温和更新一次，彻底杜绝无意义的秒级高频重组
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
            delay(15 * 60 * 1000L)
            currentSystemTimeMillis = System.currentTimeMillis()
        }
    }

    val calendar = remember(currentSystemTimeMillis / 60000L) {
        Calendar.getInstance().apply { timeInMillis = currentSystemTimeMillis }
    }

    val moonInfo = remember(city.getCacheKey(), currentSystemTimeMillis / 60000L) {
        SunMoonCalculator.calculateMoonPhaseInfo(city, calendar)
    }

    // 获取摄影级三维月球高清纹理：优先秒开静态全局缓存，未就绪时在后台 Default 线程异步生成，零阻塞 UI 主线程
    val moonBitmap by produceState<ImageBitmap?>(initialValue = LunarOpenGlRenderer.getPrecachedMoon(256)) {
        if (value == null) {
            val bitmap = withContext(Dispatchers.Default) {
                LunarOpenGlRenderer.getOrRenderMoon(256)
            }
            value = bitmap
        }
    }

    val boxModifier = if (onClick != null) {
        modifier
            .height(152.dp)
            .graphicsLayer {
                clip = true
                shape = RoundedCornerShape(20.dp)
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x7514263A))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    } else {
        modifier
            .height(152.dp)
            .graphicsLayer {
                clip = true
                shape = RoundedCornerShape(20.dp)
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x7514263A))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    }

    Box(
        modifier = boxModifier
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. 顶部标题栏（图标 + 标题 + 跳转小箭头）
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MiniMoonPhaseIcon(
                        phase = moonInfo.moonPhase,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "月相",
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "查看月相详情",
                    tint = Color.White.copy(alpha = 0.40f),
                    modifier = Modifier.size(13.dp)
                )
            }

            // 2. 中间主要区域：居中大幅 3D 拟真动态月球 (80dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .drawWithCache {
                            val w = size.width
                            val h = size.height
                            val moonCenter = Offset(w / 2f, h / 2f)
                            val moonRadius = (minOf(w, h) / 2f) * 0.94f
                            val currentPhase = moonInfo.moonPhase

                            val shadowData = buildCardLunarShadowData(moonCenter, moonRadius, currentPhase)
                            val dstSize = IntSize((moonRadius * 2f).toInt(), (moonRadius * 2f).toInt())
                            val dstOffset = IntOffset((moonCenter.x - moonRadius).toInt(), (moonCenter.y - moonRadius).toInt())
                            val baseDarkColor = Color(0xFF0F1722)
                            val strokeRimColor = Color.White.copy(alpha = 0.15f)
                            val rimStrokeStyle = Stroke(width = 0.8f)

                            onDrawBehind {
                                drawCircle(
                                    color = baseDarkColor,
                                    radius = moonRadius,
                                    center = moonCenter
                                )

                                moonBitmap?.let { bitmap ->
                                    drawImage(
                                        image = bitmap,
                                        dstOffset = dstOffset,
                                        dstSize = dstSize
                                    )
                                }

                                shadowData.render(this)

                                drawCircle(
                                    color = strokeRimColor,
                                    radius = moonRadius,
                                    center = moonCenter,
                                    style = rimStrokeStyle
                                )
                            }
                        }
                )
            }

            // 3. 底部信息行：左右两端对齐展示【月相名称】与【月出/月落时间】（统一 11.5.sp 风格）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = moonInfo.phaseName,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1
                )

                val rightTimeText = if (moonInfo.moonriseTimeStr.isNotEmpty() && moonInfo.moonriseTimeStr != "--:--") {
                    "${moonInfo.moonriseTimeStr}月出"
                } else if (moonInfo.moonsetTimeStr.isNotEmpty() && moonInfo.moonsetTimeStr != "--:--") {
                    "${moonInfo.moonsetTimeStr}月落"
                } else {
                    val illumPercent = ((1f - kotlin.math.cos(moonInfo.moonPhase * 2f * Math.PI.toFloat())) * 50f).toInt().coerceIn(0, 100)
                    "照明度 $illumPercent%"
                }

                Text(
                    text = rightTimeText,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 绘制卡片左上角微型月相指示图标
 *
 * 采用 [Modifier.drawWithCache] 进行路径缓存，避免滑动时频繁创建 Path 与 Rect 对象。
 *
 * @param phase 归一化月相周期值（0.0f ~ 1.0f）
 * @param modifier 外部修饰符 [Modifier]
 */
@Composable
private fun MiniMoonPhaseIcon(
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

            val baseDarkColor = Color.White.copy(alpha = 0.35f)
            val brightFillColor = Color.White.copy(alpha = 0.90f)

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
 * 预构建的月相阴影层数据实体
 *
 * @property path 阴影几何路径 [Path]
 * @property color 阴影层绘制颜色 [Color]
 */
private data class LunarShadowLayer(
    val path: Path,
    val color: Color
)

/**
 * 预构建的晨昏线过渡描边层数据实体
 *
 * @property path 晨昏线曲线路径 [Path]
 * @property color 描边颜色 [Color]
 * @property strokeStyle 描边线型样式 [Stroke]
 */
private data class LunarStrokeLayer(
    val path: Path,
    val color: Color,
    val strokeStyle: Stroke
)

/**
 * 月相卡片阴影预计算缓存容器
 *
 * 聚合所有预计算好的填充层和描边层，通过单一方法执行极速零分配绘制。
 *
 * @property shadowLayers 渐进阴影填充层列表
 * @property strokeLayers 晨昏线柔和描边层列表
 */
private class LunarCardShadowData(
    val shadowLayers: List<LunarShadowLayer>,
    val strokeLayers: List<LunarStrokeLayer>
) {
    /**
     * 在指定绘制作用域中执行预计算的阴影与过渡线渲染
     *
     * @param drawScope 绘制目标作用域 [DrawScope]
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
 * 预构建月相卡片中 3D 月相球体的天文学晨昏线曲面阴影与柔焦过渡数据结构
 *
 * @param moonCenter 月球中心坐标 [Offset]
 * @param moonRadius 月球圆盘半径 (px)
 * @param phase 归一化月相周期值 (0.0f ~ 1.0f)
 * @return 预构建完成的阴影缓存数据 [LunarCardShadowData]
 */
private fun buildCardLunarShadowData(
    moonCenter: Offset,
    moonRadius: Float,
    phase: Float
): LunarCardShadowData {
    val p = (phase % 1f + 1f) % 1f
    val k = cos(2.0 * PI * p).toFloat()
    val darkFraction = ((1f + k) / 2f).coerceIn(0f, 1f)

    if (darkFraction <= 0.025f) {
        return LunarCardShadowData(emptyList(), emptyList())
    }

    val brightWidthPx = moonRadius * (1f - k).coerceIn(0.01f, 2f)
    val maxFeatherAllowed = (brightWidthPx * 0.38f).coerceAtMost(moonRadius * 0.28f)
    val adaptScale = ((darkFraction - 0.025f) / 0.225f).coerceIn(0f, 1f)
    val featherPx = maxFeatherAllowed * adaptScale

    // 渐进半透明微偏移曲面阴影层配置
    val layerConfigs = listOf(
        Pair(featherPx * 1.00f, Color(0x0C121D2B)),
        Pair(featherPx * 0.85f, Color(0x14121D2B)),
        Pair(featherPx * 0.70f, Color(0x1C121D2B)),
        Pair(featherPx * 0.55f, Color(0x22121D2B)),
        Pair(featherPx * 0.40f, Color(0x26121D2B)),
        Pair(featherPx * 0.28f, Color(0x28121D2B)),
        Pair(featherPx * 0.18f, Color(0x28121D2B)),
        Pair(featherPx * 0.10f, Color(0x26121D2B)),
        Pair(featherPx * 0.05f, Color(0x22121D2B)),
        Pair(0f,                Color(0x1E121D2B))
    )

    val shadowLayers = layerConfigs.map { (offset, color) ->
        val scaledColor = if (adaptScale < 1f) color.copy(alpha = color.alpha * adaptScale) else color
        val path = createCardLunarShadowPath(moonCenter, moonRadius, phase, featherOffset = offset)
        LunarShadowLayer(path = path, color = scaledColor)
    }

    val strokeLayers = mutableListOf<LunarStrokeLayer>()
    if (adaptScale > 0.05f) {
        val maxStroke = (brightWidthPx * 0.26f).coerceAtMost(moonRadius * 0.18f)
        val strokeConfigs = listOf(
            Pair(featherPx * 0.65f, Pair(maxStroke * 1.00f * adaptScale, Color(0x0A101A26))),
            Pair(featherPx * 0.35f, Pair(maxStroke * 0.60f * adaptScale, Color(0x12101A26))),
            Pair(0f,                Pair(maxStroke * 0.30f * adaptScale, Color(0x18101A26)))
        )

        strokeConfigs.forEach { (offset, strokeInfo) ->
            val (strokeWidth, color) = strokeInfo
            if (strokeWidth > 0.5f) {
                val scaledColor = if (adaptScale < 1f) color.copy(alpha = color.alpha * adaptScale) else color
                val arcPath = createCardTerminatorArcPath(moonCenter, moonRadius, phase, featherOffset = offset)
                strokeLayers.add(
                    LunarStrokeLayer(
                        path = arcPath,
                        color = scaledColor,
                        strokeStyle = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                )
            }
        }
    }

    return LunarCardShadowData(shadowLayers, strokeLayers)
}

/**
 * 构建卡片中月球暗面阴影路径
 *
 * @param moonCenter 月球中心坐标 [Offset]
 * @param moonRadius 月球圆盘半径 (px)
 * @param phase 归一化月相周期值 (0.0f ~ 1.0f)
 * @param featherOffset 晨昏线向亮区方向延伸的羽化偏移像素 (px)
 * @return 暗面几何路径 [Path]
 */
private fun createCardLunarShadowPath(
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
        // 盈月：暗面在左，亮面在右
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
        // 亏月：暗面在右，亮面在左
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
 * 构建卡片中月球晨昏线半椭圆曲线路径
 *
 * @param moonCenter 月球中心坐标 [Offset]
 * @param moonRadius 月球圆盘半径 (px)
 * @param phase 归一化月相周期值 (0.0f ~ 1.0f)
 * @param featherOffset 晨昏线向亮区方向延伸的羽化偏移像素 (px)
 * @return 晨昏线单条半椭圆曲线路径 [Path]
 */
private fun createCardTerminatorArcPath(
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
