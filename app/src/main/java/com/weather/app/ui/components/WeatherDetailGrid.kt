package com.weather.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weather.app.model.AirQuality
import com.weather.app.model.CurrentWeather
import com.weather.app.model.WeatherData
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 气象真实观测指标详情宫格组件
 *
 * 严格遵循真实数据原则：所有卡片必须由气象台返回的真实数据字段构建；
 * 若某项数据缺失或为无效值（9999/空），则该卡片直接不渲染展示，杜绝假数据。
 * 所有卡片高度保持严格一致（152.dp），除主界面的当前温度外其余文字均不加粗。
 *
 * @param weatherData 聚合天气数据模型 [WeatherData]
 * @param modifier 外部修饰符
 */
@Composable
fun WeatherDetailGrid(
    weatherData: WeatherData,
    modifier: Modifier = Modifier
) {
    val current = weatherData.current
    val aqi = weatherData.airQuality

    // 收集所有有效真实存在的指标卡片内容
    val validCards = mutableListOf<@Composable (Modifier) -> Unit>()

    // 1. 空气质量卡片 (仅当存在真实 AQI 数据时展示)
    if (aqi != null && aqi.aqi > 0 && aqi.aqi != 9999) {
        validCards.add { mod -> AirQualityRealCard(aqi = aqi, modifier = mod) }
    }

    // 2. 体感温度卡片 (仅当存在真实体感或温度差时展示)
    if (current.feelsLike != null && current.feelsLike != 9999.0) {
        validCards.add { mod -> FeelsLikeRealCard(current = current, modifier = mod) }
    }

    // 3. 风向风速卡片 (仅当存在真实风力数据时展示)
    if (current.windDirection.isNotEmpty() || current.windPower.isNotEmpty() || current.windSpeed > 0.0) {
        validCards.add { mod -> WindRealCard(current = current, modifier = mod) }
    }

    // 4. 相对湿度卡片 (仅当存在真实湿度数据时展示)
    if (current.humidity > 0.0 && current.humidity != 9999.0) {
        validCards.add { mod -> HumidityRealCard(humidity = current.humidity.toInt(), modifier = mod) }
    }

    // 5. 大气压强卡片 (仅当存在真实气压数据时展示)
    if (current.pressure > 0.0 && current.pressure != 9999.0) {
        validCards.add { mod -> PressureRealCard(pressureHpa = current.pressure.toInt(), modifier = mod) }
    }

    // 6. 实时降水量卡片 (仅当发生真实降水时展示)
    if (current.precipitation > 0.0 && current.precipitation != 9999.0) {
        validCards.add { mod -> PrecipitationRealCard(precipitation = current.precipitation, modifier = mod) }
    }

    if (validCards.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 双列自适应排布，各行等高齐平
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

        // 底部真实数据来源与发布时刻
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                    color = Color.White.copy(alpha = 0.45f),
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
 * 真实风向风力罗盘卡片组件（100% 精确对齐设计图：外圈密集刻度与北东南西 + 居中深色表盘“2 级” + 贯穿式圆球箭尾与导向箭头）
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

    // 解析风向对应角度（气象风向定义：风吹来的方向）
    // 箭头指示风吹去的去向：吹去角度 = 吹来角度 + 180°
    val windAngleDeg = when {
        displayDirection.contains("西北") -> 135f // 西北风吹向东南
        displayDirection.contains("东北") -> 225f // 东北风吹向西南
        displayDirection.contains("西南") -> 45f  // 西南风吹向东北
        displayDirection.contains("东南") -> 315f // 东南风吹向西北
        displayDirection.contains("北") -> 90f   // 北风吹向南
        displayDirection.contains("南") -> 270f  // 南风吹向北
        displayDirection.contains("东") -> 180f  // 东风吹向西
        displayDirection.contains("西") -> 0f    // 西风吹向东
        else -> 135f
    }

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
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(72.dp)) {
                    val r = size.width / 2f
                    val c = Offset(r, r)

                    // 1. 外圈 40 根放射状刻度线 (加粗加长，避开北、东、南、西四个文字方位)
                    for (i in 0 until 40) {
                        val angleDeg = i * 9f
                        // 避开 0°(东), 90°(南), 180°(西), 270°(北) 四个文字正切点
                        if (angleDeg % 90f !in 81f..99f && angleDeg % 90f !in 0f..9f && angleDeg % 90f !in 171f..180f) {
                            val angleRad = angleDeg * (PI.toFloat() / 180f)
                            val p1 = Offset(c.x + (r - 1f) * cos(angleRad), c.y + (r - 1f) * sin(angleRad))
                            val p2 = Offset(c.x + (r - 8.5f) * cos(angleRad), c.y + (r - 8.5f) * sin(angleRad))
                            drawLine(
                                color = Color.White.copy(alpha = 0.45f),
                                start = p1,
                                end = p2,
                                strokeWidth = 2.2f,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // 2. 居中深色半透明圆形表盘底衬 (实心圆盘)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.15f),
                        radius = r * 0.56f,
                        center = c
                    )

                    // 3. 贯穿式白色风向指示箭头 (箭尾大白球 + 贯穿线 + 导向粗三角形)
                    val rad = windAngleDeg * (PI.toFloat() / 180f)
                    val oppRad = (windAngleDeg + 180f) * (PI.toFloat() / 180f)

                    val startPt = Offset(c.x + (r * 0.82f) * cos(oppRad), c.y + (r * 0.82f) * sin(oppRad))
                    val endPt = Offset(c.x + (r * 0.88f) * cos(rad), c.y + (r * 0.88f) * sin(rad))

                    // 贯穿实心线 (加粗)
                    drawLine(
                        color = Color.White,
                        start = startPt,
                        end = endPt,
                        strokeWidth = 2.8f,
                        cap = StrokeCap.Round
                    )

                    // 箭尾实心大白圆点
                    drawCircle(
                        color = Color.White,
                        radius = 4.8f,
                        center = startPt
                    )

                    // 导向实心粗三角形箭头
                    val arrowLen = 9.5f
                    val arrowAngle1 = rad + 148f * (PI.toFloat() / 180f)
                    val arrowAngle2 = rad - 148f * (PI.toFloat() / 180f)

                    val a1 = Offset(endPt.x + arrowLen * cos(arrowAngle1), endPt.y + arrowLen * sin(arrowAngle1))
                    val a2 = Offset(endPt.x + arrowLen * cos(arrowAngle2), endPt.y + arrowLen * sin(arrowAngle2))

                    val arrowPath = Path().apply {
                        moveTo(endPt.x, endPt.y)
                        lineTo(a1.x, a1.y)
                        lineTo(a2.x, a2.y)
                        close()
                    }
                    drawPath(path = arrowPath, color = Color.White)
                }

                // 外圈四方位文字 (北、东、南、西)
                Box(modifier = Modifier.size(72.dp)) {
                    Text(text = "北", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, modifier = Modifier.align(Alignment.TopCenter))
                    Text(text = "东", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterEnd))
                    Text(text = "南", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomCenter))
                    Text(text = "西", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterStart))
                }

                // 居中大字风级数字与单位 (如 2 级)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$powerLevel",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 24.sp
                    )
                    Text(
                        text = "级",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

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
                    .height(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(72.dp)) {
                    val r = size.width / 2f
                    val c = Offset(r, r)

                    val startAngleDeg = 135f
                    val totalSweepDeg = 270f
                    val tickCount = 32

                    val sweepProgressAngle = progress * totalSweepDeg
                    val activeTickThreshold = (progress * tickCount).toInt()

                    // 1. 绘制激活区域的扇形半透明高亮弧带 (加宽加亮)
                    if (sweepProgressAngle > 0f) {
                        drawArc(
                            color = Color.White.copy(alpha = 0.20f),
                            startAngle = startAngleDeg,
                            sweepAngle = sweepProgressAngle,
                            useCenter = false,
                            topLeft = Offset(5f, 5f),
                            size = Size(size.width - 10f, size.height - 10f),
                            style = Stroke(width = 9.5f, cap = StrokeCap.Butt)
                        )
                    }

                    // 2. 绘制 32 根放射状加粗加长刻度线
                    for (i in 0..tickCount) {
                        val angleDeg = startAngleDeg + (i.toFloat() / tickCount.toFloat()) * totalSweepDeg
                        val angleRad = angleDeg * (PI.toFloat() / 180f)

                        val isActive = i <= activeTickThreshold
                        val tickColor = if (isActive) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.25f)
                        val tickLen = 9.0f

                        val p1 = Offset(c.x + (r - 0.5f) * cos(angleRad), c.y + (r - 0.5f) * sin(angleRad))
                        val p2 = Offset(c.x + (r - 0.5f - tickLen) * cos(angleRad), c.y + (r - 0.5f - tickLen) * sin(angleRad))

                        drawLine(
                            color = tickColor,
                            start = p1,
                            end = p2,
                            strokeWidth = if (isActive) 2.5f else 1.8f,
                            cap = StrokeCap.Round
                        )
                    }

                    // 3. 绘制当前气压纯白圆角胶囊短粗指针 (明显加粗加长，突出于圆弧内外)
                    val curAngleDeg = startAngleDeg + sweepProgressAngle
                    val curAngleRad = curAngleDeg * (PI.toFloat() / 180f)
                    val needleP1 = Offset(c.x + (r + 3.0f) * cos(curAngleRad), c.y + (r + 3.0f) * sin(curAngleRad))
                    val needleP2 = Offset(c.x + (r - 11.0f) * cos(curAngleRad), c.y + (r - 11.0f) * sin(curAngleRad))

                    drawLine(
                        color = Color.White,
                        start = needleP1,
                        end = needleP2,
                        strokeWidth = 4.2f,
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
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 24.sp
                    )
                    Text(
                        text = "百帕",
                        color = Color.White.copy(alpha = 0.80f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

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

// ==================== 通用双列基础毛玻璃卡片 ====================

/**
 * 通用双列基础毛玻璃卡片组件容器
 *
 * 统一所有 2x2 指标卡片的高度为 152.dp，并采用 SpaceBetween 布局，使各个卡片在网格中高度严格齐平对齐。
 * 具有深色半透明磨砂底色，有效遮挡底层天空雨滴，避免文字被雨滴穿透覆盖。
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

