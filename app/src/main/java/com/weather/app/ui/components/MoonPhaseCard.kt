package com.weather.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay
import java.util.Calendar
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
 * 5. 统一为 152.dp 标准高度与深灰蓝毛玻璃质感，与气象指标宫格其他卡片完美等高对齐。
 *
 * @param city 当前城市信息对象 [CityInfo]
 * @param modifier 外部修饰符 [Modifier]
 */
@Composable
fun MoonPhaseRealCard(
    city: CityInfo,
    modifier: Modifier = Modifier
) {
    // 实时系统时钟（支持系统时间修改及应用切回前台时即时刷新生效）
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
            currentSystemTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val calendar = remember(currentSystemTimeMillis / 10000L) {
        Calendar.getInstance().apply { timeInMillis = currentSystemTimeMillis }
    }

    val moonInfo = remember(city.getCacheKey(), currentSystemTimeMillis / 10000L) {
        SunMoonCalculator.calculateMoonPhaseInfo(city, calendar)
    }

    // 获取摄影级三维月球高清纹理（使用全局安全静态缓存，杜绝生命周期误回收）
    val moonBitmap = remember {
        LunarOpenGlRenderer.getOrRenderMoon(512)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(152.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x7514263A))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // 左侧主要信息区域（占左半侧空间）
        Column(
            modifier = Modifier
                .fillMaxWidth(0.58f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. 左上角：微型月相图标与标题
            Row(verticalAlignment = Alignment.CenterVertically) {
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

        // 右侧：摄影级 3D 动态月相渲染展示区（全宽卡片右侧居中展示，加大至 124dp）
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(124.dp)
            ) {
                val w = size.width
                val h = size.height
                val moonCenter = Offset(w / 2f, h / 2f)
                val moonRadius = (minOf(w, h) / 2f) * 0.92f

                // 1. 绘制月球背面深邃球体基底（保证暗面在夜空也有微弱球体体积感）
                drawCircle(
                    color = Color(0xFF0F1722),
                    radius = moonRadius,
                    center = moonCenter
                )

                // 2. 绘制 GPU OpenGL 高清程序化三维月面纹理
                moonBitmap?.let { bitmap ->
                    val dstSize = IntSize((moonRadius * 2f).toInt(), (moonRadius * 2f).toInt())
                    val dstOffset = IntOffset((moonCenter.x - moonRadius).toInt(), (moonCenter.y - moonRadius).toInt())
                    drawImage(
                        image = bitmap,
                        dstOffset = dstOffset,
                        dstSize = dstSize
                    )
                }

                // 3. 动态晨昏线曲面物理阴影与羽化过渡层
                drawCardLunarShadow(
                    moonCenter = moonCenter,
                    moonRadius = moonRadius,
                    phase = moonInfo.moonPhase
                )

                // 4. 月球外圆周极细微柔光描边
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f),
                    radius = moonRadius,
                    center = moonCenter,
                    style = Stroke(width = 0.8f)
                )
            }
        }
    }
}

/**
 * 绘制卡片左上角微型月相指示图标
 *
 * @param phase 归一化月相周期值（0.0f ~ 1.0f）
 * @param modifier 外部修饰符 [Modifier]
 */
@Composable
private fun MiniMoonPhaseIcon(
    phase: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val r = size.width / 2f
        val c = Offset(r, r)

        // 底层暗灰圆盘
        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = r,
            center = c
        )

        // 亮区根据月相绘制亮白色半球/月牙
        val p = (phase % 1f + 1f) % 1f
        val isWaxing = p < 0.50f
        val k = cos(2.0 * PI * p).toFloat()

        val outerRect = Rect(c.x - r, c.y - r, c.x + r, c.y + r)
        val brightPath = Path()

        if (isWaxing) {
            // 盈月：亮面在右
            val termX = (k * r).coerceIn(-r, r)
            val rx = kotlin.math.abs(termX).coerceAtLeast(0.01f)
            val termRect = Rect(c.x - rx, c.y - r, c.x + rx, c.y + r)

            // 从北极沿右半圆画到南极
            brightPath.arcTo(outerRect, -90f, 180f, false)
            // 从南极沿晨昏线画回北极
            if (termX >= 0f) {
                brightPath.arcTo(termRect, 90f, -180f, false)
            } else {
                brightPath.arcTo(termRect, 90f, 180f, false)
            }
            brightPath.close()
        } else {
            // 亏月：亮面在左
            val termX = (-k * r).coerceIn(-r, r)
            val rx = kotlin.math.abs(termX).coerceAtLeast(0.01f)
            val termRect = Rect(c.x - rx, c.y - r, c.x + rx, c.y + r)

            // 从北极沿左半圆画到南极
            brightPath.arcTo(outerRect, 270f, -180f, false)
            // 从南极沿晨昏线画回北极
            if (termX <= 0f) {
                brightPath.arcTo(termRect, 90f, 180f, false)
            } else {
                brightPath.arcTo(termRect, 90f, -180f, false)
            }
            brightPath.close()
        }

        drawPath(path = brightPath, color = Color.White.copy(alpha = 0.90f))
    }
}

/**
 * 绘制月相卡片中 3D 月相球体的天文学晨昏线曲面阴影与柔焦过渡
 *
 * @param moonCenter 月球中心坐标 [Offset]
 * @param moonRadius 月球圆盘半径 (px)
 * @param phase 归一化月相周期值 (0.0f ~ 1.0f)
 */
private fun DrawScope.drawCardLunarShadow(
    moonCenter: Offset,
    moonRadius: Float,
    phase: Float
) {
    val p = (phase % 1f + 1f) % 1f
    val k = cos(2.0 * PI * p).toFloat()
    val darkFraction = ((1f + k) / 2f).coerceIn(0f, 1f)

    // 满月窗口直接返回
    if (darkFraction <= 0.025f) return

    val brightWidthPx = moonRadius * (1f - k).coerceIn(0.01f, 2f)
    val maxFeatherAllowed = (brightWidthPx * 0.38f).coerceAtMost(moonRadius * 0.28f)
    val adaptScale = ((darkFraction - 0.025f) / 0.225f).coerceIn(0f, 1f)
    val featherPx = maxFeatherAllowed * adaptScale

    // 12 级渐进半透明微偏移曲面阴影层，暗面保留 22% 层次透光度展示月球月海
    val shadowLayers = listOf(
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

    shadowLayers.forEach { (offset, color) ->
        val scaledColor = if (adaptScale < 1f) color.copy(alpha = color.alpha * adaptScale) else color
        val path = createCardLunarShadowPath(moonCenter, moonRadius, phase, featherOffset = offset)
        drawPath(path = path, color = scaledColor)
    }

    // 晨昏线柔和过渡描边
    if (adaptScale > 0.05f) {
        val maxStroke = (brightWidthPx * 0.26f).coerceAtMost(moonRadius * 0.18f)
        val strokeLayers = listOf(
            Pair(featherPx * 0.65f, Pair(maxStroke * 1.00f * adaptScale, Color(0x0A101A26))),
            Pair(featherPx * 0.35f, Pair(maxStroke * 0.60f * adaptScale, Color(0x12101A26))),
            Pair(0f,                Pair(maxStroke * 0.30f * adaptScale, Color(0x18101A26)))
        )

        strokeLayers.forEach { (offset, strokeInfo) ->
            val (strokeWidth, color) = strokeInfo
            if (strokeWidth > 0.5f) {
                val scaledColor = if (adaptScale < 1f) color.copy(alpha = color.alpha * adaptScale) else color
                val arcPath = createCardTerminatorArcPath(moonCenter, moonRadius, phase, featherOffset = offset)
                drawPath(
                    path = arcPath,
                    color = scaledColor,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
    }
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
