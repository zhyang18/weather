package com.weather.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.weather.app.R
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 沉浸式全天候拟真动态天空背景组件
 *
 * 结合当前城市实时天气现象与昼夜状态，通过高性能 Jetpack Compose Canvas 绘制具备自然光影、物理动效与深度层次的动态天空背景图。
 *
 * 涵盖全部天气类型与物理现象：
 * 1. 晴天（白昼）：太阳发光球核、呼吸光晕、自转辐射光芒与空中漂浮金色微尘；
 * 2. 晴天（夜晚）：立体明月月晕、多层次闪烁星海（带十字星芒）与天际划过的流星拖尾；
 * 3. 大气拟真动态云层与云海系统：多层平滑连续三次贝塞尔流体云浪、远中近三层视差、迎光银边高光漫射轮廓、垂直环境遮蔽自阴影与金色丁达尔云隙圣光（God Rays）；
 * 4. 高清拟真三层景深雨丝：前景粗长晶莹雨线、中景倾斜主力雨丝、远景细密雨幕，配合触地水花飞溅粒子与地面扩散同心涟漪；
 * 5. 雷阵雨：程序化折线树状分叉闪电与全屏雷暴光影脉冲；
 * 6. 雪天（小雪/暴雪）：六角几何雪晶与景深虚化雪点，伴随正弦空气动力学翻滚摇曳下落；
 * 7. 雾 / 霾：大面积柔焦弥漫波浪层缓移与浮游微粒；
 * 8. 沙尘：狂暴风沙流线与横向飞舞沙粒；
 * 9. 大风：流线型风道丝带与气流微粒。
 *
 * @param weatherText 当前天气现象描述（如 "晴", "多云", "阴", "小雨", "雷阵雨", "暴雪", "雾", "沙尘" 等）
 * @param isNight 是否为夜间（默认根据系统当前时钟自动识别，也可外部显式指定）
 * @param modifier 外部修饰符
 */
@Composable
fun WeatherSkyBackground(
    weatherText: String,
    isNight: Boolean = remember { isCurrentlyNight() },
    modifier: Modifier = Modifier
) {
    val weatherCategory = remember(weatherText, isNight) {
        resolveWeatherCategory(weatherText, isNight)
    }

    val (targetTop, targetMid, targetBottom) = getWeatherGradientColors(weatherCategory)

    val animatedTop by animateColorAsState(targetValue = targetTop, animationSpec = tween(durationMillis = 900), label = "topColor")
    val animatedMid by animateColorAsState(targetValue = targetMid, animationSpec = tween(durationMillis = 900), label = "midColor")
    val animatedBottom by animateColorAsState(targetValue = targetBottom, animationSpec = tween(durationMillis = 900), label = "bottomColor")

    val infiniteTransition = rememberInfiniteTransition(label = "dynamicWeatherTransition")

    val fastProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(850, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "fastProgress"
    )

    val mediumProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(3000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "mediumProgress"
    )

    // 3. 慢速周期驱动（云海流动、天光呼吸、星光呼吸、太阳呼吸，26s 循环）
    val slowProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(26000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "slowProgress"
    )

    val continuousRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(32000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "continuousRotation"
    )

    val lightningPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(5500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "lightningPhase"
    )

    val rainParticles = remember {
        List(180) { index ->
            when {
                index % 6 == 0 -> {
                    // 近景长雨丝 (30根)：快速掠过的大雨丝，带半透明水珠滑落感
                    RainParticle(
                        xRatio = Random.nextFloat(),
                        yOffset = Random.nextFloat(),
                        length = Random.nextFloat() * 45f + 90f,
                        speedMultiplier = Random.nextFloat() * 0.5f + 1.7f,
                        alpha = Random.nextFloat() * 0.20f + 0.80f,
                        strokeWidth = Random.nextFloat() * 0.6f + 2.2f,
                        layer = 2
                    )
                }
                index % 2 == 0 -> {
                    // 中景主力雨丝 (70根)：晶莹透亮、纵向高光
                    RainParticle(
                        xRatio = Random.nextFloat(),
                        yOffset = Random.nextFloat(),
                        length = Random.nextFloat() * 30f + 55f,
                        speedMultiplier = Random.nextFloat() * 0.4f + 1.2f,
                        alpha = Random.nextFloat() * 0.25f + 0.65f,
                        strokeWidth = Random.nextFloat() * 0.5f + 1.5f,
                        layer = 1
                    )
                }
                else -> {
                    // 远景细密雨幕 (80根)：纤细密集、快速飘落
                    RainParticle(
                        xRatio = Random.nextFloat(),
                        yOffset = Random.nextFloat(),
                        length = Random.nextFloat() * 20f + 35f,
                        speedMultiplier = Random.nextFloat() * 0.3f + 0.95f,
                        alpha = Random.nextFloat() * 0.20f + 0.35f,
                        strokeWidth = Random.nextFloat() * 0.3f + 1.0f,
                        layer = 0
                    )
                }
            }
        }
    }

    val rainSplashes = remember {
        List(22) { index ->
            RainSplashParticle(
                xRatio = 0.04f + (index.toFloat() / 22f) * 0.92f + (Random.nextFloat() * 0.04f - 0.02f),
                yRatio = 0.78f + (Random.nextFloat() * 0.18f),
                splashRadius = Random.nextFloat() * 8f + 6f,
                phaseOffset = (index * 0.17f) % 1f
            )
        }
    }

    val ripples = remember {
        List(20) { index ->
            RainRipple(
                xRatio = 0.05f + (index.toFloat() / 20f) * 0.90f + (Random.nextFloat() * 0.04f - 0.02f),
                yRatio = 0.80f + (Random.nextFloat() * 0.16f),
                maxRadius = Random.nextFloat() * 34f + 24f,
                phaseOffset = (index * 0.21f) % 1f
            )
        }
    }

    val snowParticles = remember {
        List(60) { index ->
            SnowParticle(
                xRatio = Random.nextFloat(),
                yOffset = Random.nextFloat(),
                radius = Random.nextFloat() * 4.0f + 2.0f,
                speedMultiplier = Random.nextFloat() * 0.4f + 0.6f,
                driftAmplitude = Random.nextFloat() * 35f + 15f,
                driftFrequency = Random.nextFloat() * 1.5f + 1.0f,
                isCrystal = index % 3 == 0,
                baseRotation = Random.nextFloat() * 360f
            )
        }
    }

    val starParticles = remember {
        List(55) { index ->
            StarParticle(
                xRatio = Random.nextFloat(),
                yRatio = Random.nextFloat() * 0.70f,
                baseRadius = Random.nextFloat() * 2.2f + 0.8f,
                twinkleSpeed = Random.nextFloat() * 2.5f + 1.0f,
                phase = Random.nextFloat() * 2f * PI.toFloat(),
                hasSpikes = index % 6 == 0
            )
        }
    }

    val dustParticles = remember {
        List(40) {
            DustParticle(
                xRatio = Random.nextFloat(),
                yRatio = Random.nextFloat(),
                radius = Random.nextFloat() * 2.8f + 1.0f,
                alpha = Random.nextFloat() * 0.30f + 0.10f,
                speedX = Random.nextFloat() * 0.6f + 0.4f
            )
        }
    }

    val skyTextureRes = remember(weatherCategory) {
        when (weatherCategory) {
            WeatherCategory.RAIN_LIGHT,
            WeatherCategory.RAIN_HEAVY,
            WeatherCategory.THUNDERSTORM,
            WeatherCategory.OVERCAST,
            WeatherCategory.FOG,
            WeatherCategory.SNOW_LIGHT,
            WeatherCategory.SNOW_HEAVY -> R.drawable.bg_overcast_rain
            WeatherCategory.CLOUDY -> R.drawable.bg_day_cloudy
            WeatherCategory.CLOUDY_NIGHT -> R.drawable.bg_night_cloudy
            else -> null
        }
    }

    val nowCalendar = remember { Calendar.getInstance() }
    val sunProgress = remember { calculateSunProgress(nowCalendar) }
    val moonProgress = remember { calculateMoonProgress(nowCalendar) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(animatedTop, animatedMid, animatedBottom))
            )
    ) {
        // 1. 真实摄影级自然积雨云海/多云天空底图 (仅多云/阴雨/雪天加载，晴天彻底无云，降低透明度透出深邃晴空)
        if (skyTextureRes != null) {
            val driftOffset = sin(slowProgress * 2f * PI.toFloat()) * 20f
            val textureAlpha = when (weatherCategory) {
                WeatherCategory.CLOUDY -> 0.38f // 白天多云调低亮度，避免白云过曝影响文字
                WeatherCategory.CLOUDY_NIGHT -> 0.48f
                else -> 0.60f
            }
            Image(
                painter = painterResource(id = skyTextureRes),
                contentDescription = "天空云海真实背景",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = driftOffset
                        scaleX = 1.05f
                        scaleY = 1.05f
                        alpha = textureAlpha
                    }
            )
        }

        // 顶部与居中文字区域深邃蓝天渐变压暗保护层 (增强白色文字与图标对比度，彻底解决背景过亮刺眼问题)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x5508192C),
                            Color(0x350F263F),
                            Color.Transparent,
                            Color(0x28081622)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // 2. 动态天气物理粒子与光影层
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            if (weatherCategory.isNight) {
                val moonCenter = calculateMoonCenter(width, height, moonProgress)
                drawNightStars(starParticles, mediumProgress)
                drawShootingStars(width, height, mediumProgress)
                drawGlowingMoon(width, height, moonCenter, slowProgress)
            }

            if (!weatherCategory.isNight && (weatherCategory == WeatherCategory.SUNNY || weatherCategory == WeatherCategory.CLOUDY)) {
                // 模拟太阳东升西落的真实天顶弧线坐标 (从屏幕左侧升起，正午最高，傍晚西落)
                val sunCenter = calculateSunCenter(width, height, sunProgress)

                drawSunWithRays(
                    width = width,
                    height = height,
                    sunCenter = sunCenter,
                    dayProgress = sunProgress,
                    pulseProgress = slowProgress,
                    rotation = continuousRotation,
                    isPartlyCloudy = (weatherCategory == WeatherCategory.CLOUDY)
                )
                // 丁达尔云隙圣光（God Rays 随太阳实时位置向下发散）
                drawCrepuscularGodRays(
                    width = width,
                    height = height,
                    sunCenter = sunCenter,
                    progress = slowProgress,
                    isCloudy = (weatherCategory == WeatherCategory.CLOUDY)
                )
                drawSunDust(width, height, dustParticles, slowProgress)
            }

            if (weatherCategory == WeatherCategory.FOG || weatherCategory == WeatherCategory.SANDSTORM) {
                drawAtmosphericHazeOrSand(
                    width = width,
                    height = height,
                    particles = dustParticles,
                    isSand = (weatherCategory == WeatherCategory.SANDSTORM),
                    progress = fastProgress
                )
            }

            if (weatherCategory == WeatherCategory.WINDY || weatherCategory == WeatherCategory.SANDSTORM) {
                drawWindRibbons(width = width, height = height, progress = fastProgress)
            }

            if (weatherCategory == WeatherCategory.RAIN_LIGHT ||
                weatherCategory == WeatherCategory.RAIN_HEAVY ||
                weatherCategory == WeatherCategory.THUNDERSTORM
            ) {
                val isHeavy = weatherCategory == WeatherCategory.RAIN_HEAVY || weatherCategory == WeatherCategory.THUNDERSTORM
                drawRealisticHighDefRain(
                    width = width,
                    height = height,
                    drops = rainParticles,
                    splashes = rainSplashes,
                    ripples = ripples,
                    progress = fastProgress,
                    splashProgress = mediumProgress,
                    isHeavy = isHeavy
                )
            }

            if (weatherCategory == WeatherCategory.SNOW_LIGHT || weatherCategory == WeatherCategory.SNOW_HEAVY) {
                val isHeavy = weatherCategory == WeatherCategory.SNOW_HEAVY
                drawFallingSnow(
                    width = width,
                    height = height,
                    flakes = snowParticles,
                    progress = mediumProgress,
                    rotation = continuousRotation,
                    isHeavy = isHeavy
                )
            }

            if (weatherCategory == WeatherCategory.THUNDERSTORM) {
                drawThunderstormLightning(width = width, height = height, phase = lightningPhase)
            }
        }
    }
}

// ==================== 数据模型与枚举定义 ====================

/**
 * 天气场景分类枚举
 *
 * 归纳不同天气现象所对应的视觉层级与色彩体系。
 *
 * @property isNight 该场景是否属于夜间模式
 */
enum class WeatherCategory(val isNight: Boolean) {
    /** 晴天（白昼） */
    SUNNY(isNight = false),
    /** 晴天（夜间） */
    SUNNY_NIGHT(isNight = true),
    /** 多云（白昼） */
    CLOUDY(isNight = false),
    /** 多云（夜间） */
    CLOUDY_NIGHT(isNight = true),
    /** 阴天 */
    OVERCAST(isNight = false),
    /** 小雨 / 阵雨 */
    RAIN_LIGHT(isNight = false),
    /** 大雨 / 暴雨 */
    RAIN_HEAVY(isNight = false),
    /** 雷阵雨 / 强雷暴 */
    THUNDERSTORM(isNight = false),
    /** 小雪 / 阵雪 */
    SNOW_LIGHT(isNight = false),
    /** 中大雪 / 暴雪 */
    SNOW_HEAVY(isNight = false),
    /** 雾 / 霾 */
    FOG(isNight = false),
    /** 沙尘暴 / 浮尘 / 扬沙 */
    SANDSTORM(isNight = false),
    /** 大风 / 强风 */
    WINDY(isNight = false)
}

/**
 * 高清三层景深雨滴粒子数据模型
 *
 * @property xRatio 横向相对位置 (0f ~ 1f)
 * @property yOffset 纵向初始相位偏移 (0f ~ 1f)
 * @property length 雨丝长度 (px)
 * @property speedMultiplier 速度乘数
 * @property alpha 基础透明度
 * @property strokeWidth 雨丝线条粗细 (px)
 * @property layer 景深层级（0: 远景细密雨幕, 1: 中景主力雨丝, 2: 近景疾速晶莹大雨滴）
 */
private data class RainParticle(
    val xRatio: Float,
    val yOffset: Float,
    val length: Float,
    val speedMultiplier: Float,
    val alpha: Float,
    val strokeWidth: Float,
    val layer: Int
)

/**
 * 地面雨滴触地水花飞溅粒子模型
 *
 * @property xRatio 飞溅中心横向相对位置
 * @property yRatio 飞溅中心纵向相对位置
 * @property splashRadius 飞溅扩散半径 (px)
 * @property phaseOffset 相位偏移
 */
private data class RainSplashParticle(
    val xRatio: Float,
    val yRatio: Float,
    val splashRadius: Float,
    val phaseOffset: Float
)

/**
 * 地面雨滴水波涟漪模型
 *
 * @property xRatio 涟漪中心横向相对位置
 * @property yRatio 涟漪中心纵向相对位置
 * @property maxRadius 扩散最大半径 (px)
 * @property phaseOffset 相位偏移
 */
private data class RainRipple(
    val xRatio: Float,
    val yRatio: Float,
    val maxRadius: Float,
    val phaseOffset: Float
)

/**
 * 雪花粒子模型
 *
 * @property xRatio 横向相对位置
 * @property yOffset 纵向初始相位偏移
 * @property radius 雪花半径 (px)
 * @property speedMultiplier 飘落速度乘数
 * @property driftAmplitude 横向摇曳摆动幅度 (px)
 * @property driftFrequency 横向摆动频率
 * @property isCrystal 是否为六角雪晶图案
 * @property baseRotation 初始旋转角度
 */
private data class SnowParticle(
    val xRatio: Float,
    val yOffset: Float,
    val radius: Float,
    val speedMultiplier: Float,
    val driftAmplitude: Float,
    val driftFrequency: Float,
    val isCrystal: Boolean,
    val baseRotation: Float
)

/**
 * 恒星粒子模型
 *
 * @property xRatio 横向相对位置
 * @property yRatio 纵向相对位置 (主要分布在上半屏)
 * @property baseRadius 星星基础半径 (px)
 * @property twinkleSpeed 闪烁频率
 * @property phase 呼吸相位
 * @property hasSpikes 是否展示十字星芒光彩
 */
private data class StarParticle(
    val xRatio: Float,
    val yRatio: Float,
    val baseRadius: Float,
    val twinkleSpeed: Float,
    val phase: Float,
    val hasSpikes: Boolean
)

/**
 * 悬浮微尘/沙尘颗粒模型
 *
 * @property xRatio 横向相对位置
 * @property yRatio 纵向相对位置
 * @property radius 颗粒半径 (px)
 * @property alpha 颗粒透明度
 * @property speedX 水平漂移速度乘数
 */
private data class DustParticle(
    val xRatio: Float,
    val yRatio: Float,
    val radius: Float,
    val alpha: Float,
    val speedX: Float
)

// ==================== 工具辅助方法 ====================

/**
 * 检查当前系统时间是否属于夜间时段
 *
 * 规范：晚 18:30 至次日早 06:00 判定为夜间时段。
 *
 * @return 若为夜间则返回 true，否则返回 false
 */
private fun isCurrentlyNight(): Boolean {
    val cal = Calendar.getInstance()
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    val totalMinutes = hour * 60 + minute
    return totalMinutes >= 1110 || totalMinutes < 360
}

/**
 * 解析天气文本与夜间状态对应的分类枚举
 *
 * @param text 天气现象文本
 * @param isNight 是否为夜间
 * @return 对应的天气分类 [WeatherCategory]
 */
private fun resolveWeatherCategory(text: String, isNight: Boolean): WeatherCategory {
    return when {
        text.contains("雷") -> WeatherCategory.THUNDERSTORM
        text.contains("暴雨") || text.contains("大雨") -> WeatherCategory.RAIN_HEAVY
        text.contains("雨") -> WeatherCategory.RAIN_LIGHT
        text.contains("暴雪") || text.contains("大雪") -> WeatherCategory.SNOW_HEAVY
        text.contains("雪") -> WeatherCategory.SNOW_LIGHT
        text.contains("沙") || text.contains("尘") -> WeatherCategory.SANDSTORM
        text.contains("雾") || text.contains("霾") -> WeatherCategory.FOG
        text.contains("风") && !text.contains("微风") -> WeatherCategory.WINDY
        text.contains("阴") -> if (isNight) WeatherCategory.CLOUDY_NIGHT else WeatherCategory.OVERCAST
        text.contains("云") -> if (isNight) WeatherCategory.CLOUDY_NIGHT else WeatherCategory.CLOUDY
        isNight -> WeatherCategory.SUNNY_NIGHT
        else -> WeatherCategory.SUNNY
    }
}

/**
 * 根据天气分类获取三段自然大气渐变色（顶部、中部、底部）
 *
 * @param category 天气场景分类 [WeatherCategory]
 * @return 包含顶部、中部、底部颜色的 [Triple]
 */
/**
 * 根据天气分类获取三段自然大气渐变色（顶部、中部、底部）
 *
 * @param category 天气场景分类 [WeatherCategory]
 * @return 包含顶部、中部、底部颜色的 [Triple]
 */
private fun getWeatherGradientColors(category: WeatherCategory): Triple<Color, Color, Color> {
    return when (category) {
        WeatherCategory.SUNNY -> Triple(Color(0xFF1E75C4), Color(0xFF4B9DE8), Color(0xFF9AD3FC))
        WeatherCategory.SUNNY_NIGHT -> Triple(Color(0xFF09121D), Color(0xFF122234), Color(0xFF1F3852))
        WeatherCategory.CLOUDY -> Triple(Color(0xFF2C5E8A), Color(0xFF5582AA), Color(0xFF86AECF))
        WeatherCategory.CLOUDY_NIGHT -> Triple(Color(0xFF0D1724), Color(0xFF18283C), Color(0xFF263C55))
        WeatherCategory.OVERCAST -> Triple(Color(0xFF3F4E5B), Color(0xFF5E6E7D), Color(0xFF7E8F9E))
        WeatherCategory.RAIN_LIGHT -> Triple(Color(0xFF384956), Color(0xFF556776), Color(0xFF6E8090))
        WeatherCategory.RAIN_HEAVY -> Triple(Color(0xFF263440), Color(0xFF3E4F5D), Color(0xFF566877))
        WeatherCategory.THUNDERSTORM -> Triple(Color(0xFF1B242D), Color(0xFF2E3D4A), Color(0xFF455563))
        WeatherCategory.SNOW_LIGHT -> Triple(Color(0xFF40566D), Color(0xFF627D9A), Color(0xFF90ADC8))
        WeatherCategory.SNOW_HEAVY -> Triple(Color(0xFF32475E), Color(0xFF4F6884), Color(0xFF7E9CB9))
        WeatherCategory.FOG -> Triple(Color(0xFF47525C), Color(0xFF64727D), Color(0xFF8F9DA6))
        WeatherCategory.SANDSTORM -> Triple(Color(0xFF6E5638), Color(0xFF957850), Color(0xFFC0A47B))
        WeatherCategory.WINDY -> Triple(Color(0xFF275882), Color(0xFF487AA6), Color(0xFF7BAACF))
    }
}

// ==================== 绘制各天气元素扩展方法 ====================

/**
 * 写实立体积雨云团节点配置
 *
 * @property relX 水平相对基准位置 (0f ~ 1f)
 * @property relY 垂直相对基准位置 (0f ~ 1f)
 * @property radiusRatio 云团半径相对屏幕宽度的比例
 * @property brightnessFactor 受光亮度因子 (0f ~ 1f)
 * @property driftScale 随风横向漂移速率比例
 */
private data class RealisticCloudPuffNode(
    val relX: Float,
    val relY: Float,
    val radiusRatio: Float,
    val brightnessFactor: Float,
    val driftScale: Float = 1.0f
)

/**
 * 绘制大气拟真写实立体积雨云海系统
 *
 * 结合大面积高空深邃云幕、多重体积饱满径向羽化积云球团簇（Volumetric Cumulus Masses）与三次贝塞尔流体连绵云浪脊线。
 * 具备受光面高光漫射、背光面环境遮蔽自阴影以及极柔和的边缘羽化消隐，完美呈现出波澜壮阔、翻滚涌动的真实天际云海。
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param category 天气场景分类 [WeatherCategory]
 * @param progress 动画时间相位 (0f ~ 1f)
 */
private fun DrawScope.drawAtmosphericCloudDecks(
    width: Float,
    height: Float,
    category: WeatherCategory,
    progress: Float
) {
    val isHeavy = category == WeatherCategory.OVERCAST || category == WeatherCategory.RAIN_HEAVY || category == WeatherCategory.THUNDERSTORM
    val isRainy = category == WeatherCategory.RAIN_LIGHT || category == WeatherCategory.RAIN_HEAVY || category == WeatherCategory.THUNDERSTORM
    val isNight = category.isNight

    val (topLightColor, baseBodyColor, shadowColor, rimHighlightColor) = when {
        isNight -> listOf(
            Color(0xFF3F546E),
            Color(0xFF253444),
            Color(0xFF101923),
            Color(0xFFCAD8E8).copy(alpha = 0.40f)
        )
        isHeavy -> listOf(
            Color(0xFFD4E1EB),
            Color(0xFF7E92A4),
            Color(0xFF3A4B59),
            Color(0xFFB5C8D8).copy(alpha = 0.55f)
        )
        isRainy -> listOf(
            Color(0xFFEFF5F9),
            Color(0xFF9CB1C2),
            Color(0xFF4A5D6D),
            Color(0xFFD8E7F3).copy(alpha = 0.70f)
        )
        category == WeatherCategory.CLOUDY -> listOf(
            Color(0xFFFFFFFF),
            Color(0xFFB4D0E7),
            Color(0xFF6B8DAA),
            Color(0xFFFFFDE7).copy(alpha = 0.85f)
        )
        else -> listOf(
            Color(0xFFFFFFFF),
            Color(0xFFC7DEEF),
            Color(0xFF7CA3C4),
            Color(0xFFFFFFFD).copy(alpha = 0.75f)
        )
    }

    // -------------------------------------------------------------
    // Layer 0: 远景天际深邃高空云幕 (平铺上半屏)
    // -------------------------------------------------------------
    val farShift = (progress * width * 0.25f) % width
    val farBaseY = height * 0.15f
    val farAlpha = if (isHeavy) 0.60f else if (isNight) 0.32f else 0.42f

    val farPath = Path().apply {
        moveTo(-width, farBaseY)
        for (i in 0..2) {
            val startX = (i - 1) * width + farShift
            cubicTo(
                startX + width * 0.22f, farBaseY - height * 0.05f,
                startX + width * 0.48f, farBaseY + height * 0.04f,
                startX + width * 0.72f, farBaseY - height * 0.03f
            )
            cubicTo(
                startX + width * 0.88f, farBaseY - height * 0.06f,
                startX + width * 0.96f, farBaseY + height * 0.02f,
                startX + width, farBaseY
            )
        }
        lineTo(width * 2f, height * 0.45f)
        lineTo(-width, height * 0.45f)
        close()
    }

    drawPath(
        path = farPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                topLightColor.copy(alpha = farAlpha * 0.80f),
                baseBodyColor.copy(alpha = farAlpha * 0.65f),
                shadowColor.copy(alpha = farAlpha * 0.30f),
                Color.Transparent
            ),
            startY = 0f,
            endY = height * 0.45f
        )
    )

    // -------------------------------------------------------------
    // Layer 1: 中景写实立体积雨云团海 (14个体积羽化云团节点)
    // -------------------------------------------------------------
    if (category != WeatherCategory.SUNNY && category != WeatherCategory.SUNNY_NIGHT) {
        val cloudPuffs = listOf(
            RealisticCloudPuffNode(0.05f, 0.08f, 0.32f, 0.95f, 0.4f),
            RealisticCloudPuffNode(0.28f, 0.06f, 0.38f, 1.00f, 0.5f),
            RealisticCloudPuffNode(0.55f, 0.07f, 0.35f, 0.92f, 0.45f),
            RealisticCloudPuffNode(0.82f, 0.09f, 0.34f, 0.90f, 0.4f),
            RealisticCloudPuffNode(0.15f, 0.18f, 0.30f, 0.85f, 0.6f),
            RealisticCloudPuffNode(0.42f, 0.16f, 0.36f, 0.88f, 0.55f),
            RealisticCloudPuffNode(0.70f, 0.19f, 0.32f, 0.82f, 0.6f),
            RealisticCloudPuffNode(0.95f, 0.17f, 0.28f, 0.80f, 0.5f),
            RealisticCloudPuffNode(-0.08f, 0.22f, 0.26f, 0.78f, 0.65f),
            RealisticCloudPuffNode(0.25f, 0.25f, 0.28f, 0.80f, 0.7f),
            RealisticCloudPuffNode(0.58f, 0.24f, 0.32f, 0.82f, 0.65f),
            RealisticCloudPuffNode(0.85f, 0.26f, 0.27f, 0.75f, 0.7f),
            RealisticCloudPuffNode(0.38f, 0.32f, 0.24f, 0.70f, 0.8f),
            RealisticCloudPuffNode(0.68f, 0.33f, 0.22f, 0.68f, 0.8f)
        )

        cloudPuffs.forEach { puff ->
            val drift = (progress * width * 0.30f * puff.driftScale) % (width * 1.5f)
            val cx = ((puff.relX * width + drift) % (width * 1.3f)) - width * 0.15f
            val cy = puff.relY * height
            val r = puff.radiusRatio * width

            // 1. 背光暗部自阴影层 (偏下偏深，提供真实积雨云体积与遮蔽)
            val shadowCenter = Offset(cx, cy + r * 0.18f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        shadowColor.copy(alpha = if (isHeavy) 0.75f else 0.55f),
                        shadowColor.copy(alpha = if (isHeavy) 0.40f else 0.25f),
                        Color.Transparent
                    ),
                    center = shadowCenter,
                    radius = r * 1.15f
                ),
                radius = r * 1.15f,
                center = shadowCenter
            )

            // 2. 迎光面高光与主体层 (偏上偏亮，柔和羽化消隐)
            val highlightCenter = Offset(cx - r * 0.08f, cy - r * 0.12f)
            val lightAlpha = (puff.brightnessFactor * (if (isHeavy) 0.85f else 0.95f)).coerceIn(0.4f, 1f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        topLightColor.copy(alpha = lightAlpha),
                        baseBodyColor.copy(alpha = lightAlpha * 0.70f),
                        baseBodyColor.copy(alpha = lightAlpha * 0.30f),
                        Color.Transparent
                    ),
                    center = highlightCenter,
                    radius = r
                ),
                radius = r,
                center = highlightCenter
            )
        }

        // 3. 宏观连绵云浪脊线与迎光银边轮廓 (Cubic Bezier Deck)
        val midShift = (progress * width * 0.55f) % width
        val midBaseY = height * 0.28f
        val midAlpha = if (isHeavy) 0.75f else if (isNight) 0.45f else 0.65f

        val midPath = Path().apply {
            moveTo(-width, midBaseY + height * 0.02f)
            for (i in 0..2) {
                val startX = (i - 1) * width + midShift
                cubicTo(
                    startX + width * 0.14f, midBaseY - height * 0.07f,
                    startX + width * 0.28f, midBaseY - height * 0.09f,
                    startX + width * 0.42f, midBaseY - height * 0.02f
                )
                cubicTo(
                    startX + width * 0.56f, midBaseY - height * 0.11f,
                    startX + width * 0.70f, midBaseY - height * 0.09f,
                    startX + width * 0.82f, midBaseY - height * 0.03f
                )
                cubicTo(
                    startX + width * 0.92f, midBaseY - height * 0.06f,
                    startX + width * 0.97f, midBaseY - height * 0.01f,
                    startX + width, midBaseY + height * 0.02f
                )
            }
            lineTo(width * 2f, height * 0.52f)
            lineTo(-width, height * 0.52f)
            close()
        }

        drawPath(
            path = midPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    topLightColor.copy(alpha = midAlpha * 0.85f),
                    baseBodyColor.copy(alpha = midAlpha * 0.65f),
                    shadowColor.copy(alpha = midAlpha * 0.35f),
                    Color.Transparent
                ),
                startY = midBaseY - height * 0.11f,
                endY = height * 0.52f
            )
        )

        drawPath(
            path = midPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    rimHighlightColor,
                    rimHighlightColor.copy(alpha = rimHighlightColor.alpha * 0.25f),
                    Color.Transparent
                ),
                startY = midBaseY - height * 0.11f,
                endY = midBaseY + height * 0.03f
            ),
            style = Stroke(width = 2.0f, cap = StrokeCap.Round)
        )
    }

    // -------------------------------------------------------------
    // Layer 2: 前景轻薄掠过云缕 (薄雾丝带，快速掠过)
    // -------------------------------------------------------------
    val nearShift = (progress * width * 0.95f) % width
    val nearBaseY = height * 0.38f
    val nearAlpha = if (isHeavy) 0.35f else if (isNight) 0.20f else 0.28f

    val nearPath = Path().apply {
        moveTo(-width, nearBaseY)
        for (i in 0..2) {
            val startX = (i - 1) * width + nearShift
            cubicTo(
                startX + width * 0.20f, nearBaseY - height * 0.035f,
                startX + width * 0.45f, nearBaseY + height * 0.025f,
                startX + width * 0.68f, nearBaseY - height * 0.030f
            )
            cubicTo(
                startX + width * 0.86f, nearBaseY - height * 0.040f,
                startX + width * 0.96f, nearBaseY + height * 0.015f,
                startX + width, nearBaseY
            )
        }
        lineTo(width * 2f, height * 0.48f)
        lineTo(-width, height * 0.48f)
        close()
    }

    drawPath(
        path = nearPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                topLightColor.copy(alpha = nearAlpha),
                Color.Transparent
            ),
            startY = nearBaseY - height * 0.04f,
            endY = height * 0.48f
        )
    )
}

/**
 * 计算当前时刻在白昼日照区间（默认 06:00 ~ 18:30）中的进度比例
 *
 * @param calendar 当前系统时钟实例 [Calendar]
 * @return 归一化日照进度值 (0.0f ~ 1.0f)，0f 为日出（06:00），0.5f 为正午（12:15），1f 为日落（18:30）
 */
private fun calculateSunProgress(calendar: Calendar = Calendar.getInstance()): Float {
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    val currentMinutes = hour * 60 + minute
    val sunriseMinutes = 6 * 60 // 06:00
    val sunsetMinutes = 18 * 60 + 30 // 18:30
    return ((currentMinutes - sunriseMinutes).toFloat() / (sunsetMinutes - sunriseMinutes).toFloat()).coerceIn(0f, 1f)
}

/**
 * 根据日照时间进度计算太阳在天穹弧线中的屏幕坐标
 *
 * 遵循东升西落自然物理规律：
 * - 清晨 (进度 0.0f)：位于屏幕左侧 (X: 0.12f)，较低天际 (Y: 0.28f)；
 * - 正午 (进度 0.5f)：位于屏幕正中 (X: 0.50f)，升至天顶最高点 (Y: 0.08f)；
 * - 傍晚 (进度 1.0f)：位于屏幕右侧 (X: 0.88f)，缓缓西沉至低空 (Y: 0.28f)。
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param progress 归一化日照进度 (0.0f ~ 1.0f)
 * @return 太阳中心屏幕坐标 [Offset]
 */
private fun calculateSunCenter(width: Float, height: Float, progress: Float): Offset {
    val sunX = width * (0.12f + 0.76f * progress)
    val sunY = height * (0.28f - 0.20f * sin(progress * PI.toFloat()))
    return Offset(sunX, sunY)
}

/**
 * 计算夜间时钟在月出月落区间（19:00 ~ 次日 05:30）中的进度比例
 *
 * @param calendar 当前系统时钟实例 [Calendar]
 * @return 归一化月相天空进度值 (0.0f ~ 1.0f)
 */
private fun calculateMoonProgress(calendar: Calendar = Calendar.getInstance()): Float {
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    val currentMinutes = hour * 60 + minute
    val moonRise = 19 * 60 // 19:00
    val totalNightMinutes = 10.5f * 60f // 10.5 小时

    val elapsed = if (currentMinutes >= moonRise) {
        (currentMinutes - moonRise).toFloat()
    } else {
        (currentMinutes + (24 * 60 - moonRise)).toFloat()
    }
    return (elapsed / totalNightMinutes).coerceIn(0f, 1f)
}

/**
 * 根据月夜进度计算明月在夜空弧线中的屏幕坐标
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param progress 归一化夜空月行进度 (0.0f ~ 1.0f)
 * @return 明月中心屏幕坐标 [Offset]
 */
private fun calculateMoonCenter(width: Float, height: Float, progress: Float): Offset {
    val moonX = width * (0.15f + 0.70f * progress)
    val moonY = height * (0.24f - 0.14f * sin(progress * PI.toFloat()))
    return Offset(moonX, moonY)
}

/**
 * 绘制白天丁达尔云隙圣光（God Rays）（随太阳实时位置向下发散）
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param sunCenter 太阳实时天顶屏幕坐标 [Offset]
 * @param progress 呼吸动画相位 (0f ~ 1f)
 * @param isCloudy 是否为多云天气（多云时云隙光更明显）
 */
private fun DrawScope.drawCrepuscularGodRays(
    width: Float,
    height: Float,
    sunCenter: Offset,
    progress: Float,
    isCloudy: Boolean
) {
    val maxRayLength = width * 1.2f
    val rayAlphaBase = if (isCloudy) 0.06f else 0.04f

    // 依太阳在屏幕左右位置自适应调整发散角度基准
    val baseAngleDeg = when {
        sunCenter.x < width * 0.35f -> 60f // 太阳在左，光线射向右下方
        sunCenter.x > width * 0.65f -> 120f // 太阳在右，光线射向左下方
        else -> 90f // 太阳在正中，光线射向正下方
    }

    val rayAngles = listOf(baseAngleDeg - 35f, baseAngleDeg - 18f, baseAngleDeg, baseAngleDeg + 18f, baseAngleDeg + 35f)
    val rayWidths = listOf(12f, 18f, 15f, 20f, 14f)

    rayAngles.forEachIndexed { index, angleDeg ->
        val pulse = (sin((progress + index * 0.2f) * 2f * PI.toFloat()) + 1f) / 2f
        val currentAlpha = rayAlphaBase * (0.65f + pulse * 0.35f)
        val angleRad = angleDeg * (PI / 180f)
        val beamWidthDeg = rayWidths[index] * (PI / 180f)

        val leftAngle = angleRad - beamWidthDeg * 0.5f
        val rightAngle = angleRad + beamWidthDeg * 0.5f

        val p1 = Offset(
            sunCenter.x + (maxRayLength * cos(leftAngle)).toFloat(),
            sunCenter.y + (maxRayLength * sin(leftAngle)).toFloat()
        )
        val p2 = Offset(
            sunCenter.x + (maxRayLength * cos(rightAngle)).toFloat(),
            sunCenter.y + (maxRayLength * sin(rightAngle)).toFloat()
        )

        val beamPath = Path().apply {
            moveTo(sunCenter.x, sunCenter.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            close()
        }

        drawPath(
            path = beamPath,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF9C4).copy(alpha = currentAlpha),
                    Color(0xFFFFECB3).copy(alpha = currentAlpha * 0.40f),
                    Color.Transparent
                ),
                center = sunCenter,
                radius = maxRayLength
            )
        )
    }
}

/**
 * 绘制白昼太阳光晕、核心发光球与自转辐射光芒（随时间东升西落，柔和光效避免遮挡文字）
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param sunCenter 太阳实时天顶屏幕坐标 [Offset]
 * @param dayProgress 日照时间进度 (0.0f ~ 1.0f)
 * @param pulseProgress 太阳呼吸胀缩相位 (0f ~ 1f)
 * @param rotation 光芒自转角度 (0° ~ 360°)
 * @param isPartlyCloudy 是否有多云遮挡减弱光晕
 */
private fun DrawScope.drawSunWithRays(
    width: Float,
    height: Float,
    sunCenter: Offset,
    dayProgress: Float,
    pulseProgress: Float,
    rotation: Float,
    isPartlyCloudy: Boolean
) {
    val coreRadius = width * 0.075f
    val outerGlowRadius = width * (0.26f + pulseProgress * 0.02f)
    val maxAlpha = if (isPartlyCloudy) 0.35f else 0.55f

    // 早晚偏金橙暖色 (Golden Hour)，正午偏温润柔和金黄，避免大面积白光爆闪
    val isGoldenHour = dayProgress < 0.18f || dayProgress > 0.82f
    val glowOuterColor = if (isGoldenHour) Color(0xFFFF9800) else Color(0xFFFFB300)
    val glowMidColor = if (isGoldenHour) Color(0xFFFFC107) else Color(0xFFFFD54F)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f * maxAlpha),
                glowMidColor.copy(alpha = 0.16f * maxAlpha),
                glowOuterColor.copy(alpha = 0.05f * maxAlpha),
                Color.Transparent
            ),
            center = sunCenter,
            radius = outerGlowRadius
        ),
        center = sunCenter,
        radius = outerGlowRadius
    )

    rotate(degrees = rotation, pivot = sunCenter) {
        val rayCount = 12
        for (i in 0 until rayCount) {
            val angle = (i * (360f / rayCount)) * (PI / 180f)
            val rayLength = outerGlowRadius * (if (i % 2 == 0) 0.75f else 0.55f)
            val rayEnd = Offset(
                sunCenter.x + (rayLength * cos(angle)).toFloat(),
                sunCenter.y + (rayLength * sin(angle)).toFloat()
            )
            drawLine(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = (if (i % 2 == 0) 0.14f else 0.08f) * maxAlpha),
                        Color(0xFFFFF9C4).copy(alpha = 0.02f * maxAlpha),
                        Color.Transparent
                    ),
                    center = sunCenter,
                    radius = rayLength
                ),
                start = sunCenter,
                end = rayEnd,
                strokeWidth = if (i % 2 == 0) 3.2f else 1.8f,
                cap = StrokeCap.Round
            )
        }
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.90f * maxAlpha),
                Color(0xFFFFF59D).copy(alpha = 0.65f * maxAlpha),
                Color(0xFFFFD54F).copy(alpha = 0.25f * maxAlpha),
                Color.Transparent
            ),
            center = sunCenter,
            radius = coreRadius * 1.3f
        ),
        center = sunCenter,
        radius = coreRadius * 1.3f
    )
}

/**
 * 绘制写实密集垂直细密雨帘、触地水花与水波涟漪
 *
 * 遵循设计图：雨丝保持自然垂直微倾（3.5度斜角），密集细腻晶莹，分层展现远景细雨幕、中景主力雨丝与近景晶亮雨线。
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param drops 多层雨丝粒子集合 [List]<[RainParticle]>
 * @param splashes 触地水花飞溅粒子集合 [List]<[RainSplashParticle]>
 * @param ripples 地面同心水波涟漪集合 [List]<[RainRipple]>
 * @param progress 雨丝疾速下落相位 (0f ~ 1f)
 * @param splashProgress 水花飞溅与涟漪相位 (0f ~ 1f)
 * @param isHeavy 是否为大雨/暴雨/雷雨
 */
private fun DrawScope.drawRealisticHighDefRain(
    width: Float,
    height: Float,
    drops: List<RainParticle>,
    splashes: List<RainSplashParticle>,
    ripples: List<RainRipple>,
    progress: Float,
    splashProgress: Float,
    isHeavy: Boolean
) {
    // 依设计图调整为垂直微斜 (约 3.5 度斜角)
    val slantFactor = if (isHeavy) 0.08f else 0.06f
    val activeDrops = if (isHeavy) drops else drops.take(135)

    // 暴雨水汽雨雾层
    if (isHeavy) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFF81D4FA).copy(alpha = 0.05f),
                    Color(0xFFE1F5FE).copy(alpha = 0.12f)
                ),
                startY = height * 0.60f,
                endY = height
            ),
            topLeft = Offset(0f, height * 0.60f),
            size = Size(width, height * 0.40f)
        )
    }

    activeDrops.forEach { drop ->
        val curProgress = (progress * drop.speedMultiplier + drop.yOffset) % 1f
        val startY = curProgress * (height + drop.length * 1.5f) - drop.length * 1.5f
        val curLength = if (isHeavy) drop.length * 1.2f else drop.length
        val endY = startY + curLength

        val slantX = curLength * slantFactor
        val startX = drop.xRatio * width + (curProgress * width * 0.05f)
        val endX = startX + slantX

        if (endY > 0f && startY < height) {
            val alphaMultiplier = if (isHeavy) 1.2f else 1.0f
            val baseAlpha = (drop.alpha * alphaMultiplier).coerceIn(0.18f, 1.0f)

            val rainBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = baseAlpha * 0.15f),
                    Color(0xFFE1F5FE).copy(alpha = baseAlpha * 0.85f),
                    Color.White.copy(alpha = baseAlpha * 0.95f),
                    Color.White.copy(alpha = baseAlpha * 0.30f)
                ),
                start = Offset(startX, startY),
                end = Offset(endX, endY)
            )

            drawLine(
                brush = rainBrush,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = if (isHeavy) drop.strokeWidth * 1.15f else drop.strokeWidth,
                cap = StrokeCap.Round
            )

            if (drop.layer == 2) {
                drawCircle(
                    color = Color.White.copy(alpha = baseAlpha * 0.90f),
                    radius = drop.strokeWidth * 0.75f,
                    center = Offset(endX, endY)
                )
            }
        }
    }

    splashes.forEach { splash ->
        val curSplashProgress = (splashProgress * 2.2f + splash.phaseOffset) % 1f
        if (curSplashProgress < 0.65f) {
            val splashAlpha = (1f - (curSplashProgress / 0.65f)) * 0.75f
            val cx = splash.xRatio * width
            val cy = splash.yRatio * height
            val r = splash.splashRadius * (curSplashProgress / 0.65f)

            drawCircle(
                color = Color.White.copy(alpha = splashAlpha),
                radius = 1.8f,
                center = Offset(cx - r * 1.2f, cy - r * 0.8f)
            )
            drawCircle(
                color = Color(0xFFE1F5FE).copy(alpha = splashAlpha * 0.9f),
                radius = 1.5f,
                center = Offset(cx + r * 1.4f, cy - r * 1.0f)
            )
            drawCircle(
                color = Color.White.copy(alpha = splashAlpha * 0.8f),
                radius = 1.2f,
                center = Offset(cx + r * 0.2f, cy - r * 1.4f)
            )
        }
    }

    ripples.forEach { ripple ->
        val curRippleProgress = (splashProgress + ripple.phaseOffset) % 1f
        val centerX = ripple.xRatio * width
        val centerY = ripple.yRatio * height
        val radiusX = curRippleProgress * ripple.maxRadius
        val radiusY = radiusX * 0.32f
        val alpha = (1f - curRippleProgress) * (if (isHeavy) 0.55f else 0.40f)

        if (alpha > 0.02f) {
            drawOval(
                color = Color(0xFFE1F5FE).copy(alpha = alpha),
                topLeft = Offset(centerX - radiusX, centerY - radiusY),
                size = Size(radiusX * 2f, radiusY * 2f),
                style = Stroke(width = if (isHeavy) 1.6f else 1.2f)
            )
        }
    }
}

/**
 * 绘制阳光在空中折射出的金色微光浮尘
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param particles 悬浮微尘数据集
 * @param progress 动画相位 (0f ~ 1f)
 */
private fun DrawScope.drawSunDust(
    width: Float,
    height: Float,
    particles: List<DustParticle>,
    progress: Float
) {
    particles.forEach { p ->
        val x = (p.xRatio * width + (progress - 0.5f) * 35f * p.speedX) % width
        val y = p.yRatio * height * 0.65f
        val pulse = (sin((progress + p.xRatio) * 2f * PI.toFloat()) + 1f) / 2f
        val alpha = (p.alpha * (0.4f + pulse * 0.6f)).coerceIn(0f, 1f)

        drawCircle(
            color = Color(0xFFFFF9C4).copy(alpha = alpha),
            radius = p.radius * (0.8f + pulse * 0.4f),
            center = Offset(if (x < 0) x + width else x, y)
        )
    }
}

/**
 * 绘制静谧夜空闪烁星海（带十字星芒）
 *
 * @param stars 恒星粒子数据集
 * @param time 动画时间戳相位
 */
private fun DrawScope.drawNightStars(stars: List<StarParticle>, time: Float) {
    stars.forEach { star ->
        val twinkle = (sin(time * 2f * PI.toFloat() * star.twinkleSpeed + star.phase) + 1f) / 2f
        val alpha = (0.20f + twinkle * 0.75f).coerceIn(0f, 1f)
        val center = Offset(star.xRatio * size.width, star.yRatio * size.height)
        val radius = star.baseRadius * (0.8f + twinkle * 0.4f)

        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = radius,
            center = center
        )

        if (star.hasSpikes && twinkle > 0.45f) {
            val spikeLen = radius * 3.5f * twinkle
            val spikeAlpha = alpha * 0.65f
            drawLine(
                color = Color.White.copy(alpha = spikeAlpha),
                start = Offset(center.x - spikeLen, center.y),
                end = Offset(center.x + spikeLen, center.y),
                strokeWidth = 1.0f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White.copy(alpha = spikeAlpha),
                start = Offset(center.x, center.y - spikeLen),
                end = Offset(center.x, center.y + spikeLen),
                strokeWidth = 1.0f,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * 绘制夜空划过的随机流星与光痕拖尾
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param progress 动画相位 (0f ~ 1f)
 */
private fun DrawScope.drawShootingStars(width: Float, height: Float, progress: Float) {
    val meteorActivePhase = progress in 0.20f..0.35f
    if (meteorActivePhase) {
        val meteorProgress = (progress - 0.20f) / 0.15f
        val startX = width * 0.90f - meteorProgress * (width * 0.65f)
        val startY = height * 0.05f + meteorProgress * (height * 0.25f)
        val tailLength = 95f
        val angleRad = 35f * (PI / 180f)
        val endX = startX + (tailLength * cos(angleRad)).toFloat()
        val endY = startY - (tailLength * sin(angleRad)).toFloat()
        val alpha = if (meteorProgress < 0.3f) (meteorProgress / 0.3f) else ((1f - meteorProgress) / 0.7f).coerceIn(0f, 1f)

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = alpha * 0.9f), Color.Transparent),
                start = Offset(startX, startY),
                end = Offset(endX, endY)
            ),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round
        )

        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = 2.8f,
            center = Offset(startX, startY)
        )
    }
}

/**
 * 绘制立体明月与柔美月晕（随夜间时间弧形移动）
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param moonCenter 明月实时屏幕坐标 [Offset]
 * @param pulseProgress 月晕呼吸相位
 */
private fun DrawScope.drawGlowingMoon(
    width: Float,
    height: Float,
    moonCenter: Offset,
    pulseProgress: Float
) {
    val moonRadius = width * 0.09f
    val glowRadius = width * (0.35f + pulseProgress * 0.03f)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFE2EDF8).copy(alpha = 0.30f),
                Color(0xFF8FAAC4).copy(alpha = 0.10f),
                Color.Transparent
            ),
            center = moonCenter,
            radius = glowRadius
        ),
        center = moonCenter,
        radius = glowRadius
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFDF5),
                Color(0xFFE6EEF8),
                Color(0xFFC4D5E5)
            ),
            center = Offset(moonCenter.x - moonRadius * 0.3f, moonCenter.y - moonRadius * 0.3f),
            radius = moonRadius * 1.2f
        ),
        center = moonCenter,
        radius = moonRadius
    )

    drawCircle(
        color = Color(0xFFB0C4D8).copy(alpha = 0.22f),
        radius = moonRadius * 0.32f,
        center = Offset(moonCenter.x + moonRadius * 0.25f, moonCenter.y - moonRadius * 0.15f)
    )
    drawCircle(
        color = Color(0xFFB0C4D8).copy(alpha = 0.18f),
        radius = moonRadius * 0.22f,
        center = Offset(moonCenter.x - moonRadius * 0.15f, moonCenter.y + moonRadius * 0.28f)
    )
}

/**
 * 绘制轻盈翻滚飘落的雪花与精致六角结晶
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param flakes 雪花粒子数据集
 * @param progress 下落相位
 * @param rotation 自转回旋角度
 * @param isHeavy 是否为大雪/暴雪
 */
private fun DrawScope.drawFallingSnow(
    width: Float,
    height: Float,
    flakes: List<SnowParticle>,
    progress: Float,
    rotation: Float,
    isHeavy: Boolean
) {
    val activeFlakes = if (isHeavy) flakes else flakes.take(40)
    activeFlakes.forEach { flake ->
        val curProgress = (progress * flake.speedMultiplier + flake.yOffset) % 1f
        val y = curProgress * (height + 30f) - 15f
        val drift = sin(curProgress * flake.driftFrequency * 2f * PI.toFloat()) * flake.driftAmplitude
        val x = (flake.xRatio * width + drift) % width
        val actualX = if (x < 0) x + width else x
        val alpha = if (isHeavy) 0.75f else 0.55f

        if (y in -15f..height + 15f) {
            val center = Offset(actualX, y)

            if (flake.isCrystal && flake.radius >= 3.0f) {
                rotate(degrees = rotation + flake.baseRotation, pivot = center) {
                    val r = flake.radius * 1.6f
                    for (i in 0 until 3) {
                        val angle = (i * 60f) * (PI / 180f)
                        val dx = (r * cos(angle)).toFloat()
                        val dy = (r * sin(angle)).toFloat()
                        drawLine(
                            color = Color.White.copy(alpha = alpha),
                            start = Offset(center.x - dx, center.y - dy),
                            end = Offset(center.x + dx, center.y + dy),
                            strokeWidth = 1.2f,
                            cap = StrokeCap.Round
                        )
                        val branchR = r * 0.45f
                        val branchOffset = 0.55f
                        val bx = (dx * branchOffset)
                        val by = (dy * branchOffset)
                        val perpAngle = angle + (PI / 3)
                        val pdx = (branchR * cos(perpAngle)).toFloat()
                        val pdy = (branchR * sin(perpAngle)).toFloat()
                        drawLine(
                            color = Color.White.copy(alpha = alpha * 0.8f),
                            start = Offset(center.x + bx - pdx, center.y + by - pdy),
                            end = Offset(center.x + bx + pdx, center.y + by + pdy),
                            strokeWidth = 0.9f
                        )
                    }
                }
            } else {
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = flake.radius,
                    center = center
                )
            }
        }
    }
}

/**
 * 绘制雷雨天程序化折线树状分叉闪电与全屏脉冲闪光
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param phase 雷电周期相位 (0f ~ 1f)
 */
private fun DrawScope.drawThunderstormLightning(
    width: Float,
    height: Float,
    phase: Float
) {
    val isPrimaryFlash = phase in 0.40f..0.44f
    val isSecondaryFlash = phase in 0.46f..0.48f

    if (isPrimaryFlash || isSecondaryFlash) {
        val flashAlpha = if (isPrimaryFlash) 0.38f else 0.22f

        drawRect(
            color = Color(0xFFD6E4FF).copy(alpha = flashAlpha),
            topLeft = Offset.Zero,
            size = Size(width, height)
        )

        val startX = width * 0.55f
        val startY = height * 0.08f
        val mainPath = Path().apply {
            moveTo(startX, startY)
            lineTo(startX - 25f, height * 0.16f)
            lineTo(startX + 15f, height * 0.24f)
            lineTo(startX - 35f, height * 0.34f)
            lineTo(startX - 10f, height * 0.42f)
            lineTo(startX - 45f, height * 0.52f)
        }

        val branchPath = Path().apply {
            moveTo(startX + 15f, height * 0.24f)
            lineTo(startX + 50f, height * 0.30f)
            lineTo(startX + 75f, height * 0.38f)
        }

        drawPath(
            path = mainPath,
            color = Color(0xFF90CAF9).copy(alpha = 0.65f),
            style = Stroke(width = 6.5f, cap = StrokeCap.Round)
        )
        drawPath(
            path = mainPath,
            color = Color.White,
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )
        drawPath(
            path = branchPath,
            color = Color.White.copy(alpha = 0.85f),
            style = Stroke(width = 1.6f, cap = StrokeCap.Round)
        )
    }
}

/**
 * 绘制雾霾或沙尘弥漫浮动层
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param particles 颗粒数据集
 * @param isSand 是否为沙尘暴
 * @param progress 动画相位 (0f ~ 1f)
 */
private fun DrawScope.drawAtmosphericHazeOrSand(
    width: Float,
    height: Float,
    particles: List<DustParticle>,
    isSand: Boolean,
    progress: Float
) {
    val hazeColor = if (isSand) Color(0xFFBCAAA4) else Color.White
    val slant = if (isSand) (progress - 0.5f) * 60f else (progress - 0.5f) * 20f

    particles.forEach { p ->
        val x = (p.xRatio * width + slant * p.speedX) % width
        val y = p.yRatio * height
        val actualX = if (x < 0) x + width else x

        drawCircle(
            color = hazeColor.copy(alpha = if (isSand) p.alpha * 1.2f else p.alpha),
            radius = if (isSand) p.radius * 1.3f else p.radius,
            center = Offset(actualX, y)
        )
    }
}

/**
 * 绘制大风呼啸风流光带与顺风气流
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param progress 动画相位 (0f ~ 1f)
 */
private fun DrawScope.drawWindRibbons(
    width: Float,
    height: Float,
    progress: Float
) {
    val yLevels = listOf(height * 0.20f, height * 0.38f, height * 0.58f, height * 0.75f)
    yLevels.forEachIndexed { i, y ->
        val offsetProgress = (progress + i * 0.25f) % 1f
        val ribbonLen = 220f
        val startX = offsetProgress * (width + ribbonLen) - ribbonLen
        val endX = startX + ribbonLen

        val waveY = y + sin(offsetProgress * 2f * PI.toFloat() + i) * 12f

        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.25f),
                    Color.White.copy(alpha = 0.35f),
                    Color.Transparent
                ),
                startX = startX,
                endX = endX
            ),
            start = Offset(startX, waveY),
            end = Offset(endX, y),
            strokeWidth = 2.8f,
            cap = StrokeCap.Round
        )
    }
}

