package com.weather.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.cos

/**
 * 摄影级 3D 真实月相卡片组件
 *
 * 100% 对齐视觉设计图规范：
 * 1. 左上角展示微型月相小图标与“月相”标题；
 * 2. 左侧主区域大字展示当前月相天文学中文名称（如“渐盈凸月”、“满月”、“新月”等）；
 * 3. 左侧下方展示月出时刻与下次满月公历日期，中间附有半透明纤细分割线；
 * 4. 右侧依托 OpenGL ES 纯代码程序化渲染器 [LunarOpenGlRenderer] 渲染 3D 灰白真实月面，
 *    并依据 J2000 朔望周期结合晨昏线曲面阴影算法动态呈现真实月相盈亏形态；
 * 5. 全面使用 [Modifier.drawWithCache] 与离屏预计算数据模型 [LunarCardShadowData]，
 *    在主页上下滑动期间实现 0 内存分配与极速 60fps/120fps 满帧流畅体验；
 * 6. 统一为 152.dp 标准高度与深灰蓝毛玻璃质感，与气象指标宫格其他卡片完美等高对齐；
 * 7. 支持点击跳转月相全屏详情页面。
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
    val moonBitmap by produceState<ImageBitmap?>(initialValue = LunarOpenGlRenderer.getPrecachedMoon(384)) {
        if (value == null) {
            val bitmap = withContext(Dispatchers.Default) {
                LunarOpenGlRenderer.getOrRenderMoon(384)
            }
            value = bitmap
        }
    }

    val boxModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .height(152.dp)
            .graphicsLayer {
                clip = true
                shape = RoundedCornerShape(20.dp)
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x7514263A))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    } else {
        modifier
            .fillMaxWidth()
            .height(152.dp)
            .graphicsLayer {
                clip = true
                shape = RoundedCornerShape(20.dp)
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x7514263A))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    }

    Box(
        modifier = boxModifier
    ) {
        // 左侧主要信息区域（占左半侧空间）
        Column(
            modifier = Modifier
                .fillMaxWidth(0.58f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. 左上角：微型月相图标与标题及跳转箭头
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniMoonPhaseIcon(
                    phase = moonInfo.moonPhase,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "月相",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
                if (onClick != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "查看月相详情",
                        tint = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // 2. 主标题：月相名称（如“渐盈凸月”）
            Text(
                text = moonInfo.phaseName,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 26.sp
            )

            // 3. 底部信息列表（月出时间 + 分割线 + 下次满月）
            Column(modifier = Modifier.fillMaxWidth()) {
                // 月出时刻行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "月出",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = moonInfo.moonriseTimeStr,
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 纤细半透明水平分割线
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.6.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                )

                Spacer(modifier = Modifier.height(4.dp))

                // 下次满月公历日期行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "下次满月",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = moonInfo.nextFullMoonDateStr,
                        color = Color.White.copy(alpha = 0.90f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        // 右侧：摄影级 3D 动态月相渲染展示区（全宽卡片右侧居中展示，124dp）
        // 核心性能优化：基于 drawWithCache 进行几何路径与着色缓存，滚动期间零对象分配，消除掉帧
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .size(124.dp)
                .drawWithCache {
                    val w = size.width
                    val h = size.height
                    val moonCenter = Offset(w / 2f, h / 2f)
                    val moonRadius = (minOf(w, h) / 2f) * 0.92f
                    val currentPhase = moonInfo.moonPhase

                    // 在 Cache 阶段预先构建并缓存晨昏线阴影路径数据，避免在滑动时每帧产生 GC 分配
                    val shadowData = buildCardLunarShadowData(moonCenter, moonRadius, currentPhase)

                    val dstSize = IntSize((moonRadius * 2f).toInt(), (moonRadius * 2f).toInt())
                    val dstOffset = IntOffset((moonCenter.x - moonRadius).toInt(), (moonCenter.y - moonRadius).toInt())
                    val baseDarkColor = Color(0xFF0F1722)
                    val strokeRimColor = Color.White.copy(alpha = 0.12f)
                    val rimStrokeStyle = Stroke(width = 0.8f)

                    onDrawBehind {
                        // 1. 绘制月球背面深邃球体基底（保证暗面在夜空也有微弱球体体积感）
                        drawCircle(
                            color = baseDarkColor,
                            radius = moonRadius,
                            center = moonCenter
                        )

                        // 2. 绘制 GPU OpenGL 高清程序化三维月面纹理
                        moonBitmap?.let { bitmap ->
                            drawImage(
                                image = bitmap,
                                dstOffset = dstOffset,
                                dstSize = dstSize
                            )
                        }

                        // 3. 极速绘制预构建的晨昏线曲面物理阴影（纯路径绘制，零分配）
                        shadowData.render(this)

                        // 4. 月球外圆周极细微柔光描边
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
            val isWaxing = p < 0.50f
            val k = cos(2.0 * PI * p).toFloat()

            val brightPath = Path().apply {
                if (isWaxing) {
                    val termX = (k * r).coerceIn(-r, r)
                    val rx = kotlin.math.abs(termX).coerceAtLeast(0.01f)
                    val termRect = Rect(c.x - rx, c.y - r, c.x + rx, c.y + r)
                    arcTo(outerRect, -90f, 180f, false)
                    if (termX >= 0f) {
                        arcTo(termRect, 90f, -180f, false)
                    } else {
                        arcTo(termRect, 90f, 180f, false)
                    }
                    close()
                } else {
                    val termX = (-k * r).coerceIn(-r, r)
                    val rx = kotlin.math.abs(termX).coerceAtLeast(0.01f)
                    val termRect = Rect(c.x - rx, c.y - r, c.x + rx, c.y + r)
                    arcTo(outerRect, 270f, -180f, false)
                    if (termX <= 0f) {
                        arcTo(termRect, 90f, 180f, false)
                    } else {
                        arcTo(termRect, 90f, -180f, false)
                    }
                    close()
                }
            }

            val baseDarkColor = Color.White.copy(alpha = 0.35f)
            val brightFillColor = Color.White.copy(alpha = 0.90f)

            onDrawBehind {
                drawCircle(
                    color = baseDarkColor,
                    radius = r,
                    center = c
                )
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
