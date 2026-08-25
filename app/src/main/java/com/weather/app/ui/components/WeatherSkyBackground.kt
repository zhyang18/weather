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
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
 * @param isScrollInProgress 当前水平分页手势是否处于滑动中
 * @param parallaxOffsetProvider 水平滑动分页时的视差偏移量提供者 () -> Float，绘制阶段直接读取避免触发重组
 * @param modifier 外部修饰符
 */
@Composable
fun WeatherSkyBackground(
    weatherText: String,
    city: CityInfo? = null,
    isNight: Boolean? = null,
    isScrollInProgress: Boolean = false,
    parallaxOffsetProvider: () -> Float = { 0f },
    modifier: Modifier = Modifier
) {
    val nowCalendar = remember { Calendar.getInstance() }
    val celestial = remember(city, weatherText) {
        SunMoonCalculator.calculateCelestialTimes(city, nowCalendar)
    }

    val effectiveIsNight = isNight ?: celestial.isNight

    val weatherCategory = remember(weatherText, effectiveIsNight) {
        resolveWeatherCategory(weatherText, effectiveIsNight)
    }

    val (targetTop, targetMid, targetBottom) = getWeatherGradientColors(weatherCategory)

    val animatedTop by animateColorAsState(targetValue = targetTop, animationSpec = tween(durationMillis = 800), label = "topColor")
    val animatedMid by animateColorAsState(targetValue = targetMid, animationSpec = tween(durationMillis = 800), label = "midColor")
    val animatedBottom by animateColorAsState(targetValue = targetBottom, animationSpec = tween(durationMillis = 800), label = "bottomColor")

    // 两阶段加载动效驱动 (仅当切实切换到新城市停靠后触发，主页同城上下滑动完全不误触)：
    // 阶段 1：快速渐隐上一个天气动态背景 (100ms 极速瞬滑淡出)
    // 阶段 2：新天气背景以 1.30x 近景入场，在 3000ms 内由近及远优雅推远至 1.00x 开阔全景
    val fadeAnim = remember { Animatable(1f) }
    val entranceAnim = remember { Animatable(1f) }
    var lastSettledCityKey by remember { mutableStateOf<String?>(null) }
    val currentCityKey = remember(city?.code, city?.name, weatherCategory) {
        "${city?.code}_${city?.name}_$weatherCategory"
    }

    LaunchedEffect(currentCityKey, isScrollInProgress) {
        if (!isScrollInProgress) {
            if (lastSettledCityKey != null && lastSettledCityKey != currentCityKey) {
                // 城市或天气发生实际切换时，触发两阶段加载动效
                // 1. 快速渐隐上一天气动态背景 (100ms)
                fadeAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 100, easing = LinearEasing)
                )
                // 2. 重置并触发 1.30x 由近到远的 3000ms 镜头景深推远加载展开动效
                entranceAnim.snapTo(0f)
                fadeAnim.snapTo(1f)
                entranceAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 3000, easing = FastOutSlowInEasing)
                )
            } else if (lastSettledCityKey == null) {
                // 初次进场初始化
                entranceAnim.snapTo(1f)
                fadeAnim.snapTo(1f)
            }
            lastSettledCityKey = currentCityKey
        }
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
            val fadeFactor = fadeAnim.value
            // 滑动停靠后由近到远优雅推远加载展开 (近景 1.30x -> 远景全貌 1.00x)
            val entranceZoom = 1.0f + (1f - entranceProgress) * 0.30f
            val entranceAlpha = (0.15f + 0.85f * entranceProgress).coerceIn(0f, 1f) * fadeFactor

            // Layer 1: 底层主云海 (超屏尺寸 1.35x，由近到远推镜加载展开)
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

            // Layer 2: 镜像视差深景流云 (超屏尺寸 1.50x，更高幅度由近到远推镜加载)
            Image(
                painter = painterResource(id = skyTextureRes),
                contentDescription = "深景视差流云",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val offset = parallaxOffsetProvider()
                        val layerZoom = 1.0f + (1f - entranceProgress) * 0.35f
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

        // 2. 动态天气物理粒子与光影层 (全屏无缝渲染，滑动停靠后伴随 1.30x 由近到远镜头加载展开)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val offset = parallaxOffsetProvider()
                    val entranceProgress = entranceAnim.value
                    val entranceZoom = 1.0f + (1f - entranceProgress) * 0.28f
                    val entranceAlpha = (0.15f + 0.85f * entranceProgress).coerceIn(0f, 1f) * fadeAnim.value
                    translationX = -offset * 60f
                    scaleX = entranceZoom
                    scaleY = entranceZoom
                    alpha = entranceAlpha
                }
        ) {
            val width = size.width
            val height = size.height

            // 夜间渲染群星、流星与明月（月亮出现时机由城市月出月落时间精确决定）
            if (isMoonVisible && weatherCategory.isNight) {
                val moonCenter = calculateMoonCenter(width, height, moonProgress)
                drawNightStars(starParticles, mediumProgress)
                drawShootingStars(width, height, mediumProgress)
                drawGlowingMoon(width, height, moonCenter, slowProgress)
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

        // 5. 月相与月升月落参考时间 (基于朔望周期 29.530588853 天)
        val refCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.JANUARY, 18, 17, 51, 0)
        }
        val diffMillis = calendar.timeInMillis - refCalendar.timeInMillis
        val diffDays = diffMillis.toDouble() / (1000.0 * 60.0 * 60.0 * 24.0)
        val synodicMonth = 29.530588853
        val moonAge = (diffDays % synodicMonth + synodicMonth) % synodicMonth

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
 * 真实月球暗海漫射光斑配置模型
 *
 * @property relX 相对月球中心横坐标偏移比例 (-1.0f ~ 1.0f)
 * @property relY 相对月球中心纵坐标偏移比例 (-1.0f ~ 1.0f)
 * @property radiusFactor 月海羽化半径占月球半径比例
 * @property alpha 暗斑透明度
 */
private data class LunarMariaSpot(
    val relX: Float,
    val relY: Float,
    val radiusFactor: Float,
    val alpha: Float
)

/**
 * 真实月球环形山辐射纹配置模型
 *
 * @property startRel 辐射纹起点相对月心偏移
 * @property endRel 辐射纹终点相对月心偏移
 * @property alpha 辐射纹光度
 */
private data class LunarRayLine(
    val startRel: Offset,
    val endRel: Offset,
    val alpha: Float
)

/**
 * 绘制真实摄影级清晰微光满月与大气月华月冕系统 (Photorealistic High-Definition Moon System)
 *
 * 遵循人类肉眼夜空真实观感物理规律与高清细节呈现：
 * 1. 广域深邃月晕与清透月华：微光柔和漫射，夜空中晶莹通透；
 * 2. 真实月球盘面高清地貌：
 *    - 核心月海（雨海、风暴洋、澄海、静海、危海、云海、冷海等）清晰起伏；
 *    - 第谷（Tycho）环形山亮核与 4 条纤细辐射光纹清晰可见；
 *    - 哥白尼（Copernicus）、开普勒与阿里斯塔克斯高反照亮斑点缀；
 * 3. 清晰立体轮廓与菲涅尔微光：月球边缘圆润清晰，深邃夜空浑然一体；
 * 4. 伴月璀璨行星：伴随夜空呼吸静谧微闪。
 *
 * @param width 画面宽度 (px)
 * @param height 画面高度 (px)
 * @param moonCenter 明月实时屏幕坐标 [Offset]
 * @param pulseProgress 月华呼吸动画相位 (0f ~ 1f)
 */
private fun DrawScope.drawGlowingMoon(
    width: Float,
    height: Float,
    moonCenter: Offset,
    pulseProgress: Float
) {
    val moonRadius = width * 0.076f // 清晰精致的真实月球盘面半径（约 30dp ~ 32dp）
    val glowRadius = moonRadius * (3.0f + pulseProgress * 0.20f) // 广域柔美月华光晕半径

    // 1. 最外层广阔深空柔和月华漫射 (Deep Sky Atmospheric Lunar Glow)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFB8CEE8).copy(alpha = 0.12f + pulseProgress * 0.02f),
                Color(0xFF7595BF).copy(alpha = 0.05f + pulseProgress * 0.01f),
                Color(0xFF384B66).copy(alpha = 0.02f),
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
                Color(0xFFDEE9F5).copy(alpha = 0.24f + pulseProgress * 0.03f),
                Color(0xFF9FB7D4).copy(alpha = 0.08f),
                Color.Transparent
            ),
            center = moonCenter,
            radius = moonRadius * 1.40f
        ),
        center = moonCenter,
        radius = moonRadius * 1.40f
    )

    // 3. 绘制真实月球盘面内部（使用 clipPath 限制在月球圆形范围内）
    val moonClipPath = Path().apply {
        addOval(
            Rect(
                center = moonCenter,
                radius = moonRadius
            )
        )
    }

    clipPath(moonClipPath) {
        // 3.1 真实月面基础微光银灰底色 (清晰温润，夜间护眼)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFE4EDF7), // 中心微亮银蓝白
                    Color(0xFFCAD8E8), // 中间高地银灰
                    Color(0xFFAABDD2), // 柔和过渡区
                    Color(0xFF869AB2)  // 边缘自然微深
                ),
                center = Offset(moonCenter.x + moonRadius * 0.12f, moonCenter.y - moonRadius * 0.12f),
                radius = moonRadius * 1.25f
            ),
            center = moonCenter,
            radius = moonRadius
        )

        // 3.2 真实月海地貌层（核心暗盆地 + 细部月湾，层次分明清晰可见）
        val lunarMariaList = listOf(
            // 雨海 (Mare Imbrium - 西北部大暗盆地)
            LunarMariaSpot(relX = -0.22f, relY = -0.26f, radiusFactor = 0.38f, alpha = 0.58f),
            // 风暴洋 (Oceanus Procellarum - 西侧广大暗区)
            LunarMariaSpot(relX = -0.45f, relY = -0.05f, radiusFactor = 0.36f, alpha = 0.52f),
            LunarMariaSpot(relX = -0.38f, relY = 0.12f, radiusFactor = 0.32f, alpha = 0.48f),
            // 澄海 (Mare Serenitatis - 东北偏北暗盆)
            LunarMariaSpot(relX = 0.20f, relY = -0.30f, radiusFactor = 0.32f, alpha = 0.55f),
            // 静海 (Mare Tranquillitatis - 东北部大暗海)
            LunarMariaSpot(relX = 0.35f, relY = -0.12f, radiusFactor = 0.34f, alpha = 0.52f),
            // 丰富海 (Mare Fecunditatis - 东部暗海)
            LunarMariaSpot(relX = 0.46f, relY = 0.08f, radiusFactor = 0.28f, alpha = 0.46f),
            // 危海 (Mare Crisium - 东边缘独立清晰暗圆形)
            LunarMariaSpot(relX = 0.60f, relY = -0.22f, radiusFactor = 0.18f, alpha = 0.56f),
            // 云海与湿海 (Mare Nubium & Humorum - 西南暗区)
            LunarMariaSpot(relX = -0.28f, relY = 0.26f, radiusFactor = 0.30f, alpha = 0.50f),
            LunarMariaSpot(relX = -0.46f, relY = 0.30f, radiusFactor = 0.22f, alpha = 0.44f),
            // 汽海与中央湾 (Sinus Medii - 盘面中心细纹地貌)
            LunarMariaSpot(relX = 0.02f, relY = -0.05f, radiusFactor = 0.22f, alpha = 0.48f),
            // 冷海 (Mare Frigoris - 北部弧状狭长暗带)
            LunarMariaSpot(relX = -0.05f, relY = -0.55f, radiusFactor = 0.28f, alpha = 0.45f),
            LunarMariaSpot(relX = 0.25f, relY = -0.52f, radiusFactor = 0.24f, alpha = 0.42f)
        )

        lunarMariaList.forEach { spot ->
            val spotCenter = Offset(moonCenter.x + moonRadius * spot.relX, moonCenter.y + moonRadius * spot.relY)
            val spotRadius = moonRadius * spot.radiusFactor
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2A384A).copy(alpha = spot.alpha),
                        Color(0xFF3F5268).copy(alpha = spot.alpha * 0.70f),
                        Color(0xFF627790).copy(alpha = spot.alpha * 0.30f),
                        Color.Transparent
                    ),
                    center = spotCenter,
                    radius = spotRadius
                ),
                center = spotCenter,
                radius = spotRadius
            )
        }

        // 3.3 第谷（Tycho）环形山清晰辐射纹系 (Tycho Crater & Ray System)
        val tychoPos = Offset(moonCenter.x - moonRadius * 0.08f, moonCenter.y + moonRadius * 0.48f)
        val tychoRays = listOf(
            LunarRayLine(Offset(-0.08f, 0.48f), Offset(-0.25f, 0.15f), 0.35f),
            LunarRayLine(Offset(-0.08f, 0.48f), Offset(0.12f, 0.10f), 0.30f),
            LunarRayLine(Offset(-0.08f, 0.48f), Offset(-0.35f, 0.52f), 0.28f),
            LunarRayLine(Offset(-0.08f, 0.48f), Offset(0.20f, 0.62f), 0.25f)
        )
        tychoRays.forEach { ray ->
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF).copy(alpha = ray.alpha),
                        Color(0xFFD8E6F5).copy(alpha = ray.alpha * 0.4f),
                        Color.Transparent
                    ),
                    start = Offset(moonCenter.x + moonRadius * ray.startRel.x, moonCenter.y + moonRadius * ray.startRel.y),
                    end = Offset(moonCenter.x + moonRadius * ray.endRel.x, moonCenter.y + moonRadius * ray.endRel.y)
                ),
                start = Offset(moonCenter.x + moonRadius * ray.startRel.x, moonCenter.y + moonRadius * ray.startRel.y),
                end = Offset(moonCenter.x + moonRadius * ray.endRel.x, moonCenter.y + moonRadius * ray.endRel.y),
                strokeWidth = 1.0f,
                cap = StrokeCap.Round
            )
        }

        // 第谷高反照亮核心
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFFFFF).copy(alpha = 0.75f),
                    Color(0xFFD6E6F8).copy(alpha = 0.35f),
                    Color.Transparent
                ),
                center = tychoPos,
                radius = moonRadius * 0.14f
            ),
            center = tychoPos,
            radius = moonRadius * 0.14f
        )
        drawCircle(
            color = Color(0xFFFFFFFF).copy(alpha = 0.85f),
            radius = moonRadius * 0.035f,
            center = tychoPos
        )

        // 哥白尼环形山（Copernicus - 中偏西北部清晰亮斑）
        val copernicusPos = Offset(moonCenter.x - moonRadius * 0.20f, moonCenter.y - moonRadius * 0.10f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFFFFF).copy(alpha = 0.65f),
                    Color(0xFFD4E4F5).copy(alpha = 0.28f),
                    Color.Transparent
                ),
                center = copernicusPos,
                radius = moonRadius * 0.12f
            ),
            center = copernicusPos,
            radius = moonRadius * 0.12f
        )
        drawCircle(
            color = Color(0xFFFFFFFF).copy(alpha = 0.75f),
            radius = moonRadius * 0.030f,
            center = copernicusPos
        )

        // 开普勒与阿里斯塔克斯亮斑 (Kepler & Aristarchus)
        val aristarchusPos = Offset(moonCenter.x - moonRadius * 0.44f, moonCenter.y - moonRadius * 0.24f)
        drawCircle(
            color = Color(0xFFFFFFFF).copy(alpha = 0.70f),
            radius = moonRadius * 0.028f,
            center = aristarchusPos
        )

        // 3.4 球体暗角与夜空深度立体融合层
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0x00000000),
                    Color(0x221E2C3D),
                    Color(0x48162230)
                ),
                center = moonCenter,
                radius = moonRadius
            ),
            center = moonCenter,
            radius = moonRadius
        )
    }

    // 4. 外缘清晰纯净微光轮廓（晶莹立体感，清晰划分夜空与月盘边缘）
    drawCircle(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.45f),
                Color(0xFFC8DCF0).copy(alpha = 0.25f),
                Color(0xFF7690AD).copy(alpha = 0.15f)
            ),
            start = Offset(moonCenter.x + moonRadius * 0.8f, moonCenter.y - moonRadius * 0.8f),
            end = Offset(moonCenter.x - moonRadius * 0.8f, moonCenter.y + moonRadius * 0.8f)
        ),
        radius = moonRadius,
        center = moonCenter,
        style = Stroke(width = 0.9f)
    )

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

