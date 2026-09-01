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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Public
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
 * 气象观测指标卡片类型枚举
 *
 * 用于静态布局插槽路由，避免动态创建 Composable Lambda 列表。
 */
enum class MetricCardType {
    /** 真实空气质量卡片 */
    AIR_QUALITY,
    /** 真实日出日落天体轨迹卡片 */
    SUNRISE_SUNSET,
    /** 真实昼夜晨昏线卡片 */
    EARTH_DAYLIGHT,
    /** 真实体感温度卡片 */
    FEELS_LIKE,
    /** 真实风向风速罗盘卡片 */
    WIND,
    /** 真实相对湿度卡片 */
    HUMIDITY,
    /** 真实大气压强仪表盘卡片 */
    PRESSURE,
    /** 真实实时降水量卡片 */
    PRECIPITATION,
    /** 真实紫外线强度卡片 */
    UV_INDEX,
    /** 真实水平能见度卡片 */
    VISIBILITY,
    /** 真实 3D 拟真月相小卡片 */
    MOON_PHASE,
    /** 真实生活气象指数小卡片 */
    LIFE_INDEX,
    /** 真实定位气象小地图卡片 */
    LOCATION_MAP
}

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
 * @param onSunriseSunsetClick 点击日出日落卡片跳转日出日落详情页回调
 * @param onEarthDaylightClick 点击昼夜晨昏线卡片跳转地球实时日光模拟器回调
 * @param onMoonPhaseClick 点击月相卡片跳转月相全屏详情页面回调
 * @param onLifeIndexClick 点击生活指数卡片跳转/呼出生活指数全量详情抽屉回调
 * @param mapLayerType 定位小地图图层类型
 * @param onLocationMapClick 点击定位小地图卡片跳转大地图页面回调
 * @param modifier 外部修饰符
 */
@Composable
fun WeatherDetailGrid(
    weatherData: WeatherData,
    cardConfig: CardDisplayConfig = CardDisplayConfig(),
    lastUpdatedText: String = "",
    onSunriseSunsetClick: () -> Unit = {},
    onEarthDaylightClick: () -> Unit = {},
    onMoonPhaseClick: () -> Unit = {},
    onLifeIndexClick: () -> Unit = {},
    mapLayerType: String = "dark",
    onLocationMapClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val current = weatherData.current
    val aqi = weatherData.airQuality

    // 预计算所有有效真实存在且用户已开启的指标卡片类型，严格按用户指定顺序排序：
    // 空气质量 -> 紫外线 -> 能见度 -> 体感温度 -> 风 -> 气压 -> 湿度 -> 降水 -> 昼夜晨昏线 -> 日出日落
    val validCardTypes = remember(cardConfig, aqi, current) {
        val types = mutableListOf<MetricCardType>()
        // 1. 空气质量
        if (cardConfig.showAirQuality && aqi != null && aqi.aqi > 0 && aqi.aqi != 9999) {
            types.add(MetricCardType.AIR_QUALITY)
        }
        // 2. 紫外线
        if (cardConfig.showUvIndex && current.uvIndex != null && current.uvIndex >= 0.0 && current.uvIndex != 9999.0) {
            types.add(MetricCardType.UV_INDEX)
        }
        // 3. 水平能见度 (排在体感温度之上)
        if (cardConfig.showVisibility && current.visibility != null && current.visibility > 0.0 && current.visibility != 9999.0) {
            types.add(MetricCardType.VISIBILITY)
        }
        // 4. 体感温度
        if (cardConfig.showFeelsLike && current.feelsLike != null && current.feelsLike != 9999.0) {
            types.add(MetricCardType.FEELS_LIKE)
        }
        // 5. 风向风速
        if (cardConfig.showWind && (current.windDirection.isNotEmpty() || current.windPower.isNotEmpty() || current.windSpeed > 0.0)) {
            types.add(MetricCardType.WIND)
        }
        // 6. 大气气压
        if (cardConfig.showPressure && current.pressure > 0.0 && current.pressure != 9999.0) {
            types.add(MetricCardType.PRESSURE)
        }
        // 7. 相对湿度
        if (cardConfig.showHumidity && current.humidity > 0.0 && current.humidity != 9999.0) {
            types.add(MetricCardType.HUMIDITY)
        }
        // 8. 实时降水量
        if (cardConfig.showPrecipitation && current.precipitation > 0.0 && current.precipitation != 9999.0) {
            types.add(MetricCardType.PRECIPITATION)
        }
        // 9. 昼夜晨昏线
        if (cardConfig.showEarthDaylight) {
            types.add(MetricCardType.EARTH_DAYLIGHT)
        }
        // 10. 日出日落
        if (cardConfig.showSunriseSunset) {
            types.add(MetricCardType.SUNRISE_SUNSET)
        }
        // 11. 月相
        if (cardConfig.showMoonPhase) {
            types.add(MetricCardType.MOON_PHASE)
        }
        // 12. 生活指数
        if (cardConfig.showLifeIndex) {
            types.add(MetricCardType.LIFE_INDEX)
        }
        // 13. 定位地图
        if (cardConfig.showLocationMap) {
            types.add(MetricCardType.LOCATION_MAP)
        }
        types
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. 双列自适应排列所有详细指标小卡片（包含生活指数、月相与定位地图小卡片）
        val rowCount = (validCardTypes.size + 1) / 2
        for (rowIndex in 0 until rowCount) {
            val firstIdx = rowIndex * 2
            val secondIdx = firstIdx + 1

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCardSlot(
                    type = validCardTypes[firstIdx],
                    weatherData = weatherData,
                    onSunriseSunsetClick = onSunriseSunsetClick,
                    onEarthDaylightClick = onEarthDaylightClick,
                    onMoonPhaseClick = onMoonPhaseClick,
                    onLifeIndexClick = onLifeIndexClick,
                    mapLayerType = mapLayerType,
                    onLocationMapClick = onLocationMapClick,
                    modifier = Modifier.weight(1f)
                )

                if (secondIdx < validCardTypes.size) {
                    MetricCardSlot(
                        type = validCardTypes[secondIdx],
                        weatherData = weatherData,
                        onSunriseSunsetClick = onSunriseSunsetClick,
                        onEarthDaylightClick = onEarthDaylightClick,
                        onMoonPhaseClick = onMoonPhaseClick,
                        onLifeIndexClick = onLifeIndexClick,
                        mapLayerType = mapLayerType,
                        onLocationMapClick = onLocationMapClick,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // 2. 页面最底部版本号、上次刷新时刻与真实数据来源及发布时刻说明文字
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 0.dp),
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

            // 刷新时间移至上方
            if (lastUpdatedText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.5.dp))
                Text(
                    text = lastUpdatedText,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(2.5.dp))

            // 数据源与发布时刻（文案：数据源自：数据源名，发布时间：00:00）
            val cleanPublishTime = current.publishTime.removeSuffix("发布").removeSuffix(" 发布").trim()
            val sourceAndPublishText = if (cleanPublishTime.isNotEmpty()) {
                "数据源自：${weatherData.sourceName}，发布时间：$cleanPublishTime"
            } else {
                "数据源自：${weatherData.sourceName}"
            }

            Text(
                text = sourceAndPublishText,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

/**
 * 气象指标卡片静态分发插槽
 *
 * 依据卡片类型静态路由到对应的独立 Composable 组件，
 * 避免动态生成 Lambda 列表，确保 Compose 编译器能够对各个卡片执行智能重组跳过。
 *
 * @param type 指标卡片类型枚举 [MetricCardType]
 * @param weatherData 聚合天气数据模型 [WeatherData]
 * @param onSunriseSunsetClick 点击日出日落卡片跳转日出日落详情页回调
 * @param onEarthDaylightClick 点击昼夜晨昏线卡片跳转地球实时日光模拟器回调
 * @param onMoonPhaseClick 点击月相卡片跳转月相全屏详情页面回调
 * @param onLifeIndexClick 点击生活指数卡片呼出全量生活指数详情抽屉回调
 * @param mapLayerType 定位小地图图层类型
 * @param onLocationMapClick 点击定位小地图卡片跳转大地图页面回调
 * @param modifier 外部修饰符
 */
@Composable
private fun MetricCardSlot(
    type: MetricCardType,
    weatherData: WeatherData,
    onSunriseSunsetClick: () -> Unit,
    onEarthDaylightClick: () -> Unit,
    onMoonPhaseClick: () -> Unit,
    onLifeIndexClick: () -> Unit,
    mapLayerType: String,
    onLocationMapClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val current = weatherData.current
    val aqi = weatherData.airQuality

    when (type) {
        MetricCardType.AIR_QUALITY -> {
            if (aqi != null && aqi.aqi > 0 && aqi.aqi != 9999) {
                AirQualityRealCard(aqi = aqi, modifier = modifier)
            } else {
                Spacer(modifier = modifier)
            }
        }
        MetricCardType.SUNRISE_SUNSET -> {
            SunriseSunsetRealCard(
                city = weatherData.city,
                onClick = onSunriseSunsetClick,
                modifier = modifier
            )
        }
        MetricCardType.EARTH_DAYLIGHT -> {
            EarthDaylightRealCard(
                city = weatherData.city,
                onClick = onEarthDaylightClick,
                modifier = modifier
            )
        }
        MetricCardType.FEELS_LIKE -> {
            FeelsLikeRealCard(current = current, modifier = modifier)
        }
        MetricCardType.WIND -> {
            WindRealCard(current = current, modifier = modifier)
        }
        MetricCardType.HUMIDITY -> {
            HumidityRealCard(humidity = current.humidity.toInt(), modifier = modifier)
        }
        MetricCardType.PRESSURE -> {
            PressureRealCard(pressureHpa = current.pressure.toInt(), modifier = modifier)
        }
        MetricCardType.PRECIPITATION -> {
            PrecipitationRealCard(precipitation = current.precipitation, modifier = modifier)
        }
        MetricCardType.UV_INDEX -> {
            if (current.uvIndex != null && current.uvIndex >= 0.0) {
                UvIndexRealCard(uvIndex = current.uvIndex, modifier = modifier)
            } else {
                Spacer(modifier = modifier)
            }
        }
        MetricCardType.VISIBILITY -> {
            if (current.visibility != null && current.visibility > 0.0) {
                VisibilityRealCard(visibilityKm = current.visibility, modifier = modifier)
            } else {
                Spacer(modifier = modifier)
            }
        }
        MetricCardType.MOON_PHASE -> {
            MoonPhaseRealCard(
                city = weatherData.city,
                onClick = onMoonPhaseClick,
                modifier = modifier
            )
        }
        MetricCardType.LIFE_INDEX -> {
            LifeIndexRealCard(
                lifeIndex = weatherData.lifeIndex,
                onClick = onLifeIndexClick,
                modifier = modifier
            )
        }
        MetricCardType.LOCATION_MAP -> {
            LocationMapCard(
                city = weatherData.city,
                weatherData = weatherData,
                mapLayerType = mapLayerType,
                onClick = onLocationMapClick,
                modifier = modifier
            )
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
    val adviceText = when {
        aqi.aqi <= 50 -> "健康人群无需防护"
        aqi.aqi <= 100 -> "极少数敏感人群应减少户外活动"
        aqi.aqi <= 150 -> "敏感人群应减少户外运动"
        aqi.aqi <= 200 -> "敏感人群避免外出，健康人群减少外出"
        else -> "各类人群应尽量留在室内"
    }

    MetricBaseCard(
        icon = Icons.Default.Eco,
        title = "空气质量",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. 等级与数值（上下纵向排布）
            Column {
                Text(
                    text = aqi.qualityText,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${aqi.aqi}",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. 彩色渐变刻度条 + 白色滑块指示圆点
            val aqiRatio = (aqi.aqi / 300f).coerceIn(0f, 1f)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            ) {
                val w = size.width
                val h = size.height
                val barHeight = 4.dp.toPx()
                val barY = (h - barHeight) / 2f
                val dotRadius = 4.5.dp.toPx()

                // 绘制渐变色条
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF4CAF50), // 绿 (优)
                            Color(0xFF8BC34A), // 浅绿
                            Color(0xFFFFEB3B), // 黄 (良)
                            Color(0xFFFF9800), // 橙 (轻度)
                            Color(0xFFF44336), // 红 (中度)
                            Color(0xFF9C27B0), // 紫 (重度)
                            Color(0xFF795548)  // 褐 (严重)
                        )
                    ),
                    topLeft = Offset(0f, barY),
                    size = Size(w, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barHeight / 2, barHeight / 2)
                )

                // 计算小圆点位置
                val dotX = (dotRadius + aqiRatio * (w - 2 * dotRadius)).coerceIn(dotRadius, w - dotRadius)
                val dotY = h / 2f

                // 绘制白色小圆点滑块及其外发光投影
                drawCircle(
                    color = Color(0x33000000),
                    radius = dotRadius + 1.5.dp.toPx(),
                    center = Offset(dotX, dotY + 0.5.dp.toPx())
                )
                drawCircle(
                    color = Color.White,
                    radius = dotRadius,
                    center = Offset(dotX, dotY)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. 底部健康防护建议说明
            Text(
                text = adviceText,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.5.sp,
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
        // 1. 清晨日出 (0.00 ~ 0.15)：肉眼实景一轮初升朝阳，提高太阳光强度白点值（纯白极炽光核、通透纯净）
        p < 0.15f -> {
            val t = p / 0.15f
            SolarCardVisualState(
                photosphereCenterColor = Color.White,
                photosphereEdgeColor = Color(0xFFFFB300),
                innerCoronaColor = Color(0xFFFFF59D).copy(alpha = 0.80f),
                outerHaloColor = Color(0xFFFFCA28).copy(alpha = 0.38f),
                diffractionRayColor = Color(0xFFFFF9C4).copy(alpha = 0.50f),
                skyGlowGradientColors = listOf(
                    Color(0xFFFFCA28).copy(alpha = 0.45f),
                    Color(0xFFFFB300).copy(alpha = 0.18f),
                    Color(0xFFFFA000).copy(alpha = 0.02f)
                ),
                rayIntensity = 0.0f, // 实景日出不刺眼，纯粹圆润金橙高亮日盘
                diskScale = 1.15f - 0.05f * t
            )
        }
        // 2. 晨光高照 (0.15 ~ 0.35)：太阳升起，由金橙转为璀璨暖金，星芒浮现
        p < 0.35f -> {
            val t = (p - 0.15f) / 0.20f
            SolarCardVisualState(
                photosphereCenterColor = Color(0xFFFFFDE7),
                photosphereEdgeColor = Color(0xFFFFB300),
                innerCoronaColor = Color(0xFFFFCA28).copy(alpha = 0.75f),
                outerHaloColor = Color(0xFFFFC107).copy(alpha = 0.30f),
                diffractionRayColor = Color(0xFFFFF176).copy(alpha = 0.80f),
                skyGlowGradientColors = listOf(
                    Color(0xFFFFCA28).copy(alpha = 0.30f),
                    Color(0xFFFFD54F).copy(alpha = 0.10f),
                    Color(0xFFFFCA28).copy(alpha = 0.01f)
                ),
                rayIntensity = 0.25f + 0.65f * t,
                diskScale = 1.10f - 0.10f * t
            )
        }
        // 3. 烈日正午 (0.35 ~ 0.65)：纯白极炽光核 (6500K) 与耀眼白金光芒
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
        // 4. 午后斜阳 (0.65 ~ 0.85)：温润香槟金与暖琥珀金
        p < 0.85f -> {
            val t = (p - 0.65f) / 0.20f
            SolarCardVisualState(
                photosphereCenterColor = Color(0xFFFFF8E1),
                photosphereEdgeColor = Color(0xFFFFB300),
                innerCoronaColor = Color(0xFFFFCA28).copy(alpha = 0.70f),
                outerHaloColor = Color(0xFFFFA000).copy(alpha = 0.28f),
                diffractionRayColor = Color(0xFFFFE082).copy(alpha = 0.80f),
                skyGlowGradientColors = listOf(
                    Color(0xFFFFB300).copy(alpha = 0.30f),
                    Color(0xFFFFCA28).copy(alpha = 0.09f),
                    Color(0xFFFFB300).copy(alpha = 0.01f)
                ),
                rayIntensity = 0.90f - 0.65f * t,
                diskScale = 1.00f + 0.08f * t
            )
        }
        // 5. 晚霞落日 (0.85 ~ 1.00)：显著增加晚霞值，呈现落日熔金晚霞赤橙与晚霞紫红深赤霞光
        else -> {
            val t = (p - 0.85f) / 0.15f
            SolarCardVisualState(
                photosphereCenterColor = Color(0xFFFFF3E0),
                photosphereEdgeColor = Color(0xFFFF5722),
                innerCoronaColor = Color(0xFFFF5722).copy(alpha = 0.78f),
                outerHaloColor = Color(0xFFE64A19).copy(alpha = 0.38f),
                diffractionRayColor = Color(0xFFFF7043).copy(alpha = 0.35f),
                skyGlowGradientColors = listOf(
                    Color(0xFFFF5722).copy(alpha = 0.55f),
                    Color(0xFFF4511E).copy(alpha = 0.32f),
                    Color(0xFFE91E63).copy(alpha = 0.16f),
                    Color(0xFF880E4F).copy(alpha = 0.04f)
                ),
                rayIntensity = 0.0f, // 实景落日不刺眼，纯粹沉静壮丽晚霞日盘
                diskScale = 1.10f + 0.10f * t
            )
        }
    }
}

/**
 * 绘制人类肉眼实景拟真发光太阳天体图形
 *
 * 遵循自然大气物理与人眼观测生理模型：
 * 1. 日盘实体（Photosphere Solid Disk）：饱满通透的实景日盘，具有温润的双层球体渐变与亚像素超平滑微羽化边缘；
 * 2. 紧贴日冕薄层（Corona Rim）：紧随日盘边缘的温润发光薄环；
 * 3. 广阔天际霞光晕（Ambient Rayleigh Glow）：超平滑融入卡片背景的漫散霞光；
 * 4. 强光衍射星芒（Diffraction Rays）：仅在白天光照刺眼时适度显现，晨昏日出日落时完全自然收敛，呈现一轮纯粹静谧的红日。
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

    val baseRadius = 7.0.dp.toPx() * state.diskScale * pulse

    // 1. 最外层广阔天际霞光柔光晕 (超平滑向外消散)
    val outerHaloRadius = baseRadius * 3.6f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                state.outerHaloColor,
                state.outerHaloColor.copy(alpha = state.outerHaloColor.alpha * 0.40f),
                Color.Transparent
            ),
            center = center,
            radius = outerHaloRadius
        ),
        radius = outerHaloRadius,
        center = center
    )

    // 2. 紧贴日盘的温润日冕辉光环
    val coronaRadius = baseRadius * 1.85f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                state.innerCoronaColor,
                state.innerCoronaColor.copy(alpha = state.innerCoronaColor.alpha * 0.35f),
                Color.Transparent
            ),
            center = center,
            radius = coronaRadius
        ),
        radius = coronaRadius,
        center = center
    )

    // 3. 强光刺眼时的纤细自转衍射星芒微羽 (仅在白昼强光时出现，日出日落为 0 保持纯粹红日)
    if (state.rayIntensity > 0.08f) {
        val rayAlpha = (state.rayIntensity * 0.85f).coerceIn(0f, 1f)
        rotate(degrees = rotationDeg, pivot = center) {
            // 4 束长主星芒 (0°, 90°, 180°, 270°)
            val majorRayLen = baseRadius * 2.6f
            for (i in 0 until 2) {
                val angleRad = (i * 90f) * (PI.toFloat() / 180f)
                val p1 = Offset(center.x + cos(angleRad) * majorRayLen, center.y + sin(angleRad) * majorRayLen)
                val p2 = Offset(center.x - cos(angleRad) * majorRayLen, center.y - sin(angleRad) * majorRayLen)
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            state.diffractionRayColor.copy(alpha = rayAlpha * 0.90f),
                            state.photosphereCenterColor.copy(alpha = rayAlpha),
                            state.diffractionRayColor.copy(alpha = rayAlpha * 0.90f),
                            Color.Transparent
                        ),
                        start = p1,
                        end = p2
                    ),
                    start = p1,
                    end = p2,
                    strokeWidth = 1.3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            // 4 束次星芒 (45°, 135°, 225°, 315°)
            val minorRayLen = baseRadius * 1.85f
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
                    strokeWidth = 0.9.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }

    // 4. 实景日盘本体 (Photosphere Solid Disk)：饱满、纯正、温润的实景日盘
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                state.photosphereCenterColor,
                state.photosphereEdgeColor,
                state.innerCoronaColor.copy(alpha = 0.70f)
            ),
            center = center,
            radius = baseRadius
        ),
        radius = baseRadius,
        center = center
    )

    // 5. 日盘中心微光核 (日出日落为温润赤红光核，正午为纯白高光点)
    drawCircle(
        color = state.photosphereCenterColor,
        radius = baseRadius * 0.40f,
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
 * @param onClick 卡片点击跳转地球实时日光模拟器回调
 * @param modifier 外部修饰符
 */
@Composable
private fun SunriseSunsetRealCard(
    city: CityInfo,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 系统时钟状态：进入前台 (ON_RESUME) 即时校准，前台运行期间每分钟温和更新一次，杜绝秒级重组
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
            delay(60 * 1000L)
            currentSystemTimeMillis = System.currentTimeMillis()
        }
    }

    // 太阳图形呼吸与星芒自转动画 (返回 State<Float> 并在 DrawScope 中读取，彻底避免 Composable 函数体重组)
    val animTransition = rememberInfiniteTransition(label = "CardSunAnim")
    val sunPulseState = animTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunPulse"
    )
    val rayRotationState = animTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rayRotation"
    )

    val calendar = remember(currentSystemTimeMillis / 60000L) {
        Calendar.getInstance().apply { timeInMillis = currentSystemTimeMillis }
    }

    val celestial = remember(city.getCacheKey(), currentSystemTimeMillis / 60000L) {
        SunMoonCalculator.calculateCelestialTimes(city, calendar)
    }

    val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    val sunriseStr = formatMinutesToTime(celestial.sunriseMinutes)
    val sunsetStr = formatMinutesToTime(celestial.sunsetMinutes)

    val isNight = celestial.isNight
    val sunProgress = celestial.sunProgress.coerceIn(0f, 1f)
    val solarVisualState = remember(sunProgress, isNight) {
        calculateCardSolarVisualState(sunProgress, isNight)
    }

    val cardModifier = if (onClick != null) {
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
            .padding(horizontal = 14.dp, vertical = 12.dp)
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
            .padding(horizontal = 14.dp, vertical = 12.dp)
    }

    Column(
        modifier = cardModifier,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. 顶部标题栏（微型太阳图标 + 标题 + 跳转小箭头）
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = "日出日落",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "日出日落",
                    color = Color.White.copy(alpha = 0.70f),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "查看日出日落详情",
                tint = Color.White.copy(alpha = 0.40f),
                modifier = Modifier.size(13.dp)
            )
        }

        // 2. 中间主要区域：居中加大 3D 拟真太阳运行天球拱弧动效 (充满中间区域)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val arcPath = remember { Path() }
            val passedPath = remember { Path() }
            val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f) }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
            ) {
                val w = size.width
                val h = size.height

                val startX = 6.dp.toPx()
                val endX = w - 6.dp.toPx()
                val horizonY = h - 6.dp.toPx()
                val arcHeight = h * 0.78f

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
                    color = Color.White.copy(alpha = 0.35f),
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
                    radius = 3.5.dp.toPx(),
                    center = Offset(startX, horizonY)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.90f),
                    radius = 1.8.dp.toPx(),
                    center = Offset(startX, horizonY)
                )
                // 日落锚点
                drawCircle(
                    color = Color(0xFFFF8A65).copy(alpha = 0.45f),
                    radius = 3.5.dp.toPx(),
                    center = Offset(endX, horizonY)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.90f),
                    radius = 1.8.dp.toPx(),
                    center = Offset(endX, horizonY)
                )

                // 5. 绘制高保真拟真 5 重物理光学太阳天体图形 (在 DrawScope 内部读取动画当前值，0 重组)
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
                    pulse = sunPulseState.value,
                    rotationDeg = rayRotationState.value,
                    isNight = isNight
                )
            }
        }

        // 3. 底部信息行：左右两端对齐展示【日出时刻】与【日落时刻】（统一 11.5.sp 风格）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "日出 $sunriseStr",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )
            Text(
                text = "日落 $sunsetStr",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )
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
 * @param onClick 可选的卡片点击回调函数
 * @param content 卡片内部内容插槽
 */
@Composable
private fun MetricBaseCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .height(152.dp)
            .graphicsLayer {
                // 开启独立硬件渲染图层缓存
                clip = true
                shape = RoundedCornerShape(20.dp)
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x7514263A))
            .clickable(onClick = onClick)
            .padding(14.dp)
    } else {
        modifier
            .fillMaxWidth()
            .height(152.dp)
            .graphicsLayer {
                // 开启独立硬件渲染图层缓存
                clip = true
                shape = RoundedCornerShape(20.dp)
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x7514263A))
            .padding(14.dp)
    }

    Column(
        modifier = cardModifier,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
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
            if (onClick != null) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "查看详情",
                    tint = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        content()
    }
}

// ==================== 9. 真实紫外线强度卡片 ====================

/**
 * 真实紫外线强度指标卡片组件
 *
 * 展示当前紫外线指数数值、强度级别标识、渐变刻度指示条及贴心防晒建议。
 *
 * @param uvIndex 紫外线指数数值 (0~11+)
 * @param modifier 外部修饰符
 */
@Composable
private fun UvIndexRealCard(
    uvIndex: Double,
    modifier: Modifier = Modifier
) {
    val uvValueInt = uvIndex.toInt()
    val (levelText, adviceText) = when {
        uvIndex <= 2.0 -> Pair("最弱", "几乎无晒伤风险")
        uvIndex <= 5.0 -> Pair("弱", "外出建议涂抹防晒霜")
        uvIndex <= 7.0 -> Pair("中等", "外出需防晒，佩戴墨镜与遮阳帽")
        uvIndex <= 10.0 -> Pair("强", "尽量避免午后暴晒，做好全套防晒")
        else -> Pair("极强", "极强紫外线，尽量留在室内")
    }

    MetricBaseCard(
        icon = Icons.Default.WbSunny,
        title = "紫外线指数",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. 等级与数值（上下纵向排布）
            Column {
                Text(
                    text = levelText,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$uvValueInt",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. 彩色渐变刻度条 + 白色滑块指示圆点
            val uvRatio = (uvIndex.toFloat() / 12f).coerceIn(0f, 1f)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            ) {
                val w = size.width
                val h = size.height
                val barHeight = 4.dp.toPx()
                val barY = (h - barHeight) / 2f
                val dotRadius = 4.5.dp.toPx()

                // 绘制底色/渐变色条
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF4CAF50), // 绿 (最弱 0-2)
                            Color(0xFF8BC34A), // 浅绿 (弱 3-5)
                            Color(0xFFFFEB3B), // 黄 (中等 6-7)
                            Color(0xFFFF9800), // 橙 (强 8-10)
                            Color(0xFFF44336), // 红
                            Color(0xFFE91E63)  // 粉紫 (极强 11+)
                        )
                    ),
                    topLeft = Offset(0f, barY),
                    size = Size(w, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barHeight / 2, barHeight / 2)
                )

                // 计算小圆点位置
                val dotX = (dotRadius + uvRatio * (w - 2 * dotRadius)).coerceIn(dotRadius, w - dotRadius)
                val dotY = h / 2f

                // 绘制白色小圆点滑块及其外发光投影
                drawCircle(
                    color = Color(0x33000000),
                    radius = dotRadius + 1.5.dp.toPx(),
                    center = Offset(dotX, dotY + 0.5.dp.toPx())
                )
                drawCircle(
                    color = Color.White,
                    radius = dotRadius,
                    center = Offset(dotX, dotY)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. 底部防晒建议说明
            Text(
                text = adviceText,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}

// ==================== 10. 真实水平能见度卡片 ====================

/**
 * 真实水平能见度指标卡片组件
 *
 * 展示观测站测得的水平能见度距离（公里）、能见度状况评级及出行指引。
 *
 * @param visibilityKm 水平能见度距离（单位：公里 km）
 * @param modifier 外部修饰符
 */
@Composable
private fun VisibilityRealCard(
    visibilityKm: Double,
    modifier: Modifier = Modifier
) {
    val (statusText, statusColor, guideText) = when {
        visibilityKm >= 10.0 -> Triple("极佳", Color(0xFF4CAF50), "视野清晰，适宜出行与户外活动")
        visibilityKm >= 5.0 -> Triple("良好", Color(0xFF81C784), "视线良好，交通通畅")
        visibilityKm >= 2.0 -> Triple("中等", Color(0xFFFBC02D), "视线一般，注意车距")
        visibilityKm >= 1.0 -> Triple("轻雾", Color(0xFFFF9800), "轻雾天气，开启雾灯小心驾驶")
        else -> Triple("大雾", Color(0xFFF44336), "浓雾笼罩，能见度低请减速慢行")
    }

    MetricBaseCard(
        icon = Icons.Default.Public,
        title = "能见度",
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (visibilityKm >= 10.0) "${visibilityKm.toInt()}" else String.format(Locale.US, "%.1f", visibilityKm),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "公里",
                        color = Color.White.copy(alpha = 0.80f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 能见度刻度条
            val progress = (visibilityKm.toFloat() / 20f).coerceIn(0.05f, 1f)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            ) {
                val w = size.width
                val h = size.height

                // 底色条
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.15f),
                    size = Size(w, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2, h / 2)
                )

                // 蓝色视距进度条
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF64B5F6),
                            Color(0xFF42A5F5),
                            Color(0xFF2196F3)
                        )
                    ),
                    size = Size(w * progress, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2, h / 2)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = guideText,
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 15.sp,
                maxLines = 2
            )
        }
    }
}

