package com.weather.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.model.WeatherData
import kotlin.math.PI
import kotlin.math.sin

/**
 * 带有阻尼物理缓动特性的插值计算函数 (Ease-Out Damping)
 *
 * @param progress 线性输入进度 (0.0f .. 1.0f)
 * @return 经过正弦阻尼缓动映射后的输出值 (0.0f .. 1.0f)
 */
private fun easeOutDamped(progress: Float): Float {
    val clamped = progress.coerceIn(0f, 1f)
    return sin(clamped * (PI / 2.0)).toFloat()
}

/**
 * 主界面核心温度与气象现象居中展示视图（支持物理阻尼联动）
 *
 * 集中展示顶部巨幅实时温度（大字）、温差范围与空气质量/天气状况说明，支持下拉弹性放大与向上滑动时的阻尼分层级联挤压隐藏。
 *
 * @param weatherData 聚合天气数据模型 [WeatherData]
 * @param scrollOffsetProvider 垂直滚动偏移量提供者（单位：像素）
 * @param modifier 外部修饰符
 */
@Composable
fun HeroWeatherView(
    weatherData: WeatherData,
    scrollOffsetProvider: () -> Int = { 0 },
    modifier: Modifier = Modifier
) {
    val current = weatherData.current
    val todayForecast = weatherData.dailyForecasts.firstOrNull()
    val aqi = weatherData.airQuality

    val maxT = todayForecast?.maxTemperature?.toInt() ?: current.temperature.toInt()
    val minT = todayForecast?.minTemperature?.toInt() ?: (current.temperature.toInt() - 7)
    val aqiText = if (aqi != null && aqi.qualityText.isNotEmpty() && aqi.qualityText != "-") "空气${aqi.qualityText}" else "空气优"

    // 当实时实况 weatherText 缺失或为 "-" 时，自动取当天预测详情的天气现象
    val displayWeatherText = weatherData.getDisplayWeatherText()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 50.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 巨幅主温度展示：
        // - 下拉时：产生物理阻尼弹性放大与下拉位移
        // - 上滑未受挤压时（scroll <= 220px）：完全抵消向上位移，保持在圆点指示器下方静止且 100% 完整显示
        // - 上滑受挤压时（scroll > 220px）：伴随正弦阻尼缓动平滑缩小、上推并渐隐
        Text(
            text = "${current.temperature.toInt()}°",
            style = TextStyle(
                fontSize = 92.sp,
                fontWeight = FontWeight.Light,
                color = Color.White,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.38f),
                    offset = Offset(0f, 3f),
                    blurRadius = 8f
                )
            ),
            modifier = Modifier.graphicsLayer {
                val scroll = scrollOffsetProvider().toFloat()
                val squeezeThreshold = 350f // 挤压触发阈值（像素）
                val squeezeDistance = 360f  // 阻尼挤压过渡距离（像素）

                if (scroll < 0f) {
                    // 下拉弹性阻尼阶段 (Overscroll)
                    val overscrollProgress = (-scroll / 600f).coerceIn(0f, 0.30f)
                    alpha = 1f
                    scaleX = 1f + overscrollProgress
                    scaleY = 1f + overscrollProgress
                    translationY = -scroll * 0.30f
                } else if (scroll <= squeezeThreshold) {
                    // 未受挤压阶段：保持 100% 不透明，完全抵消滚动上移，绝对静止
                    alpha = 1f
                    scaleX = 1f
                    scaleY = 1f
                    translationY = scroll
                } else {
                    // 受卡片向上挤压阶段：应用阻尼缓动缩小并渐隐
                    val linearProgress = ((scroll - squeezeThreshold) / squeezeDistance).coerceIn(0f, 1f)
                    val dampedProgress = easeOutDamped(linearProgress)
                    alpha = 1f - dampedProgress
                    val scale = 1f - dampedProgress * 0.22f
                    scaleX = scale
                    scaleY = scale
                    translationY = squeezeThreshold - dampedProgress * 32f
                }
            }
        )

        Spacer(modifier = Modifier.height(0.dp))

        // 2. 最高温与最低温：随列表正常上移并在 50px ~ 170px 期间正弦阻尼渐隐
        Text(
            text = "最高 $maxT° 最低 $minT°",
            style = TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.95f),
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.38f),
                    offset = Offset(0f, 2f),
                    blurRadius = 5f
                )
            ),
            modifier = Modifier.graphicsLayer {
                val scroll = scrollOffsetProvider().toFloat()
                if (scroll <= 50f) {
                    alpha = 1f
                } else {
                    val progress = ((scroll - 50f) / 120f).coerceIn(0f, 1f)
                    alpha = 1f - easeOutDamped(progress)
                }
            }
        )

        Spacer(modifier = Modifier.height(5.dp))

        // 3. 空气质量与天气描述：随列表正常上移并在 0px ~ 110px 期间最先正弦阻尼渐隐
        Text(
            text = "$aqiText  $displayWeatherText",
            style = TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.95f),
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.38f),
                    offset = Offset(0f, 2f),
                    blurRadius = 5f
                )
            ),
            modifier = Modifier.graphicsLayer {
                val scroll = scrollOffsetProvider().toFloat()
                val progress = (scroll / 110f).coerceIn(0f, 1f)
                alpha = 1f - easeOutDamped(progress)
            }
        )
    }
}

