package com.weather.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 气象动态与高保真矢量图标组件
 *
 * 100% 精确对齐设计稿规范，纯 Canvas 绘制 12 种标准精美气象图标：
 * 晴、多云、阴、雷阵雨、雨、暴雨、雨夹雪、雪、霾、扬沙、雾、冰雹。
 *
 * @param weatherText 天气现象描述文本（如 "晴", "多云", "雷阵雨", "暴雨" 等）
 * @param modifier 外部修饰符
 * @param size 图标尺寸，默认 24.dp
 */
@Composable
fun WeatherDynamicIcon(
    weatherText: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            when {
                weatherText.contains("晴") && !weatherText.contains("多云") && !weatherText.contains("雨") && !weatherText.contains("雪") -> {
                    drawSunnyIcon(w, h)
                }
                weatherText.contains("多云") -> {
                    drawCloudyWithSunIcon(w, h)
                }
                weatherText.contains("阴") -> {
                    drawOvercastIcon(w, h)
                }
                weatherText.contains("雷") -> {
                    drawThunderstormIcon(w, h)
                }
                weatherText.contains("暴雨") || weatherText.contains("大雨") -> {
                    drawHeavyRainIcon(w, h)
                }
                weatherText.contains("雨夹雪") -> {
                    drawSleetIcon(w, h)
                }
                weatherText.contains("雨") -> {
                    drawLightRainIcon(w, h)
                }
                weatherText.contains("雪") -> {
                    drawSnowIcon(w, h)
                }
                weatherText.contains("霾") -> {
                    drawHazeIcon(w, h)
                }
                weatherText.contains("沙") || weatherText.contains("尘") || weatherText.contains("风") -> {
                    drawSandstormIcon(w, h)
                }
                weatherText.contains("雾") -> {
                    drawFogIcon(w, h)
                }
                weatherText.contains("冰雹") -> {
                    drawHailIcon(w, h)
                }
                else -> {
                    drawCloudyWithSunIcon(w, h)
                }
            }
        }
    }
}

/**
 * 绘制标准晴天太阳图标 (金黄圆球 + 8 根光芒射线)
 *
 * @param w 宽度 (px)
 * @param h 高度 (px)
 */
private fun DrawScope.drawSunnyIcon(w: Float, h: Float) {
    val cx = w / 2f
    val cy = h / 2f
    val sunColor = Color(0xFFFFB300)
    val coreR = w * 0.22f

    // 绘制太阳核心
    drawCircle(
        color = sunColor,
        radius = coreR,
        center = Offset(cx, cy)
    )

    // 绘制 8 根等距光芒
    val rayInnerR = w * 0.32f
    val rayOuterR = w * 0.44f
    for (i in 0 until 8) {
        val rad = (i * 45f) * (PI.toFloat() / 180f)
        val p1 = Offset(cx + rayInnerR * cos(rad), cy + rayInnerR * sin(rad))
        val p2 = Offset(cx + rayOuterR * cos(rad), cy + rayOuterR * sin(rad))
        drawLine(
            color = sunColor,
            start = p1,
            end = p2,
            strokeWidth = w * 0.08f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * 绘制标准多云图标 (饱满纯白云朵 + 右上角金黄小太阳及光线)
 *
 * @param w 宽度 (px)
 * @param h 高度 (px)
 */
private fun DrawScope.drawCloudyWithSunIcon(w: Float, h: Float) {
    val sunColor = Color(0xFFFFB300)
    val sunCenter = Offset(w * 0.72f, h * 0.32f)
    val sunR = w * 0.16f

    // 1. 绘制右上角小太阳与微光
    drawCircle(
        color = sunColor,
        radius = sunR,
        center = sunCenter
    )
    for (i in -1..2) {
        val rad = (i * 35f - 45f) * (PI.toFloat() / 180f)
        val p1 = Offset(sunCenter.x + (sunR + w * 0.04f) * cos(rad), sunCenter.y + (sunR + w * 0.04f) * sin(rad))
        val p2 = Offset(sunCenter.x + (sunR + w * 0.12f) * cos(rad), sunCenter.y + (sunR + w * 0.12f) * sin(rad))
        drawLine(
            color = sunColor,
            start = p1,
            end = p2,
            strokeWidth = w * 0.06f,
            cap = StrokeCap.Round
        )
    }

    // 2. 绘制前景饱满纯白云朵
    drawSolidCloud(w = w, h = h, offsetY = h * 0.08f, scale = 0.85f, cloudColor = Color.White)
}

/**
 * 绘制标准阴天图标 (纯白饱满单体积云)
 *
 * @param w 宽度 (px)
 * @param h 高度 (px)
 */
private fun DrawScope.drawOvercastIcon(w: Float, h: Float) {
    drawSolidCloud(w = w, h = h, offsetY = 0f, scale = 0.95f, cloudColor = Color.White)
}

/**
 * 绘制雷阵雨图标 (白云 + 3 根雨滴线 + 居中黄色闪电折线)
 *
 * @param w 宽度 (px)
 * @param h 高度 (px)
 */
private fun DrawScope.drawThunderstormIcon(w: Float, h: Float) {
    drawSolidCloud(w = w, h = h, offsetY = -h * 0.12f, scale = 0.82f, cloudColor = Color.White)

    // 斜向下雨丝
    val rainColor = Color(0xFF64B5F6)
    val rainStroke = w * 0.06f
    drawLine(rainColor, Offset(w * 0.28f, h * 0.68f), Offset(w * 0.22f, h * 0.84f), rainStroke, StrokeCap.Round)
    drawLine(rainColor, Offset(w * 0.72f, h * 0.68f), Offset(w * 0.66f, h * 0.84f), rainStroke, StrokeCap.Round)

    // 居中金色闪电折线
    val lightningPath = Path().apply {
        moveTo(w * 0.52f, h * 0.58f)
        lineTo(w * 0.44f, h * 0.74f)
        lineTo(w * 0.54f, h * 0.74f)
        lineTo(w * 0.46f, h * 0.92f)
    }
    drawPath(lightningPath, color = Color(0xFFFFD54F), style = Stroke(width = w * 0.07f, cap = StrokeCap.Round))
}

/**
 * 绘制小雨/中雨图标 (白云 + 2 条斜向天蓝雨滴线)
 *
 * @param w 宽度 (px)
 * @param h 高度 (px)
 */
private fun DrawScope.drawLightRainIcon(w: Float, h: Float) {
    drawSolidCloud(w = w, h = h, offsetY = -h * 0.10f, scale = 0.85f, cloudColor = Color.White)

    val rainColor = Color(0xFF64B5F6)
    val rainStroke = w * 0.065f
    drawLine(rainColor, Offset(w * 0.38f, h * 0.66f), Offset(w * 0.32f, h * 0.84f), rainStroke, StrokeCap.Round)
    drawLine(rainColor, Offset(w * 0.58f, h * 0.66f), Offset(w * 0.52f, h * 0.84f), rainStroke, StrokeCap.Round)
}

/**
 * 绘制暴雨图标 (白云 + 4 条密集斜向雨滴线)
 *
 * @param w 宽度 (px)
 * @param h 高度 (px)
 */
private fun DrawScope.drawHeavyRainIcon(w: Float, h: Float) {
    drawSolidCloud(w = w, h = h, offsetY = -h * 0.10f, scale = 0.85f, cloudColor = Color.White)

    val rainColor = Color(0xFF42A5F5)
    val rainStroke = w * 0.06f
    for (i in 0 until 4) {
        val startX = w * (0.28f + i * 0.14f)
        drawLine(rainColor, Offset(startX, h * 0.66f), Offset(startX - w * 0.06f, h * 0.88f), rainStroke, StrokeCap.Round)
    }
}

/**
 * 绘制雨夹雪图标 (白云 + 雨丝与雪晶星点)
 *
 * @param w 宽度 (px)
 * @param h 高度 (px)
 */
private fun DrawScope.drawSleetIcon(w: Float, h: Float) {
    drawSolidCloud(w = w, h = h, offsetY = -h * 0.10f, scale = 0.85f, cloudColor = Color.White)

    val rainColor = Color(0xFF64B5F6)
    drawLine(rainColor, Offset(w * 0.32f, h * 0.66f), Offset(w * 0.26f, h * 0.84f), w * 0.06f, StrokeCap.Round)

    // 雪花小星点
    drawSnowFlakeMini(w * 0.52f, h * 0.74f, w * 0.08f)
    drawSnowFlakeMini(w * 0.72f, h * 0.74f, w * 0.08f)
}

/**
 * 绘制纯白六角雪花图标
 *
 * @param w 宽度 (px)
 * @param h 高度 (px)
 */
private fun DrawScope.drawSnowIcon(w: Float, h: Float) {
    val cx = w / 2f
    val cy = h / 2f
    val r = w * 0.40f
    val color = Color.White
    val stroke = w * 0.07f

    for (i in 0 until 3) {
        val rad = (i * 60f) * (PI.toFloat() / 180f)
        val p1 = Offset(cx - r * cos(rad), cy - r * sin(rad))
        val p2 = Offset(cx + r * cos(rad), cy + r * sin(rad))
        drawLine(color, p1, p2, stroke, StrokeCap.Round)

        // 分支小箭头
        val branchR = r * 0.65f
        for (dir in listOf(-1, 1)) {
            val bx = cx + branchR * dir * cos(rad)
            val by = cy + branchR * dir * sin(rad)
            val angle1 = rad + 45f * (PI.toFloat() / 180f)
            val angle2 = rad - 45f * (PI.toFloat() / 180f)
            val len = w * 0.10f
            drawLine(color, Offset(bx, by), Offset(bx + len * cos(angle1), by + len * sin(angle1)), stroke * 0.8f, StrokeCap.Round)
            drawLine(color, Offset(bx, by), Offset(bx + len * cos(angle2), by + len * sin(angle2)), stroke * 0.8f, StrokeCap.Round)
        }
    }
}

/**
 * 绘制霾图标 (点阵与双环扩散)
 *
 * @param w 宽度 (px)
 * @param h 高度 (px)
 */
private fun DrawScope.drawHazeIcon(w: Float, h: Float) {
    val cx = w / 2f
    val cy = h / 2f
    val color = Color.White.copy(alpha = 0.90f)

    // 8字双圆环点阵
    drawCircle(color, radius = w * 0.16f, center = Offset(cx - w * 0.14f, cy), style = Stroke(width = w * 0.06f))
    drawCircle(color, radius = w * 0.16f, center = Offset(cx + w * 0.14f, cy), style = Stroke(width = w * 0.06f))

    // 围绕的 6 颗微粒点
    val dotR = w * 0.035f
    drawCircle(color, dotR, Offset(cx, cy - w * 0.28f))
    drawCircle(color, dotR, Offset(cx, cy + w * 0.28f))
    drawCircle(color, dotR, Offset(cx - w * 0.32f, cy - w * 0.18f))
    drawCircle(color, dotR, Offset(cx + w * 0.32f, cy - w * 0.18f))
    drawCircle(color, dotR, Offset(cx - w * 0.32f, cy + w * 0.18f))
    drawCircle(color, dotR, Offset(cx + w * 0.32f, cy + w * 0.18f))
}

/**
 * 绘制扬沙图标 (三道风流线与沙粒)
 *
 * @param w 宽度 (px)
 * @param h 高度 (px)
 */
private fun DrawScope.drawSandstormIcon(w: Float, h: Float) {
    val color = Color.White
    val stroke = w * 0.06f

    // 弯曲风线
    for (i in 0 until 3) {
        val y = h * (0.32f + i * 0.20f)
        val p = Path().apply {
            moveTo(w * 0.18f, y)
            lineTo(w * 0.65f, y)
            cubicTo(w * 0.82f, y, w * 0.82f, y - h * 0.12f, w * 0.72f, y - h * 0.12f)
        }
        drawPath(p, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }

    // 沙粒点
    drawCircle(color, w * 0.035f, Offset(w * 0.25f, h * 0.22f))
    drawCircle(color, w * 0.035f, Offset(w * 0.82f, h * 0.68f))
}

/**
 * 绘制大雾图标 (半圆云顶 + 三道水平横线)
 *
 * @param w 宽度 (px)
 * @param h 高度 (px)
 */
private fun DrawScope.drawFogIcon(w: Float, h: Float) {
    val color = Color.White
    drawSolidCloud(w = w, h = h, offsetY = -h * 0.12f, scale = 0.82f, cloudColor = color)

    val stroke = w * 0.06f
    drawLine(color, Offset(w * 0.24f, h * 0.68f), Offset(w * 0.76f, h * 0.68f), stroke, StrokeCap.Round)
    drawLine(color, Offset(w * 0.20f, h * 0.78f), Offset(w * 0.80f, h * 0.78f), stroke, StrokeCap.Round)
    drawLine(color, Offset(w * 0.28f, h * 0.88f), Offset(w * 0.72f, h * 0.88f), stroke, StrokeCap.Round)
}

/**
 * 绘制冰雹图标 (八角冰晶/坚硬雪晶)
 *
 * @param w 宽度 (px)
 * @param h 高度 (px)
 */
private fun DrawScope.drawHailIcon(w: Float, h: Float) {
    drawSnowIcon(w, h)
}

/**
 * 绘制通用的饱满实心白云形状
 *
 * @param w 宽度
 * @param h 高度
 * @param offsetY 纵向偏移量
 * @param scale 缩放比例
 * @param cloudColor 云朵色彩
 */
private fun DrawScope.drawSolidCloud(
    w: Float,
    h: Float,
    offsetY: Float,
    scale: Float,
    cloudColor: Color
) {
    val cx = w / 2f
    val cy = h / 2f + offsetY

    val r1 = w * 0.16f * scale
    val r2 = w * 0.24f * scale
    val r3 = w * 0.18f * scale

    // 组合 3 个重叠圆形与 1 个底部胶囊底座
    drawCircle(cloudColor, r1, Offset(cx - w * 0.20f * scale, cy + h * 0.02f * scale))
    drawCircle(cloudColor, r2, Offset(cx - w * 0.04f * scale, cy - h * 0.08f * scale))
    drawCircle(cloudColor, r3, Offset(cx + w * 0.18f * scale, cy + h * 0.02f * scale))

    // 底部圆角矩形
    val baseW = w * 0.62f * scale
    val baseH = h * 0.22f * scale
    drawRoundRect(
        color = cloudColor,
        topLeft = Offset(cx - baseW / 2f, cy - baseH / 3f),
        size = Size(baseW, baseH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(baseH / 2f, baseH / 2f)
    )
}

/**
 * 绘制小雪花星点
 *
 * @param cx 中心 X
 * @param cy 中心 Y
 * @param r 半径
 */
private fun DrawScope.drawSnowFlakeMini(cx: Float, cy: Float, r: Float) {
    val color = Color.White
    for (i in 0 until 3) {
        val rad = (i * 60f) * (PI.toFloat() / 180f)
        drawLine(
            color = color,
            start = Offset(cx - r * cos(rad), cy - r * sin(rad)),
            end = Offset(cx + r * cos(rad), cy + r * sin(rad)),
            strokeWidth = r * 0.4f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * 天气图标辅助工具类
 */
object WeatherIcons {

    /**
     * 根据天气文本返回直观的天气 Emoji 符号（兼容备用）
     *
     * @param weatherText 天气现象描述
     * @return 对应的天气 Emoji 符号
     */
    fun getWeatherEmoji(weatherText: String): String {
        return when {
            weatherText.contains("晴") && !weatherText.contains("多云") -> "☀️"
            weatherText.contains("雷") -> "⛈️"
            weatherText.contains("暴雨") || weatherText.contains("大雨") -> "🌧️"
            weatherText.contains("雨夹雪") -> "🌨️"
            weatherText.contains("雨") -> "🌦️"
            weatherText.contains("雪") -> "❄️"
            weatherText.contains("阴") -> "☁️"
            weatherText.contains("多云") -> "⛅"
            weatherText.contains("雾") || weatherText.contains("霾") -> "🌫️"
            weatherText.contains("风") || weatherText.contains("沙") -> "🌪️"
            else -> "🌤️"
        }
    }
}

