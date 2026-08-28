package com.weather.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.model.AirQuality
import com.weather.app.model.CityInfo
import com.weather.app.model.CurrentWeather
import com.weather.app.model.WeatherData
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

import com.weather.app.model.CardDisplayConfig

/**
 * 气象真实观测指标详情宫格组件
 *
 * 严格遵循真实数据原则与用户自定义卡片显示配置：
 * 1. 支持通过 [CardDisplayConfig] 动态开启或关闭任意指标卡片；
 * 2. 所有卡片必须由气象台返回的真实数据字段构建，若某项数据缺失或为无效值（9999/空），则该卡片直接不渲染展示；
 * 3. 所有卡片高度保持严格一致（152.dp），自适应成对双列排布；
 * 4. 底部依次展示数据来源、气象观测发布时间与上次刷新时间。
 *
 * @param weatherData 聚合天气数据模型 [WeatherData]
 * @param cardConfig 卡片自定义显隐配置实体 [CardDisplayConfig]
 * @param lastUpdatedText 上次刷新时间说明文本（如 "上次刷新 15:53"）
 * @param modifier 外部修饰符
 */
@Composable
fun WeatherDetailGrid(
    weatherData: WeatherData,
    cardConfig: CardDisplayConfig = CardDisplayConfig(),
    lastUpdatedText: String = "",
    modifier: Modifier = Modifier
) {
    val current = weatherData.current
    val aqi = weatherData.airQuality

    // 收集所有有效真实存在且用户已开启的指标卡片内容
    val validCards = mutableListOf<@Composable (Modifier) -> Unit>()

    // 1. 空气质量卡片 (当用户开启且存在真实 AQI 数据时展示)
    if (cardConfig.showAirQuality && aqi != null && aqi.aqi > 0 && aqi.aqi != 9999) {
        validCards.add { mod -> AirQualityRealCard(aqi = aqi, modifier = mod) }
    }

    // 2. 日出日落卡片 (当用户开启时结合当前城市经纬度与天文算法展示)
    if (cardConfig.showSunriseSunset) {
        validCards.add { mod -> SunriseSunsetRealCard(city = weatherData.city, modifier = mod) }
    }

    // 3. 体感温度卡片 (当用户开启且存在真实体感或温度时展示)
    if (cardConfig.showFeelsLike && current.feelsLike != null && current.feelsLike != 9999.0) {
        validCards.add { mod -> FeelsLikeRealCard(current = current, modifier = mod) }
    }

    // 4. 风向风速卡片 (当用户开启且存在真实风力数据时展示)
    if (cardConfig.showWind && (current.windDirection.isNotEmpty() || current.windPower.isNotEmpty() || current.windSpeed > 0.0)) {
        validCards.add { mod -> WindRealCard(current = current, modifier = mod) }
    }

    // 5. 相对湿度卡片 (当用户开启且存在真实湿度数据时展示)
    if (cardConfig.showHumidity && current.humidity > 0.0 && current.humidity != 9999.0) {
        validCards.add { mod -> HumidityRealCard(humidity = current.humidity.toInt(), modifier = mod) }
    }

    // 6. 大气压强卡片 (当用户开启且存在真实气压数据时展示)
    if (cardConfig.showPressure && current.pressure > 0.0 && current.pressure != 9999.0) {
        validCards.add { mod -> PressureRealCard(pressureHpa = current.pressure.toInt(), modifier = mod) }
    }

    // 7. 实时降水量卡片 (当用户开启且发生真实降水时展示)
    if (cardConfig.showPrecipitation && current.precipitation > 0.0 && current.precipitation != 9999.0) {
        validCards.add { mod -> PrecipitationRealCard(precipitation = current.precipitation, modifier = mod) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (cardConfig.showMoonPhase) {
            // 当月相开启时：前 2 张卡片排首行，月相全宽单列居中，剩余卡片两两排布
            val firstBatch = validCards.take(2)
            if (firstBatch.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    firstBatch[0](Modifier.weight(1f))

                    if (firstBatch.size > 1) {
                        firstBatch[1](Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // 月相卡片：水平空间独占一整行全宽展示
            MoonPhaseRealCard(
                city = weatherData.city,
                modifier = Modifier.fillMaxWidth()
            )

            // 其余双列指标卡片
            val remainingCards = validCards.drop(2)
            val remainingRowCount = (remainingCards.size + 1) / 2
            for (rowIndex in 0 until remainingRowCount) {
                val firstIdx = rowIndex * 2
                val secondIdx = firstIdx + 1

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    remainingCards[firstIdx](Modifier.weight(1f))

                    if (secondIdx < remainingCards.size) {
                        remainingCards[secondIdx](Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            // 当月相关闭时：所有指标卡片直接自适应双列连续排列
            val rowCount = (validCards.size + 1) / 2
            for (rowIndex in 0 until rowCount) {
                val firstIdx = rowIndex * 2
                val secondIdx = firstIdx + 1

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    validCards[firstIdx](Modifier.weight(1f))

                    if (secondIdx < validCards.size) {
                        validCards[secondIdx](Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // 底部版本号、真实数据来源、发布时刻与上次刷新时间
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val versionName = androidx.compose.runtime.remember(context) {
                try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
                } catch (e: Exception) {
                    "1.0.0"
                }
            }

            // 版本号展示 (取值 versionName，颜色与数据源自一致)
            Text(
                text = "版本号 v$versionName",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "数据源自 ${weatherData.sourceName} 官方气象实况",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
            if (current.publishTime.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "气象观测发布时间：${current.publishTime}",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            if (lastUpdatedText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = lastUpdatedText,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

// ==================== 1. 真实空气质量卡片 ====================

/**
 * 真实空气质量卡片组件
 *
 * @param aqi 空气质量数据模型 [AirQuality]
 * @param modifier 外部修饰符
 */
@Composable
private fun AirQualityRealCard(
    aqi: AirQuality,
    modifier: Modifier = Modifier
) {
    MetricBaseCard(
        icon = Icons.Default.Air,
        title = "空气质量",
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = aqi.qualityText,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${aqi.aqi}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 真实 AQI 彩虹谱条指示
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF4CAF50),
                                Color(0xFFFFEB3B),
                                Color(0xFFFF9800),
                                Color(0xFFF44336),
                                Color(0xFF9C27B0)
                            )
                        )
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                val ratio = (aqi.aqi / 300f).coerceIn(0.05f, 0.95f)
                Box(
                    modifier = Modifier
                        .padding(start = (ratio * 120).dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = aqi.getHealthAdvice(),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

// ==================== 2. 真实体感温度卡片 ====================

/**
 * 真实体感温度卡片组件
 *
 * @param current 当前实时气象数据模型 [CurrentWeather]
 * @param modifier 外部修饰符
 */
@Composable
private fun FeelsLikeRealCard(
    current: CurrentWeather,
    modifier: Modifier = Modifier
) {
    val feelsLike = current.feelsLike?.toInt() ?: current.temperature.toInt()
    val actual = current.temperature.toInt()

    MetricBaseCard(
        icon = Icons.Default.Thermostat,
        title = "体感温度",
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "$feelsLike°",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "实际气温：$actual°",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(6.dp))

            val diffText = when {
                feelsLike > actual -> "受湿度与风速影响，体感更热"
                feelsLike < actual -> "受风速影响，体感较凉爽"
                else -> "体感与实际气温一致"
            }
            Text(
                text = diffText,
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2
            )
        }
    }
}

// ==================== 3. 真实风向风速罗盘卡片 ====================

/**
 * 解析气象风向描述文本并转换为屏幕 Canvas 极坐标系下的指针指向角度（单位：度）
 *
 * 屏幕极坐标系规范：0° 为东（右，+X），90° 为南（下，+Y），180° 为西（左，-X），270° 为北（上，-Y）。
 * 指针箭头指示当前风向所在的方位：
 * - 东南风：箭头指向东南（45°，右下方），箭尾圆点在西北（225°，左上方）；
 * - 东北风：箭头指向东北（315°，右上方），箭尾圆点在西南（135°，左下方）；
 * - 西南风：箭头指向西南（135°，左下方），箭尾圆点在东北（315°，右上方）；
 * - 西北风：箭头指向西北（225°，左上方），箭尾圆点在东南（45°，右下方）；
 * - 北风：箭头指向北（270°，正上方），箭尾圆点在南（90°，正下方）；
 * - 南风：箭头指向南（90°，正下方），箭尾圆点在北（270°，正上方）；
 * - 东风：箭头指向东（0°，正右方），箭尾圆点在西（180°，正左方）；
 * - 西风：箭头指向西（180°，正左方），箭尾圆点在东（0°，正右方）。
 *
 * @param direction 风向文本描述（例如 "东南风"、"西北偏北" 或 "45°"）
 * @return 屏幕极坐标系下指针指向角度（0f ~ 360f）
 */
fun parseWindDirectionAngle(direction: String): Float {
    val clean = direction.trim()
    val degreeMatch = Regex("(\\d+(?:\\.\\d+)?)").find(clean)
    if (degreeMatch != null && clean.contains("°")) {
        val metDegree = degreeMatch.groupValues[1].toFloatOrNull()
        if (metDegree != null) {
            // 气象角度转换屏幕风向角度: (metDegree - 90°) % 360°
            return (metDegree - 90f + 360f) % 360f
        }
    }

    return when {
        clean.contains("东北偏北") || clean.contains("北偏东") -> 292.5f
        clean.contains("东北偏东") || clean.contains("东偏北") -> 337.5f
        clean.contains("东南偏东") || clean.contains("东偏南") -> 22.5f
        clean.contains("东南偏南") || clean.contains("南偏东") -> 67.5f
        clean.contains("西南偏南") || clean.contains("南偏西") -> 112.5f
        clean.contains("西南偏西") || clean.contains("西偏南") -> 157.5f
        clean.contains("西北偏西") || clean.contains("西偏北") -> 202.5f
        clean.contains("西北偏北") || clean.contains("北偏西") -> 247.5f
        clean.contains("东南") -> 45f   // 东南 (右下方)
        clean.contains("东北") -> 315f  // 东北 (右上方)
        clean.contains("西南") -> 135f  // 西南 (左下方)
        clean.contains("西北") -> 225f  // 西北 (左上方)
        clean.contains("偏北") || clean.contains("北风") || clean.endsWith("北") -> 270f  // 北 (正上方)
        clean.contains("偏南") || clean.contains("南风") || clean.endsWith("南") -> 90f   // 南 (正下方)
        clean.contains("偏东") || clean.contains("东风") || clean.endsWith("东") -> 0f    // 东 (正右方)
        clean.contains("偏西") || clean.contains("西风") || clean.endsWith("西") -> 180f  // 西 (正左方)
        else -> 270f // 默认微风/无持续风向指向北
    }
}

/**
 * 真实风向风力罗盘卡片组件（100% 精确对齐设计图：外圈密集刻度与北东南西 + 居中覆盖式深色表盘“2 级” + 贯穿式加大加粗圆球箭尾与导向箭头）
 *
 * @param current 当前实时气象数据模型 [CurrentWeather]
 * @param modifier 外部修饰符
 */
@Composable
private fun WindRealCard(
    current: CurrentWeather,
    modifier: Modifier = Modifier
) {
    val rawPower = current.windPower.replace("级", "").trim()
    val powerLevel = rawPower.toIntOrNull() ?: when {
        current.windSpeed <= 1.5 -> 1
        current.windSpeed <= 3.3 -> 2
        current.windSpeed <= 5.4 -> 3
        current.windSpeed <= 7.9 -> 4
        current.windSpeed <= 10.7 -> 5
        else -> 6
    }

    val displayDirection = if (current.windDirection.isNotEmpty()) current.windDirection else "无持续风向"

    // 解析风向对应角度（指针箭头准确指示当前风向所在方位）
    val windAngleDeg = parseWindDirectionAngle(displayDirection)

    val windAdvice = when (powerLevel) {
        0, 1, 2 -> "$displayDirection，轻拂过脸颊"
        3, 4 -> "$displayDirection，清风拂面，舒爽宜人"
        5, 6 -> "$displayDirection，风力较大，注意防风"
        else -> "$displayDirection，大风强劲，注意安全"
    }

    MetricBaseCard(
        icon = Icons.Default.Air,
        title = "风",
        modifier = modifier
    ) {
        val arrowPath = remember { Path() }
        val arrowShadowPath = remember { Path() }

        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(82.dp)) {
                    val r = size.width / 2f
                    val c = Offset(r, r)

                    // 1. 外圈 40 根放射状刻度线 (放大至 82dp，刻度长 18.5f，线宽 4.5f，避开北、东、南、西四个文字方位)
                    for (i in 0 until 40) {
                        val angleDeg = i * 9f
                        // 避开 0°(东), 90°(南), 180°(西), 270°(北) 四个文字正切点
                        if (angleDeg % 90f !in 76f..104f && angleDeg % 90f !in 0f..14f && angleDeg % 90f !in 166f..180f) {
                            val angleRad = angleDeg * (PI.toFloat() / 180f)
                            val p1 = Offset(c.x + (r - 0.5f) * cos(angleRad), c.y + (r - 0.5f) * sin(angleRad))
                            val p2 = Offset(c.x + (r - 18.5f) * cos(angleRad), c.y + (r - 18.5f) * sin(angleRad))
                            drawLine(
                                color = Color.White.copy(alpha = 0.80f),
                                start = p1,
                                end = p2,
                                strokeWidth = 4.5f,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // 2. 贯穿式加大加粗高清晰风向指示指针 (箭尾大白球 + 加粗贯穿主线 + 加大立体导向箭头)
                    val rad = windAngleDeg * (PI.toFloat() / 180f)
                    val oppRad = (windAngleDeg + 180f) * (PI.toFloat() / 180f)

                    val startPt = Offset(c.x + (r * 0.88f) * cos(oppRad), c.y + (r * 0.88f) * sin(oppRad))
                    val endPt = Offset(c.x + (r * 0.94f) * cos(rad), c.y + (r * 0.94f) * sin(rad))

                    // 底层柔和立体阴影 (线宽 9.5f)
                    drawLine(
                        color = Color.Black.copy(alpha = 0.35f),
                        start = Offset(startPt.x, startPt.y + 2f),
                        end = Offset(endPt.x, endPt.y + 2f),
                        strokeWidth = 9.5f,
                        cap = StrokeCap.Round
                    )

                    // 贯穿实心主干指针 (继续加粗至 9.0f)
                    drawLine(
                        color = Color.White,
                        start = startPt,
                        end = endPt,
                        strokeWidth = 9.0f,
                        cap = StrokeCap.Round
                    )

                    // 箭尾实心大白圆点 (加大加粗至半径 12.5f)
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.30f),
                        radius = 13.5f,
                        center = Offset(startPt.x, startPt.y + 2f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 12.5f,
                        center = startPt
                    )

                    // 导向实心粗大三角形箭头 (复用 Path 避免每次重绘产生垃圾对象)
                    val arrowLen = 28.0f
                    val arrowAngle1 = rad + 150f * (PI.toFloat() / 180f)
                    val arrowAngle2 = rad - 150f * (PI.toFloat() / 180f)

                    val a1 = Offset(endPt.x + arrowLen * cos(arrowAngle1), endPt.y + arrowLen * sin(arrowAngle1))
                    val a2 = Offset(endPt.x + arrowLen * cos(arrowAngle2), endPt.y + arrowLen * sin(arrowAngle2))

                    arrowShadowPath.reset()
                    arrowShadowPath.moveTo(endPt.x, endPt.y + 2f)
                    arrowShadowPath.lineTo(a1.x, a1.y + 2f)
                    arrowShadowPath.lineTo(a2.x, a2.y + 2f)
                    arrowShadowPath.close()
                    drawPath(path = arrowShadowPath, color = Color.Black.copy(alpha = 0.35f))

                    arrowPath.reset()
                    arrowPath.moveTo(endPt.x, endPt.y)
                    arrowPath.lineTo(a1.x, a1.y)
                    arrowPath.lineTo(a2.x, a2.y)
                    arrowPath.close()
                    drawPath(path = arrowPath, color = Color.White)

                    // 3. 居中覆盖在指针上方的不透明磨砂渐变圆形表盘底衬 (中间较亮向外渐变，遮盖穿过中心的指针线，让中间文字清晰悬浮)
                    val centerCircleRadius = r * 0.48f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF384B63), // 中间较亮，柔和磨砂高光质感
                                Color(0xFF243244), // 中部平滑过渡
                                Color(0xFF16202D)  // 外圈深沉不透明深灰蓝底色
                            ),
                            center = c,
                            radius = centerCircleRadius
                        ),
                        radius = centerCircleRadius,
                        center = c
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.20f),
                        radius = centerCircleRadius,
                        center = c,
                        style = Stroke(width = 1.2f)
                    )
                }

                // 外圈四方位文字 (北、东、南、西)
                Box(modifier = Modifier.size(82.dp)) {
                    Text(text = "北", color = Color.White.copy(alpha = 0.95f), fontSize = 10.sp, fontWeight = FontWeight.Normal, modifier = Modifier.align(Alignment.TopCenter))
                    Text(text = "东", color = Color.White.copy(alpha = 0.95f), fontSize = 10.sp, fontWeight = FontWeight.Normal, modifier = Modifier.align(Alignment.CenterEnd))
                    Text(text = "南", color = Color.White.copy(alpha = 0.95f), fontSize = 10.sp, fontWeight = FontWeight.Normal, modifier = Modifier.align(Alignment.BottomCenter))
                    Text(text = "西", color = Color.White.copy(alpha = 0.95f), fontSize = 10.sp, fontWeight = FontWeight.Normal, modifier = Modifier.align(Alignment.CenterStart))
                }

                // 居中大字风级数字 (纯数字展示，去除单位，清晰悬浮于圆盘中央)
                Text(
                    text = "$powerLevel",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = windAdvice,
                color = Color.White.copy(alpha = 0.80f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

// ==================== 4. 真实相对湿度卡片 ====================

/**
 * 真实相对湿度卡片组件
 *
 * @param humidity 相对湿度百分比
 * @param modifier 外部修饰符
 */
@Composable
private fun HumidityRealCard(
    humidity: Int,
    modifier: Modifier = Modifier
) {
    MetricBaseCard(
        icon = Icons.Default.Opacity,
        title = "相对湿度",
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "$humidity%",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(10.dp))

            val advice = when {
                humidity > 70 -> "空气较为潮湿，体感闷热"
                humidity < 35 -> "空气较为干燥，注意补水"
                else -> "相对湿度适宜宜人"
            }
            Text(
                text = advice,
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2
            )
        }
    }
}

// ==================== 5. 真实大气压强卡片 ====================

/**
 * 真实大气压强仪表盘卡片组件（100% 精确对齐设计图：270° 扇形发光带与细刻度环 + 纯白胶囊短粗指针 + 居中超大字“1001 百帕”）
 *
 * @param pressureHpa 大气压强值 (hPa)
 * @param modifier 外部修饰符
 */
@Composable
private fun PressureRealCard(
    pressureHpa: Int,
    modifier: Modifier = Modifier
) {
    val progress = ((pressureHpa - 950f) / 100f).coerceIn(0f, 1f)

    val pressureAdvice = when {
        pressureHpa < 1000 -> "气压偏低，注意室内通风"
        pressureHpa in 1000..1020 -> "正常气压，感觉良好"
        else -> "气压较高，天气晴朗干爽"
    }

    MetricBaseCard(
        icon = Icons.Default.Speed,
        title = "气压",
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(82.dp)) {
                    val r = size.width / 2f
                    val c = Offset(r, r)

                    val startAngleDeg = 135f
                    val totalSweepDeg = 270f
                    val tickCount = 32

                    val sweepProgressAngle = progress * totalSweepDeg
                    val activeTickThreshold = (progress * tickCount).toInt()

                    // 1. 绘制激活区域的扇形半透明高亮弧带 (放大至 82dp，弧带宽 18.0f)
                    if (sweepProgressAngle > 0f) {
                        drawArc(
                            color = Color.White.copy(alpha = 0.25f),
                            startAngle = startAngleDeg,
                            sweepAngle = sweepProgressAngle,
                            useCenter = false,
                            topLeft = Offset(5f, 5f),
                            size = Size(size.width - 10f, size.height - 10f),
                            style = Stroke(width = 18.0f, cap = StrokeCap.Butt)
                        )
                    }

                    // 2. 绘制 32 根放射状加粗加长刻度线 (加长至 20.0f，激活加粗至 5.0f)
                    for (i in 0..tickCount) {
                        val angleDeg = startAngleDeg + (i.toFloat() / tickCount.toFloat()) * totalSweepDeg
                        val angleRad = angleDeg * (PI.toFloat() / 180f)

                        val isActive = i <= activeTickThreshold
                        val tickColor = if (isActive) Color.White.copy(alpha = 0.98f) else Color.White.copy(alpha = 0.35f)
                        val tickLen = 20.0f

                        val p1 = Offset(c.x + (r - 0.5f) * cos(angleRad), c.y + (r - 0.5f) * sin(angleRad))
                        val p2 = Offset(c.x + (r - 0.5f - tickLen) * cos(angleRad), c.y + (r - 0.5f - tickLen) * sin(angleRad))

                        drawLine(
                            color = tickColor,
                            start = p1,
                            end = p2,
                            strokeWidth = if (isActive) 5.0f else 3.5f,
                            cap = StrokeCap.Round
                        )
                    }

                    // 3. 绘制当前气压纯白圆角胶囊短粗指针 (加粗至 8.0f，明显加长突出)
                    val curAngleDeg = startAngleDeg + sweepProgressAngle
                    val curAngleRad = curAngleDeg * (PI.toFloat() / 180f)
                    val needleP1 = Offset(c.x + (r + 6.5f) * cos(curAngleRad), c.y + (r + 6.5f) * sin(curAngleRad))
                    val needleP2 = Offset(c.x + (r - 21.0f) * cos(curAngleRad), c.y + (r - 21.0f) * sin(curAngleRad))

                    drawLine(
                        color = Color.White,
                        start = needleP1,
                        end = needleP2,
                        strokeWidth = 8.0f,
                        cap = StrokeCap.Round
                    )
                }

                // 居中大字显示气压数值与“百帕” (超大字号 26sp + 11sp)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "$pressureHpa",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 26.sp
                    )
                    Text(
                        text = "百帕",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = pressureAdvice,
                color = Color.White.copy(alpha = 0.80f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

// ==================== 6. 真实降水量卡片 ====================

/**
 * 真实降水量卡片组件
 *
 * @param precipitation 实时降水量 (mm)
 * @param modifier 外部修饰符
 */
@Composable
private fun PrecipitationRealCard(
    precipitation: Double,
    modifier: Modifier = Modifier
) {
    MetricBaseCard(
        icon = Icons.Default.WaterDrop,
        title = "实时降水",
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$precipitation",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "mm",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val rainLevel = when {
                precipitation >= 25.0 -> "暴雨级别降水"
                precipitation >= 10.0 -> "大雨级别降水"
                precipitation >= 2.5 -> "中雨级别降水"
                else -> "小雨级别降水"
            }
            Text(
                text = "当前 $rainLevel，出行请注意携带雨具",
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2
            )
        }
    }
}

// ==================== 7. 真实日出日落卡片 ====================

/**
 * 格式化日历分钟数为 24 小时制时间文本（如 348 -> "05:48"）
 *
 * @param minutes 当天从 00:00 起经过的分钟数
 * @return 格式化后的时间字符串（格式为 HH:mm）
 */
private fun formatMinutesToTime(minutes: Int): String {
    val h = (minutes / 60) % 24
    val m = minutes % 60
    return String.format(Locale.CHINA, "%02d:%02d", h, m)
}

/**
 * 日出日落卡片太阳光学视觉特征参数
 *
 * @property photosphereCenterColor 日盘核心极炽光核中心色
 * @property photosphereEdgeColor 日盘核心边缘色温过渡色
 * @property innerCoronaColor 中层等离子日冕辉光色
 * @property outerHaloColor 外层瑞利散射与漫射日晕色
 * @property diffractionRayColor 衍射星芒微羽色
 * @property skyGlowGradientColors 日照天光漫射光幕垂直渐变色彩列表
 * @property rayIntensity 星芒与光芒强度比例 (0.0f ~ 1.0f)
 * @property diskScale 太阳视直径缩放因子 (0.8f ~ 1.3f)
 */
private data class SolarCardVisualState(
    val photosphereCenterColor: Color,
    val photosphereEdgeColor: Color,
    val innerCoronaColor: Color,
    val outerHaloColor: Color,
    val diffractionRayColor: Color,
    val skyGlowGradientColors: List<Color>,
    val rayIntensity: Float,
    val diskScale: Float
)

/**
 * 根据日照时间进度与昼夜状态计算太阳在卡片中的物理色温与光学参数
 *
 * 严格遵从自然光学原理与色温红移规律：
 * 1. 清晨日出 (progress < 0.12)：穿透厚大气层产生强烈瑞利散射，呈现温润朱红/朝霞金橙朝阳；
 * 2. 晨光跃升 (0.12 <= progress < 0.35)：色温迅速升高，转为璀璨金黄与暖白金；
 * 3. 烈日正午 (0.35 <= progress <= 0.65)：直射天顶，光程最短，呈现纯白极炽光核 (6500K) 与耀眼星芒；
 * 4. 午后斜阳 (0.65 < progress <= 0.88)：色温重归温润香槟金与暖琥珀金；
 * 5. 晚霞落日 (progress > 0.88)：晚霞浓郁散射，呈现落日熔金紫红与深赤霞光。
 *
 * @param progress 归一化日照进度 (0.0f ~ 1.0f)
 * @param isNight 是否为夜间模式
 * @return 太阳卡片光学视觉状态对象 [SolarCardVisualState]
 */
private fun calculateCardSolarVisualState(
    progress: Float,
    isNight: Boolean
): SolarCardVisualState {
    if (isNight) {
        return SolarCardVisualState(
            photosphereCenterColor = Color(0xFF90CAF9),
            photosphereEdgeColor = Color(0xFF37474F),
            innerCoronaColor = Color(0x3364B5F6),
            outerHaloColor = Color(0x1A1E88E5),
            diffractionRayColor = Color(0x2290CAF9),
            skyGlowGradientColors = listOf(Color(0x1564B5F6), Color(0x001E88E5)),
            rayIntensity = 0.20f,
            diskScale = 0.85f
        )
    }

    val p = progress.coerceIn(0f, 1f)
    return when {
        // 1. 清晨日出 (0.00 ~ 0.12)：朱红朝阳，温润红日
        p < 0.12f -> {
            val t = p / 0.12f
            SolarCardVisualState(
                photosphereCenterColor = Color(0xFFFFF3E0),
                photosphereEdgeColor = Color(0xFFFF5722),
                innerCoronaColor = Color(0xFFFF7043).copy(alpha = 0.70f),
                outerHaloColor = Color(0xFFFF3D00).copy(alpha = 0.30f),
                diffractionRayColor = Color(0xFFFFAB91).copy(alpha = 0.75f),
                skyGlowGradientColors = listOf(
                    Color(0xFFFF7043).copy(alpha = 0.35f),
                    Color(0xFFFF8A65).copy(alpha = 0.12f),
                    Color(0xFFFF7043).copy(alpha = 0.01f)
                ),
                rayIntensity = 0.45f + 0.30f * t,
                diskScale = 1.18f - 0.08f * t
            )
        }
        // 2. 晨光跃升 (0.12 ~ 0.35)：璀璨金橙与亮金
        p < 0.35f -> {
            val t = (p - 0.12f) / 0.23f
            SolarCardVisualState(
                photosphereCenterColor = Color.White,
                photosphereEdgeColor = Color(0xFFFFD54F),
                innerCoronaColor = Color(0xFFFFCA28).copy(alpha = 0.75f),
                outerHaloColor = Color(0xFFFFB300).copy(alpha = 0.32f),
                diffractionRayColor = Color(0xFFFFF176).copy(alpha = 0.85f),
                skyGlowGradientColors = listOf(
                    Color(0xFFFFD54F).copy(alpha = 0.32f),
                    Color(0xFFFFE082).copy(alpha = 0.10f),
                    Color(0xFFFFD54F).copy(alpha = 0.01f)
                ),
                rayIntensity = 0.75f + 0.25f * t,
                diskScale = 1.10f - 0.10f * t
            )
        }
        // 3. 烈日正午 (0.35 ~ 0.65)：纯白极炽光核 (6500K)
        p <= 0.65f -> {
            SolarCardVisualState(
                photosphereCenterColor = Color.White,
                photosphereEdgeColor = Color(0xFFFFFDE7),
                innerCoronaColor = Color(0xFFFFE082).copy(alpha = 0.80f),
                outerHaloColor = Color(0xFFFFD54F).copy(alpha = 0.35f),
                diffractionRayColor = Color(0xFFFFF9C4).copy(alpha = 0.95f),
                skyGlowGradientColors = listOf(
                    Color(0xFFFFD54F).copy(alpha = 0.30f),
                    Color(0xFFFFF59D).copy(alpha = 0.08f),
                    Color(0xFFFFD54F).copy(alpha = 0.01f)
                ),
                rayIntensity = 1.00f,
                diskScale = 1.00f
            )
        }
        // 4. 午后斜阳 (0.65 ~ 0.88)：温润香槟金与暖琥珀金
        p < 0.88f -> {
            val t = (p - 0.65f) / 0.23f
            SolarCardVisualState(
                photosphereCenterColor = Color.White,
                photosphereEdgeColor = Color(0xFFFFB74D),
                innerCoronaColor = Color(0xFFFF9800).copy(alpha = 0.75f),
                outerHaloColor = Color(0xFFF57C00).copy(alpha = 0.30f),
                diffractionRayColor = Color(0xFFFFE082).copy(alpha = 0.85f),
                skyGlowGradientColors = listOf(
                    Color(0xFFFFB74D).copy(alpha = 0.32f),
                    Color(0xFFFFCC80).copy(alpha = 0.10f),
                    Color(0xFFFFB74D).copy(alpha = 0.01f)
                ),
                rayIntensity = 1.00f - 0.30f * t,
                diskScale = 1.00f + 0.10f * t
            )
        }
        // 5. 晚霞落日 (0.88 ~ 1.00)：浓郁壮丽晚霞落日熔金
        else -> {
            val t = (p - 0.88f) / 0.12f
            SolarCardVisualState(
                photosphereCenterColor = Color(0xFFFFF3E0),
                photosphereEdgeColor = Color(0xFFFF3D00),
                innerCoronaColor = Color(0xFFFF5722).copy(alpha = 0.75f),
                outerHaloColor = Color(0xFFD84315).copy(alpha = 0.35f),
                diffractionRayColor = Color(0xFFFF8A65).copy(alpha = 0.80f),
                skyGlowGradientColors = listOf(
                    Color(0xFFFF5722).copy(alpha = 0.36f),
                    Color(0xFFE64A19).copy(alpha = 0.12f),
                    Color(0xFFFF5722).copy(alpha = 0.01f)
                ),
                rayIntensity = 0.70f - 0.30f * t,
                diskScale = 1.10f + 0.10f * t
            )
        }
    }
}

/**
 * 绘制高保真拟真发光太阳天体图形
 *
 * 采用 Compose 原生硬件加速 5 重物理光学层次：
 * 1. 广阔大气瑞利散射柔光外晕（超平滑向外消散）；
 * 2. 中层等离子日冕辉光球（充盈饱满发光体）；
 * 3. 8 束纤细通透自转衍射星芒微羽（4 束主光芒 + 4 束次光芒，双向渐变衰减）；
 * 4. 日盘本体高光过渡层（依据色温动态变化）；
 * 5. 日盘中心纯白极炽光核（高能光焦点）。
 *
 * @param center 太阳中心屏幕坐标 [Offset]
 * @param state 太阳实时光学视觉状态 [SolarCardVisualState]
 * @param pulse 呼吸脉动缩放系数
 * @param rotationDeg 星芒自转角度 (0° ~ 360°)
 * @param isNight 是否处于夜间模式
 */
private fun DrawScope.drawPhotorealisticSun(
    center: Offset,
    state: SolarCardVisualState,
    pulse: Float,
    rotationDeg: Float,
    isNight: Boolean
) {
    if (isNight) {
        // 夜间模式：地平线下方潜行天体微弱暗光标记
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF90CAF9).copy(alpha = 0.35f),
                    Color(0xFF1E88E5).copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = center,
                radius = 10.dp.toPx()
            ),
            radius = 10.dp.toPx(),
            center = center
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.50f),
            radius = 3.2.dp.toPx(),
            center = center
        )
        return
    }

    val baseRadius = 6.4.dp.toPx() * state.diskScale * pulse

    // 1. 最外层广阔大气瑞利散射柔光外晕
    val outerHaloRadius = baseRadius * 3.8f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                state.outerHaloColor,
                state.outerHaloColor.copy(alpha = state.outerHaloColor.alpha * 0.38f),
                Color.Transparent
            ),
            center = center,
            radius = outerHaloRadius
        ),
        radius = outerHaloRadius,
        center = center
    )

    // 2. 中层等离子日冕辉光球
    val coronaRadius = baseRadius * 2.15f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                state.innerCoronaColor,
                state.innerCoronaColor.copy(alpha = state.innerCoronaColor.alpha * 0.32f),
                Color.Transparent
            ),
            center = center,
            radius = coronaRadius
        ),
        radius = coronaRadius,
        center = center
    )

    // 3. 8 束纤细自转衍射星芒微羽 (Diffraction Spikes)
    if (state.rayIntensity > 0.08f) {
        val rayAlpha = (state.rayIntensity * 0.88f).coerceIn(0f, 1f)
        rotate(degrees = rotationDeg, pivot = center) {
            // 4 束长主星芒 (0°, 90°, 180°, 270°)
            val majorRayLen = baseRadius * 2.8f
            for (i in 0 until 2) {
                val angleRad = (i * 90f) * (PI.toFloat() / 180f)
                val p1 = Offset(center.x + cos(angleRad) * majorRayLen, center.y + sin(angleRad) * majorRayLen)
                val p2 = Offset(center.x - cos(angleRad) * majorRayLen, center.y - sin(angleRad) * majorRayLen)
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            state.diffractionRayColor.copy(alpha = rayAlpha * 0.90f),
                            Color.White.copy(alpha = rayAlpha),
                            state.diffractionRayColor.copy(alpha = rayAlpha * 0.90f),
                            Color.Transparent
                        ),
                        start = p1,
                        end = p2
                    ),
                    start = p1,
                    end = p2,
                    strokeWidth = 1.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            // 4 束次星芒 (45°, 135°, 225°, 315°)
            val minorRayLen = baseRadius * 1.95f
            for (i in 0 until 2) {
                val angleRad = (45f + i * 90f) * (PI.toFloat() / 180f)
                val p1 = Offset(center.x + cos(angleRad) * minorRayLen, center.y + sin(angleRad) * minorRayLen)
                val p2 = Offset(center.x - cos(angleRad) * minorRayLen, center.y - sin(angleRad) * minorRayLen)
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            state.diffractionRayColor.copy(alpha = rayAlpha * 0.55f),
                            state.photosphereCenterColor.copy(alpha = rayAlpha * 0.75f),
                            state.diffractionRayColor.copy(alpha = rayAlpha * 0.55f),
                            Color.Transparent
                        ),
                        start = p1,
                        end = p2
                    ),
                    start = p1,
                    end = p2,
                    strokeWidth = 1.0.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }

    // 4. 日盘本体高光过渡层
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                state.photosphereCenterColor,
                state.photosphereEdgeColor,
                state.innerCoronaColor.copy(alpha = 0.50f)
            ),
            center = center,
            radius = baseRadius
        ),
        radius = baseRadius,
        center = center
    )

    // 5. 日盘中心纯白极炽光核
    drawCircle(
        color = Color.White,
        radius = baseRadius * 0.46f,
        center = center
    )
}

/**
 * 真实日出日落卡片组件
 *
 * 结合当前城市地理经纬度与 NOAA 高精度天文算法，展示当日日出日落时间、写实太阳实时运行轨迹拱弧与白昼倒计时。
 * 太阳图形采用高保真 5 重物理光学模型，支持色温演变、自转衍射星芒与呼吸脉动。
 *
 * @param city 当前城市实体 [CityInfo]
 * @param modifier 外部修饰符
 */
@Composable
private fun SunriseSunsetRealCard(
    city: CityInfo,
    modifier: Modifier = Modifier
) {
    // 实时系统时钟（每秒自动校准，用户在系统设置修改时间或切回 App 时即时刷新生效）
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

    // 太阳图形呼吸与星芒自转动画
    val animTransition = rememberInfiniteTransition(label = "CardSunAnim")
    val sunPulse by animTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunPulse"
    )
    val rayRotation by animTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rayRotation"
    )

    val calendar = remember(currentSystemTimeMillis / 10000L) {
        Calendar.getInstance().apply { timeInMillis = currentSystemTimeMillis }
    }

    val celestial = remember(city.getCacheKey(), currentSystemTimeMillis / 10000L) {
        SunMoonCalculator.calculateCelestialTimes(city, calendar)
    }

    val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    val sunriseStr = formatMinutesToTime(celestial.sunriseMinutes)
    val sunsetStr = formatMinutesToTime(celestial.sunsetMinutes)

    // 计算距离下一个事件（日落或日出）的剩余时间
    val isNight = celestial.isNight
    val (primaryTime, remainingText) = if (!isNight) {
        // 白天：主要聚焦今日日落时刻
        val remaining = (celestial.sunsetMinutes - currentMinutes).coerceAtLeast(0)
        val remH = remaining / 60
        val remM = remaining % 60
        val remDesc = if (remH > 0) "${remH}小时${remM}分" else "${remM}分钟"
        Pair(sunsetStr, "距日落还有 $remDesc")
    } else {
        // 夜间：主要聚焦次日日出时刻
        val remaining = if (currentMinutes >= celestial.sunsetMinutes) {
            celestial.sunriseMinutes + 1440 - currentMinutes
        } else {
            celestial.sunriseMinutes - currentMinutes
        }.coerceAtLeast(0)
        val remH = remaining / 60
        val remM = remaining % 60
        val remDesc = if (remH > 0) "${remH}小时${remM}分" else "${remM}分钟"
        Pair(sunriseStr, "距日出还有 $remDesc")
    }

    val sunProgress = celestial.sunProgress.coerceIn(0f, 1f)
    val solarVisualState = remember(sunProgress, isNight) {
        calculateCardSolarVisualState(sunProgress, isNight)
    }

    MetricBaseCard(
        icon = Icons.Default.WbSunny,
        title = "日出日落",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            // 1. 顶部大字展示下一个事件时刻（如 18:37）
            Text(
                text = primaryTime,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            // 2. 大字下方提示小字（与其他卡片小字完全一致的统一风格：11.sp, alpha = 0.70f）
            Text(
                text = remainingText,
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            // 3. 太阳天球拱形轨迹 Canvas
            val arcPath = remember { Path() }
            val passedPath = remember { Path() }
            val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f) }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                val w = size.width
                val h = size.height

                val startX = 6.dp.toPx()
                val endX = w - 6.dp.toPx()
                val horizonY = h - 2.5.dp.toPx()
                val arcHeight = h * 0.86f

                // 1. 绘制地平线渐变细线
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.40f),
                            Color.White.copy(alpha = 0.40f),
                            Color.White.copy(alpha = 0.10f)
                        ),
                        startX = startX - 4.dp.toPx(),
                        endX = endX + 4.dp.toPx()
                    ),
                    start = Offset(startX - 4.dp.toPx(), horizonY),
                    end = Offset(endX + 4.dp.toPx(), horizonY),
                    strokeWidth = 1.2f,
                    cap = StrokeCap.Round
                )

                // 2. 绘制完整白昼拱形轨迹路径
                arcPath.reset()
                arcPath.moveTo(startX, horizonY)
                arcPath.cubicTo(
                    startX + (endX - startX) * 0.22f, horizonY - arcHeight * 1.10f,
                    startX + (endX - startX) * 0.78f, horizonY - arcHeight * 1.10f,
                    endX, horizonY
                )

                // 轨迹底虚线
                drawPath(
                    path = arcPath,
                    color = Color.White.copy(alpha = 0.32f),
                    style = Stroke(
                        width = 1.8f,
                        cap = StrokeCap.Round,
                        pathEffect = dashEffect
                    )
                )

                // 3. 计算当前太阳位置
                val sunX = startX + sunProgress * (endX - startX)
                val sunY = horizonY - sin(sunProgress * PI.toFloat()) * (arcHeight * 0.88f)

                // 如果处于白天，绘制已走过轨迹的天光漫射渐变光幕与高亮弧线
                if (!isNight && sunProgress > 0f) {
                    passedPath.reset()
                    passedPath.moveTo(startX, horizonY)
                    val stepCount = (sunProgress * 32).toInt().coerceAtLeast(2)
                    for (i in 1..stepCount) {
                        val t = (i.toFloat() / 32f).coerceAtMost(sunProgress)
                        val px = startX + t * (endX - startX)
                        val py = horizonY - sin(t * PI.toFloat()) * (arcHeight * 0.88f)
                        passedPath.lineTo(px, py)
                    }
                    passedPath.lineTo(sunX, horizonY)
                    passedPath.close()

                    drawPath(
                        path = passedPath,
                        brush = Brush.verticalGradient(
                            colors = solarVisualState.skyGlowGradientColors,
                            startY = sunY,
                            endY = horizonY
                        )
                    )
                }

                // 4. 地平线日出/日落端点精致光标 (Sunrise & Sunset Horizon Anchors)
                // 日出锚点
                drawCircle(
                    color = Color(0xFFFFAB91).copy(alpha = 0.45f),
                    radius = 3.0.dp.toPx(),
                    center = Offset(startX, horizonY)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.90f),
                    radius = 1.5.dp.toPx(),
                    center = Offset(startX, horizonY)
                )
                // 日落锚点
                drawCircle(
                    color = Color(0xFFFF8A65).copy(alpha = 0.45f),
                    radius = 3.0.dp.toPx(),
                    center = Offset(endX, horizonY)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.90f),
                    radius = 1.5.dp.toPx(),
                    center = Offset(endX, horizonY)
                )

                // 5. 绘制高保真拟真 5 重物理光学太阳天体图形
                val renderSunCenter = if (!isNight) {
                    Offset(sunX, sunY)
                } else {
                    Offset(
                        if (currentMinutes >= celestial.sunsetMinutes) endX else startX,
                        horizonY + 3.dp.toPx()
                    )
                }

                drawPhotorealisticSun(
                    center = renderSunCenter,
                    state = solarVisualState,
                    pulse = sunPulse,
                    rotationDeg = rayRotation,
                    isNight = isNight
                )
            }

            // 4. 拱形轨迹与下方时间小字之间的留白间隔
            Spacer(modifier = Modifier.height(3.dp))

            // 5. 地平线两端日出日落时间标注（纯白色高保真文字）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "日出 $sunriseStr",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = "日落 $sunsetStr",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

// ==================== 通用双列基础毛玻璃卡片 ====================

/**
 * 通用双列基础毛玻璃卡片组件容器
 *
 * 统一所有 2x2 指标卡片的高度为 152.dp，并采用 SpaceBetween 布局，使各个卡片在网格中高度严格齐平对齐。
 * 启用独立硬件渲染图层缓存 (graphicsLayer)，有效隔绝滚动过程中的重绘扩散。
 *
 * @param icon 卡片左上角气象指标图标 [ImageVector]
 * @param title 卡片标题
 * @param modifier 外部修饰符
 * @param content 卡片内部内容插槽
 */
@Composable
private fun MetricBaseCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(152.dp)
            .graphicsLayer {
                // 开启独立硬件渲染图层缓存
                clip = true
                shape = RoundedCornerShape(20.dp)
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x7514263A))
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
        }

        content()
    }
}

