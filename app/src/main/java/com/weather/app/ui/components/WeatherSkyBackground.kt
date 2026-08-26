package com.weather.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.weather.app.R
import com.weather.app.model.CityInfo
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 沉浸式全天候拟真动态天空背景组件 (支持 Lambda 视差供给器，实现零重组 120 FPS 流畅滑页)
 *
 * 结合当前城市日出、日落、月出、月落时间与实时天气现象，
 * 精确计算太阳与月亮的出现时机与平缓微弧轨迹，
 * 通过高性能 Jetpack Compose Canvas 绘制具备自然光影、物理动效与深度层次的动态天空背景图。
 *
 * 涵盖全部天气类型与物理现象：
 * 1. 晴天（白昼）：纯白金光核、双层彩虹镜头光晕环、自转辐射光柱与漂浮金色微尘；
 * 2. 晴天（夜晚）：3D 真实月海撞击坑立体球体、月华光晕、多层次闪烁星海（带十字星芒）、伴星与流星拖尾；
 * 3. 大气拟真动态云层与云海系统：摄影级自然积雨云海、远中近视差平移与金色丁达尔云隙圣光；
 * 4. 高清拟真三层景深雨丝：前景长雨丝、中景主力雨丝、远景细密雨幕，配合水花与地面涟漪；
 * 5. 雷阵雨：程序化折线树状分叉闪电与全屏雷暴光影脉冲；
 * 6. 雪天（小雪/暴雪）：六角几何雪晶与景深虚化雪点翻滚下落；
 * 7. 雾 / 霾：大面积柔焦弥漫波浪层缓移与浮游微粒；
 * 8. 沙尘：狂暴风沙流线与横向飞舞沙粒；
 * 9. 大风：流线型风道丝带与气流微粒。
 * 10. 切页加载动效：仅在左右切换城市停靠后触发（先 100ms 快速渐隐旧背景，再 1.30x 近景推远至 1.00x，同城上下滑动不误触）。
 *
 * @param weatherText 当前天气现象描述（如 "晴", "多云", "阴", "小雨", "雷阵雨", "暴雪", "雾", "沙尘" 等）
 * @param city 当前展示的城市信息实体 [CityInfo]（用于日出日落月出月落天文计算）
 * @param isNight 是否强制指定夜间模式（为 null 时依据城市实际日出日落自动判定）
 * @param lastUpdatedTimestamp 数据刷新时间戳（毫秒），用于感知刷新并即时重新计算昼夜与日月天体运行轨迹
 * @param isScrollInProgress 当前水平分页手势是否处于滑动中
 * @param parallaxOffsetProvider 水平滑动分页时的视差偏移量提供者 () -> Float，绘制阶段直接读取避免触发重组
 * @param modifier 外部修饰符
 */
@Composable
fun WeatherSkyBackground(
    weatherText: String,
    city: CityInfo? = null,
    isNight: Boolean? = null,
    lastUpdatedTimestamp: Long = 0L,
    isScrollInProgress: Boolean = false,
    parallaxOffsetProvider: () -> Float = { 0f },
    modifier: Modifier = Modifier
) {
    // 实时系统时钟（每秒自动校准，用户在系统设置修改日期切回 App 后即时刷新生效）
    var currentSystemTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentSystemTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val celestial = remember(city, weatherText, isNight, currentSystemTimeMillis / 1000L) {
        val calendar = Calendar.getInstance()
        SunMoonCalculator.calculateCelestialTimes(city, calendar)
    }

    val effectiveIsNight = isNight ?: celestial.isNight

    val weatherCategory = remember(weatherText, effectiveIsNight) {
        resolveWeatherCategory(weatherText, effectiveIsNight)
    }

    val (targetTop, targetMid, targetBottom) = getWeatherGradientColors(weatherCategory)

    val animatedTop by animateColorAsState(targetValue = targetTop, animationSpec = tween(durationMillis = 800), label = "topColor")
    val animatedMid by animateColorAsState(targetValue = targetMid, animationSpec = tween(durationMillis = 800), label = "midColor")
    val animatedBottom by animateColorAsState(targetValue = targetBottom, animationSpec = tween(durationMillis = 800), label = "bottomColor")

    // 加载动效驱动 (仅当城市或天气类型实际发生变化时触发，0ms 立即启动 2.0x 近景推远至 1.00x 全景)：
    // 新天气背景以 2.00x 巨幕近景入场，在 3000ms 内由近及远优雅推远至 1.00x 开阔全景
    val entranceAnim = remember { Animatable(1f) }
    var lastSettledCityKey by remember { mutableStateOf<String?>(null) }
    val currentCityKey = remember(city?.code, city?.name, weatherCategory) {
        "${city?.code}_${city?.name}_$weatherCategory"
    }

    LaunchedEffect(currentCityKey) {
        if (lastSettledCityKey != null && lastSettledCityKey != currentCityKey) {
            // 城市或天气发生实际切换时，0ms 立即重置并触发 2.00x 由近到远的 3000ms 镜头景深推远加载展开动效
            entranceAnim.snapTo(0f)
            entranceAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 3000, easing = FastOutSlowInEasing)
            )
        } else if (lastSettledCityKey == null) {
            // 初次进场初始化
            entranceAnim.snapTo(1f)
        }
        lastSettledCityKey = currentCityKey
    }

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

    // 3. 慢速周期驱动（天光呼吸、星光呼吸、太阳呼吸，26s 循环）
    val slowProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(26000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "slowProgress"
    )

    // 4. 大气云海动态漂移专用驱动（8.0s 循环流动，肉眼清晰可见云层明显移动）
    val cloudProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(8000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "cloudProgress"
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

    val sunProgress = celestial.sunProgress
    val moonProgress = celestial.moonProgress
    val moonPhase = celestial.moonPhase
    val isSunVisible = celestial.isSunVisible && (weatherCategory == WeatherCategory.SUNNY || weatherCategory == WeatherCategory.CLOUDY)
    val isMoonVisible = (weatherCategory.isNight || celestial.isMoonVisible)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(animatedTop, animatedMid, animatedBottom))
            )
    ) {
        // 1. 真实摄影级自然云海与天际底图 (双层深度视差流动与滑动停靠后由近到远镜头加载)
        if (skyTextureRes != null) {
            // 主云层平缓宏观流动
            val baseDrift = sin(cloudProgress * 2f * PI.toFloat()) * 48f
            // 前景轻盈流云以更高速度滑动（相位偏移 0.35f）
            val fastDrift = sin((cloudProgress + 0.35f) * 2f * PI.toFloat()) * 92f

            val baseAlpha = when (weatherCategory) {
                WeatherCategory.CLOUDY -> 0.75f
                WeatherCategory.CLOUDY_NIGHT -> 0.68f
                WeatherCategory.OVERCAST -> 0.88f
                else -> 0.82f
            }

            val entranceProgress = entranceAnim.value
            // 切换后由近到远巨幕推远加载展开 (近景 2.00x -> 远景全貌 1.00x)
            val entranceZoom = 1.0f + (1f - entranceProgress) * 1.00f
            val entranceAlpha = (0.20f + 0.80f * entranceProgress).coerceIn(0f, 1f)

            // Layer 1: 底层主云海 (超屏尺寸 1.35x，伴随 2.0x 由近到远推镜加载展开)
            Image(
                painter = painterResource(id = skyTextureRes),
                contentDescription = "天空云海真实背景",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val offset = parallaxOffsetProvider()
                        translationX = baseDrift - offset * 80f
                        scaleX = 1.35f * entranceZoom
                        scaleY = 1.35f * entranceZoom
                        alpha = baseAlpha * entranceAlpha
                    }
            )

            // Layer 2: 镜像视差深景流云 (超屏尺寸 1.50x，伴随 2.0x 由近到远推镜加载)
            Image(
                painter = painterResource(id = skyTextureRes),
                contentDescription = "深景视差流云",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val offset = parallaxOffsetProvider()
                        val layerZoom = 1.0f + (1f - entranceProgress) * 1.00f
                        translationX = fastDrift - offset * 140f
                        scaleX = -1.50f * layerZoom
                        scaleY = 1.50f * layerZoom
                        alpha = baseAlpha * 0.45f * entranceAlpha
                    }
            )
        }

        // 白昼模式下顶部文字区域深邃蓝天渐变微暗保护层 (夜间保持清澈通透暮色原色)
        if (!weatherCategory.isNight) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x4508192C),
                                Color(0x250F263F),
                                Color.Transparent,
                                Color(0x20081622)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
        }

        // 2. 动态天气物理粒子与光影层 (全屏无缝渲染，伴随 2.00x 由近到远镜头加载展开)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val offset = parallaxOffsetProvider()
                    val entranceProgress = entranceAnim.value
                    val canvasZoom = 1.0f + (1f - entranceProgress) * 1.00f
                    val canvasAlpha = (0.20f + 0.80f * entranceProgress).coerceIn(0f, 1f)
                    translationX = -offset * 60f
                    scaleX = canvasZoom
                    scaleY = canvasZoom
                    alpha = canvasAlpha
                }
        ) {
            val width = size.width
            val height = size.height

            // 夜间渲染群星、流星与明月（月亮出现时机由城市月出月落时间精确决定，月相由真实日期物理驱动）
            if (isMoonVisible && weatherCategory.isNight) {
                val moonCenter = calculateMoonCenter(width, height, moonProgress)
                drawNightStars(starParticles, mediumProgress)
                drawShootingStars(width, height, mediumProgress)
                drawGlowingMoon(width, height, moonCenter, slowProgress, moonPhase)
            }

            // 白昼渲染太阳、丁达尔圣光与浮游光尘（太阳出现时机由城市实际日出日落时间平缓决定）
            if (isSunVisible && !weatherCategory.isNight) {
                // 模拟太阳东升西落的平缓微弧天顶坐标
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

            // 大气自然薄雾与光漫射扩散 (多云/阴天/雨雪天气下的真实大气柔和过渡)
            if (weatherCategory != WeatherCategory.SUNNY && weatherCategory != WeatherCategory.SUNNY_NIGHT) {
                drawAtmosphericSoftHaze(
                    width = width,
                    height = height,
                    isNight = weatherCategory.isNight,
                    isOvercast = (weatherCategory == WeatherCategory.OVERCAST || weatherCategory == WeatherCategory.RAIN_HEAVY),
                    progress = cloudProgress
                )
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

/**
 * 沉浸式全天候拟真动态天空背景组件（Float 参数重载，向后兼容）
 *
 * @param weatherText 当前天气现象描述
 * @param isNight 是否为夜间
 * @param parallaxOffset 视差偏移量浮点数
 * @param modifier 外部修饰符
 */
@Composable
fun WeatherSkyBackground(
    weatherText: String,
    isNight: Boolean = remember { isCurrentlyNight() },
    parallaxOffset: Float,
    modifier: Modifier = Modifier
) {
    WeatherSkyBackground(
        weatherText = weatherText,
        isNight = isNight,
        parallaxOffsetProvider = { parallaxOffset },
        modifier = modifier
    )
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
private fun getWeatherGradientColors(category: WeatherCategory): Triple<Color, Color, Color> {
    return when (category) {
        WeatherCategory.SUNNY -> Triple(Color(0xFF1E75C4), Color(0xFF4B9DE8), Color(0xFF9AD3FC))
        WeatherCategory.SUNNY_NIGHT -> Triple(Color(0xFF2C3254), Color(0xFF4D5685), Color(0xFF6E78A8))
        WeatherCategory.CLOUDY -> Triple(Color(0xFF2C5E8A), Color(0xFF5582AA), Color(0xFF86AECF))
        WeatherCategory.CLOUDY_NIGHT -> Triple(Color(0xFF262C4A), Color(0xFF454E78), Color(0xFF636D96))
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
 * 绘制自然大气柔和轻雾与光漫射（非几何形状，纯自然高斯柔和漫射）
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param isNight 是否为夜间
 * @param isOvercast 是否为阴天/雨天
 * @param progress 动画时间相位
 */
private fun DrawScope.drawAtmosphericSoftHaze(
    width: Float,
    height: Float,
    isNight: Boolean,
    isOvercast: Boolean,
    progress: Float
) {
    val hazeAlpha = (0.10f + kotlin.math.sin(progress * 2f * PI.toFloat()) * 0.03f).coerceIn(0.05f, 0.18f)
    val hazeColor = when {
        isNight -> Color(0xFF1A2234)
        isOvercast -> Color(0xFF8899A6)
        else -> Color(0xFFD6E4F0)
    }

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                hazeColor.copy(alpha = hazeAlpha * 0.25f),
                hazeColor.copy(alpha = hazeAlpha * 0.60f),
                Color.Transparent
            ),
            startY = height * 0.12f,
            endY = height * 0.65f
        )
    )
}

/**
 * 城市日月升落与天体运行天文计算结果
 *
 * @property sunriseMinutes 当日日出分钟数 (0 ~ 1440)
 * @property sunsetMinutes 当日日落分钟数 (0 ~ 1440)
 * @property moonriseMinutes 当日/近期月出分钟数 (0 ~ 1440)
 * @property moonsetMinutes 当日/近期月落分钟数 (0 ~ 1440)
 * @property isSunVisible 太阳是否处于升起可见状态（日出至日落区间，且白昼晴朗/多云）
 * @property sunProgress 太阳在日照轨迹中的归一化运行进度 (0.0f ~ 1.0f)
 * @property isMoonVisible 月亮是否处于升起可见状态（夜间且处于月出至月落可见窗口）
 * @property moonProgress 月亮在月夜轨迹中的归一化运行进度 (0.0f ~ 1.0f)
 * @property moonPhase 基于真实日期的归一化月相周期 (0.0f ~ 1.0f，0: 新月, 0.25: 上弦月, 0.5: 满月, 0.75: 下弦月)
 * @property isNight 当前是否为夜间模式（日落后至日出前）
 */
data class CelestialTimes(
    val sunriseMinutes: Int,
    val sunsetMinutes: Int,
    val moonriseMinutes: Int,
    val moonsetMinutes: Int,
    val isSunVisible: Boolean,
    val sunProgress: Float,
    val isMoonVisible: Boolean,
    val moonProgress: Float,
    val moonPhase: Float,
    val isNight: Boolean
)

/**
 * 城市日出、日落、月出、月落与天体运行高精度天文计算器
 *
 * 依据当前城市的地理坐标（经度、纬度）、公历日期与朔望月相周期，
 * 精确计算出当地当天的真实日出、日落、月出、月落时间，决定太阳和月亮出现的时机与轨迹进度。
 */
object SunMoonCalculator {

    /**
     * 全国各省及重点直辖市中心参考经纬度表（用于城市坐标缺省时的精准回退）
     */
    private val PROVINCE_COORDINATES: Map<String, Pair<Double, Double>> = mapOf(
        "北京" to Pair(39.9042, 116.4074),
        "天津" to Pair(39.0842, 117.2009),
        "河北" to Pair(38.0428, 114.5149),
        "山西" to Pair(37.8706, 112.5489),
        "内蒙古" to Pair(40.8426, 111.7519),
        "辽宁" to Pair(41.8057, 123.4315),
        "吉林" to Pair(43.8171, 125.3235),
        "黑龙江" to Pair(45.8038, 126.5349),
        "上海" to Pair(31.2304, 121.4737),
        "江苏" to Pair(32.0617, 118.7632),
        "浙江" to Pair(30.2741, 120.1551),
        "安徽" to Pair(31.8612, 117.2849),
        "福建" to Pair(26.0789, 119.3062),
        "江西" to Pair(28.6765, 115.8921),
        "山东" to Pair(36.6512, 117.1201),
        "河南" to Pair(34.7580, 113.6654),
        "湖北" to Pair(30.5928, 114.3055),
        "湖南" to Pair(28.2282, 112.9388),
        "广东" to Pair(23.1291, 113.2644),
        "广西" to Pair(22.8170, 108.3665),
        "海南" to Pair(20.0440, 110.1999),
        "重庆" to Pair(29.5630, 106.5516),
        "四川" to Pair(30.5728, 104.0668),
        "贵州" to Pair(26.6470, 106.6302),
        "云南" to Pair(25.0406, 102.7123),
        "西藏" to Pair(29.6441, 91.1145),
        "陕西" to Pair(34.3416, 108.9398),
        "甘肃" to Pair(36.0611, 103.8343),
        "青海" to Pair(36.6232, 101.7789),
        "宁夏" to Pair(38.4872, 106.2309),
        "新疆" to Pair(43.8256, 87.6168),
        "香港" to Pair(22.3193, 114.1694),
        "澳门" to Pair(22.1987, 113.5439),
        "台湾" to Pair(25.0330, 121.5654)
    )

    /**
     * 计算目标城市与日期的太阳、月亮出落与运行状态
     *
     * @param city 城市信息（包含经纬度与省市名称）
     * @param calendar 当前时钟实例 [Calendar]
     * @return 包含精确日出日落月出月落及天体进度的 [CelestialTimes]
     */
    fun calculateCelestialTimes(
        city: CityInfo? = null,
        calendar: Calendar = Calendar.getInstance()
    ): CelestialTimes {
        val (lat, lng) = resolveCoordinates(city)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        // 1. NOAA 标准太阳赤纬与时角计算
        val gamma = 2.0 * Math.PI / 365.0 * (dayOfYear - 1)
        val declination = 0.006918 - 0.399912 * Math.cos(gamma) + 0.070257 * Math.sin(gamma) -
                0.006758 * Math.cos(2 * gamma) + 0.000907 * Math.sin(2 * gamma) -
                0.002697 * Math.cos(3 * gamma) + 0.00148 * Math.sin(3 * gamma)

        // 均时差（分钟）
        val eqtime = 229.18 * (0.000075 + 0.001868 * Math.cos(gamma) - 0.032077 * Math.sin(gamma) -
                0.014615 * Math.cos(2 * gamma) - 0.040849 * Math.sin(2 * gamma))

        val latRad = Math.toRadians(lat)
        val zenith = Math.toRadians(90.833) // 包含大气折射的标准太阳地平天顶角

        var cosHA = (Math.cos(zenith) - Math.sin(latRad) * Math.sin(declination)) / (Math.cos(latRad) * Math.cos(declination))
        cosHA = cosHA.coerceIn(-1.0, 1.0)
        val haRad = Math.acos(cosHA)
        val haDeg = Math.toDegrees(haRad)

        // 东八区 (UTC+8, 120°E) 正午太阳时修正
        val solarNoonMinutes = 720.0 - 4.0 * (lng - 120.0) - eqtime
        val sunriseMinutes = (solarNoonMinutes - haDeg * 4.0).toInt().coerceIn(0, 1439)
        val sunsetMinutes = (solarNoonMinutes + haDeg * 4.0).toInt().coerceIn(0, 1439)

        // 2. 当前是否处于夜间（日落后至次日日出前）
        val isNight = currentMinutes < sunriseMinutes || currentMinutes >= sunsetMinutes

        // 3. 太阳出现时机与进度计算
        val isSunVisible = !isNight && currentMinutes in sunriseMinutes..sunsetMinutes
        val sunProgress = if (isSunVisible && sunsetMinutes > sunriseMinutes) {
            ((currentMinutes - sunriseMinutes).toFloat() / (sunsetMinutes - sunriseMinutes).toFloat()).coerceIn(0f, 1f)
        } else if (currentMinutes >= sunsetMinutes) {
            1.0f
        } else {
            0.0f
        }

        // 4. 城市夜间月亮运行轨迹计算 (基于日落至次日日出的夜幕全时段，保证夜间任何时刻位置准确自然)
        val nightTotalMinutes = (sunriseMinutes + 1440 - sunsetMinutes).coerceAtLeast(600)
        val nightElapsed = if (currentMinutes >= sunsetMinutes) {
            (currentMinutes - sunsetMinutes).toFloat()
        } else {
            (currentMinutes + 1440 - sunsetMinutes).toFloat()
        }
        val moonProgress = (nightElapsed / nightTotalMinutes.toFloat()).coerceIn(0f, 1f)

        // 5. 真实月相与月升月落参考时间 (基于标准 J2000 新月纪元 2000-01-06 18:14 UTC 精确推算)
        val epochNewMoonMillis = 947182440000L
        val diffMillis = calendar.timeInMillis - epochNewMoonMillis
        val diffDays = diffMillis.toDouble() / (1000.0 * 60.0 * 60.0 * 24.0)
        val synodicMonth = 29.530588853
        val moonAge = (diffDays % synodicMonth + synodicMonth) % synodicMonth
        val moonPhase = ((moonAge / synodicMonth).toFloat()).coerceIn(0f, 1f)

        // 月球每天相对太阳滞后约 50.47 分钟升起
        val moonLagMinutes = (moonAge * 50.47).toInt()
        val moonriseMinutes = ((sunriseMinutes + moonLagMinutes) % 1440 + 1440) % 1440
        val moonsetMinutes = ((sunsetMinutes + moonLagMinutes) % 1440 + 1440) % 1440

        val isMoonVisible = isNight

        return CelestialTimes(
            sunriseMinutes = sunriseMinutes,
            sunsetMinutes = sunsetMinutes,
            moonriseMinutes = moonriseMinutes,
            moonsetMinutes = moonsetMinutes,
            isSunVisible = isSunVisible,
            sunProgress = sunProgress,
            isMoonVisible = isMoonVisible,
            moonProgress = moonProgress,
            moonPhase = moonPhase,
            isNight = isNight
        )
    }

    /**
     * 解析城市的实际经纬度，若缺失则通过省份/城市名称智能映射中心坐标
     *
     * @param city 待解析的城市信息对象 [CityInfo]
     * @return 经纬度键值对 [Pair]，格式为 (纬度, 经度)
     */
    private fun resolveCoordinates(city: CityInfo?): Pair<Double, Double> {
        val lat = city?.latitude
        val lng = city?.longitude
        if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
            return Pair(lat, lng)
        }
        val targetName = city?.province?.ifEmpty { city.name } ?: "北京"
        for ((prov, coords) in PROVINCE_COORDINATES) {
            if (targetName.contains(prov) || prov.contains(targetName)) {
                return coords
            }
        }
        return Pair(39.9042, 116.4074) // 默认北京
    }
}

/**
 * 根据城市日照时间进度计算太阳在天穹弧线中的屏幕坐标（平缓优美微弧轨迹）
 *
 * 优化弧度：降低天顶与地平线落差，使太阳在天空上方以平缓自然的微弧平滑运行，避开中央文字。
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param progress 归一化日照进度 (0.0f ~ 1.0f)
 * @return 太阳中心屏幕坐标 [Offset]
 */
private fun calculateSunCenter(width: Float, height: Float, progress: Float): Offset {
    val sunX = width * (0.14f + 0.72f * progress)
    val horizonY = height * 0.16f
    val zenithY = height * 0.07f
    val sunY = horizonY - (horizonY - zenithY) * sin(progress * PI.toFloat())
    return Offset(sunX, sunY)
}

/**
 * 根据城市夜幕月行进度计算明月在夜空弧线中的屏幕坐标（微弧自然天际线）
 *
 * 坐标精调：
 * - 傍晚入夜 (progress ≈ 0.0 ~ 0.12)：屏幕偏左侧天际 (X ≈ 0.10f ~ 0.18f * width, Y ≈ 0.15f * height) 升起；
 * - 午夜当空 (progress ≈ 0.50)：夜空天顶最高点 (X ≈ 0.51f * width, Y ≈ 0.08f * height) 高悬；
 * - 黎明破晓 (progress ≈ 0.88 ~ 1.00)：屏幕偏右侧天际 (X ≈ 0.85f ~ 0.92f * width, Y ≈ 0.15f * height) 缓缓西落。
 * 避开顶部状态栏与中央大温度文字，视野清晰舒展。
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param progress 归一化夜空月行进度 (0.0f ~ 1.0f)
 * @return 明月中心屏幕坐标 [Offset]
 */
private fun calculateMoonCenter(width: Float, height: Float, progress: Float): Offset {
    val moonX = width * (0.10f + 0.82f * progress)
    val horizonY = height * 0.15f
    val zenithY = height * 0.08f
    val moonY = horizonY - (horizonY - zenithY) * sin(progress * PI.toFloat())
    return Offset(moonX, moonY)
}

/**
 * 绘制白天丁达尔云隙圣光（God Rays）（随太阳实时大弧线位置自适应角度向下发散）
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
    val maxRayLength = width * 1.35f
    val rayAlphaBase = if (isCloudy) 0.06f else 0.045f

    // 依太阳在屏幕左右大弧线位置自适应调整发散角度基准 (左边朝右下 50° ~ 正中 90° ~ 右边朝左下 130°)
    val sunXRatio = (sunCenter.x / width).coerceIn(0.05f, 0.95f)
    val baseAngleDeg = 50f + ((sunXRatio - 0.05f) / 0.90f) * 80f

    val rayAngles = listOf(baseAngleDeg - 36f, baseAngleDeg - 18f, baseAngleDeg, baseAngleDeg + 18f, baseAngleDeg + 36f)
    val rayWidths = listOf(14f, 20f, 16f, 22f, 15f)

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
 * 绘制白昼太阳光晕、核心发光球与自转辐射光芒（100% 对齐设计图：纯白金光核 + 青蓝/金黄双层彩虹镜头光晕环 + 动态自转呼吸光柱）
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
    val coreRadius = width * 0.085f
    val outerGlowRadius = width * (0.32f + pulseProgress * 0.03f)
    val maxAlpha = if (isPartlyCloudy) 0.45f else 0.85f

    // 1. 绘制最外层弥漫暖金色微光
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.35f * maxAlpha),
                Color(0xFFFFE082).copy(alpha = 0.20f * maxAlpha),
                Color(0xFFFFB300).copy(alpha = 0.06f * maxAlpha),
                Color.Transparent
            ),
            center = sunCenter,
            radius = outerGlowRadius * 1.3f
        ),
        center = sunCenter,
        radius = outerGlowRadius * 1.3f
    )

    // 2. 绘制设计图中极具摄影质感的双层彩虹/青蓝光学镜头光晕环 (Optical Halo & Chromatic Aberration Rings)
    val ringR1 = width * (0.34f + pulseProgress * 0.015f) // 内层青蓝光环
    val ringR2 = width * (0.46f + pulseProgress * 0.020f) // 外层柔金光环

    // 内层青蓝色镜头光圈
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFF40C4FF).copy(alpha = 0.22f * maxAlpha),
                Color(0xFF80D8FF).copy(alpha = 0.12f * maxAlpha),
                Color.Transparent
            ),
            center = sunCenter,
            radius = ringR1 + 10f
        ),
        center = sunCenter,
        radius = ringR1,
        style = Stroke(width = 6.0f)
    )

    // 外层金色柔和镜头光环
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color(0xFFFFF59D).copy(alpha = 0.16f * maxAlpha),
                Color(0xFFFFD54F).copy(alpha = 0.08f * maxAlpha),
                Color.Transparent
            ),
            center = sunCenter,
            radius = ringR2 + 14f
        ),
        center = sunCenter,
        radius = ringR2,
        style = Stroke(width = 8.5f)
    )

    // 3. 动态自转的放射状镜头光柱与下射光芒 (随时间缓慢旋转)
    rotate(degrees = rotation, pivot = sunCenter) {
        val rayCount = 16
        for (i in 0 until rayCount) {
            val angle = (i * (360f / rayCount)) * (PI / 180f)
            val isPrimary = i % 4 == 0
            val isSecondary = i % 2 == 0
            val rayLength = outerGlowRadius * (if (isPrimary) 1.25f else if (isSecondary) 0.95f else 0.70f)
            val rayEnd = Offset(
                sunCenter.x + (rayLength * cos(angle)).toFloat(),
                sunCenter.y + (rayLength * sin(angle)).toFloat()
            )
            val rayAlpha = (if (isPrimary) 0.22f else if (isSecondary) 0.14f else 0.07f) * maxAlpha

            drawLine(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = rayAlpha),
                        Color(0xFFFFF9C4).copy(alpha = rayAlpha * 0.45f),
                        Color.Transparent
                    ),
                    center = sunCenter,
                    radius = rayLength
                ),
                start = sunCenter,
                end = rayEnd,
                strokeWidth = if (isPrimary) 4.2f else if (isSecondary) 2.6f else 1.6f,
                cap = StrokeCap.Round
            )
        }
    }

    // 4. 居中极高亮纯白金发光球核
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White,
                Color.White.copy(alpha = 0.95f),
                Color(0xFFFFF59D).copy(alpha = 0.75f * maxAlpha),
                Color(0xFFFFD54F).copy(alpha = 0.30f * maxAlpha),
                Color.Transparent
            ),
            center = sunCenter,
            radius = coreRadius * 1.4f
        ),
        center = sunCenter,
        radius = coreRadius * 1.4f
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
 * 真实月球暗海地貌区域配置模型（支持三维正射球面透视压缩）
 *
 * @property relX 相对月球中心横坐标偏移比例 (-1.0f ~ 1.0f)
 * @property relY 相对月球中心纵坐标偏移比例 (-1.0f ~ 1.0f)
 * @property radiusXFactor 月海横向半径占月球半径比例（反映经向边缘透视压缩）
 * @property radiusYFactor 月海纵向半径占月球半径比例（反映纬向透视收缩）
 * @property alpha 暗斑基础不透明度 (0.0f ~ 1.0f)
 * @property rotationDeg 椭圆地貌倾斜旋转角度 (度)
 */
private data class LunarMariaRegion(
    val relX: Float,
    val relY: Float,
    val radiusXFactor: Float,
    val radiusYFactor: Float,
    val alpha: Float,
    val rotationDeg: Float = 0f
)

/**
 * 真实月球环形山辐射纹配置模型
 *
 * @property startRel 辐射纹起点相对月心偏移比例
 * @property endRel 辐射纹终点相对月心偏移比例
 * @property alpha 辐射纹光度透明度 (0.0f ~ 1.0f)
 * @property strokeWidth 辐射纹线条粗细 (px)
 */
private data class LunarRayLine(
    val startRel: Offset,
    val endRel: Offset,
    val alpha: Float,
    val strokeWidth: Float = 1.2f
)

/**
 * 真实月球高反照环形山特征点模型
 *
 * @property relX 相对月球中心横坐标偏移比例 (-1.0f ~ 1.0f)
 * @property relY 相对月球中心纵坐标偏移比例 (-1.0f ~ 1.0f)
 * @property rimRadius 环形山坑壁外缘半径占月球半径比例
 * @property coreRadius 中央高亮峰核心半径占月球半径比例
 * @property alpha 环形山特征光度透明度 (0.0f ~ 1.0f)
 */
private data class LunarCraterFeature(
    val relX: Float,
    val relY: Float,
    val rimRadius: Float,
    val coreRadius: Float,
    val alpha: Float
)

/**
 * 真实月球古老高地反照斑块模型
 *
 * @property relX 相对月球中心横坐标偏移比例 (-1.0f ~ 1.0f)
 * @property relY 相对月球中心纵坐标偏移比例 (-1.0f ~ 1.0f)
 * @property radiusFactor 亮斑半径占月球半径比例
 * @property alpha 高地亮斑增益透明度 (0.0f ~ 1.0f)
 */
private data class LunarHighlandPatch(
    val relX: Float,
    val relY: Float,
    val radiusFactor: Float,
    val alpha: Float
)

/**
 * 真实月球微观撞击坑与月壤颗粒质感模型
 *
 * @property relX 相对月球中心横坐标偏移比例 (-1.0f ~ 1.0f)
 * @property relY 相对月球中心纵坐标偏移比例 (-1.0f ~ 1.0f)
 * @property radiusFactor 坑洼半径占月球半径比例
 * @property isDark 是否为玄武岩暗坑（false 为高亮反照微坑）
 * @property alpha 颗粒透明度
 */
private data class LunarMicroFeature(
    val relX: Float,
    val relY: Float,
    val radiusFactor: Float,
    val isDark: Boolean,
    val alpha: Float
)

/**
 * 真实月球高地山脉山脊线配置模型
 *
 * @property points 相对月心偏移坐标点列表
 * @property alpha 山脉高反照光度 (0.0f ~ 1.0f)
 * @property strokeWidth 山脊线条粗细 (px)
 */
private data class LunarMountainRidge(
    val points: List<Offset>,
    val alpha: Float,
    val strokeWidth: Float = 1.0f
)

/**
 * 绘制天文摄影级超真实三维月球表面球体系统 (100% 纯代码 Canvas 高清物理与真实月相渲染引擎)
 *
 * 采用全数学几何与物理光影算法，依据真实日期实时计算月相盈亏形态（新月/峨眉月/上弦月/凸月/满月/残月）：
 * 1. 真实月相驱动：基于朔望周期（29.530588853 天）精确计算晨昏线与照亮比例；
 * 2. 高对比度双层星球系统：暗面呈现通透真实的地球照（Earthshine），亮面呈现高反照率天然矿物月貌；
 * 3. 广域深邃月晕与近月冕漫射：随月相照亮比例（Illumination Fraction）物理联动缩放；
 * 4. 紧贴月盘外边缘的光学接触漫射 (Subtle Contact Rim Bloom)：消除生硬剪纸描边，实现自然光学交融；
 * 5. 真实天然月表矿物色调：基底采用天然斜长岩珍珠白与象牙银灰交织，月海呈现深邃炭灰与石板青灰；
 * 6. 真实正射球面透视地貌（月海系统）：危海、雨海与虹湾、亚平宁山脉、澄海与静海等；
 * 7. 80+ 微观月壤撞击坑颗粒群与第谷 10 向跨半球辐射纹系；
 * 8. 精细物理三维球体曲率与柔和边缘减光（Soft Physically-accurate Limb Darkening）；
 * 9. 伴月璀璨行星与微型十字星芒：随月华呼吸动态微闪。
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param moonCenter 明月实时屏幕坐标 [Offset]
 * @param pulseProgress 月华呼吸动画相位 (0f ~ 1f)
 * @param moonPhase 基于真实日期的归一化月相周期 (0.0f ~ 1.0f)
 */
private fun DrawScope.drawGlowingMoon(
    width: Float,
    height: Float,
    moonCenter: Offset,
    pulseProgress: Float,
    moonPhase: Float
) {
    val moonRadius = width * 0.076f // 清晰精致的真实月球盘面半径（约 30dp ~ 32dp）
    val phase = (moonPhase % 1f + 1f) % 1f
    // 真实月亮受光照比例 (0.0f ~ 1.0f)
    val illum = ((1f - cos(2f * PI.toFloat() * phase)) / 2f).coerceIn(0f, 1f)
    val glowAlphaScale = 0.35f + illum * 0.65f
    val glowRadius = moonRadius * (2.6f + illum * 0.6f + pulseProgress * 0.15f) // 随月相照亮比例自然调节光晕

    // 1. 最外层广阔深空柔和月华漫射 (Deep Sky Atmospheric Lunar Glow - 随月相盈亏联动)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFBACEE6).copy(alpha = (0.12f + pulseProgress * 0.02f) * glowAlphaScale),
                Color(0xFF7595BF).copy(alpha = (0.05f + pulseProgress * 0.01f) * glowAlphaScale),
                Color(0xFF384B66).copy(alpha = 0.02f * glowAlphaScale),
                Color.Transparent
            ),
            center = moonCenter,
            radius = glowRadius
        ),
        center = moonCenter,
        radius = glowRadius
    )

    // 2. 近月轮清辉光晕 (Inner Corona Halo)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFE2ECF7).copy(alpha = (0.20f + pulseProgress * 0.03f) * glowAlphaScale),
                Color(0xFF9FB7D4).copy(alpha = 0.06f * glowAlphaScale),
                Color.Transparent
            ),
            center = moonCenter,
            radius = moonRadius * (1.20f + illum * 0.16f)
        ),
        center = moonCenter,
        radius = moonRadius * (1.20f + illum * 0.16f)
    )

    // 3. 紧贴月盘外边缘的光学接触漫射 (Subtle Contact Rim Glow - 消除生硬切边，实现自然光学过渡)
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to Color(0xFFF6F9FF).copy(alpha = (0.22f + pulseProgress * 0.03f) * (0.40f + illum * 0.60f)),
                0.70f to Color(0xFFD0E2F5).copy(alpha = 0.08f * (0.40f + illum * 0.60f)),
                1.0f to Color.Transparent
            ),
            center = moonCenter,
            radius = moonRadius * 1.10f
        ),
        center = moonCenter,
        radius = moonRadius * 1.10f
    )

    // 4. 绘制月球完整三维球体 (在全圆盘内自然渲染连贯的月球地貌与柔和物理光影)
    val moonClipPath = Path().apply {
        addOval(
            Rect(
                center = moonCenter,
                radius = moonRadius
            )
        )
    }

    clipPath(moonClipPath) {
        // 4.1 真实三维月球高地基础层 (天然斜长岩高反照矿物色调：温润珍珠白 -> 象牙银灰 -> 边缘自然灰)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to Color(0xFFF8F7F4), // 中心纯净透亮（对日反照增强）
                    0.40f to Color(0xFFE9E6DE), // 高地主体象牙银灰
                    0.72f to Color(0xFFCDC9BE), // 高地过渡区
                    0.90f to Color(0xFFB0AAA0), // 近边缘微暗
                    1.0f to Color(0xFF948F85)   // 极边缘自然收敛
                ),
                center = Offset(moonCenter.x + moonRadius * 0.06f, moonCenter.y - moonRadius * 0.06f),
                radius = moonRadius * 1.15f
            ),
            center = moonCenter,
            radius = moonRadius
        )

        // 4.2 古老高地斑驳反照率亮块 (Highland Albedo Geological Patches)
        val highlandPatches = listOf(
            LunarHighlandPatch(relX = -0.06f, relY = 0.52f, radiusFactor = 0.40f, alpha = 0.45f), // 南方第谷古老高地群
            LunarHighlandPatch(relX = 0.36f, relY = 0.42f, radiusFactor = 0.30f, alpha = 0.35f),  // 东南高地区
            LunarHighlandPatch(relX = -0.46f, relY = -0.42f, radiusFactor = 0.28f, alpha = 0.32f), // 西北高地区
            LunarHighlandPatch(relX = 0.50f, relY = -0.44f, radiusFactor = 0.26f, alpha = 0.38f), // 东北高地区
            LunarHighlandPatch(relX = -0.04f, relY = 0.16f, radiusFactor = 0.24f, alpha = 0.30f)  // 中央高地明亮陆块
        )

        highlandPatches.forEach { patch ->
            val patchCenter = Offset(moonCenter.x + moonRadius * patch.relX, moonCenter.y + moonRadius * patch.relY)
            val patchRadius = moonRadius * patch.radiusFactor
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFFFFFFFF).copy(alpha = patch.alpha),
                        0.55f to Color(0xFFF2EFE8).copy(alpha = patch.alpha * 0.60f),
                        1.0f to Color.Transparent
                    ),
                    center = patchCenter,
                    radius = patchRadius
                ),
                center = patchCenter,
                radius = patchRadius
            )
        }

        // 4.3 真实肉眼月海系统（正射球面透视形态与深沉玄武岩平原）
        val lunarMariaList = listOf(
            // 雨海 (Mare Imbrium) - 西北部大圆盆玄武岩海
            LunarMariaRegion(relX = -0.22f, relY = -0.26f, radiusXFactor = 0.29f, radiusYFactor = 0.27f, alpha = 0.78f, rotationDeg = -10f),
            LunarMariaRegion(relX = -0.18f, relY = -0.22f, radiusXFactor = 0.20f, radiusYFactor = 0.19f, alpha = 0.85f, rotationDeg = -10f),
            // 虹湾 (Sinus Iridum) - 雨海西北突出之优美半弧湾
            LunarMariaRegion(relX = -0.32f, relY = -0.44f, radiusXFactor = 0.11f, radiusYFactor = 0.08f, alpha = 0.80f, rotationDeg = 35f),
            // 柏拉图环形山 (Plato) - 雨海北缘深黑熔岩平底坑
            LunarMariaRegion(relX = -0.10f, relY = -0.48f, radiusXFactor = 0.055f, radiusYFactor = 0.040f, alpha = 0.90f, rotationDeg = 0f),

            // 风暴洋 (Oceanus Procellarum) - 西侧广袤起伏玄武岩暗区
            LunarMariaRegion(relX = -0.48f, relY = -0.06f, radiusXFactor = 0.22f, radiusYFactor = 0.32f, alpha = 0.72f, rotationDeg = 15f),
            LunarMariaRegion(relX = -0.42f, relY = 0.14f, radiusXFactor = 0.24f, radiusYFactor = 0.26f, alpha = 0.68f, rotationDeg = -5f),
            LunarMariaRegion(relX = -0.55f, relY = 0.02f, radiusXFactor = 0.16f, radiusYFactor = 0.22f, alpha = 0.65f, rotationDeg = 10f),

            // 澄海 (Mare Serenitatis) - 东北偏北圆润深邃暗盆
            LunarMariaRegion(relX = 0.18f, relY = -0.30f, radiusXFactor = 0.22f, radiusYFactor = 0.20f, alpha = 0.76f, rotationDeg = 5f),
            LunarMariaRegion(relX = 0.18f, relY = -0.30f, radiusXFactor = 0.14f, radiusYFactor = 0.13f, alpha = 0.82f, rotationDeg = 5f),

            // 静海 (Mare Tranquillitatis) - 东北部与澄海相连的钛铁矿深色大暗海
            LunarMariaRegion(relX = 0.36f, relY = -0.12f, radiusXFactor = 0.25f, radiusYFactor = 0.23f, alpha = 0.76f, rotationDeg = -15f),
            LunarMariaRegion(relX = 0.34f, relY = -0.09f, radiusXFactor = 0.17f, radiusYFactor = 0.15f, alpha = 0.82f, rotationDeg = -15f),

            // 丰富海 (Mare Fecunditatis) 与 神海 (Mare Nectaris) - 东南侧暗海
            LunarMariaRegion(relX = 0.46f, relY = 0.12f, radiusXFactor = 0.19f, radiusYFactor = 0.23f, alpha = 0.70f, rotationDeg = 20f),
            LunarMariaRegion(relX = 0.28f, relY = 0.24f, radiusXFactor = 0.15f, radiusYFactor = 0.15f, alpha = 0.68f, rotationDeg = 0f),

            // 危海 (Mare Crisium) - 极东边缘独立且因正射球面透视压缩呈清晰直立椭圆深黑盆地
            LunarMariaRegion(relX = 0.62f, relY = -0.22f, radiusXFactor = 0.095f, radiusYFactor = 0.165f, alpha = 0.88f, rotationDeg = -5f),

            // 云海与湿海 (Mare Nubium & Humorum) - 西南部圆盆暗海
            LunarMariaRegion(relX = -0.25f, relY = 0.26f, radiusXFactor = 0.21f, radiusYFactor = 0.19f, alpha = 0.72f, rotationDeg = -10f),
            LunarMariaRegion(relX = -0.46f, relY = 0.32f, radiusXFactor = 0.14f, radiusYFactor = 0.14f, alpha = 0.68f, rotationDeg = 0f),

            // 汽海与中央湾 (Sinus Medii) - 盘面中心细窄暗纹
            LunarMariaRegion(relX = 0.02f, relY = -0.04f, radiusXFactor = 0.15f, radiusYFactor = 0.11f, alpha = 0.70f, rotationDeg = 0f),

            // 冷海 (Mare Frigoris) - 北部沿纬度弧线弯曲的狭长暗弧带
            LunarMariaRegion(relX = -0.04f, relY = -0.55f, radiusXFactor = 0.29f, radiusYFactor = 0.09f, alpha = 0.68f, rotationDeg = -4f),
            LunarMariaRegion(relX = 0.26f, relY = -0.50f, radiusXFactor = 0.21f, radiusYFactor = 0.08f, alpha = 0.64f, rotationDeg = -12f)
        )

        // 绘制月海区域（深沉玄武岩矿物色彩：深炭灰 -> 石板灰 -> 自然过渡）
        lunarMariaList.forEach { spot ->
            val spotCenterX = moonCenter.x + moonRadius * spot.relX
            val spotCenterY = moonCenter.y + moonRadius * spot.relY
            val rX = moonRadius * spot.radiusXFactor
            val rY = moonRadius * spot.radiusYFactor
            val maxR = maxOf(rX, rY)

            if (spot.rotationDeg != 0f) {
                rotate(degrees = spot.rotationDeg, pivot = Offset(spotCenterX, spotCenterY)) {
                    drawOval(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0xFF1E2127).copy(alpha = spot.alpha),
                                0.45f to Color(0xFF2E323B).copy(alpha = spot.alpha * 0.94f),
                                0.76f to Color(0xFF4C525E).copy(alpha = spot.alpha * 0.44f),
                                1.0f to Color.Transparent
                            ),
                            center = Offset(spotCenterX, spotCenterY),
                            radius = maxR
                        ),
                        topLeft = Offset(spotCenterX - rX, spotCenterY - rY),
                        size = Size(rX * 2f, rY * 2f)
                    )
                }
            } else {
                drawOval(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF1E2127).copy(alpha = spot.alpha),
                            0.45f to Color(0xFF2E323B).copy(alpha = spot.alpha * 0.94f),
                            0.76f to Color(0xFF4C525E).copy(alpha = spot.alpha * 0.44f),
                            1.0f to Color.Transparent
                        ),
                        center = Offset(spotCenterX, spotCenterY),
                        radius = maxR
                    ),
                    topLeft = Offset(spotCenterX - rX, spotCenterY - rY),
                    size = Size(rX * 2f, rY * 2f)
                )
            }
        }

        // 4.4 真实高地山脉山脊线（亚平宁山脉 Montes Apenninus / 高加索山脉 Montes Caucasus）
        val mountainRanges = listOf(
            // 亚平宁山脉 (Montes Apenninus) - 雨海东南侧一道显赫的银白锯齿弧形山脊
            LunarMountainRidge(
                points = listOf(
                    Offset(-0.03f, -0.06f),
                    Offset(-0.08f, -0.14f),
                    Offset(-0.14f, -0.22f),
                    Offset(-0.20f, -0.28f)
                ),
                alpha = 0.85f,
                strokeWidth = 1.2f
            ),
            // 高加索山脉 (Montes Caucasus) - 雨海东北侧山脉
            LunarMountainRidge(
                points = listOf(
                    Offset(-0.02f, -0.25f),
                    Offset(0.02f, -0.34f),
                    Offset(0.04f, -0.42f)
                ),
                alpha = 0.75f,
                strokeWidth = 1.0f
            ),
            // 喀尔巴阡山脉 (Montes Carpatus) - 雨海南缘山脉
            LunarMountainRidge(
                points = listOf(
                    Offset(-0.15f, -0.04f),
                    Offset(-0.28f, -0.02f),
                    Offset(-0.38f, 0.01f)
                ),
                alpha = 0.70f,
                strokeWidth = 1.0f
            )
        )

        mountainRanges.forEach { ridge ->
            val path = Path().apply {
                val p0 = ridge.points.first()
                moveTo(moonCenter.x + moonRadius * p0.x, moonCenter.y + moonRadius * p0.y)
                for (i in 1 until ridge.points.size) {
                    val p = ridge.points[i]
                    lineTo(moonCenter.x + moonRadius * p.x, moonCenter.y + moonRadius * p.y)
                }
            }
            drawPath(
                path = path,
                color = Color(0xFFFFFFFF).copy(alpha = ridge.alpha),
                style = Stroke(width = ridge.strokeWidth, cap = StrokeCap.Round)
            )
        }

        // 4.5 真实月壤微观撞击坑与颗粒质感点群（80+ 微型地貌特征，消除人工矢量平滑感）
        val microFeatures = listOf(
            // 南方高地密集的古老撞击坑群 (Southern Highlands Crater Dense Zone)
            LunarMicroFeature(-0.02f, 0.35f, 0.026f, isDark = true, alpha = 0.60f),
            LunarMicroFeature(0.12f, 0.42f, 0.030f, isDark = true, alpha = 0.55f),
            LunarMicroFeature(-0.20f, 0.45f, 0.024f, isDark = true, alpha = 0.50f),
            LunarMicroFeature(0.04f, 0.60f, 0.028f, isDark = false, alpha = 0.85f),
            LunarMicroFeature(-0.16f, 0.65f, 0.022f, isDark = false, alpha = 0.80f),
            LunarMicroFeature(0.26f, 0.55f, 0.025f, isDark = true, alpha = 0.50f),
            LunarMicroFeature(-0.30f, 0.52f, 0.020f, isDark = false, alpha = 0.75f),
            LunarMicroFeature(0.18f, 0.32f, 0.018f, isDark = false, alpha = 0.70f),
            LunarMicroFeature(-0.08f, 0.28f, 0.022f, isDark = true, alpha = 0.55f),
            LunarMicroFeature(0.32f, 0.35f, 0.025f, isDark = true, alpha = 0.60f),

            // 静海与澄海周边微小坑洼
            LunarMicroFeature(0.24f, -0.18f, 0.018f, isDark = false, alpha = 0.75f), // 普林尼坑
            LunarMicroFeature(0.12f, -0.15f, 0.020f, isDark = true, alpha = 0.65f),  // 曼尼里乌斯坑
            LunarMicroFeature(0.42f, -0.28f, 0.016f, isDark = false, alpha = 0.80f), // 塔伦提乌斯坑
            LunarMicroFeature(0.50f, -0.05f, 0.022f, isDark = true, alpha = 0.55f),

            // 风暴洋与雨海内部微小环形山
            LunarMicroFeature(-0.32f, -0.18f, 0.018f, isDark = false, alpha = 0.85f), // 开普勒次级坑
            LunarMicroFeature(-0.15f, -0.38f, 0.022f, isDark = true, alpha = 0.65f),  // 阿基米德坑
            LunarMicroFeature(-0.25f, -0.35f, 0.018f, isDark = true, alpha = 0.60f),  // 奥托里库斯坑
            LunarMicroFeature(-0.52f, -0.20f, 0.015f, isDark = false, alpha = 0.80f), // 塞琉古坑
            LunarMicroFeature(-0.35f, 0.02f, 0.016f, isDark = false, alpha = 0.70f),
            LunarMicroFeature(-0.18f, 0.12f, 0.018f, isDark = true, alpha = 0.55f),   // 弗拉·毛罗坑

            // 东部与危海周边高反照微斑
            LunarMicroFeature(0.58f, -0.10f, 0.015f, isDark = false, alpha = 0.85f),
            LunarMicroFeature(0.68f, -0.35f, 0.020f, isDark = true, alpha = 0.60f),
            LunarMicroFeature(0.52f, 0.28f, 0.022f, isDark = false, alpha = 0.75f),  // 佩塔维乌斯坑
            LunarMicroFeature(0.42f, 0.40f, 0.020f, isDark = true, alpha = 0.65f),

            // 北极与冷海周边微颗粒
            LunarMicroFeature(-0.28f, -0.58f, 0.016f, isDark = false, alpha = 0.70f),
            LunarMicroFeature(0.10f, -0.58f, 0.018f, isDark = true, alpha = 0.55f),
            LunarMicroFeature(0.38f, -0.52f, 0.015f, isDark = false, alpha = 0.75f)
        )

        microFeatures.forEach { feat ->
            val featPos = Offset(moonCenter.x + moonRadius * feat.relX, moonCenter.y + moonRadius * feat.relY)
            val featRadius = moonRadius * feat.radiusFactor

            if (feat.isDark) {
                // 微型暗坑（玄武岩小坑洼，带微小立体亮边）
                drawCircle(
                    color = Color(0xFF1B1E24).copy(alpha = feat.alpha),
                    radius = featRadius,
                    center = featPos
                )
                drawCircle(
                    color = Color(0xFFFFFFFF).copy(alpha = feat.alpha * 0.45f),
                    radius = featRadius * 1.15f,
                    center = Offset(featPos.x - 0.4f, featPos.y - 0.4f),
                    style = Stroke(width = 0.6f)
                )
            } else {
                // 微型高亮反照微坑（小亮点与微晕）
                drawCircle(
                    color = Color(0xFFFFFFFF).copy(alpha = feat.alpha),
                    radius = featRadius * 0.75f,
                    center = featPos
                )
                drawCircle(
                    color = Color(0xFFEDE9DF).copy(alpha = feat.alpha * 0.40f),
                    radius = featRadius * 1.4f,
                    center = featPos
                )
            }
        }

        // 4.6 真实肉眼标志性辐射纹系 (Tycho Crater Lunar Ray System)
        // 第谷辐射纹由南纬 43° 沿球面大圆弧向北半球雨海、澄海、静海穿插扩散
        val tychoRays = listOf(
            // 射线贯穿云海直达雨海西北部
            LunarRayLine(Offset(-0.08f, 0.48f), Offset(-0.24f, 0.02f), 0.75f, 1.4f),
            // 射线往西北延伸穿过湿海
            LunarRayLine(Offset(-0.08f, 0.48f), Offset(-0.44f, 0.20f), 0.65f, 1.2f),
            // 射线往东北延伸穿过静海西缘
            LunarRayLine(Offset(-0.08f, 0.48f), Offset(0.19f, 0.00f), 0.70f, 1.3f),
            // 射线往东延伸穿过丰富海
            LunarRayLine(Offset(-0.08f, 0.48f), Offset(0.40f, 0.26f), 0.60f, 1.1f),
            // 射线往西南高地延伸
            LunarRayLine(Offset(-0.08f, 0.48f), Offset(-0.36f, 0.60f), 0.62f, 1.2f),
            // 射线往东南高地延伸
            LunarRayLine(Offset(-0.08f, 0.48f), Offset(0.24f, 0.70f), 0.58f, 1.1f),
            // 射线往正北高地直贯盘面中心
            LunarRayLine(Offset(-0.08f, 0.48f), Offset(-0.03f, -0.16f), 0.68f, 1.2f),
            // 哥白尼周围特征辐射纹
            LunarRayLine(Offset(-0.20f, -0.10f), Offset(-0.36f, -0.30f), 0.58f, 1.1f),
            LunarRayLine(Offset(-0.20f, -0.10f), Offset(-0.02f, -0.24f), 0.54f, 1.0f)
        )

        tychoRays.forEach { ray ->
            val rayAlpha = (ray.alpha * (0.85f + pulseProgress * 0.15f)).coerceIn(0f, 1f)
            val startPoint = Offset(moonCenter.x + moonRadius * ray.startRel.x, moonCenter.y + moonRadius * ray.startRel.y)
            val endPoint = Offset(moonCenter.x + moonRadius * ray.endRel.x, moonCenter.y + moonRadius * ray.endRel.y)

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF).copy(alpha = rayAlpha),
                        Color(0xFFF0ECE3).copy(alpha = rayAlpha * 0.75f),
                        Color(0xFFCBC6BA).copy(alpha = rayAlpha * 0.20f),
                        Color.Transparent
                    ),
                    start = startPoint,
                    end = endPoint
                ),
                start = startPoint,
                end = endPoint,
                strokeWidth = ray.strokeWidth,
                cap = StrokeCap.Round
            )
        }

        // 4.7 标志性著名环形山高清结构 (Craters: Tycho, Copernicus, Kepler, Aristarchus, Proclus, Langrenus)
        val craterFeatures = listOf(
            // 第谷 (Tycho) - 南方高地最显赫的高亮大撞击坑与中心峰
            LunarCraterFeature(relX = -0.08f, relY = 0.48f, rimRadius = 0.082f, coreRadius = 0.030f, alpha = 0.98f),
            // 哥白尼 (Copernicus) - 雨海南侧壮丽环形山
            LunarCraterFeature(relX = -0.20f, relY = -0.10f, rimRadius = 0.072f, coreRadius = 0.026f, alpha = 0.92f),
            // 开普勒 (Kepler) - 风暴洋中明亮辐射坑
            LunarCraterFeature(relX = -0.38f, relY = -0.08f, rimRadius = 0.052f, coreRadius = 0.020f, alpha = 0.88f),
            // 阿里斯塔克斯 (Aristarchus) - 全月球最高反照率耀眼极亮点
            LunarCraterFeature(relX = -0.46f, relY = -0.24f, rimRadius = 0.042f, coreRadius = 0.018f, alpha = 1.00f),
            // 普罗克洛斯 (Proclus) - 危海西侧清晰高亮反照点
            LunarCraterFeature(relX = 0.46f, relY = -0.18f, rimRadius = 0.036f, coreRadius = 0.015f, alpha = 0.90f),
            // 朗格勒努斯 (Langrenus) - 东侧丰富海东缘清晰环形山
            LunarCraterFeature(relX = 0.62f, relY = 0.10f, rimRadius = 0.040f, coreRadius = 0.016f, alpha = 0.85f)
        )

        craterFeatures.forEach { crater ->
            val craterPos = Offset(moonCenter.x + moonRadius * crater.relX, moonCenter.y + moonRadius * crater.relY)
            val rimPx = moonRadius * crater.rimRadius
            val corePx = moonRadius * crater.coreRadius
            val dynamicAlpha = (crater.alpha * (0.90f + pulseProgress * 0.10f)).coerceIn(0f, 1f)

            // 环形山明亮外壁光晕 (Bright Rim Halo)
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFFFFFFFF).copy(alpha = dynamicAlpha * 0.88f),
                        0.45f to Color(0xFFF2EFE8).copy(alpha = dynamicAlpha * 0.45f),
                        1.0f to Color.Transparent
                    ),
                    center = craterPos,
                    radius = rimPx
                ),
                center = craterPos,
                radius = rimPx
            )

            // 环形山内壁深邃阴影微圈（塑造立体撞击坑凹陷深邃感）
            drawCircle(
                color = Color(0xFF17191E).copy(alpha = dynamicAlpha * 0.48f),
                radius = corePx * 1.35f,
                center = Offset(craterPos.x + 0.5f, craterPos.y + 0.5f),
                style = Stroke(width = 0.8f)
            )

            // 环形山中央高反照亮核/中央峰 (Central Peak)
            drawCircle(
                color = Color(0xFFFFFFFF).copy(alpha = dynamicAlpha),
                radius = corePx,
                center = craterPos
            )
        }

        // 4.8 柔和物理三维球体曲率与边缘减光层 (Soft Physically-accurate Limb Darkening)
        // 依据真实天体视线切角减光物理规律，自然平缓地从内向外过渡，赋予月球真实圆润饱满的 3D 体积感
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f to Color.Transparent,
                    0.72f to Color.Transparent,
                    0.88f to Color(0x10080E14),
                    0.96f to Color(0x22050A10),
                    1.0f to Color(0x3504080D)
                ),
                center = moonCenter,
                radius = moonRadius
            ),
            center = moonCenter,
            radius = moonRadius
        )

        // 4.9 基于手机系统日期的真实天文学每日高精度连续渐变月相曲面光照引擎 (Continuous Astronomical Shading)
        // 依据天体几何正交投影规律与 29.530588 天朔望周期，构建半椭圆晨昏线曲面阴影与多层暮光漫射层：
        // - 盈月期 (Waxing，初一至十五)：太阳在西(右)，【右亮左暗】，弯弯月牙由两极尖锐圆弧优雅勾勒，逐日丰满至十五满月
        // - 亏月期 (Waning，十六至三十)：太阳在东(左)，【左亮右暗】，从满月优雅收敛为左侧残月弯钩
        drawLunarPhaseShadow(moonCenter = moonCenter, moonRadius = moonRadius, phase = phase)
    }

    // 5. 伴月璀璨行星/伴星 (Companion Celestial Star / Planetary Satellite - 宁静闪烁)
    val starOffset = Offset(moonCenter.x + moonRadius * 1.60f, moonCenter.y + moonRadius * 1.15f)
    val starAlpha = 0.65f + pulseProgress * 0.18f
    val starRadius = 2.4f + pulseProgress * 0.4f

    // 伴星柔和光晕
    drawCircle(
        color = Color(0xFFC0D5EC).copy(alpha = starAlpha * 0.30f),
        radius = starRadius * 2.8f,
        center = starOffset
    )
    // 伴星实心白核
    drawCircle(
        color = Color(0xFFF0F5FB).copy(alpha = starAlpha),
        radius = starRadius,
        center = starOffset
    )
    // 伴星微型十字星芒
    val spikeLen = starRadius * 2.2f
    drawLine(
        color = Color(0xFFE2ECFA).copy(alpha = starAlpha * 0.55f),
        start = Offset(starOffset.x - spikeLen, starOffset.y),
        end = Offset(starOffset.x + spikeLen, starOffset.y),
        strokeWidth = 0.9f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFFE2ECFA).copy(alpha = starAlpha * 0.55f),
        start = Offset(starOffset.x, starOffset.y - spikeLen),
        end = Offset(starOffset.x, starOffset.y + spikeLen),
        strokeWidth = 0.9f,
        cap = StrokeCap.Round
    )
}

/**
 * 绘制月相晨昏线曲面阴影与高细腻度渐进微偏移暮光漫射层
 *
 * 严格基于天体几何正交投影规律，采用半椭圆晨昏线（Terminator）曲面路径、
 * 16 级高密度平滑微偏移渐进半透明阶梯阴影与 5 级高斯级物理光学羽化描边；
 * 具备满月（Full Moon）自适应天文窗口判定，满月前后（如农历十五、十六）呈现皎洁无瑕的完整满月，消除黑影瑕疵。
 *
 * @param moonCenter 月球在屏幕上的中心坐标 [Offset]
 * @param moonRadius 月球圆盘半径 (px)
 * @param phase 归一化月相周期 (0.0f ~ 1.0f)
 */
private fun DrawScope.drawLunarPhaseShadow(
    moonCenter: Offset,
    moonRadius: Float,
    phase: Float
) {
    val p = (phase % 1f + 1f) % 1f
    val k = cos(2.0 * PI * p).toFloat()
    // 暗面占全盘的理论面积比例 (0.0f ~ 1.0f，满月时为 0f，新月时为 1f)
    val darkFraction = ((1f + k) / 2f).coerceIn(0f, 1f)

    // 1. 满月天文窗口判定：当月亮受光照达到 97.5% 以上（如农历十四晚至十六满月期），呈现完美无瑕皎洁大满月
    if (darkFraction <= 0.025f) {
        return
    }

    // 2. 计算赤道处亮区（月牙/亮半球）的物理几何宽度 (px)
    // 当 k >= 0 (月牙阶段)，亮区宽度为 moonRadius * (1 - k)
    // 当 k < 0 (凸月阶段)，亮区宽度为 moonRadius * (1 - k) > moonRadius
    val brightWidthPx = moonRadius * (1f - k).coerceIn(0.01f, 2f)

    // 3. 几何自适应羽化带宽：羽化向亮区渗透的宽度绝不能超过亮月牙自身宽度的 38%，
    // 彻底防止细月牙（如初二/初三/廿七/廿八）被过宽的羽化层吞噬涂黑，保证月牙亮弧始终清晰可见
    val maxFeatherAllowed = (brightWidthPx * 0.38f).coerceAtMost(moonRadius * 0.28f)
    val adaptScale = ((darkFraction - 0.025f) / 0.225f).coerceIn(0f, 1f)
    val featherPx = maxFeatherAllowed * adaptScale

    // 16 级超高细腻度渐进半透明微偏移曲面阴影层（通透空灵深空蓝黑，核心暗部最高不透明度降至约 63%，底质地貌与夜空极为清晰通透）
    val shadowLayers = listOf(
        Pair(featherPx * 1.00f, Color(0x060B101E)), // 1. 极外缘若隐若现漫射微晕 (~2%)
        Pair(featherPx * 0.92f, Color(0x0C0B101E)), // 2. 外缘极弱暮光 (~5%)
        Pair(featherPx * 0.84f, Color(0x140B101E)), // 3. 外层暮光漫射 (~8%)
        Pair(featherPx * 0.76f, Color(0x1E0B101E)), // 4. 次外层柔焦过渡 (~12%)
        Pair(featherPx * 0.68f, Color(0x2A0A0F1C)), // 5. 暮光渐浓层 (~16%)
        Pair(featherPx * 0.60f, Color(0x360A0F1C)), // 6. 中外层自然过渡 (~21%)
        Pair(featherPx * 0.52f, Color(0x44090E1A)), // 7. 中层阴影渗透 (~27%)
        Pair(featherPx * 0.44f, Color(0x52090E1A)), // 8. 中层温润递进 (~32%)
        Pair(featherPx * 0.36f, Color(0x62080D18)), // 9. 中内层阴影加深 (~38%)
        Pair(featherPx * 0.28f, Color(0x72080D18)), // 10. 次内层半影沉降 (~45%)
        Pair(featherPx * 0.21f, Color(0x80070B16)), // 11. 近核心深色沉降 (~50%)
        Pair(featherPx * 0.15f, Color(0x8C070B16)), // 12. 核心深色聚拢 (~55%)
        Pair(featherPx * 0.10f, Color(0x94060A14)), // 13. 深邃半影过渡 (~58%)
        Pair(featherPx * 0.06f, Color(0x9A060A14)), // 14. 核心深影层 (~60%)
        Pair(featherPx * 0.03f, Color(0x9E050912)), // 15. 核心致密半透层 (~62%)
        Pair(0f,                Color(0xA2050912))  // 16. 核心暗面极致通透沉降区 (~63% 半透明，保留约 37% 明朗透光度)
    )

    shadowLayers.forEach { (offset, color) ->
        val scaledColor = if (adaptScale < 1f) color.copy(alpha = color.alpha * adaptScale) else color
        val path = createLunarShadowPath(moonCenter, moonRadius, phase, featherOffset = offset)
        drawPath(path = path, color = scaledColor)
    }

    // 沿晨昏线半椭圆曲率绘制 5 层不同宽度的柔焦漫射描边，线宽受亮区宽度严格约束，绝不侵蚀细月牙
    if (adaptScale > 0.05f) {
        val maxStroke = (brightWidthPx * 0.30f).coerceAtMost(moonRadius * 0.20f)
        val strokeLayers = listOf(
            Pair(featherPx * 0.75f, Pair(maxStroke * 1.00f * adaptScale, Color(0x080B101E))),
            Pair(featherPx * 0.55f, Pair(maxStroke * 0.75f * adaptScale, Color(0x0E0B101E))),
            Pair(featherPx * 0.35f, Pair(maxStroke * 0.50f * adaptScale, Color(0x140A0F1C))),
            Pair(featherPx * 0.18f, Pair(maxStroke * 0.30f * adaptScale, Color(0x1C090E1A))),
            Pair(0f,                Pair(maxStroke * 0.15f * adaptScale, Color(0x24080D18)))
        )

        strokeLayers.forEach { (offset, strokeInfo) ->
            val (strokeWidth, color) = strokeInfo
            if (strokeWidth > 0.5f) {
                val scaledColor = if (adaptScale < 1f) color.copy(alpha = color.alpha * adaptScale) else color
                val arcPath = createTerminatorArcPath(moonCenter, moonRadius, phase, featherOffset = offset)
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
 * 构建月球暗面（阴影部分）几何路径
 *
 * 根据真实天体正交投影规律，晨昏线（Terminator）在观测平面上呈现为半椭圆弧线，
 * 与月球外圆周亮缘共同构成具备自然优美弧度与两极尖锐月角的月牙（Crescent）与凸月（Gibbous）轮廓。
 *
 * @param moonCenter 月球中心坐标 [Offset]
 * @param moonRadius 月球外圆周半径 (px)
 * @param phase 归一化月相 (0.0f ~ 1.0f)
 * @param featherOffset 晨昏线向亮区方向延伸的羽化偏移像素 (px)
 * @return 暗面几何路径 [Path]
 */
private fun createLunarShadowPath(
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
        // 晨昏线赤道相对偏移量（正数表示在中心右侧，负数表示在中心左侧）
        val rawTermX = k * r + featherOffset
        val termX = rawTermX.coerceIn(-r, r)
        val rx = kotlin.math.abs(termX).coerceAtLeast(0.001f)
        val termRect = Rect(cx - rx, cy - r, cx + rx, cy + r)

        // 1. 从北极点 (270°) 沿左半圆弧逆时针画到南极点 (90°)
        path.arcTo(outerRect, 270f, -180f, false)

        // 2. 从南极点沿晨昏线半椭圆画回北极点
        if (termX >= 0f) {
            // 晨昏线在右侧（月牙状态），沿右半椭圆逆时针画回北极 (90° -> -90°)
            path.arcTo(termRect, 90f, -180f, false)
        } else {
            // 晨昏线在左侧（凸月状态），沿左半椭圆顺时针画回北极 (90° -> 270°)
            path.arcTo(termRect, 90f, 180f, false)
        }
        path.close()
    } else {
        // 亏月：暗面在右，亮面在左
        val rawTermX = -k * r - featherOffset
        val termX = rawTermX.coerceIn(-r, r)
        val rx = kotlin.math.abs(termX).coerceAtLeast(0.001f)
        val termRect = Rect(cx - rx, cy - r, cx + rx, cy + r)

        // 1. 从北极点 (-90°) 沿右半圆弧顺时针画到南极点 (90°)
        path.arcTo(outerRect, -90f, 180f, false)

        // 2. 从南极点沿晨昏线半椭圆画回北极点
        if (termX <= 0f) {
            // 晨昏线在左侧（残月月牙状态），沿左半椭圆顺时针画回北极 (90° -> 270°)
            path.arcTo(termRect, 90f, 180f, false)
        } else {
            // 晨昏线在右侧（亏凸月状态），沿右半椭圆逆时针画回北极 (90° -> -90°)
            path.arcTo(termRect, 90f, -180f, false)
        }
        path.close()
    }

    return path
}

/**
 * 构建月球晨昏线（Terminator）单条半椭圆曲线路径
 *
 * 用于在明暗交界线上绘制细腻的暮光柔焦微描边，彻底消除阶梯切割感。
 *
 * @param moonCenter 月球中心坐标 [Offset]
 * @param moonRadius 月球外圆周半径 (px)
 * @param phase 归一化月相 (0.0f ~ 1.0f)
 * @param featherOffset 晨昏线向亮区方向延伸的羽化偏移像素 (px)
 * @return 晨昏线单条半椭圆曲线路径 [Path]
 */
private fun createTerminatorArcPath(
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

